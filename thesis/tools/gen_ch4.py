# -*- coding: utf-8 -*-
"""Generate SVG diagrams for thesis chapter 4."""
import os
from svgkit import (Canvas, FONT, INK, LINE, BLUE_FILL, BLUE_STROKE, GREEN_FILL,
                    GREEN_STROKE, ORANGE_FILL, ORANGE_STROKE, PURPLE_FILL,
                    PURPLE_STROKE, GRAY_FILL, GRAY_STROKE, RED_FILL, RED_STROKE,
                    YELLOW_FILL, YELLOW_STROKE)

OUT = os.path.join(os.path.dirname(__file__), "..", "figures")
os.makedirs(OUT, exist_ok=True)


def fig_4_1():
    """系统三层（分层）架构图"""
    c = Canvas(980, 600)

    def band(y, h, title, fill="#f7fafc"):
        c.box(40, y, 900, h, "", fill=fill, stroke=GRAY_STROKE, rx=8)
        c.multiline(110, y + h / 2, title, fs=15, bold=True)

    def cell(x, y, w, h, lines, fill, stroke, fs=13):
        c.box(x, y, w, h, lines, fill=fill, stroke=stroke, fs=fs)

    band(24, 110, ["客户端层"])
    cell(190, 49, 230, 60, ["用户端商城", "Vue3 + Vant"], BLUE_FILL, BLUE_STROKE)
    cell(440, 49, 230, 60, ["管理后台", "Vue3 + Element Plus"], BLUE_FILL, BLUE_STROKE)
    cell(690, 49, 230, 60, ["配送员端", "Vue3 + Vant"], BLUE_FILL, BLUE_STROKE)

    band(154, 110, ["接入层"])
    cell(190, 179, 350, 60, ["Nginx", "静态资源与反向代理"], GRAY_FILL, GRAY_STROKE)
    cell(560, 179, 350, 60, ["Sa-Token 认证", "JWT 会话 + RBAC 角色"], ORANGE_FILL, ORANGE_STROKE)

    band(284, 160, ["应用层", "Spring Boot"], fill="#f0f7ff")
    cell(190, 304, 170, 56, ["Controller", "参数校验"], "#ffffff", BLUE_STROKE)
    cell(380, 304, 170, 56, ["Service", "业务/状态机"], "#ffffff", BLUE_STROKE)
    cell(570, 304, 170, 56, ["Mapper", "MyBatis-Plus"], "#ffffff", BLUE_STROKE)
    cell(760, 304, 170, 56, ["Job", "定时任务"], "#ffffff", BLUE_STROKE)
    cell(190, 374, 740, 50, ["配置与公共层：统一响应 / 全局异常 / 状态枚举 / 策略与切面"],
         "#ffffff", BLUE_STROKE, fs=13)

    band(464, 110, ["数据层"])
    cell(190, 489, 230, 60, ["MySQL 8", "业务数据与迁移"], GREEN_FILL, GREEN_STROKE)
    cell(440, 489, 230, 60, ["Redis", "会话 / 缓存 / 计数"], GREEN_FILL, GREEN_STROKE)
    cell(690, 489, 230, 60, ["本地文件", "商品图片"], GREEN_FILL, GREEN_STROKE)

    for y in (134, 264, 444):
        c.arrow(490, y + 2, 490, y + 18)
    return c.save(os.path.join(OUT, "fig-4-1.svg"))


def fig_4_2():
    """后端工程结构图"""
    c = Canvas(720, 590)
    rows = [
        ("com.freshmeal", "后端工程根包"),
        ("controller", "控制器层：参数校验、调用服务、统一响应"),
        ("service / impl", "业务层：状态机流转、事务边界、派单与运费算法"),
        ("mapper", "数据访问层：MyBatis-Plus 单表操作与统计 SQL"),
        ("entity", "实体类：与数据库表一一对应"),
        ("dto、vo", "入参与出参对象，隔离数据库结构"),
        ("enums", "状态枚举：订单、配送、支付状态集中定义"),
        ("strategy", "策略：运费计算规则、派单评分算法"),
        ("security", "认证鉴权：Sa-Token JWT、注解式 RBAC"),
        ("config", "配置：分页插件、CORS、Redis、Swagger"),
        ("common", "公共：Result 统一响应、全局异常处理"),
        ("job", "定时任务：支付超时取消、预约时段激活"),
    ]
    root_x, child_x = 36, 76
    y0, step = 40, 44
    last_y = y0 + (len(rows) - 1) * step
    c.line(root_x + 14, y0 + 14, root_x + 14, last_y + 8, marker=None, color="#a0aec0")
    for i, (name, note) in enumerate(rows):
        y = y0 + i * step
        if i == 0:
            c.text(child_x, y + 8, name, fs=15, bold=True, anchor="start",
                   family="'Consolas',monospace")
            c.text(300, y + 8, note, fs=13, anchor="start", fill="#4a5568")
        else:
            c.line(root_x + 14, y + 8, child_x - 16, y + 8, marker=None, color="#a0aec0")
            branch = "└──" if i == len(rows) - 1 else "├──"
            c.text(child_x - 14, y + 8, branch, fs=13, anchor="start",
                   family="'Consolas',monospace", fill="#718096")
            c.text(child_x + 16, y + 8, name, fs=14, anchor="start",
                   family="'Consolas',monospace", bold=True)
            c.text(300, y + 8, note, fs=13, anchor="start", fill="#4a5568")
    return c.save(os.path.join(OUT, "fig-4-2.svg"))


