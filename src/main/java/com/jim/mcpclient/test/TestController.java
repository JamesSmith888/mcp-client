package com.jim.mcpclient.test;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author James Smith
 */
@RequestMapping("/test")
@RestController
public class TestController {


/*    @Autowired
    List<McpSyncClient> mcpSyncClients;

    @GetMapping("/ai")
    public String generation(String userInput) {

        mcpSyncClients.stream().forEach(f->{
            System.out.println("MCP Client: " + f.getClass().getName());
        });
        return null;
    }*/


}
