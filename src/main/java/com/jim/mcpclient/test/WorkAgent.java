package com.jim.mcpclient.test;

/**
 * @author James Smith
 */
public class WorkAgent implements  Agent{
    @Override
    public String systemPrompt(String prompt) {
        return "";
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
