-- ============================================================
-- TWS 整机装配 MES 学习版 - 数据库初始化脚本
-- 执行方式: mysql -uroot -proot < init.sql
-- 说明: DROP + CREATE, 重跑会清空库(开发便利), 生产切勿如此
-- 表前缀约定:
--   sys_ 系统 | md_ 基础数据 | plan_ 工单 | (无前缀)执行/质量/追溯
-- ============================================================
CREATE DATABASE IF NOT EXISTS mes_tws DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE mes_tws;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. 系统用户（学习版简化 RBAC：单角色字段，四种固定角色）
--    admin 管理员 | planner 计划员(工单) | quality 质量员 | operator 操作工(过站)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username    VARCHAR(50)  NOT NULL COMMENT '登录名',
  password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  nick_name   VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  role_code   VARCHAR(20)  NOT NULL DEFAULT 'operator' COMMENT '角色: admin/planner/quality/operator',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='系统用户';

-- ------------------------------------------------------------
-- 2. 物料主数据
-- ------------------------------------------------------------
DROP TABLE IF EXISTS md_material;
CREATE TABLE md_material (
  id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
  material_code  VARCHAR(50)  NOT NULL COMMENT '物料编码',
  material_name  VARCHAR(100) NOT NULL COMMENT '物料名称',
  material_type  VARCHAR(20)  NOT NULL COMMENT 'PRODUCT成品/SEMI半成品/KEY关键料/RAW普通原料/PACK包材',
  unit           VARCHAR(10)  DEFAULT 'PCS',
  spec           VARCHAR(200) DEFAULT NULL COMMENT '规格描述',
  batch_managed  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否批次管理(关键料=1)',
  supplier       VARCHAR(100) DEFAULT NULL COMMENT '默认供应商',
  status         TINYINT      NOT NULL DEFAULT 1,
  remark         VARCHAR(200) DEFAULT NULL,
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_material_code (material_code)
) ENGINE=InnoDB COMMENT='物料主数据';

-- ------------------------------------------------------------
-- 3. 物料批次（关键料来料批次，上料/反向追溯的锚点）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS md_material_batch;
CREATE TABLE md_material_batch (
  id          BIGINT      PRIMARY KEY AUTO_INCREMENT,
  material_id BIGINT      NOT NULL,
  batch_no    VARCHAR(50) NOT NULL COMMENT '批次号',
  supplier    VARCHAR(100) DEFAULT NULL,
  arrive_time DATETIME    DEFAULT NULL COMMENT '到料时间',
  quantity    INT         DEFAULT 0,
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1可用 0冻结',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_batch (batch_no),
  KEY idx_material (material_id)
) ENGINE=InnoDB COMMENT='物料批次';

-- ------------------------------------------------------------
-- 4. BOM（成品 - 子件层级，学习版单层）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS md_bom;
CREATE TABLE md_bom (
  id                   BIGINT      PRIMARY KEY AUTO_INCREMENT,
  bom_code             VARCHAR(50) NOT NULL COMMENT 'BOM 编码',
  product_material_id  BIGINT      NOT NULL COMMENT '成品物料ID',
  version              VARCHAR(20) NOT NULL DEFAULT 'V1.0',
  status               TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  create_time          DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_time          DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bom_code (bom_code)
) ENGINE=InnoDB COMMENT='物料清单 BOM';

DROP TABLE IF EXISTS md_bom_item;
CREATE TABLE md_bom_item (
  id                 BIGINT        PRIMARY KEY AUTO_INCREMENT,
  bom_id             BIGINT        NOT NULL,
  child_material_id  BIGINT        NOT NULL,
  quantity           DECIMAL(10,2) NOT NULL DEFAULT 1 COMMENT '单位用量',
  KEY idx_bom (bom_id)
) ENGINE=InnoDB COMMENT='BOM 子件明细';

-- ------------------------------------------------------------
-- 5. 工序定义（全厂统一工序字典；工位绑定的是工序，不是路线）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS md_operation;
CREATE TABLE md_operation (
  id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
  operation_code VARCHAR(50) NOT NULL COMMENT '工序编码',
  operation_name VARCHAR(50) NOT NULL,
  operation_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL普通/TEST测试/BIND绑定/AGING老化/PACK包装/IQC检验',
  status         TINYINT     NOT NULL DEFAULT 1,
  create_time    DATETIME    DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_operation_code (operation_code)
) ENGINE=InnoDB COMMENT='工序定义';

