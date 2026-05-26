# ZN Cloud Internet Cafe Platform

**ZN云网吧平台** — 网吧算力聚合与远程使用平台。将网吧闲置算力进行聚合，提供远程游戏、算力租赁等服务。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Cloud | 2023.0.1 | 微服务治理 |
| Spring Cloud Alibaba | 2023.0.1.0 | Nacos 服务注册/发现 |
| Spring Cloud Gateway | - | API 网关 |
| MyBatis-Plus | 3.5.6 | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7 | 缓存 / 会话管理 |
| Nacos | 2.3 | 服务注册中心与配置中心 |
| Maven | 3.9.6 | 项目构建 |
| Docker | - | 容器化部署 |
| JJWT | 0.12.5 | JWT 鉴权 |
| Lombok | - | 简化代码 |

## 模块说明

```
zncloud/
├── zncloud-common              # 公共模块（工具类、通用模型）
│   └── JwtUtil                 # JWT 令牌工具类
├── zncloud-gateway             # API 网关（端口 8080）
│   ├── JwtAuthGlobalFilter     # 全局 JWT 鉴权过滤器
│   ├── CorsConfig              # CORS 跨域配置
│   └── GatewayRouteConfig      # 路由配置（RouteLocator）
├── zncloud-user-service        # 用户服务（端口 8081）
│   ├── AuthController          # 认证接口（登录/注册/刷新令牌）
│   ├── UserController          # 用户管理接口
│   └── UserService             # 用户业务逻辑
├── zncloud-device-service      # 设备服务（端口 8082）
│   ├── DeviceController        # 设备管理接口
│   ├── DeviceService           # 设备业务逻辑
│   └── OfflineDetectionTask    # 设备离线检测定时任务
├── zncloud-session-service     # 会话服务（TODO）
├── zncloud-schedule-service    # 调度服务（TODO）
└── zncloud-billing-service     # 计费服务（TODO）
```

## 快速启动

### 前置条件

- JDK 17+
- Maven 3.9+（或使用项目中的 Maven Wrapper）
- Docker & Docker Compose
- MySQL 8.0（本地或 Docker）
- Redis 7（本地或 Docker）

### 1. 启动基础设施（Docker Compose）

```bash
# 启动 MySQL、Redis、Nacos
docker compose up -d mysql redis nacos
```

### 2. 编译项目

```bash
# 使用 Maven Wrapper 编译
./mvnw clean package -DskipTests
```

### 3. 启动微服务

**方式一：Docker Compose 一键启动**

```bash
# 启动所有服务
docker compose up -d --build
```

**方式二：本地 IDE 启动**

分别启动以下 Application 类：

- `GatewayApplication` — 端口 8080
- `UserServiceApplication` — 端口 8081
- `DeviceServiceApplication` — 端口 8082

### 4. 验证服务

```bash
# 查看 Nacos 服务列表
curl http://localhost:8848/nacos/v1/ns/service/list

# 测试网关路由
curl http://localhost:8080/api/v1/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","password":"test123"}'
```

## API 路由

| 网关路径 | 目标服务 | 说明 |
|----------|---------|------|
| `/api/v1/auth/**` | user-service | 认证接口（登录/注册/刷新令牌） |
| `/api/v1/users/**` | user-service | 用户管理接口 |
| `/api/v1/devices/**` | device-service | 设备管理接口 |

### 鉴权白名单（无需 Token）

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/devices/register`
- `POST /api/v1/devices/heartbeat`

## 项目结构树

```
zncloud/
├── .github/workflows/
│   └── ci.yml                          # GitHub Actions CI/CD
├── docker/
│   └── Dockerfile.java                 # Java 服务通用多阶段构建 Dockerfile
├── scripts/
│   └── init.sql                        # 数据库初始化脚本
├── zncloud-common/
│   ├── pom.xml
│   └── src/main/java/com/zncloud/common/
│       └── util/JwtUtil.java
├── zncloud-gateway/
│   ├── pom.xml
│   └── src/main/java/com/zncloud/gateway/
│       ├── GatewayApplication.java
│       ├── config/
│       │   ├── CorsConfig.java
│       │   └── GatewayRouteConfig.java
│       └── filter/
│           └── JwtAuthGlobalFilter.java
├── zncloud-user-service/
│   ├── pom.xml
│   └── src/main/java/com/zncloud/user/
│       ├── UserServiceApplication.java
│       ├── config/
│       ├── controller/
│       ├── service/
│       ├── mapper/
│       └── model/
├── zncloud-device-service/
│   ├── pom.xml
│   └── src/main/java/com/zncloud/device/
│       ├── DeviceServiceApplication.java
│       ├── config/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       └── model/
├── zncloud-session-service/
│   └── pom.xml                         # TODO
├── zncloud-schedule-service/
│   └── pom.xml                         # TODO
├── zncloud-billing-service/
│   └── pom.xml                         # TODO
├── docker-compose.yml                  # Docker Compose 编排
├── pom.xml                             # 父 POM
├── README.md
└── .gitignore
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_ROOT_PASSWORD` | root | MySQL root 密码 |
| `MYSQL_PASSWORD` | zncloud123 | 应用数据库用户密码 |
| `REDIS_PASSWORD` | (空) | Redis 密码 |
| `NACOS_ADDR` | localhost:8848 | Nacos 服务地址 |
| `JWT_SECRET` | (内置) | JWT 签名密钥 |
