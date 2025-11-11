package com.jim.mcpclient.test;

import com.jim.mcpclient.config.UserTokenHolder;
import com.jim.mcpclient.model.ChatRequest;
import com.jim.mcpclient.model.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author James Smith
 */
@Slf4j
@Service
public class AgentOrchestrator {

    @Resource
    private ChatClient workClient;
    @Resource
    private ChatClient userChatClient;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void processUserInput(String userId, ChatRequest request) {

        String token = request.getToken();
        String messageId = request.getMessageId() != null ? request.getMessageId() : UUID.randomUUID().toString();

        // 第一步: 使用 userChatClient 处理用户输入,允许使用工具
        String userAnalysisResult = userChatClient.prompt()
                .toolContext(Map.of("token", token))
                .user(request.getMessage())
                .system("""
                        任务分析
                        
                        你的职责：
                        分析用户输入,理解用户意图。你可以使用 MCP 工具来获取必要的上下文信息。
                        
                        分析要求：
                        1. 理解用户的真实需求
                        2. 如需要,可以使用 MCP 工具获取相关信息
                        3. 输出详细的分析结果和建议的执行步骤
                        """)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .content();

        log.info("User analysis result: {}", userAnalysisResult);

        // 第二步: 将分析结果转换为结构化的任务指令(不使用工具)
        var orchestratorTask = userChatClient.prompt()
                .user(String.format("""
                        基于以下分析结果,生成可执行的任务指令列表。
                        
                        分析结果：
                        %s
                        
                        任务拆分规则：
                        1. 将需求拆分为一个或多个具体的任务指令
                        2. 每个任务指令应该清晰、独立、可执行
                        3. 任务指令之间按逻辑顺序排列
                        4. 简单请求可以是一个任务指令,复杂请求应拆分为多个任务指令
                        
                        工具使用要求：
                        - 在任务指令中明确提示优先使用 MCP 工具
                        
                        输出要求：
                        - 每个任务指令必须清晰、具体、可执行
                        - 每个验收标准必须明确、可量化验证
                        - 确保指令之间逻辑连贯,无矛盾
                        """, userAnalysisResult))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .entity(AIOrchestratorTask.class);

        log.info("Orchestrator generated task: {}", orchestratorTask);

        if (orchestratorTask == null || orchestratorTask.taskInstructions() == null || orchestratorTask.taskInstructions().isEmpty()) {
            // 无法生成任务指令或任务指令为空，发送错误消息给用户
            sendEndMessage(userId, messageId, "无法生成任务指令，请重试。");
            return;
        }

        // 获取任务指令列表
        List<AIOrchestratorTask.TaskInstruction> taskInstructions = orchestratorTask.taskInstructions();

        taskInstructions.forEach(instruction -> {
            log.info("Processing instruction: {}", instruction);
            String validation = instruction.validation();
            String taskInstr = instruction.instruction();

            // 最大重试次数
            int maxRetries = 1;

            for (int retryCount = 0; retryCount <= maxRetries; retryCount++) {
                if (retryCount > 0) {
                    log.info("Retrying instruction (attempt {}/{}): {}", retryCount + 1, maxRetries + 1, taskInstr);
                }

                // 使用 workClient 执行任务指令
                AIWorkerResponse aiWorkerResponse = doTask(userId, taskInstr, token);

                if (aiWorkerResponse == null) {
                    log.error("Worker response is null for instruction: {} (attempt {}/{})", taskInstr, retryCount + 1, maxRetries + 1);
                    if (retryCount == maxRetries) {
                        log.error("Max retries reached for null response. Skipping instruction: {}", taskInstr);
                        return;
                    }

                    // 发送错误消息给用户
                    sendMessage(userId, messageId, "任务执行失败，正在重试...");

                    continue;
                }

                if (!aiWorkerResponse.success()) {
                    log.error("Task not completed for instruction: {} (attempt {}/{})", taskInstr, retryCount + 1, maxRetries + 1);
                    if (retryCount == maxRetries) {
                        log.error("Max retries reached for failed task. Skipping instruction: {}", taskInstr);
                        return;
                    }

                    // 发送错误消息给用户
                    sendMessage(userId, messageId, "任务执行未完成，正在重试...");

                    continue;
                }

                // 执行成功，并且不需要验证，直接继续下一个任务指令
                if (!instruction.needValidation()) {
                    log.info("No validation needed for instruction: {}", taskInstr);

                    // 发送任务结果给用户
                    sendMessage(userId, messageId, aiWorkerResponse.result());
                    return;
                }

                // 根据 validation 验证任务结果是否符合要求
                String result = aiWorkerResponse.result();
                
                ValidationResp validationResult = userChatClient.prompt()
                        .user(String.format("""
                                任务结果验证
                                
                                你的职责：
                                严格验证任务执行结果是否符合验收标准。
                                
                                验收标准：
                                %s
                                
                                任务执行结果：
                                %s
                                
                                验证要求：
                                1. 逐条对照验收标准检查结果
                                2. 必须所有标准都满足才算通过
                                3. 如不符合,明确指出哪些标准未满足
                                
                                输出说明：
                                - 如果通过：返回 passed = true
                                - 如果不通过：返回 passed = false,并在 reason 中详细说明未满足的标准和具体原因
                                """, validation, result))
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                        .call()
                        .entity(ValidationResp.class);
                
                log.info("Validation result: {}", validationResult);
                
                if (validationResult == null) {
                    log.error("Validation result is null for instruction: {} (attempt {}/{})", taskInstr, retryCount + 1, maxRetries + 1);
                    if (retryCount == maxRetries) {
                        log.error("Max retries reached for null validation result. Skipping instruction: {}", taskInstr);

                        // 发送验证失败消息给用户
                        sendMessage(userId, messageId, "任务验证失败,未收到验证结果,已跳过该任务指令。");
                        return;
                    }

                    // 发送验证错误消息给用户
                    sendMessage(userId, messageId, "任务验证失败,正在重试...");

                    continue;
                }

                if (validationResult == null || !validationResult.passed()) {
                    String failureReason = validationResult == null ? "No validation result" : validationResult.reason();
                    log.error("Validation failed for instruction: {}. Reason: {} (attempt {}/{})", taskInstr, failureReason, retryCount + 1, maxRetries + 1);

                    if (retryCount == maxRetries) {
                        log.error("Max retries reached for validation failure. Skipping instruction: {}", taskInstr);

                        // 发送验证失败消息给用户
                        sendMessage(userId, messageId, String.format("任务验证失败，原因：%s，已跳过该任务指令。", failureReason));
                        return;
                    }

                    // 根据验证失败的原因，调整任务指令，重新执行
                    taskInstr = String.format("""
                            任务重试执行
                            
                            原始任务指令：
                            %s
                            
                            【重要】上次执行失败信息：
                            失败原因：%s
                            
                            重试要求：
                            1. 仔细分析上次失败的原因
                            2. 调整执行策略，避免重复相同错误
                            3. 确保本次执行结果能满足所有验收标准
                            4. 优先使用 MCP 工具完成任务
                            
                            注意事项：
                            - 这是最后一次重试机会
                            - 必须严格满足验收标准
                            - 如有疑问，采用保守稳妥的方案
                            """, instruction.instruction(), failureReason);
                    log.info("Adjusted instruction for retry: {}", taskInstr);

                    // 发送重试消息给用户
                    sendMessage(userId, messageId, "任务验证未通过，正在调整后重试...");
                    continue;
                }

                log.info("Instruction completed and validated: {}", taskInstr);
                // 任务指令执行并验证通过，继续下一个任务指令
                sendMessage(userId, messageId, result);
                return;
            }
        });

        // 所有任务指令处理完毕，返回最终结果给用户
        userChatClient.prompt("""
                        任务结果整合
                        
                        你的职责：
                        将所有任务的执行结果整合为完整的最终答案。
                        
                        整合要求：
                        1. 汇总所有任务的执行结果
                        2. 确保答案完整、连贯、易于理解
                        3. 突出重点信息和关键结论
                        4. 按照逻辑顺序组织内容
                        
                        输出格式：
                        - 使用清晰的结构化格式
                        - 如有多个部分，使用标题分隔
                        - 重要信息使用列表突出显示
                        
                        注意事项：
                        - 直接返回给用户，语言要专业且友好
                        - 避免遗漏任何重要的任务结果
                        - 如有失败的任务，需要说明情况
                        """)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .toolContext(Map.of("token", request.getToken()))
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
                // 流式传输完成,发送结束消息
                .doOnComplete(() -> sendEndMessage(userId, messageId, "任务已完成，以上是最终结果。"))
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


    }