def _entity(c, x, y, name, cn, w=170, h=64):
    c.box(x, y, w, h, "", fill="#ffffff", stroke=BLUE_STROKE, rx=4)
    c.text(x + w / 2, y + 21, name, fs=14, bold=True, family="'Consolas',monospace")
    c.text(x + w / 2, y + 45, cn, fs=12, fill="#4a5568")


def _rel(c, cx, cy, verb, w=110, h=44, fill=YELLOW_FILL, stroke=YELLOW_STROKE):
    c.parts.append(
        f'<polygon points="{cx},{cy - h / 2} {cx + w / 2},{cy} {cx},{cy + h / 2} '
        f'{cx - w / 2},{cy}" fill="{fill}" stroke="{stroke}" stroke-width="1.5"/>'
    )
    c.text(cx, cy, verb, fs=13)


def fig_4_3():
    """数据库 E-R 图"""
    c = Canvas(1180, 810)
    # user domain (boxes at x=40 w=170 => center 125)
    _entity(c, 40, 40, "t_user", "用户：账号·密码·累计消费")
    _rel(c, 125, 140, "拥有", w=100, h=40)
    _entity(c, 40, 180, "t_address", "收货地址：省市区·经纬度")
    c.line(125, 104, 125, 118)
    c.line(125, 162, 125, 180)
    # order
    _entity(c, 360, 140, "t_order", "订单：金额·状态机核心")
    _rel(c, 275, 172, "下单", w=100, h=44)
    c.poly([(210, 72), (275, 72), (275, 148)], marker="ar")
    c.line(325, 172, 360, 172)
    # order item
    _entity(c, 360, 360, "t_order_item", "订单明细：成交快照")
    _rel(c, 445, 310, "包含", w=100, h=40)
    c.line(445, 204, 445, 288)
    c.line(445, 332, 445, 360)
    # payment
    _entity(c, 40, 420, "t_payment", "支付流水：幂等凭据")
    _rel(c, 125, 370, "支付", w=100, h=40)
    c.poly([(385, 204), (385, 300), (125, 300), (125, 348)], marker="ar")
    c.line(125, 392, 125, 420)
    # delivery chain
    _entity(c, 700, 140, "t_delivery_order", "配送单：派单与状态")
    _rel(c, 600, 172, "配送", w=90, h=44)
    c.line(530, 172, 555, 172)
    c.line(645, 172, 700, 172)
    # delivery man (box 960..1130 => center 1045)
    _entity(c, 960, 40, "t_delivery_man", "配送员：位置·负载·评分")
    _rel(c, 1045, 140, "承接", w=110, h=40)
    c.line(870, 140, 986, 140)
    c.line(1045, 104, 1045, 118)
    # delivery area
    _entity(c, 700, 360, "t_delivery_area", "配送区域：运费规则")
    _rel(c, 785, 310, "所属区域", w=120, h=40)
    c.line(785, 204, 785, 288)
    c.line(785, 332, 785, 360)
    # product domain
    _entity(c, 400, 540, "t_category", "商品分类：二级分类")
    _rel(c, 645, 572, "属于", w=110, h=44)
    _entity(c, 720, 540, "t_product", "商品：营养·效期·库存", w=150)
    c.line(570, 572, 586, 572)
    c.line(704, 572, 716, 572)
    # spec & batch
    _entity(c, 710, 710, "t_product_spec", "商品规格：份量·加价", w=170)
    _rel(c, 795, 650, "包含规格", w=120, h=40)
    c.line(795, 604, 795, 628)
    c.line(795, 672, 795, 710)
    _entity(c, 1020, 540, "t_product_batch", "商品批次：FEFO 出库", w=150)
    _rel(c, 945, 572, "批次", w=100, h=44)
    c.line(870, 572, 891, 572)
    c.line(999, 572, 1016, 572)
    # cardinality hints
    for x, y, s in ((342, 160, "1:N"), (432, 240, "1:N"), (545, 160, "1:0..1"),
                    (925, 128, "N:1"), (432, 348, "1:N"), (645, 538, "N:1"),
                    (825, 616, "1:N"), (945, 538, "1:N"), (155, 170, "1:N"),
                    (155, 405, "1:0..1"), (772, 274, "1:0..1")):
        c.text(x, y, s, fs=11, fill="#2d3748")
    c.text(60, 660, "注：1:N 一对多，1:0..1 一对一（可选）", fs=12, anchor="start",
           fill="#4a5568", italic=True)
    return c.save(os.path.join(OUT, "fig-4-3.svg"))


