package com.jim.mcpclient.config;

import org.springframework.stereotype.Component;

/**
 * 用户 Token 存储器 - 使用 ThreadLocal 存储当前请求的用户 token
 * 
 * 在 WebSocket 场景下的工作流程:
 * 1. WebSocket Controller 收到消息后,调用 setToken() 存储 token
 * 2. 在同一个线程中,ChatClient 调用 MCP server tool
 * 3. AuthenticationTransportContextProvider 通过 getToken() 获取 token
 * 4. TokenAwareMcpHttpClientRequestCustomizer 将 token 添加到 HTTP header
 * 5. 请求结束后,调用 clear() 清理 token
 * 
 * 注意事项:
 * - 必须在同一线程内调用 (WebSocket 默认如此)
 * - 使用 try-finally 确保清理,避免内存泄漏
 * - 如果使用异步处理,需要使用 TransmittableThreadLocal
 */
@Component
public class UserTokenHolder {
    
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置当前线程的用户 token
     * 
     * @param token 用户认证 token
     */
    public static void setToken(String token) {
        TOKEN_HOLDER.set(token);
    }
    
    /**
     * 获取当前线程的用户 token
     * 
     * @return 用户 token,如果不存在则返回 null
     */
    public static String getToken() {
        return TOKEN_HOLDER.get();
    }
    
    /**
     * 清除当前线程的用户 token
     * 
     * 必须在请求处理完成后调用,防止内存泄漏!
     */
    public static void clear() {
        TOKEN_HOLDER.remove();
    }
}