-- ------------------------------------------------------------
-- 6. 工艺路线（版本化管理：新版本启用后老工单仍用老快照）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS md_routing;
CREATE TABLE md_routing (
  id                   BIGINT       PRIMARY KEY AUTO_INCREMENT,
  routing_code         VARCHAR(50)  NOT NULL COMMENT '路线编码',
  routing_name         VARCHAR(100) NOT NULL,
  product_material_id  BIGINT       NOT NULL COMMENT '适用成品物料',
  version              INT          NOT NULL DEFAULT 1 COMMENT '版本号(整数递增)',
  status               TINYINT      NOT NULL DEFAULT 1 COMMENT '0停用/1草稿/2已发布(可被工单引用)',
  create_time          DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_routing (routing_code, version)
) ENGINE=InnoDB COMMENT='工艺路线(版本化)';

DROP TABLE IF EXISTS md_routing_operation;
CREATE TABLE md_routing_operation (
  id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
  routing_id     BIGINT      NOT NULL,
  seq            INT         NOT NULL COMMENT '工序顺序(10,20,30...)',
  operation_code VARCHAR(50) NOT NULL COMMENT '引用工序编码',
  check_rules    JSON        DEFAULT NULL COMMENT '防呆规则配置,见下',
  UNIQUE KEY uk_routing_seq (routing_id, seq),
  KEY idx_op (operation_code)
) ENGINE=InnoDB COMMENT='工艺路线工序明细';
-- check_rules 约定(JSON):
-- {
--   "snType": "MACHINE",                 -- 本站加工的 SN 类型 MACHINE/LEFT/RIGHT/CASE/BOX/CARTON
--   "requirePrev": true,                 -- 必须先完成上一道工序
--   "testItems": [                       -- 测试项判定(过站 result=OK 时逐项校验 testData)
--     {"key":"firmware","op":"eq","value":"1.2.5"},
--     {"key":"mic_sensitivity","op":"ge","value":-38}
--   ],
--   "needBinding": ["LEFT","RIGHT","CASE"], -- 本站要求已绑定的子件类型
--   "requireLoading": true               -- 本站要求有效上料批次
-- }

-- ------------------------------------------------------------
-- 7. 产线 / 工位（工位绑定工序；过站扫的是工位）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS md_line;
CREATE TABLE md_line (
  id         BIGINT      PRIMARY KEY AUTO_INCREMENT,
  line_code  VARCHAR(50) NOT NULL COMMENT '产线编码',
  line_name  VARCHAR(100) NOT NULL,
  workshop   VARCHAR(50) DEFAULT NULL COMMENT '车间',
  status     TINYINT     NOT NULL DEFAULT 1,
  create_time DATETIME   DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_line_code (line_code)
) ENGINE=InnoDB COMMENT='产线';

DROP TABLE IF EXISTS md_station;
CREATE TABLE md_station (
  id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
  station_code   VARCHAR(50)  NOT NULL COMMENT '工位码(扫码枪输入)',
  station_name   VARCHAR(100) NOT NULL,
  line_id        BIGINT       NOT NULL,
  operation_code VARCHAR(50)  NOT NULL COMMENT '绑定工序编码',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_station_code (station_code),
  KEY idx_line (line_id)
) ENGINE=InnoDB COMMENT='工位';

-- ------------------------------------------------------------
-- 8. 不良代码字典
-- ------------------------------------------------------------
DROP TABLE IF EXISTS md_defect_code;
CREATE TABLE md_defect_code (
  id           BIGINT      PRIMARY KEY AUTO_INCREMENT,
  defect_code  VARCHAR(50) NOT NULL,
  defect_name  VARCHAR(100) NOT NULL,
  category     VARCHAR(20) DEFAULT 'OTHER' COMMENT 'APPEARANCE外观/FUNC功能/ACOUSTIC声学/ASSEMBLE装配/OTHER',
  status       TINYINT     NOT NULL DEFAULT 1,
  UNIQUE KEY uk_defect_code (defect_code)
) ENGINE=InnoDB COMMENT='不良代码';

-- ------------------------------------------------------------
-- 9. 生产工单（version 乐观锁：状态流转防并发双击）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS plan_work_order;
CREATE TABLE plan_work_order (
  id                   BIGINT      PRIMARY KEY AUTO_INCREMENT,
  wo_no                VARCHAR(50) NOT NULL COMMENT '工单号 WOyyyyMMddNNN',
  product_material_id  BIGINT      NOT NULL COMMENT '成品物料',
  bom_id               BIGINT      DEFAULT NULL,
  routing_id           BIGINT      NOT NULL COMMENT '引用工艺路线(创建时快照其明细)',
  routing_version      INT         NOT NULL COMMENT '快照时的路线版本',
  plan_qty             INT         NOT NULL COMMENT '计划数量',
  ok_qty               INT         NOT NULL DEFAULT 0 COMMENT '过站OK累计(异步)',
  ng_qty               INT         NOT NULL DEFAULT 0 COMMENT '过站NG累计(异步)',
  sn_generated         TINYINT     NOT NULL DEFAULT 0 COMMENT '整机SN是否已生成',
  status               VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/RELEASED/IN_PROGRESS/PAUSED/COMPLETED/CLOSED',
  version              INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  plan_start_date      DATE        DEFAULT NULL,
  plan_end_date        DATE        DEFAULT NULL,
  create_by            VARCHAR(50) DEFAULT NULL,
  create_time          DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_time          DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_wo_no (wo_no),
  KEY idx_status (status)
) ENGINE=InnoDB COMMENT='生产工单';

