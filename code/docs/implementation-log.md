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

**问题 #1（未解）**：MySQL root 密码探测脚本第一次有 bug——用 `grep -q "1"` 判断连接成功，但错误码 `1045` 里也含字符"1"，导致误报 root/root 可用；随后实际导入 SQL 报 `Access denied for user 'root'@'::1'`。改用退出码复测时被用户暂停。
后续方案（按优先级）：① 在 `C:\WorkSpace` 其他项目里找现成的 datasource 配置（只读）；② 少量常见密码严格复测；③ 都不行则以 `--skip-grant-tables` 临时重启 MySQL，**只新增**一个专用账号 `mes`（不改 root 密码、不动已有账号与数据），再正常重启服务——该操作会记录进审计表。

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
