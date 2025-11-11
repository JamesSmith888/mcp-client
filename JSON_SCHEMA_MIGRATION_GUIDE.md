# JSON Schema 手动转换迁移指南

## 🎯 迁移目的

由于 Spring AI 的 `.entity()` 方法存在 BUG，无法正确解析结构化输出，因此改为手动 JSON 转换方式。

## 📝 迁移方案

### 原始方式（有 BUG）
```java
AIOrchestratorTask task = chatClient.prompt("...")
    .call()
    .entity(AIOrchestratorTask.class);  // ❌ BUG: 无法正确解析
```

### 新方式（手动转换）
```java
String json = chatClient.prompt("..." + JSON_SCHEMA)  // ✅ 在 prompt 中添加 JSON Schema
    .call()
    .content();  // 获取 JSON 字符串

AIOrchestratorTask task = JsonUtils.parse(json, AIOrchestratorTask.class);  // 手动转换
```

## 🔧 实施步骤

### 1️⃣ 定义 JSON Schema 常量

为每个需要结构化输出的类定义对应的 JSON Schema 提示词：

```java
/**
 * AIOrchestratorTask 的 JSON Schema 提示词
 */
private static final String ORCHESTRATOR_TASK_JSON_SCHEMA = """
    
    ## 📋 输出格式要求
    Your response MUST be in JSON format.
    Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
    
    ### JSON 结构定义
    ```json
    {
      "multiRound": boolean,
      "simpleTaskResult": string | null,
      "complexTaskInstruction": array | null
    }
    ```
    
    ### 字段规则说明
    ... (详细的字段约束)
    """;
```

### 2️⃣ 在 Prompt 中附加 JSON Schema

```java
String json = chatClient.prompt("""
        # 任务分析与指令生成
        
        ## 你的职责
        分析用户输入，生成可执行的任务指令。
        
        ... (业务提示词)
        """ + ORCHESTRATOR_TASK_JSON_SCHEMA)  // ✅ 附加 JSON Schema
    .call()
    .content();  // 获取 JSON 字符串
```

### 3️⃣ 使用 JsonUtils 手动转换

```java
AIOrchestratorTask task = JsonUtils.parse(json, AIOrchestratorTask.class);
```

## 📊 JSON Schema 模板结构

### 标准模板格式

```markdown
## 📋 输出格式要求
Your response MUST be in JSON format.
Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.

### JSON 结构定义
```json
{
  "field1": type1,
  "field2": type2
}
```

### 字段规则说明

#### 1. field1 (类型，必填/可选)
字段描述

**当 condition = value 时：**
- 规则 1
- 规则 2

**当 condition = other 时：**
- 规则 3
- 规则 4

#### 2. field2 (类型，必填/可选)
字段描述

### ⚠️ 严格遵守规则
- 规则 1
- 规则 2
- 必须确保 JSON 格式完全符合 RFC8259 标准
- 不要添加任何 JSON 之外的解释文字
```

## ✅ 已完成的迁移

### 1. AIOrchestratorTask
- **位置**: `processUserInput()` 方法
- **JSON Schema**: `ORCHESTRATOR_TASK_JSON_SCHEMA`
- **转换方式**: `JsonUtils.parse(json, AIOrchestratorTask.class)`

### 2. ValidationResp
- **位置**: `processUserInput()` 方法中的验证逻辑
- **JSON Schema**: `VALIDATION_RESP_JSON_SCHEMA`
- **转换方式**: `JsonUtils.parse(json, ValidationResp.class)`

### 3. AIWorkerResponse
- **位置**: `doTask()` 方法
- **JSON Schema**: `WORKER_RESPONSE_JSON_SCHEMA`
- **转换方式**: `JsonUtils.parse(json, AIWorkerResponse.class)`

## 📋 JSON Schema 设计要点

### 1. 结构清晰
- 使用 Markdown 格式，层次分明
- 使用代码块展示 JSON 结构
- 使用标题和列表组织内容

### 2. 规则明确
- 对每个字段详细说明类型和是否必填
- 明确条件约束（"当 xxx = yyy 时"）
- 说明互斥字段的关系

### 3. 示例代码块
```json
{
  "field": "value"
}
```
- 使用三个反引号包裹 JSON 示例
- 标注 `json` 语言类型以启用语法高亮

### 4. 强制性语言
- 使用 "MUST"、"必须"、"仅当" 等强制性词汇
- 明确说明违反规则的后果
- 使用 ⚠️ 等符号突出重要提示

### 5. 互斥字段处理
对于互斥字段（如 `simpleTaskResult` 和 `complexTaskInstruction`）：

```markdown
**当 multiRound = false 时：**
- simpleTaskResult 必须有值（非空字符串）
- complexTaskInstruction 必须为 null 或空数组 []

**当 multiRound = true 时：**
- simpleTaskResult 必须为 null
- complexTaskInstruction 必须为非空数组，至少包含一个子任务
```

## 🎯 优势对比

### 使用 .entity() (原方式)
❌ 存在解析 BUG
❌ 无法控制 JSON Schema 的具体内容
❌ 依赖 Jackson 注解，不够灵活
❌ 难以调试和定位问题

### 手动 JSON 转换 (新方式)
✅ 完全控制 JSON Schema 内容
✅ 可以优化和调整提示词
✅ 更容易调试（可以查看原始 JSON）
✅ 不受框架 BUG 影响
✅ 提示词更加结构化和清晰

## 🔍 调试技巧

### 1. 查看原始 JSON
```java
String json = chatClient.prompt("..." + JSON_SCHEMA)
    .call()
    .content();

log.info("Raw JSON response: {}", json);  // 查看 AI 返回的原始 JSON

AIOrchestratorTask task = JsonUtils.parse(json, AIOrchestratorTask.class);
```

### 2. JSON 解析失败处理
`JsonUtils.parse()` 在解析失败时会：
- 记录警告日志
- 返回原始字符串（类型转换为对象类型）

所以始终要检查返回值是否为 null：
```java
if (task == null) {
    log.error("Failed to parse JSON: {}", json);
    // 处理错误情况
}
```

### 3. 验证 JSON 格式
使用在线 JSON 验证器检查 AI 返回的 JSON 是否符合 RFC8259 标准。

## 📚 参考文档

- [Spring AI Structured Output Converter](https://docs.spring.io/spring-ai/reference/1.1/api/structured-output-converter.html)
- [RFC 8259 - JSON 标准](https://datatracker.ietf.org/doc/html/rfc8259)
- [JsonUtils 工具类](./src/main/java/com/jim/mcpclient/config/JsonUtils.java)

## 🚀 最佳实践

1. **JSON Schema 作为常量定义**
   - 便于复用和维护
   - 集中管理所有结构化输出的 Schema

2. **详细的字段说明**
   - 每个字段都要有明确的类型说明
   - 条件约束要清晰（"当 xxx 时"）
   - 互斥关系要双向说明

3. **强调 RFC8259 合规性**
   - 在 Schema 中明确要求符合 RFC8259
   - 禁止在 JSON 外添加解释文字

4. **使用结构化的 Markdown**
   - 使用标题层次（##、###、####）
   - 使用列表和加粗突出重点
   - 使用代码块展示 JSON 结构

5. **始终检查解析结果**
   - 检查 null 值
   - 记录原始 JSON 便于调试
   - 提供合理的错误处理逻辑
