package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.snaplearn.entity.Word;
import com.snaplearn.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class WordService {

    private final WordMapper wordMapper;

    // ====== word extraction from OCR (unchanged) ======

    private static final Set<String> ENG_STOP_WORDS = Set.of(
            "the", "is", "are", "was", "were", "been", "being",
            "have", "has", "had", "does", "did", "will", "would", "could", "should",
            "may", "might", "can", "shall", "this", "that", "these", "those",
            "and", "but", "or", "not", "nor", "for", "so", "yet",
            "with", "from", "into", "onto", "upon", "etc", "via"
    );

    private static final Set<String> CHN_STOP_WORDS = Set.of(
            "等等", "例如", "比如", "所以", "但是", "而且", "虽然",
            "因此", "因为", "这个", "那个", "可以", "我们", "他们",
            "你们", "没有", "什么", "怎么", "怎么样", "这样"
    );

    private static final java.util.regex.Pattern ENG_PATTERN = java.util.regex.Pattern.compile("[a-zA-Z]{2,}");
    private static final java.util.regex.Pattern CHN_PATTERN = java.util.regex.Pattern.compile("[一-鿿]{2,4}");

    public List<String> extractWords(String text) {
        if (text == null || text.isBlank()) return List.of();

        String lower = text.toLowerCase();
        List<String> engWords = extractPattern(lower, ENG_PATTERN, ENG_STOP_WORDS);
        List<String> chnWords = extractPattern(text, CHN_PATTERN, CHN_STOP_WORDS);

        engWords.sort(String::compareTo);
        chnWords.sort(String::compareTo);

        List<String> result = new ArrayList<>(engWords);
        result.addAll(chnWords);
        return result;
    }

    private List<String> extractPattern(String text, java.util.regex.Pattern pattern, Set<String> stopWords) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            String word = matcher.group();
            if (!stopWords.contains(word) && !result.contains(word)) {
                result.add(word);
            }
        }
        return result;
    }

    // ====== word entity management (new) ======

    public Word findOrCreate(String wordText) {
        QueryWrapper<Word> qw = new QueryWrapper<>();
        qw.eq("word_text", wordText);
        Word existing = wordMapper.selectOne(qw);
        if (existing != null) return existing;

        Word w = new Word();
        w.setId(UUID.randomUUID().toString());
        w.setWordText(wordText);
        wordMapper.insert(w);
        return w;
    }

    public List<Word> batchFindOrCreate(List<String> wordTexts) {
        if (wordTexts.isEmpty()) return List.of();
        List<Word> results = new ArrayList<>();
        for (String text : wordTexts) {
            results.add(findOrCreate(text));
        }
        return results;
    }

    public Word getById(String wordId) {
        return wordMapper.selectById(wordId);
    }

    public Map<String, Word> getByIds(Collection<String> wordIds) {
        if (wordIds.isEmpty()) return Map.of();
        List<Word> words = wordMapper.selectBatchIds(wordIds);
        Map<String, Word> map = new LinkedHashMap<>();
        for (Word w : words) {
            map.put(w.getId(), w);
        }
        return map;
    }
}
