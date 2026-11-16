# -*- coding: utf-8 -*-
"""Generate SVG diagrams for thesis chapters 1-3."""
import os
from svgkit import (Canvas, FONT, INK, LINE, BLUE_FILL, BLUE_STROKE, GREEN_FILL,
                    GREEN_STROKE, ORANGE_FILL, ORANGE_STROKE, PURPLE_FILL,
                    PURPLE_STROKE, GRAY_FILL, GRAY_STROKE, RED_FILL, RED_STROKE,
                    YELLOW_FILL, YELLOW_STROKE)

OUT = os.path.join(os.path.dirname(__file__), "..", "figures")
os.makedirs(OUT, exist_ok=True)


def fig_1_1():
    """系统总体功能结构图"""
    c = Canvas(940, 500)
    # root
    c.box(320, 24, 300, 52, ["轻食预制菜线上订购配送管理系统"], fill=PURPLE_FILL,
          stroke=PURPLE_STROKE, fs=16, bold=True)
    # bus
    c.line(470, 76, 470, 100, marker=None)
    c.line(160, 100, 780, 100, marker=None)
    cols = [
        (160, "用户端", BLUE_FILL, BLUE_STROKE, [
            "商品浏览与筛选", "购物车与结算", "订单与支付", "预约配送时段", "个人中心与地址",
        ]),
        (470, "管理端", GREEN_FILL, GREEN_STROKE, [
            "商品与分类管理", "订单管理", "配送调度指派", "批次与库存管理", "经营数据看板",
        ]),
        (780, "配送员端", ORANGE_FILL, ORANGE_STROKE, [
            "待接订单大厅", "取货与送达上报", "业绩统计",
        ]),
    ]
    for cx, name, fill, stroke, children in cols:
        c.line(cx, 100, cx, 124, marker=None)
        c.box(cx - 85, 124, 170, 44, name, fill=fill, stroke=stroke, fs=16, bold=True)
        # spine
        last_y = 212 + (len(children) - 1) * 52 + 20
        c.line(cx, 168, cx, 168, marker=None)
        c.poly([(cx, 168), (cx - 105, 168), (cx - 105, last_y)], marker=None)
        for i, ch in enumerate(children):
            y = 212 + i * 52
            c.line(cx - 105, y + 20, cx - 85, y + 20, marker=None)
            c.box(cx - 85, y, 170, 40, ch, fill="#ffffff", stroke=GRAY_STROKE, fs=14)
    return c.save(os.path.join(OUT, "fig-1-1.svg"))


def fig_2_1():
    """Spring Boot 自动配置流程图"""
    c = Canvas(760, 570)
    cx = 380
    nodes = [
        (24, 48, ["启动 Spring Boot 应用（SpringApplication.run）"], BLUE_FILL, BLUE_STROKE, 14),
        (100, 48, ["@SpringBootApplication 触发组件扫描与自动配置"], BLUE_FILL, BLUE_STROKE, 14),
        (176, 48, ["读取 META-INF/spring/*.imports 自动配置类清单"], BLUE_FILL, BLUE_STROKE, 14),
        (344, 48, ["注册条件满足的 Bean（数据源、Web、Redis 等）"], GREEN_FILL, GREEN_STROKE, 14),
        (420, 48, ["用户 @Configuration 定义的 Bean 覆盖默认配置"], GREEN_FILL, GREEN_STROKE, 14),
        (496, 48, ["ApplicationContext 就绪，系统启动完成"], GREEN_FILL, GREEN_STROKE, 14),
    ]
    xs, w = 170, 420
    for y, h, lines, fill, stroke, fs in nodes:
        c.box(xs, y, w, h, lines, fill=fill, stroke=stroke, fs=fs)
    # arrows between sequential boxes / diamond
    seq = [(24, 48, 100), (100, 48, 176), (176, 48, 270 - 42), (344, 48, 420), (420, 48, 496)]
    for y, h, nxt in seq:
        c.arrow(cx, y + h, cx, nxt)
    # condition diamond
    c.diamond(cx, 270, 440, 84, ["条件注解评估：@ConditionalOnClass /",
                                  "OnBean / OnProperty 等是否满足"], fs=13)
    c.label(424, 328, "是", fs=13)
    c.arrow(cx, 312, cx, 344)
    # skip branch
    c.box(620, 246, 126, 48, ["条件不满足", "跳过该配置"], fill=GRAY_FILL,
          stroke=GRAY_STROKE, fs=13)
    c.arrow(600, 270, 616, 270)
    c.label(608, 254, "否", fs=13)
    c.poly([(683, 294), (683, 520), (594, 520)], marker="ar")
    return c.save(os.path.join(OUT, "fig-2-1.svg"))


