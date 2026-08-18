# Contributing

1. 先在 `docs/specs` 描述行为和验收条件。
2. 对公共 API 或依赖方向的修改补充 ADR。
3. 先提交失败测试，再实现最小变更。
4. 不在 API 模块引入框架或 Native 类型。
5. 不复制第三方 Java 封装代码；新增 Native ABI 前应链接对应的上游官方头文件。
6. 提交前运行 `mvn verify`。

