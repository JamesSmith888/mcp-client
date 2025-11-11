package com.jim.mcpclient.test;

import com.jim.mcpclient.config.JsonUtils;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.McpConnectionInfo;
import org.springframework.ai.mcp.McpToolFilter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CustomMcpToolFilter implements McpToolFilter {

    @Override
    public boolean test(McpConnectionInfo connectionInfo, McpSchema.Tool tool) {
        // Filter logic based on connection information and tool properties
        // Return true to include the tool, false to exclude it

        log.info("Evaluating tool: {} for connection: {}", JsonUtils.toJsonString(tool), connectionInfo);


        return true; // Include all other tools by default
    }
}
