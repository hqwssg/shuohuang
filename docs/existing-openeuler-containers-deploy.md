# 使用 Xshell 和 Xftp 部署现有 openEuler 三容器项目

## 1. 部署目标

本项目部署为 3 个基于 openEuler 的 Docker 容器：

| 容器 | 作用 | 宿主机端口映射 |
| --- | --- | --- |
| `ruoyi-db` | MySQL/MariaDB 数据库 | `53210 -> 22`、`53214 -> 3306` |
| `ruoyi-web` | 主系统前后端、碳排放模型、Redis、Nginx | `53220 -> 22`、`53221 -> 80` |
| `ruoyi-screen` | 后台运算和 GoView 大屏 | `53230 -> 22`、`53231 -> 80` |

未使用的预留端口不需要映射。容器内的 `8080`、`8000`、`5000` 等端口仅在确有服务需要时再增加。

端口说明：

- `53210`、`53220`、`53230`：分别登录 3 个容器的 SSH 端口。
- `53214`：连接数据库容器内的 MySQL `3306`。
- `53221`：主系统浏览器访问端口。
- `53231`：碳排放大屏浏览器访问端口。
- 服务器自身的 SSH 端口，例如 `2213`，与上述容器 SSH 端口不是一回事。

## 2. 本地部署包

本地已打包目录：

```text
D:\idea库\RuoYi-Cloud\deploy-package\ruoyi-docker-3containers-20260721-145332
```

部署时上传该目录中的全部内容，不能只上传单个脚本或 `index.html`。重点文件包括：

```text
mysql/db/carbon_emissions.sql
mysql/db/schema_1.sql
web-main/jar/ruoyi-admin.jar
web-main/jar/carbon-emission-model.jar
web-main/python/steam_calculator.py
web-main/html/dist
web-main/html/carbon-model
web-main/conf/nginx.conf
web-screen/jar/goview.war
web-screen/html/dist
web-screen/sqllite/goview.db
existing-openeuler/recreate-openeuler-containers.sh
existing-openeuler/host-deploy-existing-openeuler.sh
existing-openeuler/scripts
```

## 3. 使用 Xshell 登录宿主服务器

在 Xshell 中新建 SSH 会话：

1. 主机填写服务器 IP。
2. 端口填写服务器自身的 SSH 端口，例如 `2213`。
3. 用户名填写 `root`。
4. 使用服务器密码或密钥登录。

也可以在本地 PowerShell 中登录：

```powershell
ssh -p 2213 root@服务器IP
```

登录后检查 Docker：

```bash
docker --version
docker ps -a
```

## 4. 调整端口前备份

Docker 不能给已经创建的容器直接增加或修改端口映射，因此需要重建容器。

如果数据库中已有正式数据，必须先备份。把命令中的数据库密码替换为实际密码：

```bash
docker exec ruoyi-db sh -c "mysqldump -uroot -p'实际数据库密码' --all-databases" > /opt/ruoyi-db-before-port-change.sql
ls -lh /opt/ruoyi-db-before-port-change.sql
```

重建脚本不会直接删除旧容器，而是停止并改名为：

```text
ruoyi-db-old-时间戳
ruoyi-web-old-时间戳
ruoyi-screen-old-时间戳
```

新数据库会按部署包中的 SQL 初始化。正式数据是否需要导回，应在部署前确认。

## 5. 使用 Xftp 上传部署包

在 Xftp 中使用与 Xshell 相同的服务器 IP、SSH 端口、用户名和认证方式连接。

服务器目标目录：

```text
/opt/ruoyi-docker
```

将本地部署包目录内的全部内容上传到该目录。上传后检查：

```bash
cd /opt/ruoyi-docker
ls -la
ls -la existing-openeuler
test -f web-main/html/dist/index.html && echo "main web package OK"
test -f web-screen/html/dist/index.html && echo "screen package OK"
```

## 6. 准备 openEuler 镜像

默认镜像：

```text
hub.oepkgs.net/openeuler/openeuler:24.03-lts
```

服务器可访问镜像仓库时执行：

```bash
docker pull hub.oepkgs.net/openeuler/openeuler:24.03-lts
```

服务器不能访问外网镜像仓库时，在有 Docker 的本地电脑导出：

```bash
docker pull hub.oepkgs.net/openeuler/openeuler:24.03-lts
docker save -o openeuler-24.03-lts.tar hub.oepkgs.net/openeuler/openeuler:24.03-lts
```

