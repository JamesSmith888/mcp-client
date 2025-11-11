package com.jim.mcpclient.test;

public class UserAgent implements Agent {
    @Override
    public String systemPrompt(String prompt) {
        return "根据以下用户输入生成任务指令 : " + prompt;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public AgentResponse handle(String userInput) {
        return null;
    }
}
