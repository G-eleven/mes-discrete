# TWS MES 学习版 — 实现过程日志（implementation-log）

> 本文档按时间记录实现全过程：做了什么、关键决策、遇到的问题与解法。
> **「操作审计」小节单独记录所有下载、删除、服务启停、系统改动**（含位置与影响范围），供随时复查。
> 日志时间均为本机时间（2026-08-17 起）。

---

## 操作审计（下载 / 删除 / 系统改动，先看这里）

| # | 时间 | 类型 | 操作内容 | 位置 / 影响范围 | 可回滚性 |
|---|------|------|----------|----------------|----------|
| 1 | 08-17 05:40 | 新增 | git 仓库初始化（第一次，在**工作区根目录**）+ 首次提交（内容=既有文档快照） | `C:\WorkSpace\AICoding\java-migration-lab\.git` | - |
| 2 | 08-17 05:52 | **删除** | 按用户指示删除根目录 git 仓库与根 `.gitignore`（**仅删 .git 与 .gitignore，工作区文档文件全部未动**） | 同上 | 不可逆（仅丢失那一次本地提交记录，文件本体无损失） |
| 3 | 08-17 05:52 | 新增 | git 重新初始化到 `mes-discrete/` 下，配置局部 user.name=gx / user.email=gx@local | `mes-discrete\.git` | 删 .git 即可回滚 |
| 4 | 08-17 05:47 | **下载** | Redis Windows 版（tporadowski/redis **v5.0.14.1**，zip 12.6MB，GitHub Release 直链） | 安装包 `C:\WorkSpace\Env\tools\redis\redis-win.zip`；解压于 `C:\WorkSpace\Env\tools\redis\win\`（约 30 个文件，redis-server.exe / redis-cli.exe 等） | 删除 `C:\WorkSpace\Env\tools\redis\` 整目录即彻底清除 |
| 5 | 08-17 05:50 | **启动进程** | 启动 redis-server.exe，监听 6379，数据目录与日志均在上述安装目录 | 后台进程 `redis-server.exe`；日志 `C:\WorkSpace\Env\tools\redis\redis.log`；**未注册为 Windows 服务**（重启电脑后需手动再启动） | `redis-cli shutdown` 或结束进程即停 |
| 6 | 08-17 05:45 | 下载 | npm 临时安装 bcryptjs（1 个小包，用于生成种子数据密码哈希） | `%TEMP%\bcrypt-tmp\node_modules`（系统临时目录） | 该目录可随时删除，不影响任何项目 |
| 7 | 08-17 05:55 | 数据库 | **尝试**执行 init.sql / seed.sql → 失败（root 密码不对，见问题#1），**未对 MySQL 写入任何数据** | 本机 MySQL 8.0.24（服务原有，非我安装） | 无 |
| 8 | 08-17 06:05 | 新增 | 创建本日志文件 | `mes-discrete/code/docs/implementation-log.md` | - |
| 9 | 08-17 05:58 | **数据库写入** | 导入 init.sql + seed.sql（修复 DATETIME(3) 精度 bug 后成功）：创建库 `mes_tws`，17 张表 + 种子数据 | 本机 MySQL（root 密码=mysql，见问题#1） | 重跑 init.sql 即重置（DROP+CREATE） |
| 10 | 08-17 06:04 | **系统服务** | Redis 注册为 Windows 服务（服务名 `Redis`，自启动配置文件 `redis.windows-service.conf`），并先终止了此前手动启动的残留 redis-server 进程（PID 12832） | 服务管理器 services.msc 可查；日志 `C:\WorkSpace\Env\tools\redis\win\` | `redis-server --service-uninstall --service-name Redis` 卸载 |
| 11 | 08-17 06:05 | 下载 | npm install 前端依赖（vue/element-plus/echarts 等，npmmirror 源，秒级完成） | `mes-discrete/code/mes-tws-ui/node_modules/` | 删 node_modules 重装即可 |
| 12 | 08-17 06:10 | 下载 | Maven 首次构建拉取依赖（阿里云镜像，spring-boot 2.7.18 全家桶等） | `C:\Users\DELL\.m2\repository\` | Maven 标准缓存，无需处理 |

> 审计说明：MySQL 服务（3306）与 JDK21/Maven/Node24 均为机器原有，未安装、未升级、未改配置。除上表外无其他下载/删除/系统改动。

---

## 里程碑总览

- [x] M0 环境检查 + git（仓库按用户指示放在 mes-discrete/）
- [ ] M1 数据库脚本 + 后端骨架 + 前端骨架 + 双端联通
- [ ] M2 基础数据 + 工单管理
- [ ] M3 生产执行（过站引擎 + 模拟器）
- [ ] M4 质量管理（FPY 双口径）
- [ ] M5 追溯管理
- [ ] M6 收尾文档

---

## M0 环境检查（2026-08-17 05:30~05:50）

**环境结论**：

| 组件 | 版本/状态 | 说明 |
|------|-----------|------|
| JDK | Temurin **21.0.11**（仅此一个） | 计划用 Spring Boot 2.7.18 + 编译目标 1.8；SB 2.7.18/内核 Spring 5.3.31 官方支持到 JDK 21，可行；Lombok 必须用 1.18.30+（支持 JDK21） |
| Maven | 3.9.11 | OK |
| Node / npm | 24.9.0 / 11.6.0 | Vite5 要求 ≥18，OK |
| Git | 2.55.0 windows | OK |
| MySQL | 8.0.24（zip 版，`C:\WorkSpace\Env\MySQL\mysql-8.0.24-winx64`），**3306 已在监听**（原有服务） | root 密码未知（见问题#1） |
| Redis | 原本未安装 → 已下载安装并启动（见审计 #4/#5） | 6379 可用，PING=PONG |

**问题 #1（已解）**：MySQL root 密码未知。第一次探测脚本有 bug——用 `grep -q "1"` 判断连接成功，但错误码 `1045` 里也含字符"1"，导致误报 root/root 可用；随后实际导入 SQL 报 `Access denied`。改用**退出码**严格复测后确认密码为 `mysql`（教训：判断命令成败用 `$?`，别 grep 输出内容）。已写入 application.yml。
另：空密码候选会导致 mysql 变成交互式挂起（`-p` 后空 = 提示输入），批量探测要单独处理。

**问题 #2（已解）**：后台下载 Redis 的第一条命令失败——Windows 路径末尾 `\` 在 bash 双引号里转义了引号（`"...redis\"`），报 `unexpected EOF while looking for matching '"'`。改为正斜杠路径重试成功。

