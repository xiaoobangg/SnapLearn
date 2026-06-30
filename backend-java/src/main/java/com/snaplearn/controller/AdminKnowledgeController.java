package com.snaplearn.controller;

import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.entity.KnowledgeFile;
import com.snaplearn.mapper.KnowledgeFileMapper;
import com.snaplearn.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/knowledge")
public class AdminKnowledgeController {

    private final VectorStore vectorStore;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final TextSplitter splitter;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    public AdminKnowledgeController(VectorStore vectorStore, KnowledgeFileMapper knowledgeFileMapper) {
        this.vectorStore = vectorStore;
        this.knowledgeFileMapper = knowledgeFileMapper;
        this.splitter = TokenTextSplitter.builder().withChunkSize(1000).withMinChunkSizeChars(400).withMinChunkLengthToEmbed(10).withMaxNumChunks(5000).withPunctuationMarks(List.of('。', '？', '！', '；', '.', '?', '!', '\n', ';', ':', '。')).withKeepSeparator(true).build();
    }

    // ==================== 上传 ====================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file, @CurrentUser String userId) {
        String fileName = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            // 1. 保存文件到服务器
            Path knowledgeDir = Paths.get(uploadDir, "knowledge");
            Files.createDirectories(knowledgeDir);
            String fileId = UUID.randomUUID().toString();
            Path savedPath = knowledgeDir.resolve(fileId + "_" + fileName);
            Files.copy(inputStream, savedPath, StandardCopyOption.REPLACE_EXISTING);

            // 2. 向量化
            int chunkCount = doVectorize(savedPath.toString(), fileName, fileId, userId);

            // 3. 保存文件元数据
            KnowledgeFile kf = new KnowledgeFile();
            kf.setId(fileId);
            kf.setUserId(userId);
            kf.setFileName(fileName);
            kf.setFilePath(savedPath.toString());
            kf.setFileSize(file.getSize());
            kf.setChunkCount(chunkCount);
            knowledgeFileMapper.insert(kf);

