# 复用已有 openEuler 三容器部署说明

本目录用于服务器已经存在 3 个容器时部署项目，不再使用 `docker-compose up -d --build` 重建镜像。

## 一、适用条件

服务器中已经有 3 个正在运行或可启动的容器，并且容器系统必须是 openEuler。

建议三类容器如下：

- 数据库容器：运行 MySQL/MariaDB，默认容器名 `ruoyi-db`
- 主 Web 容器：运行主系统前端、模型前端、两个后端、Redis、Nginx，默认容器名 `ruoyi-web`
- 大屏容器：运行 GoView 前端、后端、Nginx，默认容器名 `ruoyi-screen`

如果服务器上的容器名不同，执行脚本时通过环境变量指定即可。

## 二、部署前检查

在 Xshell 中执行：

```bash
docker ps -a
docker exec 容器名 cat /etc/os-release
```

如果 `/etc/os-release` 中不是 openEuler，说明该容器不满足本次要求，需要使用 openEuler 镜像重新创建容器。

还需要确认端口映射已经在创建容器时设置好。Docker 不能给已经创建好的容器直接追加端口映射。

最终端口：

- 数据库容器：`53210:22`（SSH）、`53214:3306`（MySQL）
- 主 Web 容器：`53220:22`（SSH）、`53221:80`（主系统）
- 大屏容器：`53230:22`（SSH）、`53231:80`（大屏）

## 三、使用 XFTP 上传

将整个部署包目录上传到服务器：

```text
/opt/ruoyi-docker
```

上传完成后，本文件应位于：

```text
/opt/ruoyi-docker/existing-openeuler/README-existing-openeuler.md
```

## 四、使用 Xshell 执行部署

如果 `docker ps` 中不是上述端口映射，需要重建三个容器。已有容器无法追加或修改端口映射：

```bash
cd /opt/ruoyi-docker/existing-openeuler
sh recreate-openeuler-containers.sh
```

该脚本会把旧容器停止并改名备份，然后创建：

- `ruoyi-db`：`53210:22`、`53214:3306`
- `ruoyi-web`：`53220:22`、`53221:80`
- `ruoyi-screen`：`53230:22`、`53231:80`

旧数据库容器会保留为 `ruoyi-db-old-时间戳`，但正式环境仍应先导出数据库备份。

如果服务器容器名就是 `ruoyi-db`、`ruoyi-web`、`ruoyi-screen`，执行：

```bash
cd /opt/ruoyi-docker/existing-openeuler
CONTAINER_SSH_PASSWORD='请设置强密码' \
WEB_PUBLIC_URL='http://服务器IP:53221' \
SCREEN_PUBLIC_URL='http://服务器IP:53231' \
sh host-deploy-existing-openeuler.sh
```

如果只是覆盖更新主 Web 和大屏文件，不希望重新导入数据库，执行：

```bash
cd /opt/ruoyi-docker/existing-openeuler
CONTAINER_SSH_PASSWORD='请设置强密码' SKIP_DB_DEPLOY=true sh host-deploy-existing-openeuler.sh
```

如果容器名不同，例如数据库容器为 `openeuler-db`、主 Web 容器为 `openeuler-web`、大屏容器为 `openeuler-screen`，执行：

```bash
cd /opt/ruoyi-docker/existing-openeuler
CONTAINER_SSH_PASSWORD='请设置强密码' DB_CONTAINER=openeuler-db WEB_CONTAINER=openeuler-web SCREEN_CONTAINER=openeuler-screen sh host-deploy-existing-openeuler.sh
```

如果需要指定主系统访问地址、大屏访问地址、数据库 root 密码，可以执行：

```bash
cd /opt/ruoyi-docker/existing-openeuler
DB_CONTAINER=openeuler-db \
WEB_CONTAINER=openeuler-web \
SCREEN_CONTAINER=openeuler-screen \
MYSQL_ROOT_PASSWORD='你的数据库密码' \
CONTAINER_SSH_PASSWORD='请设置强密码' \
WEB_PUBLIC_URL='http://服务器IP:53221' \
SCREEN_PUBLIC_URL='http://服务器IP:53231' \
sh host-deploy-existing-openeuler.sh
```

## 五、验证

执行：

```bash
docker exec ruoyi-web sh -c "ps -ef | grep -E 'ruoyi-admin.jar|carbon-emission-model.jar' | grep -v grep && nginx -t"
docker exec ruoyi-screen sh -c "ps -ef | grep goview.war | grep -v grep && nginx -t"
docker exec ruoyi-db sh -c "mysql -uroot -p你的数据库密码 -e 'show databases;'"
curl -I http://127.0.0.1:53221/
curl -I http://127.0.0.1:53221/carbon-model/
curl -i http://127.0.0.1:53221/api/template/list
curl -I http://127.0.0.1:53231/
```

然后浏览器访问：

```text
http://服务器IP:53221
http://服务器IP:53231
```

容器 SSH 登录：

```bash
ssh -p 53210 root@服务器IP
ssh -p 53220 root@服务器IP
ssh -p 53230 root@服务器IP
```

## 六、注意事项

脚本会先导入 `carbon_emissions.sql` 创建 RuoYi 表，再导入 `schema_1.sql`，将碳模型业务表创建到同一个 `carbon_emissions` 数据库。前一个 SQL 包含 `drop table if exists`，会覆盖同名 RuoYi 表；正式环境如已有数据，请先备份数据库。

已有正式数据库只需要补充或更新碳模型业务表时，使用：

```bash
CONTAINER_SSH_PASSWORD='请设置强密码' BUSINESS_SCHEMA_ONLY=true sh host-deploy-existing-openeuler.sh
```

该模式跳过 `carbon_emissions.sql`，只导入幂等的 `schema_1.sql`。

如果容器内无法联网安装 `nginx`、`redis`、`java`、`mysql/mariadb`，需要先为 openEuler 容器配置可用的软件源，或准备离线 RPM 包。

容器重启后，如果服务没有自动恢复，可重新执行：

```bash
docker exec ruoyi-web sh /usr/local/bin/start-ruoyi-web.sh
docker exec ruoyi-screen sh /usr/local/bin/start-ruoyi-screen.sh
docker exec ruoyi-db sh /usr/local/bin/start-ruoyi-db.sh
docker exec ruoyi-web sh /usr/local/bin/start-container-sshd.sh
docker exec ruoyi-screen sh /usr/local/bin/start-container-sshd.sh
docker exec ruoyi-db sh /usr/local/bin/start-container-sshd.sh
```
