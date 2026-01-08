package com.xiaozhi.dialogue.tts.providers;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        Voice voiceObj = TTSVoice.provides().stream()
                .filter(v -> v.getShortName().equals(voiceName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到语音: " + voiceName));

        // 统一文件基名（mp3 / wav 同名）
        String baseName = java.util.UUID.randomUUID().toString().replace("-", "");

        int ratePercent = (int) ((speed - 1.0f) * 100);
        int pitchHz = (int) ((pitch - 1.0f) * 50);

        TTS ttsEngine = new TTS(voiceObj, text);

        // EdgeTTS 输出 mp3
        ttsEngine.findHeadHook()
                .storage(outputPath)     // 一般是 audio/
                .fileName(baseName)      // 不带后缀
                .overwrite(true)
                .isRateLimited(true)
                .voicePitch(pitchHz + "Hz")
                .voiceRate(ratePercent + "%")
                .formatMp3()
                .trans();

        Path mp3Path = Paths.get(outputPath).resolve(baseName + ".mp3").normalize();

        logger.info("EdgeTTS mp3 path={}", mp3Path);

        if (!Files.exists(mp3Path)) {
            throw new FileNotFoundException("EdgeTTS mp3 不存在: " + mp3Path);
        }

        long mp3Size = Files.size(mp3Path);
        if (mp3Size <= 0) {
            throw new IOException("EdgeTTS mp3 大小为0: " + mp3Path);
        }

        // mp3 -> pcm
        byte[] pcmData = AudioUtils.mp3ToPcm(mp3Path.toString());

        // pcm -> wav（同名）
        Path wavPath = Paths.get(outputPath).resolve(baseName + ".wav").normalize();
        AudioUtils.saveAsWav(wavPath, pcmData);

        if (!Files.exists(wavPath) || Files.size(wavPath) <= 44) {
            throw new IOException("WAV生成失败: " + wavPath);
        }

        return wavPath.toString();
    }
}





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

//    }
//
//}