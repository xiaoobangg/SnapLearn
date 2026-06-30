package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.entity.WordContent;
import com.snaplearn.mapper.WordContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class WordContentService {

    private final WordContentMapper wordContentMapper;
    private final LLMService llmService;

    public WordContent ensureContent(String wordId, String wordText, String context) {
        QueryWrapper<WordContent> qw = new QueryWrapper<>();
        qw.eq("word_id", wordId);
        WordContent existing = wordContentMapper.selectOne(qw);
        if (existing != null) {
            return existing;
        }

        List<LLMService.CardContent> contents = llmService.generateBatchCardContent(
                List.of(wordText), context
        );

        if (contents.isEmpty()) {
            throw new BusinessException(502, "LLM 生成内容失败");
        }

        LLMService.CardContent c = contents.get(0);
        WordContent wc = new WordContent();
        wc.setId(UUID.randomUUID().toString());
        wc.setWordId(wordId);
        wc.setPronunciation(c.pronunciation());
        wc.setPos(c.pos());
        wc.setGeneralMeaning(c.generalMeaning());
        wc.setExtendedMeaning(c.extendedMeaning());
        wc.setExampleSentence(c.exampleSentence());
        wc.setMemoryTip(c.memoryTip());
        wc.setLlmVersion("1.0");
        wordContentMapper.insert(wc);
        return wc;
    }

    public WordContent refreshContent(String wordId, String wordText) {
        QueryWrapper<WordContent> qw = new QueryWrapper<>();
        qw.eq("word_id", wordId);
        WordContent existing = wordContentMapper.selectOne(qw);

        List<LLMService.CardContent> contents = llmService.generateBatchCardContent(
                List.of(wordText), ""
        );

        if (contents.isEmpty()) {
            throw new BusinessException(502, "LLM 生成内容失败");
        }

        LLMService.CardContent c = contents.get(0);
        if (existing != null) {
            existing.setPronunciation(c.pronunciation());
            existing.setPos(c.pos());
            existing.setGeneralMeaning(c.generalMeaning());
            existing.setExtendedMeaning(c.extendedMeaning());
            existing.setExampleSentence(c.exampleSentence());
            existing.setMemoryTip(c.memoryTip());
            existing.setLlmVersion(existing.getLlmVersion() != null
                    ? String.valueOf(Integer.parseInt(existing.getLlmVersion()) + 1) : "2");
            wordContentMapper.updateById(existing);
            return existing;
        } else {
            WordContent wc = new WordContent();
            wc.setId(UUID.randomUUID().toString());
            wc.setWordId(wordId);
            wc.setPronunciation(c.pronunciation());
            wc.setPos(c.pos());
            wc.setGeneralMeaning(c.generalMeaning());
            wc.setExtendedMeaning(c.extendedMeaning());
            wc.setExampleSentence(c.exampleSentence());
            wc.setMemoryTip(c.memoryTip());
            wc.setLlmVersion("1.0");
            wordContentMapper.insert(wc);
            return wc;
        }
    }

    public WordContent getByWordId(String wordId) {
        QueryWrapper<WordContent> qw = new QueryWrapper<>();
        qw.eq("word_id", wordId);
        return wordContentMapper.selectOne(qw);
    }

    public Map<String, WordContent> getByWordIds(Collection<String> wordIds) {
        if (wordIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<WordContent> qw = new QueryWrapper<>();
        qw.in("word_id", wordIds);
        List<WordContent> list = wordContentMapper.selectList(qw);
        Map<String, WordContent> map = new LinkedHashMap<>();
        for (WordContent wc : list) {
            map.put(wc.getWordId(), wc);
        }
        return map;
    }
}
