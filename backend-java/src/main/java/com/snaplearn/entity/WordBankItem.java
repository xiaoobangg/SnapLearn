package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_word_bank_items")
public class WordBankItem {
    @TableId
    private String id;
    private String bankId;
    private String wordId;
    private LocalDateTime addedAt;
}
