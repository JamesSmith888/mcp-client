package com.jim.mcpclient.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket配置类
 * 配置STOMP协议的WebSocket端点和消息代理
 */
@Slf4j
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfigWithLogging implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    @Autowired
    private RawWebSocketHandler rawWebSocketHandler;

    /**
     * 注册原始 WebSocket 处理器（用于调试）
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("🔧 注册原始 WebSocket 处理器...");
        registry.addHandler(rawWebSocketHandler, "/ws-debug")
                .setAllowedOriginPatterns("*");
        log.info("✅ 原始 WebSocket 处理器注册完成: /ws-debug");
    }

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册STOMP端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("🔧 注册 STOMP 端点...");

        // 原生 WebSocket 端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        log.info("✅ STOMP 端点注册完成: /ws");
    }

    /**
     * 配置 WebSocket 传输
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        log.info("🔧 配置 WebSocket 传输...");

        // 设置消息大小限制
        registration.setMessageSizeLimit(128 * 1024);  // 128KB
        registration.setSendBufferSizeLimit(512 * 1024);  // 512KB
        registration.setSendTimeLimit(20 * 1000);  // 20秒

        log.info("✅ WebSocket 传输配置完成");
    }

    /**
     * 配置客户端入站通道，添加拦截器用于调试
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null) {
                    StompCommand command = accessor.getCommand();

                    if (StompCommand.CONNECT.equals(command)) {
                        log.info("📥 收到CONNECT请求");
                        log.info("   Headers: {}", accessor.toNativeHeaderMap());
                    } else if (StompCommand.SUBSCRIBE.equals(command)) {
                        log.info("📥 收到SUBSCRIBE请求: {}", accessor.getDestination());
                    } else if (StompCommand.SEND.equals(command)) {
                        log.info("📥 收到SEND请求到: {}", accessor.getDestination());
                    } else if (StompCommand.DISCONNECT.equals(command)) {
                        log.info("📥 收到DISCONNECT请求");
                    }
                }
                return message;
            }
        });
    }

    /**
     * 配置客户端出站通道，添加拦截器用于调试
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null) {
                    StompCommand command = accessor.getCommand();

                    if (StompCommand.CONNECTED.equals(command)) {
                        log.info("📤 发送CONNECTED响应");
                        log.info("   Headers: {}", accessor.toNativeHeaderMap());
                    } else if (StompCommand.MESSAGE.equals(command)) {
                        log.info("📤 发送MESSAGE到: {}", accessor.getDestination());
                    } else if (StompCommand.ERROR.equals(command)) {
                        log.error("📤 发送ERROR: {}", accessor.getMessage());
                    }
                }
                return message;
            }
        });
    }
}
