# RuoYi-Vue 后端接入文件

目标版本：RuoYi-Vue 3.9.2（Java 17、Spring Boot 4.1.0）。

1. 按 `docs/integrations/ruoyi-vue.md` 添加 starter 与引擎依赖。
2. 将 `MediaEngineController.java` 复制到 `ruoyi-admin` 对应包。
3. 执行前端接入包中的 `sql/menu.sql`，并为角色授权。
4. 生产环境使用 ZLM 插件；联调阶段推荐 Mock 插件。

Controller 由应用持有，便于项目自行决定权限、审计和 API 暴露范围。