用 Xftp 上传镜像文件后，在服务器导入：

```bash
docker load -i /opt/ruoyi-docker/openeuler-24.03-lts.tar
```

## 7. 重建三个容器

执行：

```bash
cd /opt/ruoyi-docker/existing-openeuler
sh recreate-openeuler-containers.sh
```

脚本默认创建：

```text
ruoyi-db      53210:22    53214:3306
ruoyi-web     53220:22    53221:80
ruoyi-screen  53230:22    53231:80
```

如果服务器中的 openEuler 镜像名称不同：

```bash
OPENEULER_IMAGE='实际镜像名称' sh recreate-openeuler-containers.sh
```

如果容器名称不同：

```bash
DB_CONTAINER='数据库容器名' \
WEB_CONTAINER='主Web容器名' \
SCREEN_CONTAINER='大屏容器名' \
sh recreate-openeuler-containers.sh
```

检查映射：

```bash
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}'
```

## 8. 部署程序并配置容器 SSH

部署脚本会在 3 个容器中安装并启动 `sshd`，开启 root 密码登录。仅做 `-p 53210:22` 端口映射而没有启动容器内 `sshd`，SSH 仍然无法连接。

为了避免密码直接保存在命令历史中，可先安全读取密码：

```bash
cd /opt/ruoyi-docker/existing-openeuler
read -s -p "Container SSH password: " CONTAINER_SSH_PASSWORD
echo
export CONTAINER_SSH_PASSWORD
```

设置项目访问地址和数据库密码：

```bash
export WEB_PUBLIC_URL='http://服务器IP:53221'
export SCREEN_PUBLIC_URL='http://服务器IP:53231'
export MYSQL_ROOT_PASSWORD='实际数据库密码'
sh host-deploy-existing-openeuler.sh
```

脚本会完成：

- 在 3 个容器中安装并启动 OpenSSH Server。
- 为 3 个容器的 root 用户设置 `CONTAINER_SSH_PASSWORD`。
- 安装并初始化数据库。
- 先创建 RuoYi 表，再将 `schema_1.sql` 的碳模型业务表创建到同一个 `carbon_emissions` 数据库。
- 部署主系统前端、主后端、碳排放模型、Redis 和 Nginx。
- 部署 GoView 前端、后端、SQLite 数据库和 Nginx。
- 修正静态文件读取权限并检查 Nginx 配置。

首次部署完成后清理当前 shell 中的密码变量：

```bash
unset CONTAINER_SSH_PASSWORD MYSQL_ROOT_PASSWORD
```

## 9. 防火墙和安全组

服务器使用 `firewalld` 时执行：

```bash
for port in 53210 53214 53220 53221 53230 53231; do
  firewall-cmd --permanent --add-port=${port}/tcp
done
firewall-cmd --reload
firewall-cmd --list-ports
```

云服务器还需要在云平台安全组中添加相同端口。建议：

| 端口 | 建议来源范围 |
| --- | --- |
| `53221`、`53231` | 需要访问系统的客户端网段 |
| `53210`、`53220`、`53230` | 仅管理员公网 IP |
| `53214` | 仅数据库管理端 IP；不需要远程管理时不要向公网开放 |

## 10. 部署验证

检查宿主机监听：

```bash
ss -lntp | grep -E ':53210|:53214|:53220|:53221|:53230|:53231'
```

检查主系统和大屏：

```bash
curl -I http://127.0.0.1:53221/
curl -I http://127.0.0.1:53221/carbon-model/
curl -i http://127.0.0.1:53221/api/template/list
curl -I http://127.0.0.1:53231/
curl -i http://127.0.0.1:53231/api/goview/sys/getOssInfo
```

检查容器内进程：

```bash
docker exec ruoyi-web sh -c "ps -ef | grep -E 'ruoyi-admin.jar|carbon-emission-model.jar|redis-server|nginx|sshd' | grep -v grep"
docker exec ruoyi-screen sh -c "ps -ef | grep -E 'goview.war|nginx|sshd' | grep -v grep"
docker exec ruoyi-db sh -c "ps -ef | grep -E 'mysqld|mariadbd|sshd' | grep -v grep"
```

## 11. 浏览器访问

直接访问服务器：

```text
主系统：http://服务器IP:53221/
大屏：http://服务器IP:53231/
```

主系统中的“碳排放大屏展示”按钮会按当前访问方式自动跳转：

