package com.jim.mcpclient.config;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springframework.stereotype.Component;

/**
 * @author James Smith
 */
@Component
@Slf4j
public class LoggingHandler {

    @McpLogging(clients = "ledger-mcp-client1")
    public void handleLoggingMessage(McpSchema.LoggingMessageNotification notification) {
        log.info("Received log: {} - {}", notification.level(), notification.data());
    }
}
