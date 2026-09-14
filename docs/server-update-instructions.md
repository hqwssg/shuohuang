# 当前版本服务器增量更新

## 本地生成内容

运行 `powershell -ExecutionPolicy Bypass -File tools/server-update/build-update.ps1`。
脚本构建三个后端（执行 Maven 测试）和三个生产前端，生成：

- `deploy-package/server-update-TIMESTAMP.tar.gz`：可上传增量包。
- 同名 `.tar.gz.sha256`：压缩包 SHA256 校验。
- `deploy-package/server-update-TIMESTAMP/`：展开目录，含脚本、SQL、程序及逐文件校验。
- `deploy-package/build-logs-TIMESTAMP/`：六个构建日志。

包含主系统、报告模块/Python 渲染器、模型系统、GoView，以及统一权限和日志更新。
不包含本地 `goview.db`、上传文件、数据库全量初始化、报告输出及演示核算数据。
这是已有三容器的更新包，不是空服务器首次安装包。

## 1. 上传（本机 PowerShell）

将下方包名替换为已生成的实际包名，端口替换为**宿主机** SSH 端口。
端口 53220 是 Web 容器 SSH，通常不能在其中管理另外两个容器。

```powershell
$Archive = "D:\idea库\RuoYi-Cloud\deploy-package\server-update-TIMESTAMP.tar.gz"
scp -P <宿主机SSH端口> $Archive "$Archive.sha256" root@202.115.17.253:/opt/
ssh -p <宿主机SSH端口> root@202.115.17.253
```

## 2. 检查（宿主机 Bash）

```bash
cd /opt
sha256sum -c server-update-TIMESTAMP.tar.gz.sha256
tar -xzf server-update-TIMESTAMP.tar.gz
cd /opt/server-update-TIMESTAMP
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
export DB_CONTAINER=ruoyi-db WEB_CONTAINER=ruoyi-web SCREEN_CONTAINER=ruoyi-screen
export WEB_HOST_PORT=53221 SCREEN_HOST_PORT=53231
export WEB_PUBLIC_URL=http://202.115.17.253:53221
export SCREEN_PUBLIC_URL=http://202.115.17.253:53231
read -rsp 'MariaDB root密码: ' MYSQL_ROOT_PASSWORD; echo
export MYSQL_ROOT_PASSWORD
bash deploy-update.sh --check
```

如果容器名称/映射不同，以 `docker ps` 的实际结果为准修改环境变量。
本次前端包使用公网大屏端口 53231；如果实际大屏端口不同，还需要用实际
`VUE_APP_GOVIEW_URL` 重新构建主前端，不能只改服务器启动环境变量。
多网络容器可显式指定 `DB_HOST` 与 `WEB_INTERNAL_HOST` 为互通的容器内网 IP。

## 3. 执行更新

先保证 Web 容器有 Java 17+、Redis、Nginx、Python 3.10+。
报告需要 Python venv/pip、LibreOffice 和中文字体；脚本尝试安装缺少的运行依赖，
容器必须能够访问软件源和 Python 包源。默认 Python 太旧时，先安装新版，再指定：

```bash
# 仅在已经安装 python3.11 时使用
export REPORT_PYTHON=python3.11
```

权限清理会重排角色、清除旧示例部门/用户、设置账号 2–9 为权限测试账号。
测试账号的密码沿用管理员当前密码（不是固定的 admin123）。
**只有确认要同步这项历史变更时才启用以下开关**，并在更新后立即重设/禁用测试账号。
脚本发现真实用户/角色/岗位占用了这些编号、旧部门仍被真实用户使用，或清理会删除非示例通知，会停止。
这时必须重新规划可用编号，不能绕过检查。

```bash
export APPLY_PERMISSION_RESET=true
set -o pipefail
bash deploy-update.sh 2>&1 | tee /opt/server-update-执行日志.txt
```

不启用重置时执行 `APPLY_PERMISSION_RESET=false bash deploy-update.sh`。
此模式仍更新程序和功能权限，但不完成角色编号重排、旧示例清理和测试账号同步。

脚本自动执行：压缩包内容核验、TCP 数据库认证、生产库/程序备份、SQLite 全目录一致性备份、
运行环境检查、按顺序迁移、应用替换、双方一致的审计密钥配置、启动和 HTTP 健康检查。
环境准备完成后暂停业务 Java 进程，再进行备份和迁移；请在维护窗口执行。
不会重建容器、修改映射端口或 SSH 密码，不会初始化/覆盖生产数据库。
备份位于 `/opt/ruoyi-backups/执行时间戳/`；现有上传文件与 GoView 项目保留。
GoView 新增 `dept_id` 由新版后端启动迁移；历史 NULL 部门项目沿用代码的兼容规则。
新角色权限会按最新方案重新分配；请事先记录自定义授权。

## 4. 联调验收

浏览器 Ctrl+F5 后访问主系统，退出并重新登录刷新权限缓存。

1. 首页模型/参数/报告/日志入口显示正确，模型原模板可以打开，资源无 404。
2. 修改一个模型或参数，检查日志中的操作人、变动详情及 `sys_oper_log` 记录。
3. 建立报告、选择核算数据、校验并下载 Word/PDF；检查精度及中文字体。
4. 从主系统进入 GoView，无需再次登录；修改项目后统一日志能够查询。
5. 用不同角色测试允许/拒绝操作，尤其地区隔离与只读角色。

```bash
docker exec ruoyi-web tail -100 /home/ruoyi/logs/ruoyi-admin.log
docker exec ruoyi-web tail -100 /home/ruoyi/logs/carbon-emission-model.log
docker exec ruoyi-screen tail -100 /home/goview/logs/goview.log
docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" ruoyi-db mysql --protocol=TCP -h127.0.0.1 -P3306 -uroot carbon_emissions -e 'SELECT COUNT(*) FROM sys_oper_log; SELECT COUNT(*) FROM sys_logininfor;'
```

HTTP 健康检查不等于完整业务验收；保护接口返回 401/403 仅表示认证限制在生效。
禁止在已有数据库重新运行 `carbon_emissions.sql`、`schema.sql`、`schema_1.sql`。
本地观测用 V12 不自动导入；不能用本地 SQLite 文件替换服务器数据库。

## 故障与恢复

失败先保留日志和备份，不要重复初始化数据库。迁移失败可能已发生部分 DDL，不能假定自动回滚。
恢复前停止 Web/Screen Java，按实际备份目录将 `web.tgz`、`screen.tgz` 复制入对应容器并解压到 `/`，
恢复完整 `goview-sqllite` 目录（连同 WAL 文件），用 TCP 将 `database.sql` 导回数据库。
恢复原启动脚本/环境文件后重启并验收。不要只回滚 JAR 而忽略已经改变的表结构。
容器重启后若原容器入口不负责拉起业务，手动运行对应 `/usr/local/bin/start-ruoyi-web.sh`
和 `/usr/local/bin/start-ruoyi-screen.sh`；本次更新不更换现有容器的入口命令。
