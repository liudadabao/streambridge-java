# ADR-0001：六边形内核与双重插件发现

## 状态

Accepted。

## 决策

使用以下依赖方向：

```text
应用 / RuoYi
      ↓
框架 starter → streambridge-api ← 引擎插件
                       ↑
                streambridge-core
```

- `streambridge-api` 定义领域模型和 SPI，不依赖框架、JNA 或具体引擎。
- `streambridge-core` 实现注册表、`ServiceLoader` 发现、事件与统一生命周期。
- 引擎插件依赖 API/Core，可在内部使用 JNA、HTTP 客户端或厂商 SDK。
- starter 依赖 API/Core，只负责配置和应用生命周期。
- 显式注册优先用于测试和依赖注入；`ServiceLoader` 用于零配置上手。

## 资源所有权

- 谁创建引擎，谁负责关闭。
- `StreamBridge` 跟踪自己创建的引擎并在关闭时逆序释放。
- `StreamHandle.close()` 仅关闭对应逻辑流，必须幂等。
- 原生插件负责把 Java 生命周期映射到 Native 生命周期。

## 兼容性

- API/Core/Mock/ZLM：`--release 8`。
- Spring Boot/RuoYi：`--release 17`，目标为 Spring Boot 4.1.x。
- 公共 API 遵循语义化版本；引擎能力通过 `EngineCapability` 探测。

## 独立实现边界

仅使用 ZLMediaKit 官方头文件定义的公开 ABI 名称与签名。Java 类型体系、模块划分、异常、测试、配置键和文档均独立设计。