def fig_4_4():
    """订单状态迁移图（含触发方）"""
    c = Canvas(900, 750)
    # legend
    legend = [(60, "用户", BLUE_STROKE), (240, "商家", ORANGE_STROKE),
              (420, "系统/定时任务", GREEN_STROKE), (650, "配送员", PURPLE_STROKE),
              (790, "取消/异常", RED_STROKE)]
    for x, name, col in legend:
        c.parts.append(f'<circle cx="{x}" cy="36" r="6" fill="{col}"/>')
        c.text(x + 14, 36, name, fs=12, anchor="start")
    W, H, FS = 150, 56, 13
    row1 = [(60, ["待支付", "WAIT_PAY(0)"]), (270, ["待接单", "WAIT_ACCEPT(1)"]),
            (480, ["备餐中", "PREPARING(2)"]), (690, ["待配送时段", "WAIT_SLOT(3)"])]
    for x, lines in row1:
        c.box(x, 100, W, H, lines, fill=BLUE_FILL, stroke=BLUE_STROKE, fs=FS)
    c.parts.append('<circle cx="24" cy="128" r="9" fill="#1a202c"/>')
    c.arrow(36, 128, 56, 128)
    for gap in (210, 420, 630):
        c.arrow(gap + 2, 128, gap + 58, 128)
    c.label(240, 86, "支付成功", fs=12, fill=BLUE_STROKE)
    c.label(450, 86, "商家接单", fs=12, fill=ORANGE_STROKE)
    c.label(660, 86, "商家出餐", fs=12, fill=ORANGE_STROKE)
    # row2 (flows right -> left)
    c.box(690, 300, W, H, ["待配送", "WAIT_DELIVER(4)"], fill=BLUE_FILL,
          stroke=BLUE_STROKE, fs=FS)
    c.box(370, 300, W, H, ["配送中", "DELIVERING(5)"], fill=ORANGE_FILL,
          stroke=ORANGE_STROKE, fs=FS)
    c.box(160, 300, W, H, ["已完成", "FINISHED(6)"], fill=GREEN_FILL,
          stroke=GREEN_STROKE, fs=FS)
    # slot -> wait deliver (scheduled)
    c.arrow(765, 156, 765, 296)
    c.label(826, 230, "到点定时触发", fs=12, fill=GREEN_STROKE)
    # preparing -> wait deliver (direct)
    c.poly([(555, 156), (555, 264), (720, 264), (720, 296)], marker="ar")
    c.label(637, 252, "立即配送（非预约单）", fs=12, fill=GREEN_STROKE)
    # dispatch -> delivering -> finished
    c.arrow(686, 328, 524, 328)
    c.label(607, 296, "派单/抢单·取货", fs=12, fill=PURPLE_STROKE)
    c.arrow(366, 328, 314, 328)
    c.label(342, 296, "送达", fs=12, fill=PURPLE_STROKE)
    # cancelled
    c.box(330, 520, 220, H, ["已取消", "CANCELLED(9)"], fill=RED_FILL,
          stroke=RED_STROKE, fs=FS)
    c.poly([(135, 156), (135, 548), (325, 548)], marker="arr", color=RED_STROKE)
    c.label(240, 534, "用户取消 / 支付超时", fs=12, fill=RED_STROKE)
    c.poly([(345, 156), (345, 500), (440, 500), (440, 516)], marker="arr", color=RED_STROKE)
    c.label(392, 488, "商家拒单", fs=12, fill=RED_STROKE)
    c.poly([(765, 356), (765, 470), (515, 470), (515, 516)], marker="arr", color=RED_STROKE)
    c.label(640, 458, "管理员干预取消", fs=12, fill=RED_STROKE)
    # refund
    c.box(150, 660, 170, H, ["退款中", "REFUNDING(7)"], fill=PURPLE_FILL,
          stroke=PURPLE_STROKE, fs=FS)
    c.box(440, 660, 170, H, ["已退款", "REFUNDED(8)"], fill=PURPLE_FILL,
          stroke=PURPLE_STROKE, fs=FS)
    c.poly([(235, 356), (235, 632), (235, 656)], marker="ar")
    c.label(300, 470, "售后退款申请", fs=12)
    c.arrow(324, 688, 436, 688)
    c.label(380, 670, "退款完成", fs=12, fill=PURPLE_STROKE)
    return c.save(os.path.join(OUT, "fig-4-4.svg"))


