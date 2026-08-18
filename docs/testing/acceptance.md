# 验收测试清单

## 自动化

- 插件注册拒绝空 ID 和重复 ID。
- `ServiceLoader` 能发现 Mock 插件。
- 引擎生命周期状态按 `NEW → STARTING → RUNNING → STOPPING → STOPPED` 转换。
- 启动失败进入 `FAILED`，并保留原始异常原因。
- Mock 拉流、查询、关闭和事件通知形成闭环。
- Mock 可验证录像、RTP、WebRTC、媒体输入、播放器和运行时配置扩展。
- `StreamBridge.close()` 关闭所有已创建引擎。
- Spring Boot 自动配置可以选择 Mock 引擎并随容器启停。
- 非 RuoYi 应用不创建 RuoYi 门面；RuoYi 应用自动创建。
- ZLM 插件通过假的 Native Client 验证初始化、端口启动和停止调用顺序。
- ZLM 插件通过假的 Native Client 验证流查询、拉流和录像委派。
- RuoYi-Vue3 API 路径、权限标识和后端 Controller 契约一致。

## 发布前手工验证

- Windows x86-64 动态库加载和退出。
- Linux x86-64 动态库加载和 SIGTERM 退出。
- RuoYi-Vue 3.9.2 添加两个依赖与 YAML 后正常启动。
- RuoYi-Vue3 3.9.2 页面能够通过 Vite 6.4.1 生产构建。
- RuoYi-Vue 3.9.2 七模块 reactor 能编译扩展后的管理 Controller。
- 未配置动态库时错误信息包含平台、架构和查找路径。