    private void sendMessage(String userId, String messageId, String message) {
        log.info("Sending message chunk to user {}: {}", userId, message);
        sendMessage(userId, ChatResponse.builder()
                .messageId(messageId)
                .content(message)
                .type(ChatResponse.MessageType.CHUNK)
                .isFinal(false)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private void sendEndMessage(String userId, String messageId, String message) {
        log.info("Sending end message to user {}: {}", userId, message);

        sendMessage(userId, messageId, message);

        // 流式传输完成,发送结束消息
        sendMessage(userId, ChatResponse.builder()
                .messageId(messageId)
                .type(ChatResponse.MessageType.END)
                .isFinal(true)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    private AIWorkerResponse doTask(String userId, String prompt, String token) {
        // 第一步: 执行任务(允许使用 MCP 工具)
        String taskResult = workClient.prompt()
                .toolContext(Map.of("token", token))
                .user(String.format("""
                        任务执行
                        %s
                        执行要求：
                        1. 严格按照上述指令执行任务
                        2. 优先使用可用的 MCP 工具
                        3. 确保执行结果准确、完整
                        4. 如遇到问题,说明具体情况
                        5. 直接返回执行结果,不需要 JSON 格式
                        """, prompt))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .content();
        
        log.info("Worker task result: {}", taskResult);
        if (taskResult == null) {
            log.error("Worker task result is null");
            return null;
        }

        // 第二步: 将结果转换为结构化格式(不使用工具,直接获取结构化对象)
        AIWorkerResponse aiWorkerResponse = userChatClient.prompt()
                .user(String.format("""
                        结果格式化
                        
                        任务执行结果：
                        %s
                        
                        你的职责：
                        将上述任务执行结果转换为结构化格式。
                        
                        转换要求：
                        1. 分析任务结果,判断是否成功完成
                        2. 提取关键信息作为 result 字段内容
                        3. success 为 true 表示成功,false 表示失败
                        """, taskResult))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .entity(AIWorkerResponse.class);
        
        log.info("Worker response: {}", aiWorkerResponse);
        return aiWorkerResponse;
    }


    /**
     * 发送消息到指定用户
     */
    private void sendMessage(String userId, ChatResponse response) {
        messagingTemplate.convertAndSend("/queue/messages/" + userId, response);
    }
}
