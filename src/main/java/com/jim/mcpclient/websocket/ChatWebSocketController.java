package com.jim.mcpclient.websocket;

import com.jim.mcpclient.config.UserTokenHolder;
import com.jim.mcpclient.model.ChatRequest;
import com.jim.mcpclient.model.ChatResponse;
import com.jim.mcpclient.test.AIWorkerResponse;
import com.jim.mcpclient.test.AgentOrchestrator;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 聊天控制器
 * 处理客户端发送的聊天消息，并返回AI响应
 */
@Controller
public class ChatWebSocketController {

    @Resource(name = "workClient")
    private ChatClient chatClient;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Resource
    private AgentOrchestrator agentOrchestrator;


    /**
     * 处理聊天消息
     * 客户端发送消息到 /app/chat
     * 服务器响应到 /queue/messages/{userId}
     */
    @MessageMapping("/chat")
    public void chat(@Payload ChatRequest request) {
        String userId = request.getUserId();
        String messageId = request.getMessageId() != null ?
                request.getMessageId() : UUID.randomUUID().toString();

        try {
            // 发送开始消息
            sendMessage(userId, ChatResponse.builder()
                    .messageId(messageId)
                    .type(ChatResponse.MessageType.START)
                    .isFinal(false)
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 使用ChatClient处理消息，带会话记忆
            String response = chatClient.prompt()
                    .user(request.getMessage())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                    .call()
                    .content();

            // 模拟流式传输 - 将响应分块发送
            sendStreamResponse(userId, messageId, response);

            // 发送结束消息
            sendMessage(userId, ChatResponse.builder()
                    .messageId(messageId)
                    .type(ChatResponse.MessageType.END)
                    .isFinal(true)
                    .timestamp(System.currentTimeMillis())
                    .build());

        } catch (Exception e) {
            // 发送错误消息
            sendMessage(userId, ChatResponse.builder()
                    .messageId(messageId)
                    .type(ChatResponse.MessageType.ERROR)
                    .error(e.getMessage())
                    .isFinal(true)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 处理流式聊天消息(真正的流式传输)
     * 客户端发送消息到 /app/chat/stream
     */
    @MessageMapping("/chat/stream")
    public void chatStream(@Payload ChatRequest request) {
        String userId = request.getUserId();
        String messageId = request.getMessageId() != null ?
                request.getMessageId() : UUID.randomUUID().toString();

        try {
            // 发送开始消息
            sendMessage(userId, ChatResponse.builder()
                    .messageId(messageId)
                    .type(ChatResponse.MessageType.START)
                    .isFinal(false)
                    .timestamp(System.currentTimeMillis())
                    .build());


            // 使用 userChatClient 处理用户输入，生成任务指令
/*
            var orchestratorTask = chatClient.prompt("""
                        任务执行

                        执行要求：
                        1. 严格按照上述指令执行任务
                        2. 优先使用可用的 MCP 工具
                        3. 确保执行结果准确、完整
                        4. 如遇到问题，说明具体情况

                        """ )
                    .toolContext(Map.of("token", request.getToken()))
                    .user(request.getMessage())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                    .call()
                    .content();
                    //.entity(AIWorkerResponse.class);
            sendMessage(userId, ChatResponse.builder()
                    .messageId(messageId)
                    .content(orchestratorTask.toString())
                    .type(ChatResponse.MessageType.CHUNK)
                    .isFinal(true)
                    .timestamp(System.currentTimeMillis())
                    .build());
*/


            agentOrchestrator.processUserInput(userId, request);


            // 使用ChatClient的stream功能进行真正的流式传输
            /*chatClient.prompt()
                    .toolContext(Map.of("token", request.getToken()))
                    .user(request.getMessage())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        // 发送每个内容块
                        sendMessage(userId, ChatResponse.builder()
                                .messageId(messageId)
                                .content(chunk)
                                .type(ChatResponse.MessageType.CHUNK)
                                .isFinal(false)
                                .timestamp(System.currentTimeMillis())
                                .build());
                    })
                    .doOnComplete(() -> {
                        // 流式传输完成,发送结束消息
                        sendMessage(userId, ChatResponse.builder()
                                .messageId(messageId)
                                .type(ChatResponse.MessageType.END)
                                .isFinal(true)
                                .timestamp(System.currentTimeMillis())
                                .build());
                    })
                    .doOnError(error -> {
                        // 发送错误消息
                        sendMessage(userId, ChatResponse.builder()
                                .messageId(messageId)
                                .type(ChatResponse.MessageType.ERROR)
                                .error(error.getMessage())
                                .isFinal(true)
                                .timestamp(System.currentTimeMillis())
                                .build());
                    })
                    .doFinally(_ -> {
                        // ⚠️ 重要: 清理 ThreadLocal,防止内存泄漏
                        UserTokenHolder.clear();
                    })
                    .subscribe();
*/
        } catch (Exception e) {
            // 发送错误消息
            sendMessage(userId, ChatResponse.builder()
                    .messageId(messageId)
                    .type(ChatResponse.MessageType.ERROR)
                    .error(e.getMessage())
                    .isFinal(true)
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 确保异常情况下也清理 ThreadLocal
            UserTokenHolder.clear();
        }
    }


    /**
     * 模拟流式发送响应（将完整响应分块发送）
     */
    private void sendStreamResponse(String userId, String messageId, String fullResponse) {
        // 按字符分块，模拟流式传输
        int chunkSize = 10; // 每次发送10个字符
        for (int i = 0; i < fullResponse.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, fullResponse.length());
            String chunk = fullResponse.substring(i, end);

            sendMessage(userId, ChatResponse.builder()
                    .messageId(messageId)
                    .content(chunk)
                    .type(ChatResponse.MessageType.CHUNK)
                    .isFinal(false)
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 添加小延迟，模拟流式效果
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 发送消息到指定用户
     */
    private void sendMessage(String userId, ChatResponse response) {
        messagingTemplate.convertAndSend("/queue/messages/" + userId, response);
    }
}