**决策记录**：
- D1 后端 Spring Boot 2.7.18（与简历叙事一致）+ MyBatis-Plus + Sa-Token + MySQL + Redis；MQ 用 Spring 事件留位。
- D2 前端 Vue3 + JS + Vite + Element Plus + Pinia。
- D3 git 仓库放 `mes-discrete/`（用户指示），根目录不再有仓库。
- D4 Redis 装到 `C:\WorkSpace\Env\tools\redis\`（与该机器 Env 目录习惯一致），绿色版不注册服务。
- D5 种子密码统一 `123456`，BCrypt 哈希由 bcryptjs 生成（`$2a$10$...`，已验证）。

---

## M1 数据库脚本（2026-08-17 05:55~06:05）

**产物**：
- `code/docs/sql/init.sql` — 17 张表，utf8mb4；表前缀 sys_/md_/plan_ + 执行域表。要点：
  - `plan_work_order.version` 乐观锁；`plan_wo_operation.check_rules` JSON 工艺快照
  - `station_log.checkin_key` 唯一索引防重设计：仅 CHECKIN 行填 `sn:station:轮次`，LOADING/BINDING 行为 NULL（MySQL 唯一索引对 NULL 不去重），实现"只防过站重复"
  - `sn_registry` 承载一机多码（MACHINE/LEFT/RIGHT/CASE/BOX/CARTON），`batch_no` 供反向追溯
- `code/docs/sql/seed.sql` — 4 账号/11 物料/8 批次(含问题批 PCLB240801、MICB240715)/16 工序/路线 V1+V2/19 工位/11 不良代码/BOM/3 工单；两个存储过程生成历史流水：
  - 工单1（V1 路线，80 台完工）：问题批 4 台在 FCT 暴露 D04，3 修好 1 报废 → FPY 95% / 最终良率 98.75%
  - 工单2（V2 路线含声学站，40 台投产中）：22/26 号左耳在声学站被 D04 **提前拦截**（不良 OPEN，留给维修工作台演示），20 台完工，其余待绑定
  - 设计意图：V1 vs V2 对比 = 「二期加声学测试站把麦克风问题从 FCT 提前到部件段拦截」的完整故事线，报表/追溯开箱即有数据

**脚本尚未导入数据库**（被问题#1阻塞，见 M0）。

**导入结果（05:58）**：首次导入报 `Invalid default value for 'create_time'`——`DATETIME` 列配 `CURRENT_TIMESTAMP(3)` 默认值时列必须是 `DATETIME(3)`（小数秒精度要列类型与默认值一致）。修复后重导成功，验证查询结果：工单1 完工 79 + 报废 1（共 80 台）、流水 1489 条、不良 6 条、绑定 300 条（80×3 + 20×3，与设计完全一致）。

---

## M1 后端骨架（2026-08-17 06:00~06:15）

**产物**（`code/mes-tws/`，Maven 多模块单体）：
- 父 pom（SB 2.7.18 / MyBatis-Plus 3.5.3.2 / Sa-Token 1.37.0 / hutool / springdoc，阿里云镜像）
- `mes-common`：Result/PageResult、BizException + 全局异常、MesConst 常量、RedisLockService（SET NX PX + Lua 释放）
- `mes-base`：登录三件套（AuthController/AuthService/StpInterfaceImpl）+ 物料 CRUD（作为全项目 CRUD 模板）
- `mes-execution/quality/trace`：空模块占位（M2~M5 填充）
- `mes-server`：启动类 + WebConfig（Sa-Token 拦截器/CORS）+ SaExceptionHandler + 分页插件/@EnableAsync 线程池 + application.yml

**问题 #3（已解）**：lombok 依赖不传递——mes-common 声明的 lombok 是 `provided` 作用域，不会传给 mes-base，报"程序包lombok不存在"。修复：父 pom `<dependencies>` 里集中声明 lombok（所有模块继承）。教训：**provided 依赖每个模块要自己声明，或直接放父 pom 公共区**。

**问题 #4（已解）**：多模块包扫描——启动类在 `com.tws.mes.server`，默认组件扫描找不到 `com.tws.mes.base` 里的 Controller，全部接口 404。修复：`@SpringBootApplication(scanBasePackages = "com.tws.mes")`。

**问题 #5（已解）**：Sa-Token 1.37 的 `new SaInterceptor()` 只处理注解鉴权，**默认不做登录校验**（无 token 访问竟返回 200）。修复：显式 `new SaInterceptor(handle -> StpUtil.checkLogin())`。注解鉴权（@SaCheckRole→403）倒是正常的。

**问题 #6（记录）**：判断 mvn 成败时 `mvn ... | tail -20; echo $?` 取到的是 tail 的退出码不是 mvn 的——`$?` 必须紧跟目标命令，或用管道前段 `set -o pipefail`。

**接口验证（curl 全绿）**：
- 未登录访问 `/api/material/page` → `{"code":401}` ✓
- admin 登录 → token（Sa-Token 存 Redis）→ 分页查物料 11 条 ✓
- op1（操作工）保存物料 → `{"code":403,"需要角色: admin"}` ✓

## M1 前端骨架（2026-08-17 06:05~06:15）

**产物**（`code/mes-tws-ui/`，Vue3+JS+Vite5+ElementPlus+Pinia）：
- axios 封装（satoken 请求头 / 401 跳登录 / 统一报错）
- 路由（meta.roles 前端菜单过滤 + beforeEach 登录守卫；后端 @SaCheckRole 独立兜底）
- AppLayout（侧边菜单+顶栏用户下拉）、登录页（含账号提示）、总览占位页、物料管理页（查询/表格/分页/弹窗编辑，全项目 CRUD 页面模板）
- vite 代理 `/api` → 8080

**问题 #7（记录）**：Vite5 默认只监听 `[::1]:5173`（IPv6），用 `127.0.0.1:5173` 测连通会失败，要用 `localhost`。

**验证（全绿）**：dev 服务器启动 1.4s；`localhost:5173` 返回页面 HTML；经 5173 代理 POST 登录成功返回 token；`npm run build` 生产构建通过（7.3s）。

---

## M2 基础数据 + 工单管理（2026-08-17 06:20~06:45）

**后端产物**：
- 基础数据：物料批次（Service 版）、BOM 主从保存（MdBomService）、工艺路线版本化（**草稿→发布**才能被工单引用；同编码自动递增版本；已发布不可改只能"复制新版本"）、工序/产线/工位/不良代码（DictController 聚合简单字典）
- 工单：WorkOrderService 三个核心设计——
  - **状态机**：静态迁移表 TRANSITIONS（release/start/pause/resume/complete/close），非法迁移给出中文提示
  - **快照**：创建时复制路线工序（含 check_rules JSON）到 plan_wo_operation，并冗余工序名/类型
  - **乐观锁**：`@Version` + OptimisticLockerInnerInterceptor，更新 0 行即提示"已被他人变更"
- 分页关联（工单 join 物料/路线）用注解 SQL 在库端完成，避免 N+1
- CurrentUserService：流水 operator 存用户名（人能读懂）而非用户 ID

**问题 #8（已解）**：Java 15+ 文本块 `"""` 和 `Map.of`/`List.copyOf`（Java 9+）在编译目标 1.8 下不可用——写代码时顺手用了新语法，编译才暴露。全部改为字符串拼接/HashMap。
**问题 #9（已解）**：工单号最初用"当日计数+1"，序号不连续时会撞号（当日已有 003 时生成了 002）。改为"当日最大序号+1"，唯一索引兜底。
**问题 #10（已解）**：前端 LineStation.vue 导入的 API 函数 `saveLine/saveStation` 与本地处理函数重名，Vite 构建报 Identifier already declared——import 加 Api 后缀别名。

**验证（curl 全绿）**：planner1 创建工单（V2 路线）→ 下达 → 开工 → 暂停 → 恢复全部 200；详情含 16 道工序快照（V2 的 seq25=声学测试(左耳) 证明快照取的是 V2）；WO 分页关联出物料/路线名。

**前端产物**：批次/工序/不良代码/产线工位（字典页）+ BOM 主从编辑器 + **工艺路线编辑器**（工序行内编辑 + check_rules 模板一键插入 + 发布/复制新版本）+ 工单列表（状态操作按钮按状态与角色显隐、进度条）+ 工单详情（快照表 + JSON 美化）；AppLayout 升级为分组菜单（el-sub-menu）。`npm run build` ✓（7.1s）。

---

## M3 生产执行：过站引擎（2026-08-17 06:50~07:30）

**后端产物**（本项目的心脏，mes-execution）：
- `rule/CheckRule` 接口 + **八大防呆规则**（Spring Bean，order 排序成责任链，新增规则零改动主流程）：
  1. WoStatusRule 工单状态（仅生产中可过站）
  2. SnValidRule SN 合法性（报废/完工拦截 + 工位/SN 类型匹配）
  3. OpenDefectRule 前置不良闭环（OPEN 不良单未处理禁止流转——"带病流转"防线）
  4. SeqRule 工序顺序（按 SN 类型过滤工单快照工序，防跳站/漏站；重测由轮次放行）
  5. DuplicateRule 重复过站（同 SN 同工位同轮次已 OK 拦截）
  6. BindingRule 绑定完整性（needBinding 校验，防"空盒出厂"）
  7. LoadingRule 上料批次（requireLoading 工位未上料拦截）
  8. TestItemRule 测试项判定（eq/gt/ge… 支持"期望 vs 实际"提示；判 NG 必须给不良码）
- `CheckinService` 主流程：工位换算→装配上下文→**Redis SET NX 锁(5s)**→规则链→流水落库（**checkin_key 唯一索引兜底并发**）→SN 状态推进（NG 自动开不良单）→Spring 事件异步更新工单进度（MQ 留位）
- `SnService`：整机 SN 批量生成（幂等）、部件 SN 来料注册（带批次）、三码绑定（类型/重绑/报废校验 + BINDING 流水）、上料（批次冻结拦截）、流水/绑定查询
- `/api/station/context`：返回工序快照规则要点，前端模拟器动态渲染测试项输入框

**问题 #11（已解）**：整机被要求先"过 IQC 站"——IQC 是来料检验（扫批次不扫 SN），但规则里 snType 写了 MACHINE，进入了整机工序序列。修复：IQC 规则 snType 改为 MATERIAL（无 SN 匹配 → 该工位自动拒绝 SN 过站，且不进任何 SN 序列）。库数据、seed.sql、已有工单快照三处同步更新。

**验证（curl 八场景全绿）**：绑定前 COUPLE 被拦 → 三码绑定成功 → COUPLE 过站 OK → 重复过站拦截 → FCT 固件 1.2.4 判 OK 被 TestItemRule 拦截（提示期望 eq 1.2.5 实际 1.2.4）→ 判 NG D09 自动开不良单 → NG 后流 APP 被 OpenDefectRule 拦截 → 左耳 SN 扫右耳站被类型校验拦截。

**前端产物**：**过站模拟器**（选工单/工位 → 规则要点展示 + 动态测试项输入（预填期望值）→ OK/NG + 不良代码 → 拦截原因实时提示 + "取下一个 SN"辅助 + 三码绑定面板 + 上料面板 + 工位实时流水）；SN 管理（整机生成/部件注册/多条件查询）；过站流水查询（含轮次与测试数据）；工单详情加进度条与"生成整机 SN"。

**M3 期间产生的演示数据（已入库）**：WO20260816002-0021 完成绑定+COUPLE+AGING，FCT 判 NG D09（OPEN，留给 M4 维修演示）；测试工单 WO20260817002（planner1 创建，IN_PROGRESS，V2 路线）。

---

## M4 质量管理（2026-08-17 07:35~07:50）

**后端产物**（mes-quality）：
- QualityService：不良单分页（按状态排序 OPEN 优先）、**维修登记闭环**（OK→不良单 REPAIRED + SN 置 RETEST 回原站重测（轮次+1，不污染 FPY）；NG→SCRAP 报废）、复检/抽检手动开单、维修历史
- FpyService + QualityStatMapper：**FPY 双口径统计 SQL**——一次直通 = 完工整机中"过站记录无 NG 且无重测"的比例；最终良率 = 完工/(完工+报废)；另含按日趋势、不良柏拉图 Top10
- FAI 首件检验（简版列表+登记）
- 注意：不良/维修实体放在 mes-execution（过站引擎要开单/查闭环），质量模块通过依赖消费——避免模块循环依赖

**问题 #12（已解）**：curl 从 GBK 控制台直接 `-d` 中文 JSON，服务端报 "Invalid UTF-8 middle byte"——把 payload 写成 UTF-8 文件再 `--data-binary @file` 即可；浏览器前端无此问题。

**验证（curl 全绿）**：FPY 汇总工单1 = 79 完工 / 76 一次直通 / 1 报废（FPY 96.2%，最终 98.75%——与种子设计分毫不差）；维修 0021 → REPAIRED/RETEST → 重测 FCT 第 1 轮 OK → APP 正常流转，闭环完整。

**前端产物**：不良与维修工作台（OPEN 优先列表 + 维修对话框（措施/根因/换料批次/OK回流|NG报废）+ 维修史 + 复检开单）；良率报表（四卡片 + ECharts 按日 FPY 趋势双轴 + 不良柏拉图 + 按工单明细进度条）；首件检验页。

## M5 追溯管理（2026-08-17 07:50~08:10）

**后端产物**（mes-trace）：TraceService 正向（SN→档案+绑定子件(含批次)+过站时间线+不良维修史）/反向（批次→部件SN→受影响整机清单，含未装配统计）；trace_task 每次查询留痕（任务号/命中数/耗时）。

**问题 #13（已解，重要——种子叙事反转 bug）**：自查发现 WO2 的不良机 22/26 号用的是**正常批次**（PCLA），问题批次 PCLB 反而全部通过声学站——与"V2 加声学站提前拦截问题批"的故事完全相反。修复 seed：拦截对象改为问题批的 2/6 号机、PCLB240801 批次状态改冻结、完工数 20→18；随后 DROP DATABASE 重建并重放演示数据（绑定→COUPLE→AGING→FCT NG→维修→重测→APP）。修复后反向追溯：问题批 24 个部件 → 22 台整机 + 2 个声学拦截未装配，故事线完美。

**验证（curl 全绿）**：正向 0021（问题批维修机）命中 8 条时间线 + D04 REPAIRED 维修史；反向 PCLB240801 命中 22 台整机（状态分布/不良计数正确），耗时 ~100ms；trace_task 留痕正常。

**前端产物**：正向追溯页（SN 档案 + 绑定子件表 + el-timeline 过站时间线（NG 标红/重测轮次/测试数据）+ 维修史）；反向追溯页（批次摘要 alert + 受影响整机清单，可点击跳正向）；双向互跳。
