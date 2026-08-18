-- RuoYi-Vue 3.9.2 / MySQL
-- 先安装后端 Controller，再执行本脚本并为目标角色授权。

SET @streambridge_menu_id = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);

INSERT INTO sys_menu VALUES(
    @streambridge_menu_id,
    '流媒体引擎',
    '2',
    '7',
    'mediaEngine',
    'media/engine/index',
    '',
    '',
    1,
    0,
    'C',
    '0',
    '0',
    'media:engine:query',
    'monitor',
    'admin',
    SYSDATE(),
    '',
    NULL,
    'StreamBridge 流媒体引擎监控'
);

SET @streambridge_edit_id = (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu);
INSERT INTO sys_menu VALUES(
    @streambridge_edit_id,
    '流媒体引擎操作',
    @streambridge_menu_id,
    '1',
    '',
    '',
    '',
    '',
    1,
    0,
    'F',
    '0',
    '0',
    'media:engine:edit',
    '#',
    'admin',
    SYSDATE(),
    '',
    NULL,
    'StreamBridge 拉推流、录像、RTP 与关流操作'
);