            return Map.of("ok", true, "file_name", fileName, "chunks", chunkCount);
        } catch (Exception e) {
            log.error("Upload failed", e);
            throw new BusinessException(e);
        }
    }

    // ==================== 列表 ====================

    @GetMapping
    public List<Map<String, Object>> list(@CurrentUser String userId) {
        var qw = new QueryWrapper<KnowledgeFile>();
        qw.eq("user_id", userId).orderByDesc("upload_time");
        return knowledgeFileMapper.selectList(qw).stream().map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", f.getId());
            m.put("file_name", f.getFileName());
            m.put("file_size", f.getFileSize() != null ? f.getFileSize() : 0);
            m.put("chunks", f.getChunkCount() != null ? f.getChunkCount() : 0);
            m.put("upload_time", f.getUploadTime() != null ? f.getUploadTime().toString() : "");
            return m;
        }).toList();
    }

    // ==================== 预览 ====================

    @GetMapping("/{fileId}/preview")
    public Map<String, Object> preview(@PathVariable String fileId, @CurrentUser String userId) {
        KnowledgeFile kf = getFileOrNull(fileId, userId);
        if (kf == null) {
            return Map.of("ok", false, "detail", "文件不存在");
        }
        try {
            String content = Files.readString(Path.of(kf.getFilePath()));
            return Map.of("ok", true, "file_name", kf.getFileName(), "content", content);
        } catch (Exception e) {
            return Map.of("ok", false, "detail", e.getMessage());
        }
    }

    // ==================== 块详情 ====================

    @GetMapping("/chunks/{fileId}")
    public List<Map<String, Object>> getChunks(@PathVariable String fileId) {
        return queryChunksByFileId(fileId).stream().map(d -> (Map<String, Object>) new HashMap<>(Map.of("chunk_index", d.getMetadata().getOrDefault("chunk_index", -1), "content", d.getText().substring(0, Math.min(d.getText().length(), 200)), "full_content", d.getText()))).toList();
    }

    // ==================== 删除 ====================

    @DeleteMapping("/{fileId}")
    public Map<String, Object> delete(@PathVariable String fileId, @CurrentUser String userId) {
        KnowledgeFile kf = getFileOrNull(fileId, userId);
        String fileName = kf != null ? kf.getFileName() : fileId;
        List<Document> docs = queryChunksByFileId(fileId);
        if (!docs.isEmpty()) {
            vectorStore.delete(docs.stream().map(Document::getId).toList());
        }
        var qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KnowledgeFile>();
        qw.eq("id", fileId).eq("user_id", userId);
        knowledgeFileMapper.delete(qw);
        log.info("Deleted file: {}, chunks: {}", fileName, docs.size());
        return Map.of("ok", true, "deleted_chunks", docs.size());
    }

    // ==================== 重新向量化 ====================

    @PostMapping("/{fileId}/revectorize")
    public Map<String, Object> revectorize(@PathVariable String fileId, @CurrentUser String userId) {
        KnowledgeFile kf = getFileOrNull(fileId, userId);
        if (kf == null) {
            return Map.of("ok", false, "detail", "文件不存在");
        }
        try {
            // 删旧 chunk
            List<Document> oldDocs = queryChunksByFileId(fileId);
            if (!oldDocs.isEmpty()) {
                vectorStore.delete(oldDocs.stream().map(Document::getId).toList());
            }
            // 重新向量化
            int chunkCount = doVectorize(kf.getFilePath(), kf.getFileName(), fileId, userId);
            kf.setChunkCount(chunkCount);
            knowledgeFileMapper.updateById(kf);
            return Map.of("ok", true, "chunks", chunkCount);
        } catch (Exception e) {
            log.error("Revectorize failed", e);
            return Map.of("ok", false, "detail", e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 解析 + 分块 + 向量化，返回 chunk 数量
     */
    private int doVectorize(String filePath, String fileName, String fileId, String userId) throws Exception {
        List<Document> rawDocs = parseFile(filePath, fileName);
        rawDocs.forEach(doc -> {
            doc.getMetadata().put("file_name", fileName);
            doc.getMetadata().put("file_id", fileId);
            doc.getMetadata().put("user_id", userId);
            doc.getMetadata().put("upload_time", Instant.now().toString());
        });
        List<Document> chunks = splitter.transform(rawDocs);
        // 也保留原始文档
        chunks.addAll(rawDocs);
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put("chunk_index", i);
            chunks.get(i).getMetadata().put("total_chunks", chunks.size());
        }
        // DashScope embedding API 限制每批最多 10 条
        for (int i = 0; i < chunks.size(); i += 10) {
            int end = Math.min(i + 10, chunks.size());
            vectorStore.add(chunks.subList(i, end));
        }
        return chunks.size();
    }

    /**
     * 解析文件，返回原始文档列表
     */
    private List<Document> parseFile(String filePath, String fileName) throws Exception {
        String lower = fileName.toLowerCase();
        InputStream stream = Files.newInputStream(Path.of(filePath));
        List<Document> docs;
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder().withAdditionalMetadata("source", "snaplearn-upload").build();
            docs = new MarkdownDocumentReader(new InputStreamResource(stream), config).get();
        } else {
            Supplier<Parser> ps = AutoDetectParser::new;
            Supplier<ContentHandler> hs = () -> new BodyContentHandler(10 * 1024 * 1024);
            Supplier<Metadata> ms = () -> {
                Metadata m = new Metadata();
                m.set("source", "snaplearn-upload");
                return m;
            };
            Supplier<ParseContext> pc = ParseContext::new;
            docs = new TikaDocumentParser(ps, hs, ms, pc).parse(stream);
        }
        stream.close();
        return docs;
    }

    /**
     * 按 file_id 查询向量
     */
    private List<Document> queryChunksByFileId(String fileId) {
        SearchRequest request = SearchRequest.builder()
                .query("")
                .topK(100)
                .filterExpression(String.format("%s == '%s'", "file_id", fileId.replace("'", "\\'")))
                .build();
        return vectorStore.similaritySearch(request);
    }

    /**
     * 按 fileId + userId 查文件记录
     */
    private KnowledgeFile getFileOrNull(String fileId, String userId) {
        var qw = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KnowledgeFile>();
        qw.eq("id", fileId).eq("user_id", userId);
        return knowledgeFileMapper.selectOne(qw);
    }
}
