# 使用 Xshell 与 Xftp 部署 openEuler 三容器项目操作手册

## 1. 最终容器与端口

| 容器名称 | 用途 | 必须使用的端口映射 |
| --- | --- | --- |
| `ruoyi-db` | 专用数据库容器 | `53210:22`、`53214:3306` |
| `ruoyi-web` | 主 Web 页面、后端、碳排放模型、Redis、Nginx | `53220:22`、`53221:80` |
| `ruoyi-screen` | 后台运算和 GoView 大屏 | `53230:22`、`53231:80` |

最终访问地址：

```text
主系统：http://服务器IP:53221/
大屏：http://服务器IP:53231/
MySQL：服务器IP:53214
```

三个容器的 SSH 入口：

```text
数据库容器：服务器IP:53210
主 Web 容器：服务器IP:53220
大屏容器：服务器IP:53230
```

`53210/53220/53230` 是容器 SSH 映射端口。Xshell、Xftp 登录宿主服务器时仍使用宿主服务器实际 SSH 端口，例如 `2213`。

## 2. 本地已经准备好的部署包

本地目录：

```text
D:\idea库\RuoYi-Cloud\deploy-package\ruoyi-docker-3containers-20260721-145332
```

该目录已包含主系统、大屏、数据库 SQL 和 openEuler 部署脚本。部署时无需再执行 Maven、npm 或 pnpm 打包。

数据库初始化会先导入 RuoYi 表，再导入 `schema_1.sql`，两部分表均位于 `carbon_emissions` 数据库中。碳模型嵌入主系统后直接进入模版管理，不再显示两个按钮的独立首页。

必须完整上传以下目录：

```text
mysql
web-main
web-screen
existing-openeuler
```

## 3. 用 Xshell 登录服务器

1. 打开 Xshell，选择“新建会话”。
2. 协议选择 `SSH`。
3. 主机填写服务器 IP。
4. 端口填写宿主服务器 SSH 端口，例如 `2213`。
5. 用户名填写 `root`。
6. 使用宿主服务器密码或密钥登录。

登录后检查：

```bash
docker --version
docker ps -a
```

## 4. 用 Xftp 上传文件

1. 打开 Xftp。
2. 使用与 Xshell 相同的服务器 IP、宿主 SSH 端口、用户名和认证方式。
3. 在服务器创建 `/opt/ruoyi-docker`。
4. 将本地部署包目录中的全部内容上传到 `/opt/ruoyi-docker`。

上传完成后的主要结构：

```text
/opt/ruoyi-docker/mysql
/opt/ruoyi-docker/web-main
/opt/ruoyi-docker/web-screen
/opt/ruoyi-docker/existing-openeuler
```

在 Xshell 中检查：

```bash
cd /opt/ruoyi-docker
test -f web-main/html/dist/index.html && echo "main web OK"
test -f web-screen/html/dist/index.html && echo "screen OK"
test -f existing-openeuler/host-deploy-existing-openeuler.sh && echo "scripts OK"
```

## 5. 数据库备份

修改端口映射需要重建容器。数据库已有正式数据时，先执行：

```bash
docker exec ruoyi-db sh -c "mysqldump -uroot -p'实际数据库密码' --all-databases" > /opt/ruoyi-db-before-port-change.sql
ls -lh /opt/ruoyi-db-before-port-change.sql
```

脚本会停止旧容器并改名为 `容器名-old-时间戳`，不会直接删除旧容器。

## 6. 准备 openEuler 镜像

服务器能访问镜像仓库时：

```bash
docker pull hub.oepkgs.net/openeuler/openeuler:24.03-lts
```

服务器不能访问外网时，在有 Docker 的电脑执行：

```bash
docker pull hub.oepkgs.net/openeuler/openeuler:24.03-lts
docker save -o openeuler-24.03-lts.tar hub.oepkgs.net/openeuler/openeuler:24.03-lts
```

用 Xftp 上传 tar 文件后，在服务器执行：

```bash
docker load -i /opt/ruoyi-docker/openeuler-24.03-lts.tar
```

## 7. 重建三个 openEuler 容器

在 Xshell 中执行：

```bash
cd /opt/ruoyi-docker/existing-openeuler
sh recreate-openeuler-containers.sh
```

脚本使用以下 Docker 参数：

```text
ruoyi-db      -p 53210:22 -p 53214:3306
ruoyi-web     -p 53220:22 -p 53221:80
ruoyi-screen  -p 53230:22 -p 53231:80
```

检查：

```bash
docker ps --format 'table {{.Names}}\t{{.Ports}}'
```

## 8. 部署项目并设置容器 SSH 密码

容器内必须安装并启动 `sshd`，仅映射容器的 `22` 端口不能自动提供 SSH 服务。

先在 Xshell 中安全读取容器 root 密码：

```bash
cd /opt/ruoyi-docker/existing-openeuler
read -s -p "Container SSH password: " CONTAINER_SSH_PASSWORD
echo
export CONTAINER_SSH_PASSWORD
```