def fig_4_5():
    """派单算法流程图"""
    c = Canvas(860, 930)
    cx = 430
    c.ellipse(cx, 46, 180, 54, "开始派单", fill=GRAY_FILL, stroke=GRAY_STROKE, fs=15)
    boxes = [
        (90, 52, ["订单流转至待配送，触发派单"], BLUE_FILL, BLUE_STROKE, 14),
        (178, 88, ["候选集筛选（SQL 条件）：",
                   "账号启用 & 在线 & 空闲；覆盖收货区域；",
                   "在途单量 < 负载上限"], "#ffffff", BLUE_STROKE, 13),
        (436, 96, ["三因子归一化打分：",
                   "距离分 = max(0, 1 − d/10km)",
                   "负载分 = 1 − 在途单量/上限",
                   "评分分 = rating / 5"], GREEN_FILL, GREEN_STROKE, 13),
        (556, 56, ["加权求和：score = 0.5×距离 + 0.3×负载 + 0.2×评分"],
         GREEN_FILL, GREEN_STROKE, 13),
        (636, 52, ["按综合得分降序排序，取最高者"], GREEN_FILL, GREEN_STROKE, 14),
        (712, 52, ["写入派单日志（各因子得分与选中结果）"], "#ffffff", BLUE_STROKE, 14),
        (788, 52, ["分配配送单，状态置为已分配"], ORANGE_FILL, ORANGE_STROKE, 14),
    ]
    for y, h, lines, fill, stroke, fs in boxes:
        c.box(cx - 210, y, 420, h, lines, fill=fill, stroke=stroke, fs=fs)
    c.arrow(cx, 142, cx, 178)
    c.arrow(cx, 266, cx, 300)
    # decision
    c.diamond(cx, 340, 340, 80, ["候选集为空？"], fs=14)
    c.arrow(cx, 380, cx, 436)
    c.label(cx + 44, 408, "否", fs=13)
    # empty branch
    c.box(640, 316, 200, 56, ["等待配送员抢单", "或管理员指派"], fill=GRAY_FILL,
          stroke=GRAY_STROKE, fs=13)
    c.arrow(600, 340, 636, 340)
    c.label(618, 324, "是", fs=13)
    for i in range(len(boxes) - 1):
        y, h = boxes[i][0], boxes[i][1]
        ny = boxes[i + 1][0]
        c.arrow(cx, y + h, cx, ny)
    c.arrow(cx, 840, cx, 858)
    c.ellipse(cx, 886, 180, 54, "结束", fill=GRAY_FILL, stroke=GRAY_STROKE, fs=15)
    return c.save(os.path.join(OUT, "fig-4-5.svg"))


