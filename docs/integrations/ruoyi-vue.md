# RuoYi-Vue 3.9.2 集成

本适配基线来自用户提供的 `RuoYi-Vue-master.zip`：Java 17、Spring Boot 4.1.0。StreamBridge 不修改若依公共模块，也不会自动注册未授权 Controller。

## 1. 添加依赖

在 `ruoyi-admin/pom.xml` 添加：

```xml
<dependency>
    <groupId>io.github.streambridge</groupId>
    <artifactId>streambridge-ruoyi-vue-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>io.github.streambridge</groupId>
    <artifactId>streambridge-engine-zlm</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

开发期可以把第二个依赖替换为 `streambridge-engine-mock`。

## 2. 配置

```yaml
streambridge:
  enabled: true
  engine: zlm-embedded
  auto-start: true
  options:
    zlm.library: D:/media/mk_api.dll
    zlm.log.level: 1
    zlm.log.mask: 1
    zlm.http.port: 7788
    zlm.rtsp.port: 554
    zlm.rtmp.port: 1935
```

StreamBridge 不会在线下载动态库。生产环境应由部署系统提供并校验动态库。

## 3. 在若依 Controller 中使用

```java
@RestController
@RequestMapping("/media/engine")
public class MediaEngineController {
    private final RuoyiStreamBridgeFacade streamBridge;

    public MediaEngineController(RuoyiStreamBridgeFacade streamBridge) {
        this.streamBridge = streamBridge;
    }

    @PreAuthorize("@ss.hasPermi('media:engine:query')")
    @GetMapping("/status")
    public AjaxResult status() {
        return AjaxResult.success(streamBridge.status());
    }
}
```

权限标识、审计注解和 URL 均由若依应用自己决定，starter 不绕过现有安全体系。

## 4. Mock 开发配置

```yaml
streambridge:
  engine: mock
```

Mock 引擎不监听端口、不加载 Native 库，适合 Controller、权限和业务编排测试。

