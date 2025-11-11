package com.jim.mcpclient.test;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * @author James Smith
 */
@Configuration
public class ChatClientConfig {

    /**
     * 跟用户交互的模型
     */
    @Bean(name = "userChatClient")
    public ChatClient userChatClient(List<McpSyncClient> mcpSyncClients, GoogleGenAiChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(SyncMcpToolCallbackProvider.syncToolCallbacks(mcpSyncClients))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }


    /**
     * 执行任务的模型
     */
    @Bean(name = "workClient")
    @Primary
    public ChatClient workClient(List<McpSyncClient> mcpSyncClients, GoogleGenAiChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(SyncMcpToolCallbackProvider.syncToolCallbacks(mcpSyncClients))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
