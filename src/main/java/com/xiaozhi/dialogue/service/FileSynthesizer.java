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

    // 从配置文件读取TTS相关参数
    @Value("${tts.timeout.ms:10000}")
    private long TTS_TIMEOUT_MS = 10000;

    @Value("${tts.max.retry.count:1}")
    private int MAX_RETRY_COUNT = 1;

    @Value("${tts.retry.delay.ms:1000}")
    private long TTS_RETRY_DELAY_MS = 1000;

    @Value("${tts.max.concurrent.per.session:3}")
    private int MAX_CONCURRENT_PER_SESSION = 3;

    public FileSynthesizer(ChatSession session, MessageService messageService,
                       TtsService ttsService, Player player) {
        super(session,player);
        this.messageService = messageService;
        this.ttsService = ttsService;
    }

    @Override
    protected void doSynthesize(Sentence sentence) {
        // 检查是否已被中断或中止，避免abort后继续请求TTS
        if (Thread.currentThread().isInterrupted() || aborted) {
            return;
        }

        String text = sentence.getText4Speech();
        
        try {
            // TODO 超时须在ttsFactory里设置。
            String audioPath = ttsService.textToSpeech(text);
            logger.debug("executeTtsTask audioPath:{}", audioPath);
            java.nio.file.Path p = java.nio.file.Path.of(audioPath);
            if (!java.nio.file.Files.exists(p) || java.nio.file.Files.size(p) <= 44) {
                throw new java.io.FileNotFoundException("TTS 返回音频不存在或过小: " + audioPath);
            }
            // 记录TTS生成时间
            sentence.setEndSynthesis(Instant.now());

            // 成功生成音频
            handleTtsSuccess(sentence, audioPath);
        } catch (Exception e) {
            logger.error("TTS任务执行失败 - 句子序号: {}, 提供商: {}, 语音: {}, 原因: {}",
                    sentence.getSeq(), ttsService.getProviderName(), ttsService.getVoiceName(), e.getMessage());
            handleTtsFailure(sentence, e.getMessage());
        }
    }

    private void handleTtsSuccess(Sentence sentence, String audioPath) {

        // 记录日志
        logger.info("句子音频生成完成 - 序号: {}, 对话ID: {},  语音生成: {}毫秒, 内容: \"{}\"",
                sentence.getSeq(), sentence.getAssistantTimeMillis(),
                sentence.getSynthesisDuration(),
                sentence.getText());

        try {
            // 设置音频路径到句子对象
            sentence.setAudio(Path.of(audioPath));

            // 标记合成完成
            sentence.setSynthesisCompleted(true);

        } catch (Exception e) {
            logger.error("读取音频文件失败 - 序号: {}, 文件: {}", sentence.getSeq(), audioPath, e);
            handleTtsFailure(sentence, "读取音频文件失败: " + e.getMessage());
            return;
        }

        // 检查是否已被中断或中止，避免abort后仍添加旧句子
        if (Thread.currentThread().isInterrupted() || aborted) {
            logger.debug("TTS任务已被中止，跳过句子添加 - 序号: {}", sentence.getSeq());
            return;
        }

        // 确保当前Synthesizer仍然是session中的活跃Synthesizer
        // 防止abort后新对话创建了新的Synthesizer，但旧的重试句子仍然完成并尝试播放
        if (chatSession.getSynthesizer() != this) {
            logger.debug("当前Synthesizer已被替换，跳过句子播放 - 序号: {}", sentence.getSeq());
            return;
        }

        // 发送到客户端
        player.append(sentence);
        player.play();
        // 从队列中移除已处理的句子
        removeSentence(sentence);
    }

    /**
     * 处理TTS失败
     */
    private void handleTtsFailure(Sentence sentence, String reason) {
        // 检查是否已被中止，避免abort后继续重试
        if (aborted) {
            return;
        }

        // TODO 考虑创建新的任务对象而不是重用原对象，避免数据污染
        sentence.retryCount++;
        sentence.isRetry = true;
        // 异常或失败，发送类似心跳包，避免设备端误判为会话终止
        messageService.sendEmotion(chatSession, "happy");

        if (sentence.retryCount <= MAX_RETRY_COUNT) {

            logger.info("TTS任务重试 - 序号: {}, 重试次数: {}/{}, 内容: \"{}\", 原因: {}",
                    sentence.getSeq(), sentence.retryCount, MAX_RETRY_COUNT, sentence.getText(), reason);

            // 没必要延迟重试，因为对话的时间是很有限的。如果延时，那这句话也就赶不上播放速度了，也就等于放弃这句话了。
            doSynthesize(sentence);
        } else {
            // 超过最大重试次数，标记为失败
            logger.error("TTS任务失败 - 序号: {}, 重试次数: {}/{}, 已达最大重试次数, 原因: {}",
                    sentence.getSeq(), sentence.retryCount, MAX_RETRY_COUNT, reason);

            // 即使失败也标记为准备好，以便队列继续处理
            sentence.setAudio(null);
            sentence.setEndSynthesis(Instant.now());
        }
    }
}
