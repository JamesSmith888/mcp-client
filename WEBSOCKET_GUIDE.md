# WebSocket 聊天后端实现说明

## 概述
这个后端实现了基于 Spring Boot WebSocket 的聊天系统，支持 AI 流式响应和会话记忆功能。

## 架构说明

### 1. WebSocket 配置
- **端点**: `/ws`
- **协议**: STOMP over WebSocket
- **消息前缀**: `/app` (客户端发送), `/queue` (服务器推送)
- **支持**: SockJS (用于不支持 WebSocket 的浏览器)

### 2. 消息流程

#### 客户端 → 服务器
```
发送到: /app/chat 或 /app/chat/stream
消息格式: ChatRequest
{
  "userId": "user123",
  "message": "你好",
  "messageId": "msg-001"
}
```

#### 服务器 → 客户端
```
订阅: /queue/messages/{userId}
消息格式: ChatResponse
{
  "messageId": "msg-001",
  "content": "你好！",
  "type": "CHUNK",
  "isFinal": false,
  "timestamp": 1234567890
}
```

### 3. 消息类型

- **START**: 开始流式传输
- **CHUNK**: 内容片段（流式传输中）
- **END**: 流式传输结束
- **ERROR**: 发生错误

### 4. API 端点

#### 非流式聊天
- **路径**: `/app/chat`
- **说明**: 获取完整响应后分块模拟流式发送

#### 真正的流式聊天
- **路径**: `/app/chat/stream`
- **说明**: 使用 AI 的流式 API，实时推送响应片段

## 前端连接示例

### JavaScript (使用 SockJS + Stomp)

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

// 1. 创建连接
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// 2. 连接到服务器
stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // 3. 订阅消息
    const userId = 'user123';
    stompClient.subscribe(`/queue/messages/${userId}`, function(message) {
        const response = JSON.parse(message.body);
        console.log('Received:', response);
        
        switch(response.type) {
            case 'START':
                console.log('Stream started');
                break;
            case 'CHUNK':
                console.log('Content:', response.content);
                break;
            case 'END':
                console.log('Stream ended');
                break;
            case 'ERROR':
                console.error('Error:', response.error);
                break;
        }
    });
});

// 4. 发送消息
function sendMessage(message) {
    stompClient.send('/app/chat/stream', {}, JSON.stringify({
        userId: 'user123',
        message: message,
        messageId: 'msg-' + Date.now()
    }));
}

// 5. 断开连接
function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
}
```

### React Native (使用 stompjs)

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
    brokerURL: 'ws://localhost:8080/ws',
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    onConnect: () => {
        console.log('Connected');
        
        // 订阅消息
        client.subscribe('/queue/messages/user123', (message) => {
            const response = JSON.parse(message.body);
            handleMessage(response);
        });
    },
    onStompError: (frame) => {
        console.error('STOMP error', frame);
    }
});

// 激活连接
client.activate();

// 发送消息
function sendMessage(text) {
    client.publish({
        destination: '/app/chat/stream',
        body: JSON.stringify({
            userId: 'user123',
            message: text,
            messageId: 'msg-' + Date.now()
        })
    });
}
```

## 功能特性

### 1. 会话记忆
- 使用 `userId` 作为会话标识
- 自动保存和加载历史对话
- 支持多用户并发聊天

### 2. 流式响应
提供两种模式：
- **模拟流式**: 完整响应后分块发送
- **真实流式**: AI 实时生成内容并推送

### 3. 错误处理
- 自动捕获异常
- 发送 ERROR 类型消息给客户端
- 包含错误详情

### 4. 消息追踪
- 使用 `messageId` 追踪每条消息
- 时间戳记录消息发送时间

## 配置说明

### application.yml
```yaml
server:
  port: 8080

spring:
  ai:
    google:
      genai:
        api-key: YOUR_API_KEY
```

### 生产环境注意事项

1. **CORS 配置**: 在 `CorsConfig.java` 和 `WebSocketConfig.java` 中将 `allowedOriginPatterns("*")` 改为具体域名

```java
.setAllowedOriginPatterns("https://yourdomain.com")
```

2. **安全认证**: 添加 Spring Security 进行用户认证

3. **消息队列**: 大规模部署时考虑使用 RabbitMQ 或 Redis 作为消息代理

## 测试

### 使用 Postman 测试 WebSocket
1. 创建新的 WebSocket Request
2. URL: `ws://localhost:8080/ws`
3. 发送 CONNECT 帧
4. 订阅: `/queue/messages/user123`
5. 发送消息到: `/app/chat/stream`

### 日志监控
后端会输出详细日志，包括：
- WebSocket 连接状态
- 消息收发记录
- AI 响应内容

## 故障排查

### 常见问题

1. **连接失败**
   - 检查端口是否被占用
   - 确认防火墙设置
   - 验证 CORS 配置

2. **消息未收到**
   - 确认订阅路径正确: `/queue/messages/{userId}`
   - 检查 userId 是否匹配
   - 查看后端日志

3. **流式响应中断**
   - 检查网络稳定性
   - 增加超时时间
   - 查看 AI API 配额

## 扩展功能建议

1. **添加认证**: 集成 JWT 或 OAuth2
2. **消息持久化**: 保存聊天记录到数据库
3. **文件上传**: 支持图片和文档分析
4. **群聊功能**: 使用 `/topic` 实现广播
5. **消息撤回**: 添加撤回和编辑功能
6. **在线状态**: 显示用户在线/离线状态

## 性能优化

1. **连接池**: 配置数据库和 HTTP 连接池
2. **异步处理**: 使用 `@Async` 处理耗时操作
3. **缓存**: Redis 缓存频繁查询的数据
4. **负载均衡**: 多实例部署 + Nginx

## 相关文件

- `WebSocketConfig.java`: WebSocket 配置
- `ChatWebSocketController.java`: 消息处理控制器
- `ChatRequest.java`: 请求模型
- `ChatResponse.java`: 响应模型
- `CorsConfig.java`: 跨域配置

## 启动应用

```bash
# 编译项目
mvn clean package

# 运行应用
java -jar target/mcp-client-0.0.1-SNAPSHOT.jar

# 或直接运行
mvn spring-boot:run
```

访问: `http://localhost:8080`

WebSocket 端点: `ws://localhost:8080/ws`
