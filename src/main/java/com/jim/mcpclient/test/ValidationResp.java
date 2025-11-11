package com.jim.mcpclient.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 任务验证响应
 * @author James Smith
 */
@JsonPropertyOrder({"passed", "reason"})
public record ValidationResp(

        /**
         * 任务是否通过验收。 true 表示通过，false 表示未通过
         */
        @JsonProperty(required = true)
        @JsonPropertyDescription("""
                验收结果：
                - true: 任务结果完全符合验收标准，所有要求都已满足
                - false: 任务结果不符合验收标准，存在未满足的要求，此时 reason 必须说明具体原因
                """)
        boolean passed,

        /**
         * 未通过的原因描述，仅在 passed 为 false 时有值
         */
        @JsonPropertyDescription("""
                验收未通过的详细原因。
                规则：
                - 当 passed = false 时，此字段必须提供明确、具体的原因，说明哪些验收标准未满足
                - 当 passed = true 时，此字段可以为 null 或空字符串
                要求：说明具体哪些标准未达到，以及实际结果与期望的差距
                """)
        String reason

) {
}