def fig_3_1():
    """角色用例图"""
    c = Canvas(980, 730)
    # system boundary
    c.raw('<rect x="210" y="16" width="560" height="694" rx="8" fill="none" '
          'stroke="#a0aec0" stroke-width="1.5" stroke-dasharray="8 6"/>')
    c.text(490, 36, "轻食预制菜线上订购配送管理系统", fs=14, bold=True, fill="#4a5568")
    # actors
    c.actor(90, 60, "消费者")
    c.actor(90, 430, "配送员")
    c.actor(890, 60, "商家管理员")
    c.actor(890, 470, "超级管理员")
    left = [(80, "浏览商品与搜索"), (148, "加入购物车"), (216, "提交订单与支付"),
            (284, "预约配送时段"), (352, "评价与个人中心")]
    left_r = [(460, "任务大厅抢单"), (528, "确认取货"), (596, "确认送达"), (664, "查看业绩统计")]
    right = [(80, "商品与分类管理"), (148, "订单处理与干预"), (216, "配送调度指派"),
             (284, "批次与库存管理"), (352, "经营数据看板")]
    right_s = [(500, "用户与账号管理"), (568, "系统配置管理"), (636, "全部数据权限")]
    for cy, t in left:
        c.ellipse(340, cy, 180, 52, t, fs=13)
        c.line(112, 110, 250, cy, marker=None, color="#718096", sw=1.3)
    for cy, t in left_r:
        c.ellipse(340, cy, 180, 52, t, fill="#fffaf0", stroke=ORANGE_STROKE, fs=13)
        c.line(112, 480, 250, cy, marker=None, color="#718096", sw=1.3)
    for cy, t in right:
        c.ellipse(640, cy, 180, 52, t, fill="#f0fff4", stroke=GREEN_STROKE, fs=13)
        c.line(868, 110, 730, cy, marker=None, color="#718096", sw=1.3)
    for cy, t in right_s:
        c.ellipse(640, cy, 180, 52, t, fill="#f3eaff", stroke=PURPLE_STROKE, fs=13)
        c.line(868, 520, 730, cy, marker=None, color="#718096", sw=1.3)
    return c.save(os.path.join(OUT, "fig-3-1.svg"))


