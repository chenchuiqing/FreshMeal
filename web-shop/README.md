# FreshMeal 用户端

FreshMeal 用户端是面向移动 H5 的商城客户端，工程基于 [vue-h5-template 2.0.0](https://github.com/sunniejs/vue-h5-template) 二次开发，模板提交为 `d1780c1f1b64ab5be9e20c5fc8895d2c1045bc2d`。

## 技术栈

- Vue 3、TypeScript、Vite 8
- Vant 4、Pinia、Vue Router
- Axios、TanStack Query
- Vitest、Vue Test Utils、Stylelint

## 启动

要求 Node.js 22.12+、pnpm 9.12+，并先启动 FreshMeal 后端。

```bash
cd web-shop
pnpm install
pnpm dev
```

开发地址默认为 `http://127.0.0.1:5175`，`/api` 代理到 `http://127.0.0.1:8080`。

## 常用命令

```bash
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm api:generate
pnpm check
```

## 工程边界

- `src/api`：统一 `Result<T>` 请求客户端与业务接口模块。
- `src/store`：登录会话等客户端状态。
- `src/router`：显式路由与登录守卫。
- `src/views`：用户端页面。
- `openapi`：用户端接口契约与生成类型。

三个前端工程保持独立，不创建 `web-shared`。
