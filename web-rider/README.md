# FreshMeal 配送端

FreshMeal 配送员端是面向配送员的移动 H5 工程。骑手账号由管理员创建，工程不提供注册入口。

## 技术栈

- Vue 3、TypeScript、Vite 8
- Vant 4、Pinia、Vue Router
- Axios、Vitest、Vue Test Utils

## 启动

要求 Node.js 22.12+、pnpm 9.12+，并先启动 FreshMeal 后端。

```bash
cd web-rider
pnpm install
pnpm dev
```

开发地址默认为 `http://127.0.0.1:5176`，`/api` 代理到 `http://127.0.0.1:8080`。

种子骑手账号为 `rider01`，初始密码为 `123456`。

## 常用命令

```bash
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm check
```

## 工程边界

- `src/api`：统一 `Result<T>` 请求客户端与骑手认证接口。
- `src/store`：骑手登录会话状态。
- `src/router`：显式路由与登录守卫。
- `src/views`：登录、任务大厅和我的页面。

三个前端工程保持独立，不创建 `web-shared`。
