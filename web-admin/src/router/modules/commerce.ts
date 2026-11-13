import { $t } from "@/plugins/i18n";

export default {
  path: "/commerce",
  redirect: "/commerce/category/index",
  meta: {
    icon: "ri/shopping-cart-2-line",
    title: $t("menus.pureCommerce"),
    // 0.5 保证排在首页（rank=0）之后、其余整数 rank 菜单之前，避免与既有 rank 冲突
    rank: 0.5
  },
  children: [
    {
      path: "/commerce/category/index",
      name: "CommerceCategory",
      component: () => import("@/views/category/index.vue"),
      meta: {
        title: $t("menus.pureCategory"),
        icon: "ri/list-unordered",
        keepAlive: true,
        roles: ["admin"]
      }
    },
    {
      path: "/commerce/product/index",
      name: "CommerceProduct",
      component: () => import("@/views/product/index.vue"),
      meta: {
        title: $t("menus.pureProduct"),
        icon: "ri/shopping-bag-line",
        keepAlive: true,
        roles: ["admin"]
      }
    }
  ]
} satisfies RouteConfigsTable;
