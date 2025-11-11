package com.jim.mcpclient.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 原始 WebSocket 处理器（用于调试）
 */
@Slf4j
@Component
public class RawWebSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("🔗 WebSocket 连接建立");
        log.info("   Session ID: {}", session.getId());
        log.info("   Remote Address: {}", session.getRemoteAddress());
        log.info("   URI: {}", session.getUri());
        log.info("   Accepted Protocol: {}", session.getAcceptedProtocol());
        log.info("   Sub-protocols requested: {}", session.getHandshakeHeaders().get("Sec-WebSocket-Protocol"));
        log.info("   Headers: {}", session.getHandshakeHeaders());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("📨 收到消息: {}", payload);
        
        // 如果是 STOMP CONNECT 命令
        if (payload.startsWith("CONNECT")) {
            log.info("🎯 检测到 STOMP CONNECT 命令！");
            log.info("   完整消息: {}", payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("❌ WebSocket 连接关闭");
        log.info("   Session ID: {}", session.getId());
        log.info("   Status: {}", status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("⚠️ WebSocket 传输错误", exception);
    }
}
