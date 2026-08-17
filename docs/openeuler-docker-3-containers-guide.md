# Docker 创建 3 台 openEuler 容器操作文档

## 1. 适用说明

当前服务器无法使用 KVM 虚拟化，表现为：

```bash
ls -l /dev/kvm
```

返回：

```text
No such file or directory
```

因此不能在当前服务器上通过 KVM/libvirt 创建真正的 openEuler 虚拟机。可替代方案是使用 Docker 创建 3 个 openEuler 容器，每个容器都可以作为一个独立的 openEuler 用户空间环境使用。

注意：

- Docker 容器不是完整虚拟机。
- 容器与宿主机共用 Linux 内核。
- 容器适合测试命令、安装软件、运行普通服务。
- 如果必须使用完整虚拟机，需要服务器支持 `/dev/kvm`。

## 2. 检查 Docker 是否可用

```bash
docker --version
docker ps
```

如果能正常显示 Docker 版本和容器列表，说明 Docker 可用。

## 3. 拉取 openEuler 操作系统镜像

```bash
docker pull openeuler/openeuler:24.03-lts
```

查看镜像是否拉取成功：

```bash
docker images | grep openeuler
```

如果能看到 `openeuler/openeuler`，说明镜像已拉取成功。

## 4. 创建并运行 3 台 openEuler 容器

创建第 1 台容器：

```bash
docker run -dit \
  --name openeuler-01 \
  --hostname openeuler-01 \
  openeuler/openeuler:24.03-lts \
  /bin/bash
```

创建第 2 台容器：

```bash
docker run -dit \
  --name openeuler-02 \
  --hostname openeuler-02 \
  openeuler/openeuler:24.03-lts \
  /bin/bash
```

创建第 3 台容器：

```bash
docker run -dit \
  --name openeuler-03 \
  --hostname openeuler-03 \
  openeuler/openeuler:24.03-lts \
  /bin/bash
```

说明：

- `-d`：后台运行容器。
- `-i`：保持标准输入打开。
- `-t`：分配终端。
- `--name`：设置容器名称。
- `--hostname`：设置容器内部主机名。
- `/bin/bash`：容器启动后保持 Bash 运行。

## 5. 查看 3 台容器状态

```bash
docker ps -a
```

正常情况下可以看到：

```text
openeuler-01
openeuler-02
openeuler-03
```

并且状态为 `Up`。

## 6. 连接进入容器使用

进入第 1 台容器：

```bash
docker exec -it openeuler-01 /bin/bash
```

进入第 2 台容器：

```bash
docker exec -it openeuler-02 /bin/bash
```

进入第 3 台容器：

```bash
docker exec -it openeuler-03 /bin/bash
```

进入容器后，查看 openEuler 系统版本：

```bash
cat /etc/os-release
```

退出容器：

```bash
exit
```

## 7. 容器常用管理命令

启动 3 台容器：

```bash
docker start openeuler-01 openeuler-02 openeuler-03
```

停止 3 台容器：

```bash
docker stop openeuler-01 openeuler-02 openeuler-03
```

重启 3 台容器：

```bash
docker restart openeuler-01 openeuler-02 openeuler-03
```

查看容器日志：

```bash
docker logs openeuler-01
docker logs openeuler-02
docker logs openeuler-03
```

删除 3 台容器：

```bash
docker rm -f openeuler-01 openeuler-02 openeuler-03
```

## 8. 可选：为容器配置固定网络

如果希望 3 台容器在同一个自定义 Docker 网络内通信，可以先创建网络：

```bash
docker network create openeuler-net
```

然后重新创建容器：

```bash
docker rm -f openeuler-01 openeuler-02 openeuler-03

docker run -dit --name openeuler-01 --hostname openeuler-01 --network openeuler-net openeuler/openeuler:24.03-lts /bin/bash
docker run -dit --name openeuler-02 --hostname openeuler-02 --network openeuler-net openeuler/openeuler:24.03-lts /bin/bash
docker run -dit --name openeuler-03 --hostname openeuler-03 --network openeuler-net openeuler/openeuler:24.03-lts /bin/bash
```

在容器之间测试通信：

```bash
docker exec -it openeuler-01 /bin/bash
ping openeuler-02
ping openeuler-03
```

## 9. 如果 Docker 拉取镜像失败

如果执行：

```bash
docker pull openeuler/openeuler:24.03-lts
```

失败，先检查网络和 DNS：

```bash
ping -c 3 8.8.8.8
ping -c 3 registry-1.docker.io
ping -c 3 repo.openeuler.org
cat /etc/resolv.conf
```

如果能 ping 通 IP，但不能解析域名，可临时设置 DNS：

```bash
cp -a /etc/resolv.conf /etc/resolv.conf.bak

cat > /etc/resolv.conf <<EOF
nameserver 223.5.5.5
nameserver 114.114.114.114
nameserver 8.8.8.8
EOF
```

然后重新拉取镜像：

