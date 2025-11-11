package com.jim.mcpclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息请求
 * 前端发送给后端的消息
 */
@Data
public class ChatRequest {

    /**
     * 用户ID(用于识别不同用户的会话)
     */
    private String userId;

    /**
     * 用户发送的消息内容
     */
    private String message;

    /**
     * 消息ID(可选,用于追踪消息)
     */
    private String messageId;
    
    /**
     * 认证 Token (用于传递给 MCP server)
     * 前端在 WebSocket 消息中携带此 token
     */
    private String token;
}
