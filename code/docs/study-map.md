# 学习地图（study-map）

> 面向"先读懂、再自己写"的路线：每个模块给出**读什么、对应飞书设计文档哪一章、动手练习点**。
> 飞书主文档：《TWS 蓝牙耳机整机装配 MES 项目全景文档》 https://my.feishu.cn/wiki/KmC9wjMyhioJgxkL95XcMRomnZd

## 0. 推荐阅读顺序（由浅入深）

```
登录/鉴权(Sa-Token) → 物料CRUD(标准三层模板) → 工艺路线版本+快照 → 工单状态机+乐观锁
→ 过站引擎八规则链(核心!) → 维修回流闭环 → FPY双口径SQL → 正反向追溯
```

## 1. 代码地图：模块 ↔ 飞书设计文档对照

| 代码位置 | 内容 | 飞书文档对应章节 | 学习重点 |
|----------|------|------------------|----------|
| `mes-base/AuthController + AuthService + StpInterfaceImpl` | 登录/角色 | 系统架构-鉴权 | Sa-Token 用法：拦截器、StpUtil、@SaCheckRole |
| `mes-base/MdMaterialService/Controller` | 物料 CRUD | 基础数据模块 | **全项目 CRUD 模板**：分页 Wrapper/唯一校验/统一返回 |
| `mes-base/MdRoutingService` | 工艺路线版本化 | 工单管理-路线版本 | 草稿→发布状态流、整单覆盖式保存、JSON 规则校验 |
| `mes-execution/WorkOrderService` | 工单 | 工单管理-状态机 | **快照**（创建时复制路线）、**乐观锁 @Version**、迁移表写法 |
| `mes-execution/rule/` 八个规则类 | 过站防呆 | 生产执行-防呆八大类 | **责任链模式**：接口+order+Spring 注入，加规则零改主流程 |
| `mes-execution/CheckinService` | 过站引擎 | 生产执行-过站时序图 | Redis SET NX 锁、唯一索引兜底、NG 开单、事件异步 |
| `mes-execution/SnService` | SN/绑定/上料 | 生产执行-一机多码 | 批量生成、绑定防重(uk)、上料批次 |
| `mes-quality/QualityService` | 不良+维修 | 质量管理 | 维修闭环状态流：OPEN→REPAIRED/SCRAP、RETEST 回流 |
| `mes-quality/QualityStatMapper + FpyService` | FPY 双口径 | 质量管理-良率双口径 | 统计 SQL 口径推导（一次直通 vs 最终良率） |
| `mes-trace/TraceService` | 追溯 | 追溯管理 | 正向档案装配、反向批次→整机、任务留痕 |
| `mes-tws-ui/src/views/exec/Simulator.vue` | 过站模拟器 | 生产执行-现场作业 | 动态表单（按规则渲染测试项）、异常提示 UX |
| `mes-tws-ui/src/views/quality/FpyReport.vue` | 良率报表 | 质量管理-看板 | ECharts 双轴趋势+柏拉图 |

## 2. 逐模块导读

### 2.1 先看懂一条过站请求（最重要的一条链路）

1. 前端 `Simulator.vue` 提交 `POST /api/station/checkin`
2. `WebConfig` 拦截器校验登录 → `ExecutionController.checkin`
3. `CheckinService.checkin`：
   - 工位码 → 工序；SN → 工单 → **快照工序**（不是主数据！这就是快照的意义）
   - Redis 锁 `SET NX PX 5000`（防双击/重放）
   - 八规则链按 order 执行（任一失败即拦截，错误信息面向操作工）
   - 落 `station_log`（checkin_key 唯一索引兜底并发）
   - OK → 推进 SN；NG → 开 `defect_record` OPEN
   - 发 `StationCheckinEvent` → `@Async` 更新工单数量（MQ 留位）

练习：把 `TestItemRule` 支持的 `op` 扩展出 `between`；给 `SeqRule` 写 3 个单测用例。

### 2.2 状态机与乐观锁