-- ------------------------------------------------------------
-- 10. 工单工序快照（工单创建时从 md_routing_operation 复制，
--     之后路线改版不影响已建工单 —— 本项目核心设计点之一）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS plan_wo_operation;
CREATE TABLE plan_wo_operation (
  id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
  work_order_id  BIGINT      NOT NULL,
  seq            INT         NOT NULL,
  operation_code VARCHAR(50) NOT NULL,
  operation_name VARCHAR(50) NOT NULL COMMENT '快照时冗余工序名(展示用)',
  operation_type VARCHAR(20) NOT NULL,
  check_rules    JSON        DEFAULT NULL COMMENT '防呆规则快照',
  UNIQUE KEY uk_wo_seq (work_order_id, seq)
) ENGINE=InnoDB COMMENT='工单工序快照';

-- ------------------------------------------------------------
-- 11. SN 注册表（一机多码：整机/左耳/右耳/盒/彩盒/中箱 全在这里）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sn_registry;
CREATE TABLE sn_registry (
  id              BIGINT      PRIMARY KEY AUTO_INCREMENT,
  sn              VARCHAR(64) NOT NULL COMMENT '序列号',
  sn_type         VARCHAR(10) NOT NULL COMMENT 'MACHINE整机/LEFT左耳/RIGHT右耳/CASE充电盒/BOX彩盒/CARTON中箱',
  work_order_id   BIGINT      DEFAULT NULL COMMENT '所属工单(整机SN必有;部件SN可空)',
  batch_no        VARCHAR(50) DEFAULT NULL COMMENT '来料批次(部件SN带批次,反向追溯锚点)',
  parent_sn       VARCHAR(64) DEFAULT NULL COMMENT '父SN(绑定后回填,如左耳->整机)',
  current_seq     INT         NOT NULL DEFAULT 0 COMMENT '已通过的最大工序顺序',
  status          VARCHAR(20) NOT NULL DEFAULT 'INIT' COMMENT 'INIT/IN_LINE/NG/RETEST/DONE/SCRAP',
  firmware_version VARCHAR(20) DEFAULT NULL COMMENT '烧录站写入的固件版本',
  create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sn (sn),
  KEY idx_wo (work_order_id),
  KEY idx_batch (batch_no),
  KEY idx_parent (parent_sn),
  KEY idx_status_type (status, sn_type)
) ENGINE=InnoDB COMMENT='SN 注册表';

-- ------------------------------------------------------------
-- 12. 过站流水（学习版单表；生产按月分表,在线12月,归档3年）
--     record_type: CHECKIN过站 | LOADING上料 | BINDING绑定
--     checkin_key: 仅 CHECKIN 行填充(sn:station:轮次),其余为 NULL,
--                  利用唯一索引+NULL 可重复 实现"仅过站防重"兜底(第一道防线是 Redis 锁)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS station_log;
CREATE TABLE station_log (
  id             BIGINT      PRIMARY KEY AUTO_INCREMENT,
  sn             VARCHAR(64) NOT NULL,
  work_order_id  BIGINT      DEFAULT NULL,
  station_code   VARCHAR(50) NOT NULL,
  operation_code VARCHAR(50) DEFAULT NULL,
  seq            INT         DEFAULT NULL,
  record_type    VARCHAR(10) NOT NULL DEFAULT 'CHECKIN',
  result         VARCHAR(5)  DEFAULT NULL COMMENT 'OK/NG(LOADING/BINDING为NULL)',
  ng_code        VARCHAR(50) DEFAULT NULL COMMENT 'NG 不良代码',
  test_data      JSON        DEFAULT NULL COMMENT '测试数据/上料批次/绑定明细',
  batch_no       VARCHAR(50) DEFAULT NULL COMMENT '上料批次(LOADING)',
  retest_round   INT         NOT NULL DEFAULT 0 COMMENT '维修回流轮次:0首过,1..n重测',
  checkin_key    VARCHAR(120) DEFAULT NULL COMMENT '防重唯一键: sn:stationCode:round',
  operator       VARCHAR(50) DEFAULT NULL,
  create_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_checkin_key (checkin_key),
  KEY idx_sn (sn),
  KEY idx_wo (work_order_id),
  KEY idx_time (create_time),
  KEY idx_batch (batch_no)
) ENGINE=InnoDB COMMENT='过站/上料/绑定流水';

