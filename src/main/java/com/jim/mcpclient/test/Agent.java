package com.jim.mcpclient.test;

/**
 * @author James Smith
 */
public interface Agent {

    /** 系统提示，用于引导 LLM 行为 */
    String systemPrompt(String prompt);

    /** 是否已完成任务 */
    boolean isDone();

    /** 处理用户输入或者 Agent 问题的响应，返回下一步提示给用户或继续内部处理 */
    AgentResponse handle(String userInput);
}
