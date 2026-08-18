# StreamBridge

StreamBridge 是一个框架无关、完全可插拔的 Java 流媒体引擎集成层。业务代码只依赖稳定的 Java API；ZLMediaKit、远程媒体服务、Mock 引擎及 Spring/RuoYi 集成都作为可替换插件存在。

> 当前为 `0.1.0-SNAPSHOT` 开发版。ZLMediaKit 插件已覆盖服务生命周期、全局流事件、流查询/关闭、拉推代理、HLS/MP4 录像、RTP/GB28181、WebRTC SDP、媒体帧输入、播放器/帧回调和运行时配置。

## 设计目标

- 核心 API 保持 Java 8 字节码兼容，不依赖任何应用框架。
- 插件可显式注册，也可通过 Java `ServiceLoader` 自动发现。
- 原生能力与业务 API 隔离，避免在业务代码中传播 JNA 类型和 C 风格资源句柄。
- 生命周期、错误信息和资源释放可测试、可观测。
- RuoYi-Vue 适配独立发布，不污染核心模块。

## 模块

| 模块 | 作用 | Java 基线 |
| --- | --- | --- |
| `streambridge-api` | 稳定 API、领域对象和插件 SPI | 8 |
| `streambridge-core` | 插件注册、发现、生命周期与事件基础设施 | 8 |
| `streambridge-engine-mock` | 测试和本地开发引擎 | 8 |
| `streambridge-engine-zlm` | ZLMediaKit 嵌入式原生引擎插件 | 8 |
| `streambridge-spring-boot-autoconfigure` | Spring Boot 自动配置 | 17 |
| `streambridge-spring-boot-starter` | 通用 Spring Boot starter | 17 |
| `streambridge-ruoyi-vue-starter` | RuoYi-Vue 3.9.2 适配门面 | 17 |
| `streambridge-example-plain-java` | 无框架使用示例 | 8 |

`integrations/ruoyi-vue3` 提供与用户指定的 RuoYi-Vue3 3.9.2 匹配的 API 文件、Vue 页面和菜单 SQL。

能力不是集中在一个巨型接口中。调用方通过 `engine.extension(RecordingOperations.class)` 等方式按需获取小型扩展，未使用的能力不会进入业务耦合面。完整列表见 [能力矩阵](docs/architecture/capability-matrix.md)。

## 纯 Java 快速开始

```java
try (StreamBridge bridge = StreamBridges.builder().discoverPlugins(true).build();
     StreamEngine engine = bridge.open("mock", EngineConfiguration.empty())) {
    StreamHandle handle = engine.pull(PullRequest.builder()
        .sourceUri("rtsp://example.test/live/camera-1")
        .target(StreamKey.of("__defaultVhost__", "live", "camera-1"))
        .build());
    System.out.println(handle.key());
}
```

## 构建

完整 reactor 包含 Spring Boot 4/RuoYi 适配器，因此构建需要 JDK 17；API、Core、Mock 与 ZLM 插件产物仍兼容 Java 8。

```powershell
.\mvnw.cmd verify
```

Linux/macOS：

```bash
./mvnw verify
```

## RuoYi-Vue

用户提供的 RuoYi-Vue `3.9.2` 使用 Java 17 与 Spring Boot `4.1.0`；RuoYi-Vue3 前端使用 Vue 3.5.26、Vite 6.4.1 与 Element Plus 2.13.1。后端步骤见 [RuoYi-Vue 集成指南](docs/integrations/ruoyi-vue.md)，前端文件见 [`integrations/ruoyi-vue3`](integrations/ruoyi-vue3)。

## 开发工作流

本项目采用 SDD + TDD：

1. 需求与非目标写入 `docs/specs`。
2. 不可逆架构选择写入 `docs/architecture`。
3. 先补验收场景或失败测试，再实现最小代码。
4. Maven 全量测试通过后才完成变更。

## 独立实现与致谢

StreamBridge 的 Java 架构、API、文档与测试为独立设计。ZLMediaKit 的公开 C ABI 名称必须与上游保持一致；其余实现不复制参考项目源码。详见 [NOTICE](NOTICE)。

## License

Apache License 2.0。