- 通过服务器 IP 访问主系统时，跳转到同一服务器 IP 的 `53231`。
- 通过本机 SSH 隧道访问主系统时，跳转到 `127.0.0.1:13000`。

## 12. 通过 PowerShell 建立 SSH 隧道

在本地 PowerShell 中执行。这里的 `2213` 是宿主服务器 SSH 端口：

```powershell
ssh -p 2213 -N `
  -L 127.0.0.1:18080:127.0.0.1:53221 `
  -L 127.0.0.1:13000:127.0.0.1:53231 `
  root@服务器IP
```

保持该 PowerShell 窗口运行，然后访问：

```text
主系统：http://127.0.0.1:18080/
大屏：http://127.0.0.1:13000/
```

如果出现 `administratively prohibited`，在宿主服务器检查 `/etc/ssh/sshd_config`：

```text
AllowTcpForwarding yes
```

修改后重启宿主服务器的 SSH 服务。不要把该设置误改到容器内。

## 13. 直接 SSH 登录三个容器

从外部电脑执行：

```powershell
ssh -p 53210 root@服务器IP
ssh -p 53220 root@服务器IP
ssh -p 53230 root@服务器IP
```

输入部署时设置的 `CONTAINER_SSH_PASSWORD`。首次连接出现主机指纹确认时输入 `yes`。

## 14. 更新部署

只更新程序、不重建容器和数据库时：

1. 用 Xftp 覆盖上传新的部署包文件。
2. 在 Xshell 中执行：

```bash
cd /opt/ruoyi-docker/existing-openeuler
read -s -p "Container SSH password: " CONTAINER_SSH_PASSWORD
echo
export CONTAINER_SSH_PASSWORD
SKIP_DB_DEPLOY=true sh host-deploy-existing-openeuler.sh
unset CONTAINER_SSH_PASSWORD
```

浏览器按 `Ctrl+F5` 强制刷新缓存。

本次需要把 `schema_1.sql` 业务表补充到已有 RuoYi 数据库时，将上面的执行命令改为：

```bash
BUSINESS_SCHEMA_ONLY=true sh host-deploy-existing-openeuler.sh
```

该模式只更新碳模型业务表，不重新导入 RuoYi 表。

## 15. 常用重启命令

```bash
docker exec ruoyi-db sh /usr/local/bin/start-container-sshd.sh
docker exec ruoyi-web sh /usr/local/bin/start-container-sshd.sh
docker exec ruoyi-screen sh /usr/local/bin/start-container-sshd.sh

docker exec ruoyi-db sh /usr/local/bin/start-ruoyi-db.sh
docker exec ruoyi-web sh /usr/local/bin/start-ruoyi-web.sh
docker exec ruoyi-screen sh /usr/local/bin/start-ruoyi-screen.sh
```

## 16. 常见故障

### 16.1 外网打不开但服务器 curl 正常

依次检查：

```bash
docker ps
ss -lntp | grep -E ':53221|:53231'
firewall-cmd --list-ports
```

如果三项正常，通常是云平台安全组、上级网络访问控制或客户端代理未放行。

### 16.2 Nginx 返回 403 Forbidden

检查首页和权限：

```bash
docker exec ruoyi-web sh -c "find /home/ruoyi/projects/ruoyi-ui -maxdepth 3 -name index.html -print; tail -50 /var/log/nginx/error.log"
docker exec ruoyi-screen sh -c "find /home/goview/projects/goview -maxdepth 3 -name index.html -print; tail -50 /var/log/nginx/error.log"
```

重新上传完整的 `web-main/html/dist`、`web-screen/html/dist` 和 `existing-openeuler` 后，再执行更新部署。

### 16.3 主系统按钮能跳转但大屏报错

先直接验证：

```bash
curl -I http://127.0.0.1:53231/
curl -i http://127.0.0.1:53231/api/goview/sys/getOssInfo
docker exec ruoyi-screen sh -c "tail -120 /home/goview/logs/goview.log; tail -80 /var/log/nginx/error.log"
```

页面返回 `200`、接口返回 JSON 后，大屏才算完整可用。

### 16.4 容器 SSH Connection refused

检查端口和 `sshd`：

```bash
docker exec ruoyi-web sh -c "/usr/sbin/sshd -t && pgrep -a sshd"
docker exec ruoyi-screen sh -c "/usr/sbin/sshd -t && pgrep -a sshd"
docker exec ruoyi-db sh -c "/usr/sbin/sshd -t && pgrep -a sshd"
```

如未运行，执行第 15 节中的 SSH 启动命令。