-- ------------------------------------------------------------
-- 13. SN 绑定关系（多级: 左/右耳+盒->整机, 整机->彩盒->中箱）
--     uk_child: 一个子 SN 每种类型只能绑一次,防重复绑定
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sn_binding;
CREATE TABLE sn_binding (
  id            BIGINT      PRIMARY KEY AUTO_INCREMENT,
  parent_sn     VARCHAR(64) NOT NULL,
  child_sn      VARCHAR(64) NOT NULL,
  bind_type     VARCHAR(10) NOT NULL COMMENT 'LEFT/RIGHT/CASE/BOX/CARTON',
  work_order_id BIGINT      DEFAULT NULL,
  station_code  VARCHAR(50) DEFAULT NULL,
  operator      VARCHAR(50) DEFAULT NULL,
  create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_child_bind (child_sn, bind_type),
  KEY idx_parent (parent_sn)
) ENGINE=InnoDB COMMENT='SN 绑定关系';

-- ------------------------------------------------------------
-- 14. 不良记录（过站 NG 自动开单；status: OPEN/REPAIRED/SCRAP）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS defect_record;
CREATE TABLE defect_record (
  id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
  sn             VARCHAR(64)  NOT NULL,
  work_order_id  BIGINT       DEFAULT NULL,
  station_code   VARCHAR(50)  DEFAULT NULL,
  operation_code VARCHAR(50)  DEFAULT NULL,
  defect_code    VARCHAR(50)  NOT NULL,
  defect_desc    VARCHAR(200) DEFAULT NULL,
  discover_type  VARCHAR(20)  NOT NULL DEFAULT 'CHECKIN' COMMENT 'CHECKIN过站/RECHECK复检/AUDIT抽检',
  repair_round   INT          NOT NULL DEFAULT 0 COMMENT '第几轮维修',
  status         VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sn (sn),
  KEY idx_status (status),
  KEY idx_wo (work_order_id)
) ENGINE=InnoDB COMMENT='不良记录';

-- ------------------------------------------------------------
-- 15. 维修记录（维修 OK -> SN 置 RETEST 回流重测）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS repair_record;
CREATE TABLE repair_record (
  id               BIGINT       PRIMARY KEY AUTO_INCREMENT,
  defect_id        BIGINT       NOT NULL,
  sn               VARCHAR(64)  NOT NULL,
  action           VARCHAR(200) NOT NULL COMMENT '维修措施',
  root_cause       VARCHAR(200) DEFAULT NULL COMMENT '根因(如:麦克风批次来料不良)',
  change_batch_no  VARCHAR(50)  DEFAULT NULL COMMENT '换料批次(若有)',
  result           VARCHAR(5)   NOT NULL COMMENT 'OK修好/NG报废',
  repairer         VARCHAR(50)  DEFAULT NULL,
  create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  KEY idx_defect (defect_id),
  KEY idx_sn (sn)
) ENGINE=InnoDB COMMENT='维修记录';

-- ------------------------------------------------------------
-- 16. 首件检验 FAI（简版：换线/换批次首件确认）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS fai_record;
CREATE TABLE fai_record (
  id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
  work_order_id  BIGINT       NOT NULL,
  operation_code VARCHAR(50)  DEFAULT NULL,
  sn             VARCHAR(64)  DEFAULT NULL,
  result         VARCHAR(5)   NOT NULL COMMENT 'PASS/FAIL',
  checker        VARCHAR(50)  DEFAULT NULL,
  remark         VARCHAR(200) DEFAULT NULL,
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  KEY idx_wo (work_order_id)
) ENGINE=InnoDB COMMENT='首件检验';

-- ------------------------------------------------------------
-- 17. 追溯任务（每次追溯查询留痕：口径/耗时/命中数）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS trace_task;
CREATE TABLE trace_task (
  id           BIGINT      PRIMARY KEY AUTO_INCREMENT,
  task_no      VARCHAR(50) NOT NULL COMMENT '任务号 TRyyyyMMddNNNN',
  trace_type   VARCHAR(10) NOT NULL COMMENT 'FORWARD正向/REVERSE反向',
  query_key    VARCHAR(64) NOT NULL COMMENT 'SN 或 批次号',
  result_count INT         DEFAULT 0 COMMENT '命中结果数',
  cost_ms      INT         DEFAULT 0 COMMENT '耗时毫秒',
  create_by    VARCHAR(50) DEFAULT NULL,
  create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP,
  KEY idx_key (query_key)
) ENGINE=InnoDB COMMENT='追溯任务';

SET FOREIGN_KEY_CHECKS = 1;

-- 初始化默认账号由 seed.sql 写入(密码 123456 的 BCrypt 文密)
