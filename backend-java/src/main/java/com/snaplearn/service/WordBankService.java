package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.entity.Card;
import com.snaplearn.entity.UserDailyPool;
import com.snaplearn.entity.Word;
import com.snaplearn.entity.WordBank;
import com.snaplearn.entity.WordBankItem;
import com.snaplearn.mapper.UserDailyPoolMapper;
import com.snaplearn.mapper.WordBankItemMapper;
import com.snaplearn.mapper.WordBankMapper;
import com.snaplearn.mapper.WordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WordBankService {

    private final WordBankMapper wordBankMapper;
    private final WordBankItemMapper wordBankItemMapper;
    private final WordMapper wordMapper;
    private final UserDailyPoolMapper userDailyPoolMapper;

    /**
     * Create a new word bank
     */
    public WordBank create(String name, String type, String createdBy, String description) {
        WordBank bank = new WordBank();
        bank.setId(UUID.randomUUID().toString());
        bank.setName(name);
        bank.setType(type);
        bank.setCreatedBy(createdBy);
        bank.setDescription(description);
        wordBankMapper.insert(bank);
        return bank;
    }

    /**
     * Import words from a card group into a word bank
     */
    public void importFromCardGroup(String bankId, List<Card> cards, String userId) {
        WordBank bank = wordBankMapper.selectById(bankId);
        if (bank == null) throw new BusinessException(404, "词库不存在");

        for (Card card : cards) {
            QueryWrapper<WordBankItem> itemQw = new QueryWrapper<>();
            itemQw.eq("bank_id", bankId).eq("word_id", card.getWordId());
            if (wordBankItemMapper.selectCount(itemQw) > 0) continue; // skip duplicates

            WordBankItem item = new WordBankItem();
            item.setId(UUID.randomUUID().toString());
            item.setBankId(bankId);
            item.setWordId(card.getWordId());
            wordBankItemMapper.insert(item);
        }
    }

    /**
     * Add single word to a bank
     */
    public void addWord(String bankId, String wordText, String userId) {
        WordBank bank = wordBankMapper.selectById(bankId);
        if (bank == null) throw new BusinessException(404, "词库不存在");

        Word word = wordMapper.selectOne(
                new QueryWrapper<Word>().eq("word_text", wordText)
        );
        if (word == null) throw new BusinessException(404, "单词不存在: " + wordText);

        QueryWrapper<WordBankItem> itemQw = new QueryWrapper<>();
        itemQw.eq("bank_id", bankId).eq("word_id", word.getId());
        if (wordBankItemMapper.selectCount(itemQw) > 0) return;

        WordBankItem item = new WordBankItem();
        item.setId(UUID.randomUUID().toString());
        item.setBankId(bankId);
        item.setWordId(word.getId());
        wordBankItemMapper.insert(item);
    }

    /**
     * Remove a word from a bank
     */
    public void removeWord(String bankId, String wordId) {
        QueryWrapper<WordBankItem> qw = new QueryWrapper<>();
        qw.eq("bank_id", bankId).eq("word_id", wordId);
        wordBankItemMapper.delete(qw);
    }

    /**
     * List user's word banks
     */
    public List<WordBank> listByUser(String userId) {
        QueryWrapper<WordBank> qw = new QueryWrapper<>();
        qw.eq("created_by", userId).or().in("type", "preset", "system");
        qw.orderByDesc("created_at");
        return wordBankMapper.selectList(qw).stream()
                .filter(b -> {
                    if ("system".equals(b.getType())) {
                        // System banks: show only if user has words in daily pool
                        QueryWrapper<UserDailyPool> pqw = new QueryWrapper<>();
                        pqw.eq("user_id", userId).eq("bank_id", b.getId());
                        return userDailyPoolMapper.selectCount(pqw) > 0;
                    }
                    // User/preset banks: show only if they have items
                    return wordBankItemMapper.selectCount(
                            new QueryWrapper<WordBankItem>().eq("bank_id", b.getId())) > 0;
                })
                .toList();
    }

    /**
     * Get words in a bank
     */
    public List<Map<String, Object>> getBankWords(String bankId) {
        QueryWrapper<WordBankItem> qw = new QueryWrapper<>();
        qw.eq("bank_id", bankId);
        List<WordBankItem> items = wordBankItemMapper.selectList(qw);
        List<Map<String, Object>> results = new ArrayList<>();
        for (WordBankItem item : items) {
            Word word = wordMapper.selectById(item.getWordId());
            if (word != null) {
                results.add(Map.of(
                        "item_id", item.getId(),
                        "word_id", word.getId(),
                        "word_text", word.getWordText(),
                        "added_at", item.getAddedAt() != null ? item.getAddedAt().toString() : ""
                ));
            }
        }
        return results;
    }

    public WordBank getById(String bankId) {
        return wordBankMapper.selectById(bankId);
    }

    public void delete(String bankId, String userId) {
        QueryWrapper<WordBank> qw = new QueryWrapper<>();
        qw.eq("id", bankId).eq("created_by", userId);
        wordBankMapper.delete(qw);
    }
}
