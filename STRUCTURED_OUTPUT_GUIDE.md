# Spring AI 结构化输出优化指南

## 问题分析

### 原始问题
当 `multiRound = false` 时，`complexTaskInstruction` 仍然有数据，这违反了业务逻辑：
- `multiRound = false` → 应该只有 `simpleTaskResult` 有值
- `multiRound = true` → 应该只有 `complexTaskInstruction` 有值

### 根本原因
AI 模型需要明确的字段级别的约束和描述，仅靠代码注释是不够的。

## 解决方案

### 1. 使用 Jackson 注解控制字段行为

Spring AI 的 `BeanOutputConverter` 会根据以下 Jackson 注解生成 JSON Schema：

#### 核心注解

```java
// 1. @JsonPropertyDescription - 字段的详细描述（最重要！）
@JsonPropertyDescription("""
    是否需要多轮交互处理：
    - true: 复杂任务，需要拆分为多个子任务分步执行，此时 complexTaskInstruction 必须有值，simpleTaskResult 必须为 null
    - false: 简单任务，可直接处理完成，此时 simpleTaskResult 必须有值，complexTaskInstruction 必须为 null 或空列表
    """)
boolean multiRound

// 2. @JsonProperty(required = true) - 标记必填字段
@JsonProperty(required = true)
boolean success

// 3. @JsonPropertyOrder - 控制字段顺序（影响 AI 理解优先级）
@JsonPropertyOrder({"multiRound", "simpleTaskResult", "complexTaskInstruction"})
public record AIOrchestratorTask(...)
```

### 2. 字段描述的最佳实践

#### ✅ 好的描述
```java
@JsonPropertyDescription("""
    简单任务的执行结果。
    规则：仅当 multiRound = false 时此字段必须有值，否则必须为 null。
    内容：直接可返回给用户的完整答案。
    """)
String simpleTaskResult
```

**特点：**
- 明确说明字段用途
- 清晰的条件约束（"仅当 xxx 时"）
- 具体的值要求

#### ❌ 差的描述
```java
// AI简单任务的执行结果。仅在 multiRound 为 false 时有值
String simpleTaskResult
```

**问题：**
- 只有代码注释，AI 模型看不到
- 描述不够明确
- 缺少强制性约束

### 3. 互斥字段的处理技巧

对于 `simpleTaskResult` 和 `complexTaskInstruction` 这种互斥关系：

```java
@JsonPropertyDescription("""
    简单任务的执行结果。
    规则：仅当 multiRound = false 时此字段必须有值，否则必须为 null。
    内容：直接可返回给用户的完整答案。
    """)
String simpleTaskResult,

@JsonPropertyDescription("""
    复杂任务的子任务指令列表。
    规则：仅当 multiRound = true 时此字段必须有值（非空列表），否则必须为 null 或空列表。
    内容：包含多个子任务，每个子任务包含执行指令和验收标准。
    """)
List<ComplexTaskInstruction> complexTaskInstruction
```

**关键点：**
- 在两个字段的描述中都明确说明互斥关系
- 使用 "仅当 xxx 时" 的条件句式
- 说明非条件情况下应该是什么值（null 或空列表）

### 4. 嵌套对象的字段顺序

```java
@JsonPropertyOrder({"instruction", "needValidation", "validation"})
public record ComplexTaskInstruction(...)
```

**原因：**
- 最重要的字段放在前面（instruction）
- 控制字段放中间（needValidation）
- 依赖字段放最后（validation 依赖于 needValidation）
- AI 模型按顺序理解，顺序影响生成质量

### 5. 条件必填字段的处理

```java
@JsonPropertyDescription("""
    该子任务的验收标准。
    规则：当 needValidation = true 时必须提供明确的验收标准，否则可为空。
    要求：具体、可量化、可验证，明确说明什么情况算完成。
    """)
String validation
```

**技巧：**
- 明确条件："当 xxx = true 时必须xxx"
- 说明非条件情况："否则可为空"
- 提供质量要求："具体、可量化、可验证"

## 生成的 JSON Schema 示例

Spring AI 会将这些注解转换为 JSON Schema 并发送给 AI 模型：

```json
{
  "type": "object",
  "properties": {
    "multiRound": {
      "type": "boolean",
      "description": "是否需要多轮交互处理：\n- true: 复杂任务，需要拆分为多个子任务分步执行，此时 complexTaskInstruction 必须有值，simpleTaskResult 必须为 null\n- false: 简单任务，可直接处理完成，此时 simpleTaskResult 必须有值，complexTaskInstruction 必须为 null 或空列表"
    },
    "simpleTaskResult": {
      "type": "string",
      "description": "简单任务的执行结果。\n规则：仅当 multiRound = false 时此字段必须有值，否则必须为 null。\n内容：直接可返回给用户的完整答案。"
    },
    "complexTaskInstruction": {
      "type": "array",
      "description": "复杂任务的子任务指令列表。\n规则：仅当 multiRound = true 时此字段必须有值（非空列表），否则必须为 null 或空列表。\n内容：包含多个子任务，每个子任务包含执行指令和验收标准。"
    }
  },
  "required": ["multiRound"]
}
```

## 效果对比

### 优化前
```json
{
  "multiRound": false,
  "simpleTaskResult": "这是答案",
  "complexTaskInstruction": [
    {"instruction": "步骤1", "needValidation": false}
  ]
}
```
❌ 违反了业务规则

### 优化后
```json
{
  "multiRound": false,
  "simpleTaskResult": "这是答案",
  "complexTaskInstruction": null
}
```
✅ 符合业务规则

或者复杂任务：
```json
{
  "multiRound": true,
  "simpleTaskResult": null,
  "complexTaskInstruction": [
    {
      "instruction": "步骤1：查询数据库",
      "needValidation": true,
      "validation": "必须返回至少10条记录"
    }
  ]
}
```
✅ 符合业务规则

## 最佳实践总结

1. **@JsonPropertyDescription 是关键**
   - 这是 AI 模型能看到的唯一约束
   - 使用多行字符串详细描述
   - 明确说明条件和规则

2. **使用 @JsonPropertyOrder**
   - 重要字段在前
   - 控制字段在中
   - 依赖字段在后

3. **@JsonProperty(required = true)**
   - 标记关键字段
   - 防止 AI 遗漏必填字段

4. **描述要具体、明确**
   - 使用"必须"、"仅当"、"否则"等强制性词汇
   - 说明具体的值要求
   - 提供正反两方面的说明

5. **互斥字段要双向说明**
   - 在两个互斥字段中都说明关系
   - 明确各自的生效条件
   - 说明非生效时应该是什么值

## 参考文档

- [Spring AI Structured Output Converter](https://docs.spring.io/spring-ai/reference/1.1/api/structured-output-converter.html)
- [Jackson JsonPropertyDescription](https://fasterxml.github.io/jackson-annotations/javadoc/2.13/com/fasterxml/jackson/annotation/JsonPropertyDescription.html)
- [Jackson JsonPropertyOrder](https://fasterxml.github.io/jackson-annotations/javadoc/2.13/com/fasterxml/jackson/annotation/JsonPropertyOrder.html)
