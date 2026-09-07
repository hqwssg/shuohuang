三容器 openEuler 部署包

本目录只保留当前可完整部署项目所需文件，部署方式为：
复用或重建服务器上的 3 个 openEuler 容器，然后将程序文件复制进容器内启动。

上传方式：
使用 Xftp 将本目录全部内容上传到服务器：
/opt/ruoyi-docker

详细步骤：
- openEuler三容器部署操作手册.docx

默认容器名：
- ruoyi-db：数据库容器
- ruoyi-web：主系统 Web 容器
- ruoyi-screen：碳排放大屏容器

最终端口分配：
- ruoyi-db：53210 -> 22（SSH），53214 -> 3306（MySQL）
- ruoyi-web：53220 -> 22（SSH），53221 -> 80（主系统）
- ruoyi-screen：53230 -> 22（SSH），53231 -> 80（大屏）

服务器执行：
注意：下列重建脚本会重建数据库容器。正式数据库必须先使用 mysqldump 备份。
旧容器会被停止并改名为“原容器名-old-时间戳”，不会直接删除。

cd /opt/ruoyi-docker/existing-openeuler
sh recreate-openeuler-containers.sh
CONTAINER_SSH_PASSWORD='请设置强密码' \
WEB_PUBLIC_URL='http://服务器IP:53221' \
SCREEN_PUBLIC_URL='http://服务器IP:53231' \
sh host-deploy-existing-openeuler.sh

如果容器名不是默认名称：
cd /opt/ruoyi-docker/existing-openeuler
DB_CONTAINER=实际数据库容器名 WEB_CONTAINER=实际主Web容器名 SCREEN_CONTAINER=实际大屏容器名 sh recreate-openeuler-containers.sh
CONTAINER_SSH_PASSWORD='请设置强密码' DB_CONTAINER=实际数据库容器名 WEB_CONTAINER=实际主Web容器名 SCREEN_CONTAINER=实际大屏容器名 sh host-deploy-existing-openeuler.sh

保留内容：
- mysql/db/carbon_emissions.sql
- mysql/db/schema_1.sql
- web-main/conf/nginx.conf
- web-main/html/dist
- web-main/html/carbon-model
- web-main/jar/ruoyi-admin.jar
- web-main/jar/carbon-emission-model.jar
- web-main/python/steam_calculator.py
- web-screen/conf/nginx.conf
- web-screen/html/dist
- web-screen/jar/goview.war
- web-screen/sqllite/goview.db
- web-screen/upload
- existing-openeuler 部署脚本

访问地址：
- 主系统：http://服务器IP:53221/
- 碳排放大屏：http://服务器IP:53231/
- MySQL：服务器IP:53214

三个容器的 SSH 登录：
- ssh -p 53210 root@服务器IP
- ssh -p 53220 root@服务器IP
- ssh -p 53230 root@服务器IP

如服务器公网端口不可直接访问，可使用 SSH 本地隧道：
ssh -p 2213 -N -L 127.0.0.1:18080:127.0.0.1:53221 -L 127.0.0.1:13000:127.0.0.1:53231 root@服务器公网IP

然后本机浏览器访问：
- 主系统：http://127.0.0.1:18080/
- 碳排放大屏：http://127.0.0.1:13000/

主系统中的“碳排放大屏展示”按钮已改为自动适配：
- 通过 127.0.0.1/localhost 访问主系统时，自动跳转到 13000 端口的大屏。
- 通过服务器 IP 访问主系统时，自动跳转到同一服务器 IP 的 53231 端口大屏。

首页六个功能按钮已更新：
- 管理员账号可直接进入六个功能页，不会被前端路由权限二次拦截。
- “碳排放大屏展示”按钮直接打开 GoView 大屏地址。
- “碳排放模型设置”使用主站同源地址 /carbon-model/，不再依赖本机开发端口 5177。
- 主 Web 容器会同时启动 ruoyi-admin.jar（8081）和 carbon-emission-model.jar（8082）。
- 数据库先导入 RuoYi 表，再把 schema_1.sql 中的碳模型业务表创建到同一个 carbon_emissions 数据库。
- 碳模型嵌入 RuoYi 后直接进入模版管理，不再显示两个按钮的独立首页。
- 其余按钮使用明确路由名跳转，不再显示路由成功后的错误误报。

如果服务器已经部署过，需要用 Xftp 覆盖上传本目录中的以下内容：
- web-main/html/dist
- web-main/html/carbon-model
- web-main/jar/carbon-emission-model.jar
- web-main/python/steam_calculator.py
- web-main/conf/nginx.conf
- web-screen/html/dist
- mysql/db/schema_1.sql
- existing-openeuler

然后在服务器执行：
cd /opt/ruoyi-docker/existing-openeuler
CONTAINER_SSH_PASSWORD='请设置强密码' BUSINESS_SCHEMA_ONLY=true sh host-deploy-existing-openeuler.sh

BUSINESS_SCHEMA_ONLY=true 只更新 schema_1.sql 中的碳模型业务表，不会重新导入或删除现有 RuoYi 表。
后续仅更新程序且数据库结构没有变化时，仍可使用 SKIP_DB_DEPLOY=true。

验证主系统和模型服务：
docker exec ruoyi-web sh -c "ps -ef | grep -E 'ruoyi-admin.jar|carbon-emission-model.jar' | grep -v grep"
curl -I http://127.0.0.1:53221/
curl -I http://127.0.0.1:53221/carbon-model/
curl -i http://127.0.0.1:53221/api/template/list

验证大屏：
docker exec ruoyi-screen sh -c "find /home/goview/projects/goview -maxdepth 3 -name index.html -print && nginx -t"
curl -I http://127.0.0.1:53231/

更新后浏览器按 Ctrl+F5 强制刷新缓存。

如果访问 http://127.0.0.1:18080/ 出现 403 Forbidden，通常是主系统前端没有被复制到
Nginx 根目录，或 Xftp 上传成了 web-main/html/dist/dist/index.html。
先在服务器检查：
docker exec ruoyi-web sh -c "find /home/ruoyi/projects/ruoyi-ui -maxdepth 3 -name index.html -print && ls -al /home/ruoyi/projects/ruoyi-ui | head"

然后重新上传本目录中的 web-main/html/dist 和 existing-openeuler/scripts，
再执行：
cd /opt/ruoyi-docker/existing-openeuler
CONTAINER_SSH_PASSWORD='请设置强密码' SKIP_DB_DEPLOY=true sh host-deploy-existing-openeuler.sh

如果访问 http://127.0.0.1:13000/ 出现 403 Forbidden，说明大屏容器内缺少可读取的
/home/goview/projects/goview/index.html。覆盖上传 web-screen/html/dist 和 existing-openeuler，
重新执行部署脚本；更新后的脚本会自动处理多套一层 dist 的情况。

大屏默认账号：
- 账号：admin
- 密码：admin