def fig_3_2():
    """系统业务流程图"""
    c = Canvas(980, 800)
    W, H = 170, 44

    def box(cx, y, lines, fill=BLUE_FILL, stroke=BLUE_STROKE, h=H, fs=14):
        c.box(cx - W / 2, y, W, h, lines, fill=fill, stroke=stroke, fs=fs)

    x1, x2 = 280, 700
    # column 1
    box(x1, 30, "浏览商品")
    box(x1, 104, "加入购物车")
    box(x1, 178, "结算预览")
    box(x1, 252, "提交订单")
    c.diamond(x1, 350, 200, 76, ["库存充足？"], fs=14)
    box(x1, 414, ["扣减库存（乐观锁+FEFO）"], h=48, fs=13)
    box(x1, 498, "用户支付")
    c.diamond(x1, 590, 200, 76, ["支付成功？"], fs=14)
    box(x1, 654, "商家接单")
    box(x1, 728, "备餐出餐")
    seq1 = [30, 104, 178, 252]
    for i in range(len(seq1) - 1):
        c.arrow(x1, seq1[i] + H, x1, seq1[i + 1])
    c.arrow(x1, 296, x1, 312)          # 提交订单 -> 库存菱形
    c.arrow(x1, 388, x1, 414)          # 是
    c.label(x1 + 46, 401, "是", fs=13)
    c.arrow(x1, 462, x1, 498)
    c.arrow(x1, 542, x1, 552)
    c.arrow(x1, 628, x1, 654)
    c.label(x1 + 46, 641, "是", fs=13)
    c.arrow(x1, 698, x1, 728)
    # negative branches
    c.box(20, 395, 150, 48, ["提示库存不足", "返回购物车"], fill=RED_FILL,
          stroke=RED_STROKE, fs=13)
    c.poly([(x1 - 100, 350), (95, 350), (95, 395)], marker="ar", color=RED_STROKE)
    c.label(140, 340, "否", fs=13, fill=RED_STROKE)
    c.box(20, 635, 150, 56, ["超时任务自动取消", "回补库存与批次"], fill=RED_FILL,
          stroke=RED_STROKE, fs=12)
    c.poly([(x1 - 100, 590), (95, 590), (95, 635)], marker="ar", color=RED_STROKE)
    c.label(140, 580, "否", fs=13, fill=RED_STROKE)
    # wrap connector col1 -> col2
    c.poly([(x1, 772), (x1, 786), (560, 786), (560, 16), (x2, 16)], marker="ar")
    # column 2
    c.diamond(x2, 70, 200, 76, ["是否预约时段？"], fs=14)
    box(x2, 132, "待配送时段（预约单）", h=44, fs=13)
    box(x2, 220, "待配送", fill=GREEN_FILL, stroke=GREEN_STROKE)
    box(x2, 284, "生成配送单", fill=GREEN_FILL, stroke=GREEN_STROKE)
    box(x2, 344, ["分配配送单", "系统派单 / 抢单 / 指派"], h=56, fs=13)
    box(x2, 426, "配送员取货", fill=ORANGE_FILL, stroke=ORANGE_STROKE)
    box(x2, 486, "配送中", fill=ORANGE_FILL, stroke=ORANGE_STROKE)
    box(x2, 550, "送达上报", fill=ORANGE_FILL, stroke=ORANGE_STROKE)
    box(x2, 614, "订单完成", fill=GREEN_FILL, stroke=GREEN_STROKE)
    box(x2, 678, "用户评价", fill=PURPLE_FILL, stroke=PURPLE_STROKE)
    c.arrow(x2, 108, x2, 132)
    c.label(x2 + 52, 120, "是", fs=13)
    c.arrow(x2, 176, x2, 220)
    c.label(x2 + 130, 198, "到点定时触发", fs=12)
    # arrows between column-2 nodes (explicit heights: 分配配送单 box is taller)
    seq2 = [(220, 44), (284, 44), (344, 56), (426, 44), (486, 44), (550, 44),
            (614, 44)]
    for i in range(len(seq2) - 1):
        y, h = seq2[i]
        c.arrow(x2, y + h, x2, seq2[i + 1][0])
    # 否 branch of reservation diamond
    c.poly([(x2 + 100, 70), (870, 70), (870, 242), (x2 + 85, 242)], marker="ar")
    c.label(848, 92, "否", fs=13)
    return c.save(os.path.join(OUT, "fig-3-2.svg"))


def fig_3_3():
    """订单状态机 UML 图"""
    c = Canvas(1020, 770)
    W, H, FS = 150, 56, 14
    row1 = [(60, ["待支付", "WAIT_PAY(0)"]), (250, ["待接单", "WAIT_ACCEPT(1)"]),
            (440, ["备餐中", "PREPARING(2)"]), (630, ["待配送时段", "WAIT_SLOT(3)"]),
            (820, ["待配送", "WAIT_DELIVER(4)"])]
    for x, lines in row1:
        c.box(x, 100, W, H, lines, fill=BLUE_FILL, stroke=BLUE_STROKE, fs=FS)
    # initial pseudostate
    c.parts.append('<circle cx="24" cy="128" r="9" fill="#1a202c"/>')
    c.arrow(36, 128, 56, 128)
    # row1 arrows + labels
    for gap in (210, 400, 590, 780):
        c.arrow(gap + 2, 128, gap + 38, 128)
    c.label(230, 86, "支付成功", fs=12)
    c.label(420, 86, "商家接单", fs=12)
    c.label(610, 86, "商家出餐", fs=12)
    c.label(800, 86, "到点激活", fs=12)
    # row2
    c.box(560, 340, W, H, ["已完成", "FINISHED(6)"], fill=GREEN_FILL, stroke=GREEN_STROKE, fs=FS)
    c.box(820, 340, W, H, ["配送中", "DELIVERING(5)"], fill=ORANGE_FILL,
          stroke=ORANGE_STROKE, fs=FS)
    c.arrow(895, 156, 895, 336)
    c.label(962, 250, "分配·取货", fs=12)
    c.arrow(816, 368, 714, 368)
    c.label(767, 332, "送达", fs=12)
    # cancelled
    c.box(620, 560, 170, H, ["已取消", "CANCELLED(9)"], fill=RED_FILL, stroke=RED_STROKE, fs=FS)
    c.poly([(85, 156), (85, 588), (615, 588)], marker="arr", color=RED_STROKE)
    c.label(350, 574, "用户取消 / 支付超时", fs=12, fill=RED_STROKE)
    c.poly([(325, 156), (325, 530), (645, 530), (645, 556)], marker="arr", color=RED_STROKE)
    c.label(490, 518, "商家拒单", fs=12, fill=RED_STROKE)
    # refund chain
    c.box(780, 680, 160, H, ["退款中", "REFUNDING(7)"], fill=PURPLE_FILL,
          stroke=PURPLE_STROKE, fs=FS)
    c.box(540, 680, 160, H, ["已退款", "REFUNDED(8)"], fill=PURPLE_FILL,
          stroke=PURPLE_STROKE, fs=FS)
    c.poly([(580, 396), (580, 640), (860, 640), (860, 676)], marker="ar")
    c.label(700, 626, "售后退款", fs=12)
    c.arrow(776, 708, 704, 708)
    c.label(740, 690, "退款完成", fs=12)
    return c.save(os.path.join(OUT, "fig-3-3.svg"))