执行完整部署：

```bash
export MYSQL_ROOT_PASSWORD='实际数据库密码'
export WEB_PUBLIC_URL='http://服务器IP:53221'
export SCREEN_PUBLIC_URL='http://服务器IP:53231'
sh host-deploy-existing-openeuler.sh
```

部署结束后：

```bash
unset CONTAINER_SSH_PASSWORD MYSQL_ROOT_PASSWORD
```

## 9. 开放端口

服务器使用 `firewalld` 时：

```bash
for port in 53210 53214 53220 53221 53230 53231; do
  firewall-cmd --permanent --add-port=${port}/tcp
done
firewall-cmd --reload
firewall-cmd --list-ports
```

云服务器安全组也要开放相应端口：

- `53221`、`53231`：允许业务用户访问。
- `53210`、`53220`、`53230`：建议只允许管理员公网 IP。
- `53214`：建议只允许数据库管理员 IP，不要对全网开放。

## 10. 浏览器验证

先在服务器本机验证：

```bash
curl -I http://127.0.0.1:53221/
curl -I http://127.0.0.1:53221/carbon-model/
curl -I http://127.0.0.1:53231/
curl -i http://127.0.0.1:53231/api/goview/sys/getOssInfo
```

再在本地浏览器访问：

```text
http://服务器IP:53221/
http://服务器IP:53231/
```

## 11. 登录三个容器

可以从 Xshell 新建 3 个会话，也可以在 PowerShell 执行：

```powershell
ssh -p 53210 root@服务器IP
ssh -p 53220 root@服务器IP
ssh -p 53230 root@服务器IP
```

登录密码是第 8 节设置的 `CONTAINER_SSH_PASSWORD`。

## 12. PowerShell SSH 隧道

不希望把 Web 端口直接开放到公网时，可通过宿主服务器 SSH 建立隧道：

```powershell
ssh -p 2213 -N `
  -L 127.0.0.1:18080:127.0.0.1:53221 `
  -L 127.0.0.1:13000:127.0.0.1:53231 `
  root@服务器IP
```

保持命令窗口运行，浏览器访问：

```text
http://127.0.0.1:18080/
http://127.0.0.1:13000/
```

## 13. 更新项目

后续只更新项目文件时：

1. 使用 Xftp 覆盖上传更新后的部署包。
2. 使用 Xshell 执行：

```bash
cd /opt/ruoyi-docker/existing-openeuler
read -s -p "Container SSH password: " CONTAINER_SSH_PASSWORD
echo
export CONTAINER_SSH_PASSWORD
SKIP_DB_DEPLOY=true sh host-deploy-existing-openeuler.sh
unset CONTAINER_SSH_PASSWORD
```

不需要再次执行容器重建脚本。

本次需要把 `schema_1.sql` 合并到已有 RuoYi 数据库时，使用 `BUSINESS_SCHEMA_ONLY=true` 代替 `SKIP_DB_DEPLOY=true`；该模式不会重新导入 RuoYi 表。

## 14. 常见故障检查

### 14.1 外网无法访问

```bash
docker ps
ss -lntp | grep -E ':53221|:53231'
firewall-cmd --list-ports
```

服务器 `curl` 正常但外网失败时，检查云安全组、上级防火墙和客户端代理。

### 14.2 页面显示 403

```bash
docker exec ruoyi-web sh -c "find /home/ruoyi/projects/ruoyi-ui -maxdepth 3 -name index.html -print; tail -50 /var/log/nginx/error.log"
docker exec ruoyi-screen sh -c "find /home/goview/projects/goview -maxdepth 3 -name index.html -print; tail -50 /var/log/nginx/error.log"
```

确认上传的是完整 `dist` 目录，并重新执行第 13 节更新部署。

### 14.3 页面能打开但接口失败

```bash
docker exec ruoyi-web sh -c "tail -120 /home/ruoyi/logs/ruoyi-admin.log"
docker exec ruoyi-web sh -c "tail -120 /home/ruoyi/logs/carbon-emission-model.log"
docker exec ruoyi-screen sh -c "tail -120 /home/goview/logs/goview.log"
```

### 14.4 容器 SSH 拒绝连接

```bash
docker exec ruoyi-db sh /usr/local/bin/start-container-sshd.sh
docker exec ruoyi-web sh /usr/local/bin/start-container-sshd.sh
docker exec ruoyi-screen sh /usr/local/bin/start-container-sshd.sh
```

随后再次使用 `53210`、`53220`、`53230` 登录。

## 15. 最终检查清单

- 3 个容器的镜像均为 openEuler。
- 每个容器都映射了内部 `22` 端口。
- MySQL 使用 `53214 -> 3306`。
- 主系统使用 `53221 -> 80`。
- 大屏使用 `53231 -> 80`。
- 3 个容器内的 `sshd` 均已启动。
- 主系统和大屏可分别直接访问。
- 主系统中的大屏按钮可以跳转到 `53231`。
