# TWS 整机装配 MES 学习版（Vue3 + Spring Boot 2.7）

离散制造 MES 的可运行学习实现：以本仓库飞书设计文档（TWS 蓝牙耳机整机装配 MES 项目全景）为蓝本，
覆盖**工单管理 / 生产执行（过站引擎）/ 质量管理 / 追溯管理**四大认领模块 + 最小基础数据。
技术栈与简历叙事一致（Spring Boot 2.7 + MyBatis-Plus + Sa-Token + Redis），前端按学习需求采用 **Vue3**。

> 学习地图与模块导读见 `docs/study-map.md`；实现全过程（含踩坑）见 `docs/implementation-log.md`。

## 技术栈

| 端 | 技术 |
|----|------|
| 后端 | Spring Boot 2.7.18 · MyBatis-Plus 3.5.3 · Sa-Token 1.37（token 存 Redis）· MySQL 8 · Redis · Maven 多模块单体 |
| 前端 | Vue 3.4 · Vite 5 · Element Plus · Pinia · Vue Router · Axios · ECharts |

后端模块：`mes-common`（通用）/ `mes-base`（基础数据+登录）/ `mes-execution`（工单+过站+SN）/ `mes-quality`（不良+维修+FPY）/ `mes-trace`（追溯）/ `mes-server`（启动聚合，8080）。

## 环境要求

- JDK 8~21（本机用 21 亦可，编译目标 1.8）+ Maven 3.6+（阿里云镜像）
- Node.js ≥ 18 + npm
- MySQL 8（本机示例账号 root/mysql，改配置见 `mes-tws/mes-server/src/main/resources/application.yml`）
- Redis（Windows 可用 tporadowski 版；本机已装为服务 `Redis`，见 implementation-log 审计 #10）

## 启动步骤

```bash
# 1. 初始化数据库（重跑会清库重来）
cd docs/sql
mysql -uroot -p < init.sql
mysql -uroot -p < seed.sql

# 2. 启动后端（8080）
cd ../../mes-tws
mvn -pl mes-server spring-boot:run
# 接口文档: http://localhost:8080/swagger-ui.html

# 3. 启动前端（5173，已配 /api 代理到 8080）
cd ../mes-tws-ui
npm install
npm run dev
# 浏览器打开 http://localhost:5173
```

## 账号（密码均为 123456）

| 账号 | 角色 | 能做什么 |
|------|------|----------|
| admin | 管理员 | 全部功能（基础数据维护） |
| planner1 | 计划员 | 工单创建/下达/开工、生成整机 SN |
| qc1 | 质量员 | 不良处理/维修登记、首件、批次登记 |
| op1 | 操作工 | 过站模拟器（过站/绑定/上料）、部件 SN 注册 |

## 十分钟体验路线（用种子数据）

1. **总览**：登录 admin，看四张卡与"最近过站"。
2. **基础数据→工艺路线**：看 `RT-TWSX1` 的 V1/V2 两版——V2 多了声学测试站（这就是"快照"的意义：老工单走老版本）。
3. **计划管理→工单管理**：`WO20260801001` 已完工、`WO20260816002` 生产中；点工单号看**工序快照**。
4. **生产执行→过站模拟器**（op1 登录体验更真实）：
   - 选工单 `WO20260816002` + 工位 `L1-COUPLE` → 点"取下一个"拿整机 SN → 右侧"三码绑定"把左耳/右耳/盒三个子件绑上 → 回过站判定 OK；
   - 换 `L1-AGING` → OK；换 `L1-FCT` → 故意把固件改成 1.2.4 判 OK，看**测试项拦截**；改判 NG 选 D09，看不良单生成；
   - 这时再过 `L1-APP` 会被"前置不良闭环"拦住——去质量管理处理它。
5. **质量管理→不良与维修**：对 OPEN 单"维修处理"（结果选 OK）→ SN 回流 RETEST；回模拟器 `L1-FCT` 重测（第 1 轮）OK → 继续 APP 一路到 WEIGH 完工。
6. **质量管理→良率报表**：看 FPY（一次直通）与最终良率双口径、按日趋势、不良柏拉图。
7. **追溯管理→反向追溯**：输入（或下拉选）问题批次 `PCLB240801` → 受影响整机清单（含工单1 被 FCT 检出的 4 台与工单2 被声学站提前拦截的 2 个未装配部件）；点任一整机跳正向时间线。

> 种子里埋了一条完整故事线：问题麦克风批次（MICB240715）→ 协丰贴片成 PCBA 问题批（PCLB240801）→
> V1 时代到 FCT 才暴露 4 台（3 修 1 废）→ 二期加声学站后同类问题在部件段即被拦截。面试讲"改善故事"直接用。

## 与"生产版"的差异（学习版简化项）

| 项 | 学习版 | 叙事生产版 | 升级路径 |
|----|--------|-----------|----------|
| 过站事件 | Spring 事件 @Async | RabbitMQ 削峰 | 监听器换成 MQ 消费者，主流程零改动 |
| station_log | 单表+索引 | 按月分表、在线12月/归档3年 | 分表中间件或按月建表+路由 |
| MySQL | 单机 | 主从读写分离 | ShardingSphere / 读写分离组件 |
| 定时任务 | @Scheduled | XXL-Job | 接入调度中心 |
| RBAC | 单角色字段 | 菜单/按钮级权限 | sys_menu + Sa-Token 注解 |
| 前端 | **Vue3**（学习需求） | 叙事口径 Vue2 | 如需对齐可再议 |

## 常用命令

```bash
# 后端打包
cd mes-tws && mvn -DskipTests package
# 前端生产构建
cd mes-tws-ui && npm run build   # 产物 dist/
# 重置数据库
mysql -uroot -p < docs/sql/init.sql && mysql -uroot -p < docs/sql/seed.sql
```