def fig_3_4():
    """配送业务流程图"""
    c = Canvas(1020, 400)
    W, H = 150, 52
    y1 = 60
    c.box(30, y1, W, H, "订单进入待配送", fill=GREEN_FILL, stroke=GREEN_STROKE, fs=14)
    c.box(220, y1, W, H, ["生成配送单", "状态：待分配"], fill=BLUE_FILL,
          stroke=BLUE_STROKE, fs=13)
    c.diamond(490, 86, 190, 100, ["分配方式"], fs=14)
    c.arrow(180 + 2, 86, 220 - 4, 86)
    c.arrow(370 + 2, 86, 395 - 4, 86)
    # branch boxes
    c.box(615, 6, 140, 40, "系统智能派单", fill=GREEN_FILL, stroke=GREEN_STROKE, fs=13)
    c.box(615, 66, 140, 40, "配送员抢单", fill=ORANGE_FILL, stroke=ORANGE_STROKE, fs=13)
    c.box(615, 126, 140, 40, "管理员指派", fill=PURPLE_FILL, stroke=PURPLE_STROKE, fs=13)
    c.arrow(540, 61, 611, 30)
    c.arrow(585, 86, 611, 86)
    c.arrow(540, 111, 611, 142)
    # converge
    c.box(790, y1, W, H, "配送员确认接单", fill=BLUE_FILL, stroke=BLUE_STROKE, fs=14)
    c.poly([(755, 26), (770, 26), (770, 86), (786, 86)])
    c.line(755, 86, 786, 86)
    c.poly([(755, 146), (778, 146), (778, 86), (786, 86)])
    # wrap to row2
    c.poly([(865, 112), (865, 220), (105, 220), (105, 256)], marker="ar")
    y2 = 260
    row2 = [(30, "到店取货"), (220, "配送中"), (410, "到达收货地址"),
            (600, "状态：已送达"), (790, "订单完成")]
    for x, t in row2:
        c.box(x, y2, W, H, t, fill=ORANGE_FILL if x in (30, 220, 410) else GREEN_FILL,
              stroke=ORANGE_STROKE if x in (30, 220, 410) else GREEN_STROKE, fs=14)
    for x in (200, 390, 580, 770):
        c.arrow(x + 2, y2 + H / 2, x + 38, y2 + H / 2)
    c.label(200, 246, "确认取货", fs=12)
    c.label(390, 246, "取货完成", fs=12)
    c.label(580, 246, "到达地址", fs=12)
    c.label(770, 246, "确认送达", fs=12)
    c.text(510, 350, "配送单与订单状态全程联动，轨迹写入配送轨迹表", fs=13,
           fill="#4a5568", italic=True)
    return c.save(os.path.join(OUT, "fig-3-4.svg"))


if __name__ == "__main__":
    for fn in (fig_1_1, fig_2_1, fig_3_1, fig_3_2, fig_3_3, fig_3_4):
        print("wrote", fn())
