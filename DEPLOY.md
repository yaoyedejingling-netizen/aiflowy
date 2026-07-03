# Aiflowy 项目 Docker Compose 部署指南

本文档提供了如何使用 `docker-compose` 快速部署 Aiflowy 全栈项目的详细步骤。

## 1. 环境准备

在开始部署之前，请确保您的系统已安装以下软件：

- **Docker**: >= 20.10.0
- **Docker Compose**: >= 2.0.0
- **内存建议**: 至少 4GB 可用内存

## 2. 快速开始

在项目根目录下（即 `docker-compose.yml` 所在目录），按照以下顺序执行命令：

### 第一步：一键构建并启动
```bash
docker compose up --build -d
```

### 第二步：检查服务状态
```bash
docker compose ps
```
确保所有容器（api, admin, usercenter, mysql, redis）均显示为 `Up` 或 `Healthy`。

## 3. 服务矩阵

| 服务名称 | 访问地址 | 默认账号/密码 | 说明 |
| :--- | :--- | :--- | :--- |
| **管理后台 (Admin UI)** | [http://localhost:8081](http://localhost:8081) | admin / 123456 | 智能体、模型及系统管理 |
| **用户中心 (User UI)** | [http://localhost:8082](http://localhost:8082) | admin / 123456 | 聊天、工作流执行界面 |
| **后端 API** | [http://localhost:8080](http://localhost:8080) | - | 核心业务服务接口 |
| **MySQL 数据库** | localhost:3306 | root / 123456 | 持久化存储 (数据库名: aiflowy) |
| **Redis** | localhost:6379 | aiflowy_redis_2026 | 缓存与 Sa-Token 存储 |

## 4. 关键配置说明

### 4.1 自动初始化 (SQL)
- 首次启动时，MySQL 容器会自动加载 `./sql` 目录下的脚本：
  - `01-aiflowy-v2.ddl.sql`: 数据库表结构定义。
  - `02-aiflowy-v2.data.sql`: 基础数据填充（已内置 UTF-8 声明及 npx 兼容性修复）。

### 4.2 Nginx 透明代理
- 前端容器内置 Nginx，负责处理 `/api/` 和 `/userCenter/` 的请求转发。
- **重要**：如需修改转发规则，请编辑各前端目录下的 `scripts/deploy/nginx.conf`。

### 4.3 字符集优化
- 已在 `docker-compose.yml` 环境参数中强制设置 `useUnicode=true&characterEncoding=utf-8&rewriteBatchedStatements=true` 以防止中文乱码。

### 4.4 API 网关配置
- 前端服务的 `VITE_GLOB_API_URL` 应留空，由 Nginx 代理转发至后端 API。
- 若需直接访问后端 API（绕过 Nginx），需将该变量设置为实际 API 地址，例如 `https://api.example.com`。

### 4.5 Redis 密码配置
- 默认密码为 `aiflowy_redis_2026`，可通过环境变量 `REDIS_PASSWORD` 自定义：
  ```bash
  export REDIS_PASSWORD=your_secure_password
  docker compose up -d
  ```

## 5. 常用运维命令

- **查看日志**:
  ```bash
  docker compose logs -f [service_name]
  ```
- **强制重置（含数据库数据）**:
  > [!CAUTION]
  > 此操作会删除 MySQL Data Volume，导致所有非初始化数据丢失。
  ```bash
  docker compose down -v && docker compose up --build -d
  ```
- **单独重启某个服务**:
  ```bash
  docker compose restart aiflowy-api
  ```

## 6. 常见问题 (FAQ)

**Q: 登录时验证码 405 错误？**
A: 这是因为 Nginx 缺少 `/userCenter/` 转发规则，当前版本的部署文档对应的配置文件已修复此问题。

**Q: 菜单或数据出现中文乱码？**
A: 请确保数据库初始化脚本首行包含 `SET NAMES utf8mb4;`，且连接字符串已带编码参数（本项目已默认配置）。

---
**Last Updated**: 2026-01-14
