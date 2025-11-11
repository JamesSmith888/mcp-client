package com.jim.mcpclient.config;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Optional;

/**
 * MCP HTTP 请求自定义器,用于在调用 MCP server 时添加认证 token
 *
 * 工作原理:
 * 1. Spring AI MCP 在发送 HTTP 请求到 MCP server 前,会调用此 customizer
 * 2. 我们从 TransportContext 中获取 token (通过 TransportContextProvider 注入)
 * 3. 将 token 添加到 HTTP header 中
 */
@Slf4j
@Component
public class TokenAwareMcpHttpClientRequestCustomizer implements McpSyncHttpClientRequestCustomizer {


    @Override
    public void customize(HttpRequest.Builder requestBuilder, String method, URI endpoint, String body, McpTransportContext context) {

        log.info("""
                                requestBuilder: {}
                                method: {}
                                endpoint: {}
                                body: {}
                                context: {}
                        """,
                requestBuilder,
                method,
                endpoint,
                body,
                context
        );

        // 从 context 中获取 token (由 TransportContextProvider 提供)
        Object token = context.get("userToken");

        String token1 = UserTokenHolder.getToken();
        System.out.println("TokenAwareMcpHttpClientRequestCustomizer token========================: " + token1);
        requestBuilder.header("Authorization", Optional.ofNullable(token1).map(t -> "Bearer " + t).orElse("Bearer test111"));

        if (token != null) {
            // 添加 Authorization header
            requestBuilder.header("Authorization", "Bearer " + token);

            // 或者使用自定义 header
            // requestBuilder.header("X-User-Token", token.toString());
        }

    }
}