- `WorkOrderService.TRANSITIONS`：把"允许的前置状态+目标状态"写成表——新增流转只加一行。
- `@Version` 注解 + `OptimisticLockerInnerInterceptor`：两个计划员同时操作，后者 update 返回 0 行 → 提示刷新。

练习：用两个浏览器分别登录 admin/planner1，同时点"下达"同一工单，观察乐观锁提示。

### 2.3 FPY 双口径 SQL（面试必问）

口径推导（`QualityStatMapper.fpyByWo`）：
- `finished`：整机 SN status=DONE
- `firstPass`：完工 SN 中 LEFT JOIN（有 NG 记录或 retest_round>0 的 SN 集合）为 NULL
- `FPY = firstPass/finished`；`最终良率 = finished/(finished+SCRAP)`

为什么重测不污染 FPY：重测过站 `retest_round>0`，LEFT JOIN 子查询把它当"非一次直通"。

### 2.4 追溯链路

正向：`sn_registry` + `sn_binding`（多级绑定）+ `station_log`（时间线）+ `defect/repair`。
反向：`sn_registry.batch_no`（部件 SN 带批次）→ `parent_sn` → 整机清单。
简化说明：关键料（麦克风等）批次通过"部件 PCBA 批次"间接承载——真实工厂会在上料/工单侧再挂一层，见 study 练习 3.4。

## 3. 自己动手练习清单（按难度排序）

1. **入门**：给 `DictController` 的工序接口加"启用/停用"快捷接口；给物料列表加导出 CSV。
2. **前端**：把 `Material.vue` 复制成"客户管理"页（新增表+接口+菜单三件套），体会 CRUD 模板复制流程。
3. **规则扩展**（后端核心练习）：
   - 3.1 新增 `ShiftRule`：夜班 0:00-6:00 禁止过 AGING 站（练习：新 Bean 加 order 即入链）
   - 3.2 `TestItemRule` 加 `between` 运算与友好提示
   - 3.3 `LoadingRule` 升级：校验上料批次未冻结且在有效期内（当前只查"是否上过料"）
   - 3.4 反向追溯升级：把关键料批次（MIC/BAT）独立建 SN，绑定时登记，实现"物料批次→整机"直连追溯
4. **状态机**：加"强制关闭 FORCE_CLOSE"动作（任意状态→CLOSED，需 admin），并记操作日志表。
5. **性能专项**（对齐叙事压测故事）：
   - 用 JMeter/k6 对 `/api/station/checkin` 压 100 并发，看 P95；
   - 给 `station_log` 造 100 万行（存储过程循环插入），对比索引前后 `logPage` 查询；
   - 把 `StationEventListener` 换成 RabbitMQ（docker 起 rabbit，发布/消费两个类，主流程不动）。
6. **分表**：把 `station_log` 按月分表（`station_log_202608`...），写入路由 + 查询归并（进阶，可只做设计）。

## 4. 调试技巧

- 后端 SQL 日志已开（控制台直接看 MyBatis-Plus 打的 SQL）
- swagger：http://localhost:8080/swagger-ui.html （登录接口先拿 token，右上角 Authorize 填 `satoken` 头）
- 前端代理：`vite.config.js` 里 `/api → 8080`，改端口要同步改
- 想看 Redis 里的锁/token：`redis-cli keys 'mes:lock:*'` / `keys 'satoken:login:token:*'`

## 5. 面试串讲（把代码讲成故事）

- 为什么快照：客户审厂要求"按当时工艺执行"的证据 → 工单创建即固化路线版本（`plan_wo_operation`），路线升版不影响在制。
- 防重复过站三道防线：Redis 锁（挡并发双击）→ 规则链查库（挡重放）→ 唯一索引（兜底一切）。
- FPY 与最终良率并列：客户考核口径不同，重测机制保证两套口径都算得清。
- 事件异步：过站 P95 只包含"校验+落库"，统计走异步（学习版 Spring 事件，生产 MQ）。
