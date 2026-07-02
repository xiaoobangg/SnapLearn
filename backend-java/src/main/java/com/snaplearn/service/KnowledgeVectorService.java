package com.snaplearn.service;

import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

/**
 * Extracted from AdminKnowledgeController — reusable vectorization logic
 * used by both the existing knowledge-base upload flow and the new document management module.
 */
@Slf4j
@Service
public class KnowledgeVectorService {

    private final VectorStore vectorStore;
    private final TextSplitter splitter;

    public KnowledgeVectorService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(1000)
                .withMinChunkSizeChars(400)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(5000)
                .withPunctuationMarks(List.of('。', '？', '！', '；', '.', '?', '!', '\n', ';', ':', '。'))
                .withKeepSeparator(true)
                .build();
    }

    /**
     * Parse a file and vectorize its content into the vector store.
     *
     * @return number of chunks created
     */
    public int vectorize(String filePath, String fileName, String fileId, String userId) throws Exception {
        List<Document> rawDocs = parseFile(filePath, fileName);
        rawDocs.forEach(doc -> {
            doc.getMetadata().put("file_name", fileName);
            doc.getMetadata().put("file_id", fileId);
            doc.getMetadata().put("user_id", userId);
            doc.getMetadata().put("upload_time", Instant.now().toString());
        });
        List<Document> chunks = splitter.transform(rawDocs);
        chunks.addAll(rawDocs);
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put("chunk_index", i);
            chunks.get(i).getMetadata().put("total_chunks", chunks.size());
        }
        for (int i = 0; i < chunks.size(); i += 10) {
            int end = Math.min(i + 10, chunks.size());
            vectorStore.add(chunks.subList(i, end));
        }
        return chunks.size();
    }

    /**
     * Delete all vector chunks belonging to a file.
     */
    public int deleteByFileId(String fileId) {
        List<Document> docs = queryChunksByFileId(fileId);
        if (!docs.isEmpty()) {
            vectorStore.delete(docs.stream().map(Document::getId).toList());
        }
        return docs.size();
    }

    /**
     * Parse a file into Spring AI Document list.
     */
    public List<Document> parseFile(String filePath, String fileName) throws Exception {
        String lower = fileName.toLowerCase();
        InputStream stream = Files.newInputStream(Path.of(filePath));
        List<Document> docs;
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withAdditionalMetadata("source", "snaplearn-upload")
                    .build();
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
     * Query all vector chunks for a given file_id.
     */
    public List<Document> queryChunksByFileId(String fileId) {
        SearchRequest request = SearchRequest.builder()
                .query("")
                .topK(100)
                .filterExpression(String.format("%s == '%s'", "file_id", fileId.replace("'", "\\'")))
                .build();
        return vectorStore.similaritySearch(request);
    }
}