def fig_4_6():
    """运费计算流程图"""
    c = Canvas(810, 950)
    cx = 400
    c.ellipse(cx, 44, 180, 54, "开始计费", fill=GRAY_FILL, stroke=GRAY_STROKE, fs=15)
    boxes = [
        (90, 48, ["结算预览 / 提交订单触发运费计算"], BLUE_FILL, BLUE_STROKE, 14),
        (162, 48, ["读取门店坐标与收货地址经纬度"], BLUE_FILL, BLUE_STROKE, 14),
        (234, 48, ["Haversine 公式计算直线距离 d"], BLUE_FILL, BLUE_STROKE, 14),
        (306, 48, ["按区县/城市匹配启用中的配送区域"], BLUE_FILL, BLUE_STROKE, 14),
        (470, 46, ["运费 = 起步价（含首公里）"], "#ffffff", GREEN_STROKE, 14),
        (540, 46, ["运费 += max(0, d − 1km) × 每公里单价"], "#ffffff", GREEN_STROKE, 14),
        (830, 46, ["运费保留两位小数并返回"], ORANGE_FILL, ORANGE_STROKE, 14),
    ]
    for y, h, lines, fill, stroke, fs in boxes:
        c.box(cx - 190, y, 380, h, lines, fill=fill, stroke=stroke, fs=fs)
    c.arrow(cx, 71, cx, 90)
    for i in range(3):
        y, h = boxes[i][0], boxes[i][1]
        c.arrow(cx, y + h, cx, boxes[i + 1][0])
    # decision 1: area matched
    c.diamond(cx, 406, 340, 76, ["匹配到配送区域？"], fs=14)
    c.arrow(cx, 354, cx, 368)
    c.arrow(cx, 444, cx, 470)
    c.label(cx + 44, 457, "是", fs=13)
    c.box(610, 386, 180, 44, ["结算报错", "拒绝下单"], fill=RED_FILL,
          stroke=RED_STROKE, fs=13)
    c.arrow(570, 406, 606, 406)
    c.label(588, 390, "否", fs=13, fill=RED_STROKE)
    # decision 2: night
    c.diamond(cx, 646, 340, 76, ["夜间时段 22:00-06:00？"], fs=13)
    c.arrow(cx, 586, cx, 608)
    c.arrow(cx, 684, cx, 712)
    c.label(cx + 44, 698, "否", fs=13)
    c.box(610, 626, 180, 44, ["加收夜间附加费"], fill=YELLOW_FILL,
          stroke=YELLOW_STROKE, fs=13)
    c.arrow(570, 646, 606, 646)
    c.label(588, 630, "是", fs=13)
    c.poly([(700, 670), (800, 670), (800, 806), (500, 806), (500, 826)], marker="ar")
    # decision 3: free shipping
    c.diamond(cx, 750, 360, 76, ["商品金额 ≥ 免运门槛？"], fs=13)
    c.arrow(cx, 788, cx, 830)
    c.label(cx + 44, 810, "否", fs=13)
    c.box(610, 730, 180, 44, ["运费置 0", "（满额免运）"], fill=GREEN_FILL,
          stroke=GREEN_STROKE, fs=13)
    c.arrow(580, 750, 606, 750)
    c.label(593, 734, "是", fs=13)
    c.poly([(700, 774), (700, 806)], marker=None)
    c.arrow(cx, 876, cx, 894)
    c.ellipse(cx, 922, 180, 54, "计费完成", fill=GRAY_FILL, stroke=GRAY_STROKE, fs=15)
    return c.save(os.path.join(OUT, "fig-4-6.svg"))


def fig_4_7():
    """支付时序图"""
    c = Canvas(1120, 690)
    heads = [
        (110, ["用户"]),
        (330, ["用户端（前端）"]),
        (560, ["后端订单支付服务"]),
        (800, ["模拟收银台"]),
        (1010, ["数据库", "MySQL"]),
    ]
    hw = 150
    for x, lines in heads:
        c.box(x - hw / 2, 30, hw, 48, lines, fill=BLUE_FILL, stroke=BLUE_STROKE, fs=14)
        c.line(x, 78, x, 656, marker=None, color="#a0aec0", dash="6 6", sw=1.3)

    def msg(y, x1, x2, text, color=LINE):
        c.line(x1, y, x2, y, marker="ar", color=color)
        c.text((x1 + x2) / 2, y - 14, text, fs=13)

    def selfmsg(y, x, text):
        c.poly([(x, y - 8), (x + 46, y - 8), (x + 46, y + 14), (x + 2, y + 14)],
               marker="ar")
        c.text(x + 54, y + 3, text, fs=13, anchor="start")

    msg(120, 110, 330, "1. 点击支付，选择支付方式")
    msg(165, 330, 560, "2. POST /api/shop/order/{orderNo}/pay")
    msg(210, 560, 1010, "3. 创建支付流水（status=待支付，写超时时间）")
    msg(255, 560, 330, "4. 返回收银台参数")
    msg(300, 330, 800, "5. 跳转模拟收银台，选择微信/支付宝")
    msg(345, 800, 560, "6. POST /pay/mock/notify 支付回调")
    msg(390, 560, 1010, "7. 幂等校验：pay_no 唯一、status≠成功", color=RED_STROKE)
    selfmsg(440, 560, "8. 校验订单归属与状态")
    msg(500, 560, 1010, "9. 更新 t_payment 成功、t_order 已支付")
    selfmsg(550, 560, "10. 状态机：WAIT_PAY → WAIT_ACCEPT，写流转日志")
    msg(600, 560, 330, "11. 返回 code=200，订单进入待接单")
    msg(640, 330, 110, "12. 展示支付成功与状态时间轴")
    return c.save(os.path.join(OUT, "fig-4-7.svg"))


if __name__ == "__main__":
    for fn in (fig_4_1, fig_4_2, fig_4_3, fig_4_4, fig_4_5, fig_4_6, fig_4_7):
        print("wrote", fn())
