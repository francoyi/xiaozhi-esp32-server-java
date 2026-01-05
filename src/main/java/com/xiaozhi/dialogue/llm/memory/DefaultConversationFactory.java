package com.xiaozhi.dialogue.llm.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.xiaozhi.entity.SysDevice;
import com.xiaozhi.entity.SysRole;

@Primary
@Service
@Slf4j
public class DefaultConversationFactory implements ConversationFactory{

    @Value("${conversation.max-messages:16}")
    private int maxMessages;

    @Autowired
    private ChatMemory chatMemory;

    @Override
    public Conversation initConversation(SysDevice device, SysRole role, String sessionId) {

        // 兜底：role 为空也不让 WS 掉线
        if (role == null) {
            log.warn("role is null when initConversation. deviceId={}, sessionId={}, fallback to window conversation",
                    device != null ? device.getDeviceId() : null, sessionId);
            return MessageWindowConversation.builder()
                    .chatMemory(chatMemory)
                    .maxMessages(maxMessages)
                    .role(null)
                    .device(device)
                    .sessionId(sessionId)
                    .build();
        }

        // 兜底：memoryType 为空默认 window
        String memoryType = role.getMemoryType();
        if (memoryType == null || memoryType.isBlank()) {
            log.warn("role.memoryType is null/blank. roleId={}, deviceId={}, sessionId={}, fallback to 'window'",
                    role.getRoleId(),
                    device != null ? device.getDeviceId() : null,
                    sessionId);
            memoryType = "window";
        }

        return switch (memoryType) {
            case "window" -> MessageWindowConversation.builder()
                    .chatMemory(chatMemory)
                    .maxMessages(maxMessages)
                    .role(role)
                    .device(device)
                    .sessionId(sessionId)
                    .build();
            default -> {
                log.warn("系统目前不支持这类未知的记忆类型：{} ，将启用默认的MessageWindowConversation", memoryType);
                yield MessageWindowConversation.builder()
                        .chatMemory(chatMemory)
                        .maxMessages(maxMessages)
                        .role(role)
                        .device(device)
                        .sessionId(sessionId)
                        .build();
            }
        };
    }

}
