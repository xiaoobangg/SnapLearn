package com.snaplearn.service.tts;

/**
 * TTS 引擎抽象。
 */
public interface TtsProvider {

    String code();

    /**
     * 合成语音。
     * @param text 待合成文本
     * @param voiceCode 音色标识
     * @param model 合成模型
     * @param format 音频格式 mp3/pcm/wav/opus
     * @param sampleRate 采样率
     * @param volume 音量 0~100
     * @param speechRate 语速 0.5~2.0
     * @param pitch 音调 0.5~2.0
     * @param instruction 指令文本，可为 null
     */
    byte[] synthesize(String text, String voiceCode, String model, String format, int sampleRate, int volume, double speechRate, double pitch, String instruction) throws Exception;
}