```bash
docker pull openeuler/openeuler:24.03-lts
```

## 10. 总结

当前服务器不具备 `/dev/kvm`，不适合创建真正的 openEuler 虚拟机。建议使用 Docker 创建 3 个 openEuler 容器作为替代环境：

```bash
docker pull openeuler/openeuler:24.03-lts
docker run -dit --name openeuler-01 --hostname openeuler-01 openeuler/openeuler:24.03-lts /bin/bash
docker run -dit --name openeuler-02 --hostname openeuler-02 openeuler/openeuler:24.03-lts /bin/bash
docker run -dit --name openeuler-03 --hostname openeuler-03 openeuler/openeuler:24.03-lts /bin/bash
docker exec -it openeuler-01 /bin/bash
```

## 11. 容器创建成功后的容器内操作步骤

容器创建成功后，先查看 3 个容器状态：

```bash
docker ps -a
```

确认 `openeuler-01`、`openeuler-02`、`openeuler-03` 的状态为 `Up`。

### 11.1 进入容器

进入第 1 台容器：

```bash
docker exec -it openeuler-01 /bin/bash
```

进入第 2 台容器：

```bash
docker exec -it openeuler-02 /bin/bash
```

进入第 3 台容器：

```bash
docker exec -it openeuler-03 /bin/bash
```

### 11.2 查看 openEuler 系统信息

进入容器后执行：

```bash
cat /etc/os-release
uname -a
```

说明：

- `cat /etc/os-release` 用于查看 openEuler 版本。
- `uname -a` 用于查看内核信息。
- Docker 容器与宿主机共用内核，因此 `uname -a` 显示的内核通常是宿主机内核。

### 11.3 配置 DNS

如果容器内不能访问域名，可以临时配置 DNS：

```bash
cat > /etc/resolv.conf <<EOF
nameserver 223.5.5.5
nameserver 114.114.114.114
nameserver 8.8.8.8
EOF
```

测试网络：

```bash
ping -c 3 repo.openeuler.org
```

如果提示没有 `ping` 命令，可以先执行后面的安装常用工具步骤。

### 11.4 更新软件源缓存

openEuler 通常使用 `dnf`：

```bash
dnf clean all
dnf makecache
```

如果容器中没有 `dnf`，可尝试：

```bash
yum clean all
yum makecache
```

### 11.5 安装常用工具

使用 `dnf` 安装：

```bash
dnf install -y vim wget curl net-tools iproute iputils procps-ng tar gzip unzip
```

如果使用 `yum`：

```bash
yum install -y vim wget curl net-tools iproute iputils procps-ng tar gzip unzip
```

说明：

- `vim`：文本编辑器。
- `wget`、`curl`：下载和接口测试工具。
- `net-tools`、`iproute`、`iputils`：网络查看和测试工具。
- `procps-ng`：提供 `ps`、`top` 等进程查看命令。
- `tar`、`gzip`、`unzip`：压缩解压工具。

### 11.6 查看容器 IP

在容器内执行：

```bash
ip addr
```

也可以在宿主机执行：

```bash
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' openeuler-01
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' openeuler-02
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' openeuler-03
```

### 11.7 测试容器之间通信

在 `openeuler-01` 容器内执行：

```bash
ping openeuler-02
ping openeuler-03
```

如果容器名不能直接 ping 通，可在宿主机查看 Docker 网络：

```bash
docker network ls
docker network inspect bridge
```

建议使用第 8 节中的自定义网络 `openeuler-net` 创建容器，这样容器之间可以通过容器名通信。

### 11.8 设置 root 密码，可选

在容器内执行：

```bash
passwd root
```

如果没有 `passwd` 命令：

```bash
dnf install -y passwd
passwd root
```

### 11.9 开启 SSH 服务，可选

一般情况下，Docker 容器推荐使用 `docker exec` 进入，不需要单独开启 SSH。

如果必须通过 SSH 登录容器，可在容器内执行：

```bash
dnf install -y openssh-server openssh-clients passwd
ssh-keygen -A
passwd root
```

允许 root 和密码登录：

```bash
sed -i 's/^#*PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config
sed -i 's/^#*PasswordAuthentication.*/PasswordAuthentication yes/' /etc/ssh/sshd_config
```

启动 SSH：

```bash
/usr/sbin/sshd
```

在宿主机查看容器 IP：

```bash
docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' openeuler-01
```

从宿主机连接容器：

```bash
ssh root@容器IP
```

### 11.10 对另外两台容器重复初始化

退出当前容器：

```bash
exit
```

进入第二台容器：

```bash
docker exec -it openeuler-02 /bin/bash
```

进入第三台容器：

```bash
docker exec -it openeuler-03 /bin/bash
```

每台容器建议至少执行以下初始化命令：

```bash
cat /etc/os-release
dnf clean all
dnf makecache
dnf install -y vim wget curl net-tools iproute iputils procps-ng tar gzip unzip
```
