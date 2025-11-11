package com.jim.mcpclient.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息响应
 * 后端发送给前端的消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    
    /**
     * 消息ID（与请求的messageId对应）
     */
    private String messageId;
    
    /**
     * AI回复的内容（可能是流式传输的片段）
     */
    private String content;
    
    /**
     * 消息类型：start, chunk, end, error
     */
    private MessageType type;
    
    /**
     * 是否是流式传输的最后一条消息
     */
    private boolean isFinal;
    
    /**
     * 错误信息（如果有）
     */
    private String error;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        START,   // 开始流式传输
        CHUNK,   // 内容片段
        END,     // 结束流式传输
        ERROR    // 错误
    }
}
