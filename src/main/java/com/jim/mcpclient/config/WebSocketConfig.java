/*
package com.jim.mcpclient.config;

// import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

*/
/**
 * WebSocket配置类
 * 配置STOMP协议的WebSocket端点和消息代理
 *//*

@Configuration  // 暂时禁用，使用WebSocketConfigWithLogging
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    */
/**
     * 配置消息代理
     * /topic - 用于广播消息
     * /queue - 用于点对点消息
     *//*

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，用于向客户端发送消息
        config.enableSimpleBroker("/topic", "/queue");
        // 设置客户端发送消息的前缀
        config.setApplicationDestinationPrefixes("/app");
        // 设置用户目的地前缀（用于点对点消息）
        config.setUserDestinationPrefix("/user");
    }

    */
/**
     * 注册STOMP端点
     * 客户端将使用此端点连接WebSocket
     *//*

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册原生WebSocket端点（用于React Native等移动端）
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
*/
