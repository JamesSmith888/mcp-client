package com.jim.mcpclient.test;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * AI 编排任务
 * @author James Smith
 */
@JsonPropertyOrder({"taskInstructions"})
public record AIOrchestratorTask(

        /**
         * 任务指令列表
         */
        @JsonProperty(required = true)
        @JsonPropertyDescription("""
                任务指令列表，包含一个或多个子任务。
                规则：必须为非空列表，至少包含一个任务指令。
                说明：即使是简单任务也需要拆分为至少一个子任务指令。
                """)
        List<TaskInstruction> taskInstructions


) {

    @JsonPropertyOrder({"instruction", "needValidation", "validation"})
    public record TaskInstruction(
            /**
             * 该任务的具体执行步骤/指令
             */
            @JsonProperty(required = true)
            @JsonPropertyDescription("""
                    该任务的具体执行指令。
                    要求：清晰、具体、可执行，明确说明要做什么。
                    建议：提示执行者优先使用 MCP 工具。
                    """)
            String instruction,

            /**
             * 该任务是否需要验证（并不是所有任务都有必要验证）。true 表示需要验证，false 表示不需要验证
             */
            @JsonProperty(required = true)
            @JsonPropertyDescription("""
                    该任务是否需要验证：
                    - true: 需要验证，此时 validation 字段必须有明确的验收标准
                    - false: 不需要验证，此时 validation 字段可以为空或 null
                    """)
            boolean needValidation,

            /**
             * 该任务的验证/验收标准，用于判断任务是否完成
             */
            @JsonPropertyDescription("""
                    该任务的验收标准。
                    规则：当 needValidation = true 时必须提供明确的验收标准，否则可为空。
                    要求：具体、可量化、可验证，明确说明什么情况算完成。
                    """)
            String validation

    ) {
    }

}
