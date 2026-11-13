# FreshMeal 轻食预制菜订购配送管理系统

基于 Spring Boot 3 的轻食预制菜线上订购与配送管理系统，包含用户端商城、管理后台和配送员端三个前端工程。

## 工程结构

| 目录 | 说明 |
|---|---|
| `server` | Spring Boot 3.2 后端，提供三端 REST 接口 |
| `web-shop` | 用户端移动 H5（Vue 3 + Vant） |
| `web-rider` | 配送员端移动 H5（Vue 3 + Vant） |
| `web-admin` | 管理后台（Vue 3 + Element Plus，基于 vue-pure-admin） |
| `docs` | 需求分析、数据库设计、系统总体设计、论文大纲与开发计划 |

## 环境要求

| 组件 | 版本 | 默认地址 |
|---|---|---|
| JDK | 17 | — |
| Maven | 3.9+ | — |
| MySQL | 8 | `127.0.0.1:3306` |
| Redis | 7 | `127.0.0.1:6379` |
| Node.js | 22.12+（管理端 22.22.1+） | — |
| pnpm | 9.12+（管理端 11+） | — |

## 启动步骤

### 1. 初始化数据库

Flyway 在首次启动时自动建表并写入种子数据，但数据库本身需要提前创建：

```sql
CREATE DATABASE freshmeal DEFAULT CHARACTER SET utf8mb4;
```

### 2. 启动 Redis

按默认配置启动 Redis 7 即可，无需创建库或额外配置。

### 3. 启动后端

```bash
cd server
export JWT_SECRET_KEY="<替换为长度足够的随机字符串>"
mvn spring-boot:run
```

PowerShell：

```powershell
$env:JWT_SECRET_KEY = "<替换为长度足够的随机字符串>"
mvn spring-boot:run
```

可用的环境变量：

| 变量 | 默认值 |
|---|---|
| `JWT_SECRET_KEY` | 无，必须设置 |
| `DB_URL` | `jdbc:mysql://127.0.1:3306/freshmeal?...` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | `root` |
| `REDIS_HOST` | `127.0.1` |
| `REDIS_PORT` | `6379` |
| `SERVER_PORT` | `8080` |

启动后访问 `http://127.0.0.1:8080/swagger-ui.html` 查看接口文档。

### 4. 启动三个前端

三个工程分别安装依赖并启动，均将 `/api` 代理到 `http://127.0.0.1:8080`。

```bash
cd web-shop
pnpm install
pnpm dev
```

```bash
cd web-rider
pnpm install
pnpm dev
```

```bash
cd web-admin
pnpm install
pnpm dev
```

| 端 | 开发地址 |
|---|---|
| 用户端 | `http://127.0.0.1:5175` |
| 配送员端 | `http://127.0.0.1:5176` |
| 管理端 | `http://127.0.0.1:8848` |

## 种子账号

| 端 | 用户名 | 初始密码 |
|---|---|---|
| 用户端 | `user01` | `123456` |
| 配送员端 | `rider01` | `123456` |
| 管理端 | `admin` | `123456` |

## 常用命令

后端：

```bash
cd server
mvn test
```

前端（以 `web-shop` 为例，`web-rider` 相同）：

```bash
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

## 文档

- `docs/01-需求分析.md`
- `docs/02-数据库设计.md`
- `docs/03-系统总体设计.md`
- `docs/04-论文大纲与写作指引.md`
- `docs/05-开发计划与进度.md`
