# MCP Server Token 传递实现方案

## 📋 需求说明

将前端通过 WebSocket 传来的 token 传递给 MCP server,使 MCP server 能够获取用户信息进行权限控制。

## 🎯 实现方案

采用 **HTTP Request Customizer** 方案,通过 Spring AI MCP 提供的扩展点在 HTTP 层面添加认证 header。

## 🔄 完整流程图

```
前端 WebSocket 消息 (包含 token)
        ↓
ChatWebSocketController 接收
        ↓
UserTokenHolder.setToken(token)  ← 存入 ThreadLocal
        ↓
ChatClient.prompt().call()
        ↓
Spring AI 调用 MCP Tool
        ↓
AuthenticationTransportContextProvider.getContext()
        ↓
UserTokenHolder.getToken()  ← 从 ThreadLocal 获取
        ↓
返回 TransportContext(包含 token)
        ↓
TokenAwareMcpHttpClientRequestCustomizer.customize()
        ↓
从 context 获取 token,添加到 HTTP header
        ↓
HTTP Request 发送到 MCP Server
        ↓
MCP Server 从 header 中获取 token,验证用户身份
```

## 📦 核心组件

### 1. UserTokenHolder (ThreadLocal 存储)
```java
// 位置: config/UserTokenHolder.java
// 作用: 使用 ThreadLocal 存储当前请求的 token
// 调用: WebSocket Controller 中设置,TransportContextProvider 中获取
```

### 2. AuthenticationTransportContextProvider (上下文提供器)
```java
// 位置: config/AuthenticationTransportContextProvider.java
// 作用: 实现 TransportContextProvider 接口,在 MCP 调用时提供 token
// 原理: 从 UserTokenHolder 获取 token,封装到 TransportContext
```

### 3. TokenAwareMcpHttpClientRequestCustomizer (HTTP 请求定制器)
```java
// 位置: config/TokenAwareMcpHttpClientRequestCustomizer.java
// 作用: 实现 McpSyncHttpClientRequestCustomizer 接口,添加 HTTP header
// 原理: 从 TransportContext 获取 token,添加到 Authorization header
```

### 4. McpClientConfiguration (配置类)
```java
// 位置: config/McpClientConfiguration.java
// 作用: 注册 McpSyncClientCustomizer,将 TransportContextProvider 注入到 MCP Client
// 关键: 如果不配置这个,整个机制不会生效!
```

## 🔧 使用方式

### 前端调用示例

```javascript
// WebSocket 发送消息时携带 token
const message = {
    userId: "user123",
    message: "查询我的数据",
    token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."  // JWT token
};

stompClient.send("/app/chat/stream", {}, JSON.stringify(message));
```

### MCP Server 接收 Header

```python
# MCP Server 端从 request header 中获取 token
@mcp.tool()
async def get_user_data(request: Request):
    # 获取认证 token
    token = request.headers.get("Authorization")  # "Bearer eyJ..."
    # 或者使用自定义 header
    # token = request.headers.get("X-User-Token")
    
    # 验证 token,获取用户信息
    user_info = verify_token(token)
    
    # 执行业务逻辑
    return {"data": f"User {user_info.user_id} data"}
```

## ⚠️ 重要注意事项

### 1. ThreadLocal 清理
```java
// ✅ 正确: 必须在 doFinally 中清理
.doFinally(__ -> UserTokenHolder.clear())

// ❌ 错误: 如果只在 try-catch 清理,异步场景可能泄漏
try {
    // ...
} finally {
    UserTokenHolder.clear();  // 异步流式传输时可能不会执行
}
```

### 2. 同步 vs 异步
- 当前实现使用 `McpSyncHttpClientRequestCustomizer` (同步)
- 如果需要异步支持,使用 `McpAsyncHttpClientRequestCustomizer`
- 两者接口签名类似,只是返回类型不同 (Mono vs 直接返回)

### 3. Header 名称选择
```java
// 方式1: 标准 Authorization header (推荐)
requestBuilder.header("Authorization", "Bearer " + token);

// 方式2: 自定义 header
requestBuilder.header("X-User-Token", token);
requestBuilder.header("X-Auth-Token", token);
```

## 🧪 测试验证

### 1. 日志验证
在各组件添加日志:
```java
// UserTokenHolder
log.info("设置 Token: {}", token.substring(0, 10) + "...");

// AuthenticationTransportContextProvider
log.info("获取 Token 用于 MCP 请求: {}", token != null);

// TokenAwareMcpHttpClientRequestCustomizer
log.info("添加 Authorization header: Bearer {}", token.substring(0, 10) + "...");
```

### 2. MCP Server 端验证
```python
@mcp.tool()
async def test_auth(request: Request):
    auth_header = request.headers.get("Authorization")
    return {"received_token": auth_header is not None}
```

## 🔄 备选方案对比

| 方案 | 实现方式 | 优点 | 缺点 | 推荐度 |
|------|---------|------|------|--------|
| **HTTP Customizer** (当前) | TransportContext + Request Customizer | ✅ 符合 HTTP 标准<br>✅ 代码清晰<br>✅ 职责分离 | - | ⭐⭐⭐⭐⭐ |
| ToolContext + Meta | 通过 ToolContext 传递到 meta | ✅ 纯 Spring AI 方案 | ❌ meta 用途不明确<br>❌ MCP server 需要从 meta 解析 | ⭐⭐ |

## 📚 相关源码参考

- `SyncMcpToolCallback.java` (L127-140): Tool 调用入口
- `StreamableHttpHttpClientTransportAutoConfiguration.java` (L109-120): HTTP Customizer 配置
- `HttpClientStreamableHttpTransport`: HTTP 传输层实现

## 🚀 后续优化建议

1. **Token 缓存**: 如果 token 不常变化,可以考虑缓存
2. **Token 刷新**: 添加 token 过期检测和自动刷新机制
3. **安全增强**: 
   - HTTPS 强制
   - Token 加密存储
   - 请求签名
4. **监控**: 添加 token 使用情况监控和告警

---

## 📞 联系方式

如有问题,请参考:
- Spring AI MCP 文档: https://docs.spring.io/spring-ai/reference/
- MCP 协议规范: https://modelcontextprotocol.io/
