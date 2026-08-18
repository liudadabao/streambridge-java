# RuoYi-Vue3 前端接入包

目标基线来自用户提供的 RuoYi-Vue3 3.9.2：Vue 3.5.26、Vite 6.4.1、Element Plus 2.13.1。

## 复制文件

- `src/api/media/engine.js` → 若依前端同路径。
- `src/views/media/engine/index.vue` → 若依前端同路径。
- 执行 `sql/menu.sql` 创建菜单，再为角色授权。

页面只依赖以下稳定 REST 契约：

| 请求 | 权限 | `data` |
| --- | --- | --- |
| `GET /media/engine/status` | `media:engine:query` | `{ engine, state, capabilities }` |
| `GET /media/engine/list` | `media:engine:query` | `[{ virtualHost, application, stream, sourceUri, readerCount }]` |
| `GET /media/engine/operations` | `media:engine:query` | 托管操作列表 |
| `POST /media/engine/pull` | `media:engine:edit` | 创建拉流代理 |
| `POST /media/engine/push` | `media:engine:edit` | 创建推流代理 |
| `DELETE /media/engine/stream` | `media:engine:edit` | 强制关闭流 |
| `POST /media/engine/recording/{start|stop}` | `media:engine:edit` | MP4/HLS 录像 |
| `POST /media/engine/rtp/open` | `media:engine:edit` | RTP/GB28181 接收 |
| `POST /media/engine/webrtc/answer` | `media:engine:edit` | WebRTC SDP 交换 |

若依的请求封装负责 Token、错误提示和 `code` 校验。前端不直接接触 Native 配置或动态库路径。
