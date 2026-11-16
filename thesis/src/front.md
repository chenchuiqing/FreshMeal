# 摘要

随着健康意识增强与预制菜行业发展，轻食预制菜线上订购对系统提出了营养检索、效期管理与配送调度等要求。本文设计并实现了面向轻食预制菜场景的线上订购配送管理系统，服务消费者、配送员与商家管理员三类角色。系统采用 Vue 3 与 Spring Boot 前后端分离架构，以 MySQL 与 Redis 支撑数据层。论文完成了四项核心设计：以集中式迁移表与副作用钩子实现订单状态机管控；设计候选集筛选与距离、负载、评分三因子加权的派单算法，仿真实验表明其较最近距离策略将平均负载率由 39.8% 降至 13.2%；以策略模式实现起步价、里程、夜间附加与满额免运的可配置运费规则；设计乐观锁扣减与批次 FEFO 出库的双层库存模型，兼顾防超卖与效期管理。测试结果表明，78 项后端测试与 66 项前端测试全部通过，核心接口平均响应时间低于 500ms，注入、越权与金额篡改用例均被拦截，系统达到验收标准。

**关键词：** Spring Boot；轻食预制菜；在线订购；配送调度；状态机

# Abstract

With the growth of health awareness and the prepared-food industry, online ordering and delivery management for light-meal prepared foods raises specialized requirements in nutrition retrieval, batch expiry management, and dispatch scheduling. This thesis designs and implements an online ordering and delivery management system for light-meal prepared foods, serving three types of users: consumers, delivery riders, and merchant administrators. The system adopts a decoupled architecture built with Vue 3 and Spring Boot, using MySQL for persistence and Redis for caching. Four core designs are completed: order lifecycle management through a centralized state-transition table with side-effect hooks; a dispatch algorithm that filters candidates and ranks them by weighted distance, workload, and rating factors, where simulation results show the average rider workload dropping from 39.8% to 13.2% compared with the nearest-first strategy; a configurable freight rule implemented with the strategy pattern covering base fee, mileage fee, night surcharge, and free-shipping threshold; and a two-layer inventory model combining optimistic-lock stock deduction with batch-level FEFO picking to prevent overselling and respect expiry dates. Test results show that all 78 backend tests and 66 frontend tests pass, the average response time of three core interfaces is below 500 ms, and security cases such as injection, unauthorized access, and price tampering are effectively blocked. The system meets the acceptance criteria.

**Keywords:** Spring Boot; light-meal prepared foods; online ordering; delivery dispatch; state machine
