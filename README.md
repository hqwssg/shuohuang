# RuoYi Spring Boot 单体版

本项目已从 RuoYi-Cloud 微服务结构调整为纯 Spring Boot 单体结构。后端只需要启动 `ruoyi-admin`，原认证、系统、代码生成、定时任务、文件服务等能力作为内部 Maven 模块运行在同一个进程中。

## 当前结构

```text
com.ruoyi
├── ruoyi-admin                 # 单体后端启动入口 [8081]
├── ruoyi-auth                  # 认证业务模块
├── ruoyi-api                   # 内部接口与领域对象
├── ruoyi-common                # 公共能力
├── ruoyi-modules               # system/gen/job/file 业务模块
├── ruoyi-ui                    # Vue 前端
├── sql                         # 数据库脚本
└── docker                      # 单体部署脚本
```

## 架构变化

- 移除网关、Nacos、Sentinel、Seata、Spring Boot Admin 等微服务运行内容。
- 移除 OpenFeign 远程调用，改为 `ruoyi-admin` 内部本地 Bean 调用。
- 保留前端原有接口前缀：`/auth`、`/system`、`/code`、`/schedule`、`/file`。
- 原网关中的鉴权和验证码逻辑已迁移为 Servlet 过滤器和 MVC 控制器。
- Docker 部署改为 `ruoyi-db`、`ruoyi-web`、`ruoyi-screen` 三容器。

## 本地启动

1. 导入 `sql/carbon_emissions.sql`。
2. 启动 MySQL 和 Redis。
3. 根据本地环境调整 `ruoyi-admin/src/main/resources/application.yml` 的数据库、Redis 和文件上传路径。
4. 启动 `com.ruoyi.admin.RuoYiAdminApplication`。
5. 前端开发环境代理已指向 `http://localhost:8081`。

## Docker

```bash
cd docker
sh copy.sh
docker-compose up -d ruoyi-db ruoyi-web ruoyi-screen
```

三容器职责：

- `ruoyi-db`：专用数据库容器，运行 MySQL。
- `ruoyi-web`：业务 Web 容器，运行主系统前端、`ruoyi-admin` 后端和后端所需 Redis。
- `ruoyi-screen`：大屏/后台展示容器，运行 GoView 前端和 GoView 后端。

碳排放相关前端与已有业务代码保留。仓库中的独立碳排放 Spring Boot/JPA 后端仍作为普通 Boot 应用保留，不再依赖 RuoYi 微服务治理链路。
