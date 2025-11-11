package com.jim.mcpclient.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * AI 工作任务执行响应
 * @author James Smith
 */
@JsonPropertyOrder({"success", "result"})
public record AIWorkerResponse(

        /**
         * 任务是否成功。 true 表示任务成功完成，false 表示任务失败或未完成
         */
        @JsonProperty(required = true)
        @JsonPropertyDescription("""
                任务执行是否成功：
                - true: 任务已成功完成，result 字段包含执行结果
                - false: 任务执行失败或未完成，result 字段应说明失败原因
                """)
        boolean success,

        /**
         * 任务执行结果或失败原因
         */
        @JsonProperty(required = true)
        @JsonPropertyDescription("""
                任务执行的详细结果或失败原因。
                - 当 success = true 时：包含任务的完整执行结果
                - 当 success = false 时：详细说明任务失败的具体原因
                必须提供具体、完整的信息，不能为空。
                """)
        String result

) {
}
