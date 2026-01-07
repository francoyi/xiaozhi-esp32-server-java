package com.xiaozhi.dialogue.tts.providers;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xiaozhi.dialogue.tts.TtsService;
import com.xiaozhi.utils.AudioUtils;

public class EdgeTtsService implements TtsService {
    private static final Logger logger = LoggerFactory.getLogger(EdgeTtsService.class);

    private static final String PROVIDER_NAME = "edge";

    // 音频名称
    private String voiceName;

    // 音频输出路径
    private String outputPath;
    
    // 语音音调 (0.5-2.0)
    private Float pitch;
    
    // 语音语速 (0.5-2.0)
    private Float speed;

    public EdgeTtsService(String voiceName, Float pitch, Float speed, String outputPath) {
        this.voiceName = voiceName;
        this.pitch = pitch;
        this.speed = speed;
        this.outputPath = outputPath;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getVoiceName() {
        return voiceName;
    }

    @Override
    public Float getSpeed() {
        return speed;
    }

    @Override
    public Float getPitch() {
        return pitch;
    }

    @Override
    public String audioFormat() {
        return "mp3";
    }

    @Override
    public String textToSpeech(String text) throws Exception {
        // 获取中文语音
        Voice voiceObj = TTSVoice.provides().stream()
                .filter(v -> v.getShortName().equals(voiceName))
                .collect(Collectors.toList()).get(0);

        TTS ttsEngine = new TTS(voiceObj, text);
        
        // 计算Edge TTS的rate参数 (将0.5-2.0映射到-50%到+100%)
        // speed=0.5 -> rate=-50%, speed=1.0 -> rate=+0%, speed=2.0 -> rate=+100%
        int ratePercent = (int)((speed - 1.0f) * 100);
        
        // 计算Edge TTS的pitch参数 (将0.5-2.0映射到-50Hz到+50Hz)
        // pitch=0.5 -> -50Hz, pitch=1.0 -> 0Hz, pitch=2.0 -> +50Hz
        int pitchHz = (int)((pitch - 1.0f) * 50);
        
        // 执行TTS转换获取音频文件
        String audioFilePath = ttsEngine.findHeadHook()
                .storage(outputPath)
                .fileName(getAudioFileName().split("\\.")[0])
                .isRateLimited(true)
                .overwrite(false)
                .voicePitch(pitchHz + "Hz")
                .voiceRate(ratePercent + "%")
                .formatMp3()
                .trans();





        // audioFilePath 可能是相对/绝对/包含 outputPath，修改后代码
        java.nio.file.Path mp3Path = java.nio.file.Paths.get(audioFilePath);
        if (!mp3Path.isAbsolute()) {
            mp3Path = java.nio.file.Paths.get(outputPath).resolve(audioFilePath).normalize();
        }

        logger.info("EdgeTTS trans()返回: audioFilePath={}, outputPath={}, mp3Path={}",
                audioFilePath, outputPath, mp3Path);

        if (!java.nio.file.Files.exists(mp3Path)) {
            throw new java.io.FileNotFoundException("EdgeTTS 生成的 mp3 文件不存在: " + mp3Path);
        }
        long size = java.nio.file.Files.size(mp3Path);
        logger.info("EdgeTTS mp3 文件大小: {} bytes, path={}", size, mp3Path);
        if (size <= 0) {
            throw new java.io.IOException("EdgeTTS mp3 文件大小为0: " + mp3Path);
        }

        // 1) mp3 -> pcm
        byte[] pcmData = AudioUtils.mp3ToPcm(mp3Path.toString());

        // 2) pcm -> wav
        String resampledFilePath = AudioUtils.saveAsWav(pcmData);

        // 3) 调试阶段：先不要删 mp3，等你确认完全正常后再恢复删除
        // Files.deleteIfExists(mp3Path);
        return resampledFilePath;











        //* 原代码*
//        String fullPath = outputPath + audioFilePath;
//
//        // 1. 将MP3转换为PCM (已经设置为16kHz采样率和单声道)
//        byte[] pcmData = AudioUtils.mp3ToPcm(fullPath);
//
//        // 2. 将PCM转换回WAV (使用AudioUtils中的设置：16kHz, 单声道, 160kbps)
//        String resampledFilePath = AudioUtils.saveAsWav(pcmData);
//
//        // 3. 删除原始文件
//        Files.deleteIfExists(Paths.get(fullPath));
//
//        // 4. 返回重采样后的文件路径
//        return resampledFilePath;

    }

}