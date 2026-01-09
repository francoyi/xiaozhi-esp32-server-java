package com.xiaozhi.dialogue.service;

import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.dialogue.tts.TtsService;
import com.xiaozhi.service.SysMessageService;
import com.xiaozhi.utils.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 语音合成器，用于处理一个服务器响应时的多个句子，实现更好的输出。
 * 由TTS进行拉取，也就是取决于TtsModel的处理速度。TtsModel刚开始时处理一个短句，其后处理LLM已经生成的句子。避免切分得过短过细。
 * 再由播放器将所有句子按顺序存储成为一个音频文件（未来或者有可能将句子文本以 Lyrics 歌词的形式保存在同一个文件里）。
 * 以短句为单位存储，可能过于零散。
 * TODO 这个 Synthesizer 还没有抽象好设计，音频文件的本地保存功能先不在这里实现，而是下放到Player中处理。
 *
 */
public class FileSynthesizer extends ThreadSynthesizer  {

    private static final Logger logger = LoggerFactory.getLogger(FileSynthesizer.class);

    private final MessageService messageService;
    private final TtsService ttsService;

    @Value("${tts.timeout.ms:10000}")
    private long TTS_TIMEOUT_MS = 10000;

    @Value("${tts.max.retry.count:1}")
    private int MAX_RETRY_COUNT = 1;

    @Value("${tts.retry.delay.ms:1000}")
    private long TTS_RETRY_DELAY_MS = 1000;

    @Value("${tts.max.concurrent.per.session:3}")
    private int MAX_CONCURRENT_PER_SESSION = 3;

    // ★ 新增：音频根目录（建议与你的 systemd WorkingDirectory=/opt/xiaozhi/app 配套）
    @Value("${xiaozhi.audio.dir:/opt/xiaozhi/app/audio}")
    private String AUDIO_DIR;

    // ★ 新增：短等待参数（专治“3ms 竞态”）
    @Value("${tts.file.wait.max.ms:500}")
    private long TTS_FILE_WAIT_MAX_MS;

    @Value("${tts.file.wait.step.ms:50}")
    private long TTS_FILE_WAIT_STEP_MS;

    public FileSynthesizer(ChatSession session, MessageService messageService,
                           TtsService ttsService, Player player) {
        super(session, player);
        this.messageService = messageService;
        this.ttsService = ttsService;
    }

    @Override
    protected void doSynthesize(Sentence sentence) {
        if (Thread.currentThread().isInterrupted() || aborted) {
            return;
        }

        String text = sentence.getText4Speech();

        try {
            String audioPath = ttsService.textToSpeech(text);
            logger.debug("executeTtsTask audioPath(raw):{}", audioPath);

            // ★ 1) 解析绝对路径（优先按 user.dir，其次按配置的 AUDIO_DIR 兜底）
            Path absPath = resolveAudioPath(audioPath);

            // ★ 2) 短等待：最多 500ms（默认），直到文件存在且大小>44
            waitForFileReady(absPath, audioPath);

            // 记录TTS生成时间
            sentence.setEndSynthesis(Instant.now());

            // ★ 3) 成功后传入 absPath（后续读取文件不再踩相对路径坑）
            handleTtsSuccess(sentence, audioPath, absPath);

        } catch (Exception e) {
            logger.error("TTS任务执行失败 - 句子序号: {}, 提供商: {}, 语音: {}, 原因: {}",
                    sentence.getSeq(), ttsService.getProviderName(), ttsService.getVoiceName(), e.getMessage());
            handleTtsFailure(sentence, e.getMessage());
        }
    }

    // ★ 新增：把 audio/xxx.mp3 解析成绝对路径
    private Path resolveAudioPath(String audioPath) {
        Path p = Path.of(audioPath);

        if (p.isAbsolute()) {
            return p.normalize();
        }

        // 常规情况：按 user.dir + 相对路径（你 systemd WorkingDirectory=/opt/xiaozhi/app 就会落到 /opt/xiaozhi/app/audio/xxx）
        String userDir = System.getProperty("user.dir");
        Path byUserDir = Path.of(userDir).resolve(p).normalize();

        // 兜底：有些情况下 audioPath 可能是 "xxx.mp3" 或 user.dir 不符合预期
        // 尝试按 AUDIO_DIR + 文件名兜底（确保落到 /opt/xiaozhi/app/audio）
        Path byAudioDir = Path.of(AUDIO_DIR).resolve(p.getFileName()).normalize();

        // 优先返回更“像音频目录”的那个
        if (byUserDir.toString().contains("/audio/")) return byUserDir;
        return byAudioDir;
    }

