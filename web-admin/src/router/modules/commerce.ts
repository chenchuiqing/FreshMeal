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
    },
    {
      path: "/commerce/delivery/index",
      name: "CommerceDelivery",
      component: () => import("@/views/delivery/index.vue"),
      meta: {
        title: $t("menus.pureDelivery"),
        icon: "ri/truck-line",
        keepAlive: true,
        roles: ["admin"]
      }
    },
    {
      path: "/commerce/order/index",
      name: "CommerceOrder",
      component: () => import("@/views/order/index.vue"),
      meta: {
        title: $t("menus.pureOrderManage"),
        icon: "ri/file-list-3-line",
        keepAlive: true,
        roles: ["admin"]
      }
    },
    {
      path: "/commerce/rider/index",
      name: "CommerceRider",
      component: () => import("@/views/delivery/rider.vue"),
      meta: {
        title: $t("menus.pureRiderManage"),
        icon: "ri/riders-line",
        keepAlive: true,
        roles: ["admin"]
      }
    },
    {
      path: "/commerce/delivery-config/index",
      name: "CommerceDeliveryConfig",
      component: () => import("@/views/delivery/config.vue"),
      meta: {
        title: $t("menus.pureDeliveryConfig"),
        icon: "ri/time-line",
        keepAlive: true,
        roles: ["admin"]
      }
    },
    {
      path: "/commerce/dispatch/index",
      name: "CommerceDispatchLog",
      component: () => import("@/views/delivery/dispatch.vue"),
      meta: {
        title: $t("menus.pureDispatchLog"),
        icon: "ri/route-line",
        keepAlive: true,
        roles: ["admin"]
      }
    },
    {
      path: "/commerce/inventory/index",
      name: "CommerceInventory",
      component: () => import("@/views/inventory/index.vue"),
      meta: {
        title: $t("menus.pureInventory"),
        icon: "ri/archive-line",
        keepAlive: true,
        roles: ["admin"]
      }
    }
  ]
} satisfies RouteConfigsTable;
