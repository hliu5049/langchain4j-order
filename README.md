# LangChain4j Order Agent

基于 LangChain4j 和 Spring Boot 的智能订单管理系统，支持多Agent协作和会话记忆管理。

## 功能特性

- 🤖 **多Agent架构**: 包含创建、查询、更新、取消订单的专门Agent
- 🧠 **智能路由**: TriageAgent自动识别用户意图并分发到对应Agent
- 💾 **会话记忆**: 支持会话级别和Agent级别的对话记忆管理
- 🔄 **跨Agent上下文**: 支持Agent间的上下文传递
- 🌐 **REST API**: 提供完整的HTTP接口
- 🎨 **Web界面**: 内置测试页面，方便交互测试

## 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+
- OpenAI API Key (可选，用于实际AI功能)

### 2. 配置

#### 设置OpenAI API Key (推荐)

```bash
# Windows
set OPENAI_API_KEY=your-api-key-here

# Linux/Mac
export OPENAI_API_KEY=your-api-key-here
```

#### 或修改配置文件

编辑 `src/main/resources/application.yml`:

```yaml
openai:
  api:
    key: your-api-key-here
    base-url: https://api.openai.com/v1  # 或其他兼容的API地址
  model:
    name: gpt-3.5-turbo  # 或其他模型
```

### 3. 运行项目

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run
```

或者:

```bash
# 打包运行
mvn clean package
java -jar target/langchain4j-order-0.0.1-SNAPSHOT.jar
```

### 4. 测试

项目启动后，访问:

- **Web测试界面**: http://localhost:8080/chat.html
- **健康检查**: http://localhost:8080/api/chat/health

## API 接口

### 基础接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/chat/message` | 发送消息（自动生成会话ID） |
| POST | `/api/chat/message/{sessionId}` | 发送消息（指定会话ID） |
| GET | `/api/chat/session/{sessionId}/status` | 获取会话状态 |
| DELETE | `/api/chat/session/{sessionId}` | 清除会话记忆 |
| GET | `/api/chat/health` | 健康检查 |

### 请求示例

#### 发送消息

```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "我想创建一个新订单"}'
```

#### 响应示例

```json
{
  "success": true,
  "response": "好的，我来帮您创建订单。请告诉我您需要订购什么商品？",
  "sessionId": "session-1703123456789-abc123",
  "timestamp": 1703123456789
}
```

## 测试示例

### 创建订单
```
用户: 我想创建一个新订单
Agent: 好的，我来帮您创建订单。请告诉我您需要订购什么商品？

用户: 我要买2个iPhone 15
Agent: 好的，我为您创建一个iPhone 15的订单，数量为2个。订单创建成功！
```

### 查询订单
```
用户: 查询我的订单
Agent: 我来帮您查询订单信息。请问您要查询哪个订单？可以提供订单ID吗？

用户: 查询订单123
Agent: 正在查询订单ID为123的详细信息...
```

### 更新订单
```
用户: 我想修改订单信息
Agent: 好的，我来帮您更新订单。请告诉我您要修改哪个订单，以及要修改什么内容？
```

### 取消订单
```
用户: 取消订单
Agent: 我来帮您取消订单。请提供要取消的订单ID。
```

## 架构说明

### Agent架构

```
TriageAgent (路由Agent)
├── CreateOrderAgent (创建订单)
├── QueryOrderAgent (查询订单)
├── UpdateOrderAgent (更新订单)
└── CancelOrderAgent (取消订单)
```

### 核心组件

- **ChatMemoryManager**: 统一的对话记忆管理
- **ChatMemoryConfig**: 记忆配置管理
- **OrderService**: 订单业务逻辑
- **各种Tool**: 具体的订单操作工具

### 记忆管理

- **会话级记忆**: 每个用户会话独立的对话历史
- **Agent级记忆**: 每个Agent独立的对话历史
- **跨Agent上下文**: 支持Agent间的上下文传递
- **自动清理**: 定时清理过期会话

## 配置说明

### Chat Memory 配置

```yaml
chat:
  memory:
    default-max-messages: 20          # 默认最大消息数
    session-max-messages: 50          # 会话最大消息数
    agent-max-messages: 30            # Agent最大消息数
    enable-cross-agent-context: true  # 启用跨Agent上下文
    session-timeout-minutes: 30       # 会话超时时间(分钟)
    enable-session-persistence: false # 启用会话持久化
    memory-cleanup-interval-minutes: 60 # 内存清理间隔(分钟)
    max-active-sessions: 100          # 最大活跃会话数
```

## 开发说明

### 添加新的Agent

1. 创建Agent类，继承基础模式
2. 实现对应的Tool类
3. 在TriageAgent中添加路由逻辑
4. 更新ChatMemoryManager支持新Agent

### 自定义配置

可以通过修改 `application.yml` 或设置环境变量来自定义配置。

## 故障排除

### 常见问题

1. **编译错误**: 确保Java版本为17+
2. **启动失败**: 检查端口8080是否被占用
3. **AI响应异常**: 检查OpenAI API Key配置
4. **内存不足**: 调整JVM参数或减少最大会话数

### 日志查看

项目使用SLF4J日志框架，可以通过修改 `application.yml` 中的日志级别来调整日志输出。

## 许可证

本项目采用 MIT 许可证。