# MCP Client - AI Agent 对话客户端

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.0--M4-blue.svg)](https://spring.io/projects/spring-ai)
[![Java](https://img.shields.io/badge/Java-24-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Status](https://img.shields.io/badge/Status-In%20Development-red.svg)]()

**基于 MCP (Model Context Protocol) 的智能对话客户端**

[English](README_EN.md) | 简体中文

</div>

## 📖 项目简介

MCP Client 是一个基于 **Model Context Protocol (MCP)** 实现的智能 AI Agent 对话客户端。它不仅是一个简单的聊天应用，更是一个能够**自主执行复杂任务**的 AI 助手系统。

### ✨ 核心亮点

- 🤖 **AI Agent 模式**: 基于 MCP 协议实现的智能代理，可自动分析用户意图并执行多步骤任务
- 🔗 **MCP 工具集成**: 无缝集成 MCP Server 提供的各种工具，实现登录、注册、记账、数据分析等功能
- 💬 **实时通信**: 基于 WebSocket 的实时双向通信，支持流式响应
- 🧠 **会话记忆**: 支持多轮对话的上下文记忆，提供连贯的对话体验
- 🎯 **任务编排**: 智能分解复杂任务为可执行的子任务，按序执行并整合结果
- 🔧 **可扩展架构**: 基于 Spring AI 框架，易于扩展和集成新的 AI 模型

## 🏗️ 技术架构

### 技术栈

- **后端框架**: Spring Boot 3.5.0
- **AI 框架**: Spring AI 1.1.0-M4
- **AI 模型**: Google Gemini
- **通信协议**: WebSocket (STOMP)
- **MCP 协议**: Spring AI MCP Client
- **构建工具**: Maven
- **JDK 版本**: Java 24

### 架构设计

```
┌─────────────────────────────────────────────────────┐
│                   客户端应用                          │
│            (React Native / Web App)                 │
└────────────────┬────────────────────────────────────┘
                 │ WebSocket (STOMP)
┌────────────────▼────────────────────────────────────┐
│              MCP Client (本项目)                     │
│  ┌──────────────────────────────────────────────┐  │
│  │          Agent Orchestrator                   │  │
│  │  ┌────────────┐  ┌──────────────┐           │  │
│  │  │ User Chat  │  │  Work Client │           │  │
│  │  │   Client   │  │              │           │  │
│  │  └────────────┘  └──────────────┘           │  │
│  │         │                │                    │  │
│  │         ▼                ▼                    │  │
│  │   任务分析        任务执行 (with Tools)        │  │
│  └──────────────────────────────────────────────┘  │
└────────────────┬────────────────────────────────────┘
                 │ MCP Protocol
┌────────────────▼────────────────────────────────────┐
│               MCP Server                             │
│     (Ledger Server - 业务工具提供者)                 │
│  - 用户认证    - 账本管理    - 交易记录              │
│  - 数据分析    - 分类管理    - 统计查询              │
└─────────────────────────────────────────────────────┘
```

## 🚀 核心功能

### 1. AI Agent 智能编排

系统采用三阶段任务处理流程：

#### 阶段一：任务分析
```java
// 使用 userChatClient 分析用户输入
// - 理解用户真实需求
// - 可调用 MCP 工具获取上下文信息
// - 输出详细分析结果
```

#### 阶段二：任务拆解
```java
// 将分析结果转换为结构化任务指令
// - 拆分为多个可执行子任务
// - 定义每个任务的执行要求和验收标准
// - 确定任务执行顺序
```

#### 阶段三：任务执行与整合
```java
// 按序执行各个子任务
// - 调用 MCP 工具完成具体操作
// - 收集每个任务的执行结果
// - 整合所有结果返回给用户
```

### 2. MCP 工具调用

支持通过 MCP 协议调用远程工具：

```java
@McpTool(description = "创建交易记录")
public String createTransaction(
    String name, 
    BigDecimal amount, 
    Integer type,
    Long ledgerId,
    Long categoryId
) {
    // 工具实现由 MCP Server 提供
}
```

### 3. WebSocket 实时通信

- **STOMP 协议**: 基于 STOMP over WebSocket 的消息传递
- **流式响应**: 支持 AI 生成内容的实时流式传输
- **会话管理**: 基于用户 ID 的独立消息队列

### 4. 会话记忆

```java
chatClient.prompt()
    .user(request.getMessage())
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
    .call()
    .content();
```

## 📦 项目结构

```
mcp-client/
├── src/main/java/com/jim/mcpclient/
│   ├── config/                      # 配置类
│   │   ├── WebSocketConfigWithLogging.java    # WebSocket 配置
│   │   ├── UserTokenHolder.java              # Token 上下文管理
│   │   └── RawWebSocketHandler.java          # WebSocket 处理器
│   ├── model/                       # 数据模型
│   │   ├── ChatRequest.java                  # 聊天请求
│   │   ├── ChatResponse.java                 # 聊天响应
│   │   └── ...
│   ├── test/                        # 核心业务逻辑
│   │   ├── AgentOrchestrator.java           # Agent 编排器 ⭐
│   │   ├── AIWorkerResponse.java            # Worker 响应模型
│   │   └── AIOrchestratorTask.java          # 任务指令模型
│   ├── websocket/                   # WebSocket 控制器
│   │   └── ChatWebSocketController.java     # 聊天控制器
│   └── McpClientApplication.java    # 应用入口
├── src/main/resources/
│   ├── application.yml              # 应用配置
│   └── static/
│       └── test-websocket.html      # WebSocket 测试页面
└── pom.xml                          # Maven 配置
```

## 🔧 快速开始

### 前置要求

- Java 24+
- Maven 3.8+
- MCP Server (ledger-server) 运行中
- Google Gemini API Key

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd mcp-client
```

2. **配置环境**

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    google-genai:
      api-key: ${GOOGLE_GEMINI_API_KEY}  # 配置你的 API Key
    mcp:
      client:
        transports:
          ledger-api:
            http:
              url: http://localhost:8082/mcp  # MCP Server 地址
```

3. **启动 MCP Server**
```bash
# 确保 ledger-server 已启动
cd ../ledger-server
mvn spring-boot:run
```

4. **启动应用**
```bash
mvn clean install
mvn spring-boot:run
```

5. **访问测试页面**

打开浏览器访问: `http://localhost:8080/test-websocket.html`

## 💡 使用示例

### 简单对话

```javascript
// WebSocket 连接
const client = new Stomp.Client({
    brokerURL: 'ws://localhost:8080/ws',
    onConnect: () => {
        // 订阅消息
        client.subscribe('/queue/messages/' + userId, (message) => {
            console.log(JSON.parse(message.body));
        });
        
        // 发送消息
        client.publish({
            destination: '/app/chat/stream',
            body: JSON.stringify({
                userId: 'user123',
                message: '今天我花了多少钱？',
                token: 'your-auth-token'
            })
        });
    }
});
```

### AI Agent 自动执行任务

**用户输入**: "帮我记一笔午餐花费，50元"

**系统处理流程**:
1. 分析意图：用户想要创建一笔支出记录
2. 拆解任务：
   - 创建交易记录（类型：支出，金额：50，分类：餐饮）
3. 执行任务：
   - 调用 MCP 工具 `createTransaction`
4. 返回结果："已成功记录午餐支出 50 元"

## 🎯 核心类说明

### AgentOrchestrator

Agent 编排器，负责整个任务的生命周期管理：

```java
@Service
public class AgentOrchestrator {
    // 处理用户输入
    public void processUserInput(String userId, ChatRequest request)
    
    // 执行单个任务
    private AIWorkerResponse doTask(String userId, String prompt, String token)
}
```

**主要职责**:
- 用户输入分析
- 任务指令生成
- 任务执行与监控
- 结果整合与反馈

### ChatWebSocketController

WebSocket 消息处理控制器：

```java
@Controller
public class ChatWebSocketController {
    // 流式聊天接口
    @MessageMapping("/chat/stream")
    public void chatStream(@Payload ChatRequest request)
}
```

## 🔌 MCP 协议集成

### 配置 MCP Client

```yaml
spring:
  ai:
    mcp:
      client:
        transports:
          ledger-api:
            http:
              url: http://localhost:8082/mcp
```

### 使用 MCP 工具

```java
workClient.prompt()
    .toolContext(Map.of("token", token))  // 传递认证信息
    .user("执行任务指令")
    .call()
    .content();
```

## 🛠️ 开发指南

### 添加新的 AI 能力

1. 在 MCP Server 中定义新工具
2. MCP Client 自动发现并可调用

### 扩展任务编排逻辑

修改 `AgentOrchestrator.java` 中的任务处理流程：

```java
// 自定义任务分析提示词
String userAnalysisResult = userChatClient.prompt()
    .system("你的自定义分析规则")
    .user(request.getMessage())
    .call()
    .content();
```

## 📊 性能优化

- **流式响应**: 减少用户等待时间
- **并发处理**: 支持多用户同时对话
- **会话复用**: 减少模型调用次数
- **Token 管理**: ThreadLocal 防止内存泄漏

## 🐛 调试工具

### WebSocket 调试页面

访问 `http://localhost:8080/test-websocket.html` 进行实时调试

### 日志配置

```yaml
logging:
  level:
    com.jim.mcpclient: DEBUG
    org.springframework.ai: DEBUG
```

## 🚧 待开发功能

- [ ] 多模型支持 (OpenAI, Claude)
- [ ] 任务执行历史记录
- [ ] 更复杂的任务编排策略
- [ ] 工具调用统计与分析
- [ ] 用户偏好学习
- [ ] 语音输入支持

## 📝 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

- 作者: James Smith
- Email: your.email@example.com
- GitHub: [@your-username](https://github.com/your-username)

---

⭐ 如果这个项目对你有帮助，请给它一个星标！
