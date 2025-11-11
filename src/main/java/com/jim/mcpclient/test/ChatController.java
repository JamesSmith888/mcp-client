package com.jim.mcpclient.test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.Map;

@RestController
public class ChatController {

    @Resource
    GoogleGenAiChatModel chatModel;

    @Resource
    private ChatClient chatClient;


    @GetMapping("/ai/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {

        ChatClient.CallResponseSpec call = chatClient.prompt()
                .user(message)
                .advisors(a->a.param(ChatMemory.CONVERSATION_ID, "user1"))
                .call();

        String content = call.content();
        System.out.println("content = " + content);
        return content;
    }

    @GetMapping("/ai/generate1")
    public String generate1(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {

        ChatClient.CallResponseSpec call = chatClient.prompt()
                .user(message)
                .advisors(a->a.param(ChatMemory.CONVERSATION_ID, "user2"))
                .call();

        String content = call.content();
        System.out.println("content = " + content);
        return content;
    }


    @GetMapping("/ai/generateStream")
    public Flux<ChatResponse> generateStream(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        return this.chatModel.stream(prompt);
    }
}