    // ★ 新增：短等待，避免“文件刚生成但尚未落盘”导致 exists=false
    private void waitForFileReady(Path absPath, String rawPath) throws Exception {
        long deadline = System.currentTimeMillis() + TTS_FILE_WAIT_MAX_MS;

        long lastSize = -1;
        int stableCount = 0;

        while (true) {
            if (Thread.currentThread().isInterrupted() || aborted) {
                throw new java.lang.InterruptedException("TTS wait interrupted/aborted");
            }

            if (java.nio.file.Files.exists(absPath)) {
                long size = java.nio.file.Files.size(absPath);

                // mp3 建议用更合理的阈值
                if (size >= 1024) {
                    if (size == lastSize) stableCount++;
                    else stableCount = 0;

                    // 连续两次相同（≈ 100ms）认为写入完成
                    if (stableCount >= 2) {
                        logger.debug("TTS file ready: raw={}, abs={}, size={}, user.dir={}",
                                rawPath, absPath, size, System.getProperty("user.dir"));
                        return;
                    }
                }

                lastSize = size;
            }

            if (System.currentTimeMillis() >= deadline) {
                boolean exists = java.nio.file.Files.exists(absPath);
                long size = exists ? java.nio.file.Files.size(absPath) : -1;
                throw new java.io.FileNotFoundException(
                        "TTS 音频未就绪(超时). raw=" + rawPath +
                                ", abs=" + absPath +
                                ", exists=" + exists +
                                ", size=" + size +
                                ", user.dir=" + System.getProperty("user.dir")
                );
            }

            Thread.sleep(TTS_FILE_WAIT_STEP_MS);
        }
    }


    // ★ 修改：多传一个 absPath，sentence.setAudio 用 absPath
    private void handleTtsSuccess(Sentence sentence, String audioPath, Path absPath) {

        logger.info("句子音频生成完成 - 序号: {}, 对话ID: {},  语音生成: {}毫秒, 内容: \"{}\"",
                sentence.getSeq(), sentence.getAssistantTimeMillis(),
                sentence.getSynthesisDuration(),
                sentence.getText());

        try {
            // ★ 用绝对路径，确保后续读取一致
            sentence.setAudio(absPath);
            sentence.setSynthesisCompleted(true);

        } catch (Exception e) {
            logger.error("读取音频文件失败 - 序号: {}, raw: {}, abs: {}", sentence.getSeq(), audioPath, absPath, e);
            handleTtsFailure(sentence, "读取音频文件失败: " + e.getMessage());
            return;
        }

        if (Thread.currentThread().isInterrupted() || aborted) {
            logger.debug("TTS任务已被中止，跳过句子添加 - 序号: {}", sentence.getSeq());
            return;
        }

        if (chatSession.getSynthesizer() != this) {
            logger.debug("当前Synthesizer已被替换，跳过句子播放 - 序号: {}", sentence.getSeq());
            return;
        }

        player.append(sentence);
        player.play();
        removeSentence(sentence);
    }

    private void handleTtsFailure(Sentence sentence, String reason) {
        if (aborted) {
            return;
        }

        sentence.retryCount++;
        sentence.isRetry = true;
        messageService.sendEmotion(chatSession, "happy");

        if (sentence.retryCount <= MAX_RETRY_COUNT) {

            logger.info("TTS任务重试 - 序号: {}, 重试次数: {}/{}, 内容: \"{}\", 原因: {}",
                    sentence.getSeq(), sentence.retryCount, MAX_RETRY_COUNT, sentence.getText(), reason);

            doSynthesize(sentence);
        } else {
            logger.error("TTS任务失败 - 序号: {}, 重试次数: {}/{}, 已达最大重试次数, 原因: {}",
                    sentence.getSeq(), sentence.retryCount, MAX_RETRY_COUNT, reason);

            sentence.setAudio(null);
            sentence.setEndSynthesis(Instant.now());
        }
    }
}
