package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_voices")
public class Voice {
    @TableId
    private String id;
    private String name;
    private String provider;
    private String voiceCode;
    /** TTS 合成模型，默认 cosyvoice-v3-plus */
    private String ttsModel;
    /** 音频格式，mp3/pcm/wav/opus，默认 mp3 */
    private String format;
    /** 采样率，默认 22050 */
    private Integer sampleRate;
    /** 音量，0~100，默认 50 */
    private Integer volume;
    /** 语速，0.5~2.0，默认 1.0 */
    private Double speechRate;
    /** 音调，0.5~2.0，默认 1.0 */
    private Double pitch;
    /** 指令文本，如"语气亲切自然"，仅部分模型+音色支持 */
    private String instruction;
    private String description;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
