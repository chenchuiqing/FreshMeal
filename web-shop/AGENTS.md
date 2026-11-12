# FreshMeal 用户端工程规范

## 架构

- Vue 3 SFC 页面使用显式 Vue Router 路由懒加载。
- Pinia 仅管理客户端状态，TanStack Query 管理服务端状态。
- 页面不得直接导入 Axios，只能通过 `src/api/modules` 访问接口。
- 三个前端工程完全独立，禁止创建或引用 `web-shared`。

## 目录边界

- `src/api/client.ts`：请求、鉴权头、响应校验和错误归一化。
- `src/api/modules`：按业务域封装接口。
- `src/components`：可复用展示与交互组件。
- `src/composables`：Vue 生命周期和响应式编排。
- `src/store`：客户端状态。
- `src/router`：路由、元信息与守卫。
- `src/views`：页面级组合。
- `openapi`：后端契约与生成类型来源。

## API 规则

- 所有成功响应业务码固定为 `200`。
- 所有响应结构固定为 `code`、`message`、`data`、`timestamp`。
- 错误响应的 `data` 为 `null`，并保留语义化 HTTP 状态码。
- Bearer JWT 统一由请求拦截器附加。
- 不得使用 `any` 绕过接口契约。
- 契约变化必须更新 `openapi/schema.yaml` 并执行 `pnpm api:generate`。

## UI 规则

- 仅使用 Vant 4，不引入 NutUI、Varlet 或其他移动端组件库。
- 保持 44 CSS 像素触摸目标、安全区、键盘缩放和清晰焦点状态。
- 业务页面在 375px 宽度下不得出现横向滚动。
- 使用 `src/styles/index.scss` 的设计令牌，不重复建立局部色阶。

## 验证

```bash
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm check
```

修改路由、认证或关键用户流程时，应补充相应测试。
