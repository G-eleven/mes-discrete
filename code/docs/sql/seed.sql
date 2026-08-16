-- ============================================================
-- TWS 整机装配 MES 学习版 - 种子数据
-- 前置: 先执行 init.sql
-- 内容: 账号/物料/批次/BOM/工序/工艺路线(V1+V2)/产线工位/不良代码
--       3 张工单, 并用存储过程生成历史过站流水(报表与追溯开箱即用)
-- 账号密码均为 123456 (BCrypt)
-- ============================================================
USE mes_tws;
SET NAMES utf8mb4;

-- ---------------- 用户 ----------------
INSERT INTO sys_user (id, username, password, nick_name, role_code) VALUES
(1,'admin',   '$2a$10$VQU0iNHaSjNxQbIRj5nf9.xlFXYRo1YTAmtpKKwUx.u4GYdYQodke','系统管理员','admin'),
(2,'planner1','$2a$10$VQU0iNHaSjNxQbIRj5nf9.xlFXYRo1YTAmtpKKwUx.u4GYdYQodke','王计划','planner'),
(3,'qc1',     '$2a$10$VQU0iNHaSjNxQbIRj5nf9.xlFXYRo1YTAmtpKKwUx.u4GYdYQodke','李质量','quality'),
(4,'op1',     '$2a$10$VQU0iNHaSjNxQbIRj5nf9.xlFXYRo1YTAmtpKKwUx.u4GYdYQodke','张操作','operator');

-- ---------------- 物料 ----------------
INSERT INTO md_material (id, material_code, material_name, material_type, unit, spec, batch_managed, supplier, remark) VALUES
(1, 'TWS-X1',    'TWS蓝牙耳机 X1 整机', 'PRODUCT', 'PCS', '蓝牙5.3/ANC/续航28h', 0, NULL,    '对客户交付的成品'),
(2, 'PCBA-LEFT', '左耳PCBA半成品',      'SEMI',    'PCS', '含主控芯片',           1, '协丰电子', '外协加工,批次管理'),
(3, 'PCBA-RIGHT','右耳PCBA半成品',      'SEMI',    'PCS', '含电池触点',           1, '协丰电子', '外协加工,批次管理'),
(4, 'PCBA-CASE', '充电盒PCBA半成品',    'SEMI',    'PCS', NULL,                   1, '协丰电子', '外协加工,批次管理'),
(5, 'MIC-6027',  'MEMS麦克风',          'KEY',     'PCS', '-38dB±3dB',            1, '美声电声', '关键料,每台4颗'),
(6, 'BAT-401',   '扣式锂电池',          'KEY',     'PCS', '3.7V/45mAh',           1, '新能动力', '关键料,每台2颗'),
(7, 'SPK-301',   '动圈喇叭',            'KEY',     'PCS', '8mm/16Ω',              1, '声学精密', '关键料,每台2颗'),
(8, 'EAR-TIP',   '硅胶耳塞',            'RAW',     'PAIR', NULL,                   0, NULL, NULL),
(9, 'CASE-SHELL','充电盒外壳',          'RAW',     'PCS', NULL,                   0, NULL, NULL),
(10,'GIFT-BOX',  '彩盒(含内衬)',        'PACK',    'PCS', NULL,                   0, NULL, NULL),
(11,'CARTON-M',  '中箱(20台装)',        'PACK',    'PCS', NULL,                   0, NULL, NULL);

-- ---------------- 物料批次(注意 PCL240801-B 是"问题批次") ----------------
INSERT INTO md_material_batch (id, material_id, batch_no, supplier, arrive_time, quantity, status) VALUES
(1, 2, 'PCLA240801', '协丰电子', '2026-08-01 08:30:00', 5000, 1),
(2, 2, 'PCLB240801', '协丰电子', '2026-08-01 10:00:00', 300,  1), -- 左耳PCBA问题批:贴片使用了华科问题麦克风
(3, 3, 'PCRA240801', '协丰电子', '2026-08-01 08:30:00', 5000, 1),
(4, 4, 'PCCA240801', '协丰电子', '2026-08-01 08:30:00', 5000, 1),
(5, 5, 'MICA240701', '美声电声', '2026-07-01 09:00:00', 50000,1),
(6, 5, 'MICB240715', '华科电声', '2026-07-15 14:00:00', 20000,0), -- 麦克风问题批(灵敏度偏移),已冻结
(7, 6, 'BATA240801', '新能动力', '2026-08-01 08:00:00', 20000,1),
(8, 7, 'SPKA240801', '声学精密', '2026-08-01 08:00:00', 20000,1);

-- ---------------- 工序字典 ----------------
INSERT INTO md_operation (id, operation_code, operation_name, operation_type) VALUES
(1, 'OP-IQC',       'IQC来料检验',        'IQC'),
(2, 'OP-BURN-L',    '烧录/MAC写入(左耳)', 'TEST'),
(3, 'OP-BURN-R',    '烧录/MAC写入(右耳)', 'TEST'),
(4, 'OP-ACOUS-L',   '声学测试(左耳)',     'TEST'),
(5, 'OP-ACOUS-R',   '声学测试(右耳)',     'TEST'),
(6, 'OP-RF-L',      'RF测试(左耳)',       'TEST'),
(7, 'OP-RF-R',      'RF测试(右耳)',       'TEST'),
(8, 'OP-CASE-ASSY', '充电盒组装',         'NORMAL'),
(9, 'OP-CASE-TEST', '充放电测试',         'TEST'),
(10,'OP-COUPLE',    '三码绑定',           'BIND'),
(11,'OP-AGING',     '老化测试',           'AGING'),
(12,'OP-FCT',       'FCT全检',            'TEST'),
(13,'OP-APP',       '外观检验',           'NORMAL'),
(14,'OP-BOXING',    '彩盒包装',           'PACK'),
(15,'OP-CARTON',    '中箱装箱',           'PACK'),
(16,'OP-WEIGH',     '称重入库',           'NORMAL');

-- ---------------- 工艺路线 ----------------
-- V1(一期上线版,无声学站): 麦克风问题只能在 FCT 间接暴露
INSERT INTO md_routing (id, routing_code, routing_name, product_material_id, version, status) VALUES
(1, 'RT-TWSX1', 'TWS-X1 整机装配工艺路线', 1, 1, 2),
(2, 'RT-TWSX1', 'TWS-X1 整机装配工艺路线(二期+声学)', 1, 2, 2);

INSERT INTO md_routing_operation (routing_id, seq, operation_code, check_rules) VALUES
-- V1（IQC 扫批次不扫 SN，snType=MATERIAL 使其不进入任何 SN 的工序序列）
(1, 10, 'OP-IQC',       JSON_OBJECT('snType','MATERIAL','requirePrev',false)),
(1, 20, 'OP-BURN-L',    JSON_OBJECT('snType','LEFT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','firmware','op','eq','value','1.2.5')))),
(1, 30, 'OP-BURN-R',    JSON_OBJECT('snType','RIGHT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','firmware','op','eq','value','1.2.5')))),
(1, 40, 'OP-RF-L',      JSON_OBJECT('snType','LEFT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','tx_power','op','ge','value',-2)))),
(1, 50, 'OP-RF-R',      JSON_OBJECT('snType','RIGHT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','tx_power','op','ge','value',-2)))),
(1, 60, 'OP-CASE-ASSY', JSON_OBJECT('snType','CASE','requirePrev',true,'requireLoading',true)),
(1, 70, 'OP-CASE-TEST', JSON_OBJECT('snType','CASE','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','charge_cur','op','ge','value',100)))),
(1, 80, 'OP-COUPLE',    JSON_OBJECT('snType','MACHINE','requirePrev',false,'needBinding',JSON_ARRAY('LEFT','RIGHT','CASE'))),
(1, 90, 'OP-AGING',     JSON_OBJECT('snType','MACHINE','requirePrev',true)),
(1, 100,'OP-FCT',       JSON_OBJECT('snType','MACHINE','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','firmware','op','eq','value','1.2.5')))),
(1, 110,'OP-APP',       JSON_OBJECT('snType','MACHINE','requirePrev',true)),
(1, 120,'OP-BOXING',    JSON_OBJECT('snType','MACHINE','requirePrev',true)),
(1, 130,'OP-CARTON',    JSON_OBJECT('snType','MACHINE','requirePrev',true)),
(1, 140,'OP-WEIGH',     JSON_OBJECT('snType','MACHINE','requirePrev',true)),
-- V2(二期: 烧录后加左右耳声学测试,提前拦截麦克风问题批次)
(2, 10, 'OP-IQC',       JSON_OBJECT('snType','MATERIAL','requirePrev',false)),
(2, 20, 'OP-BURN-L',    JSON_OBJECT('snType','LEFT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','firmware','op','eq','value','1.2.5')))),
(2, 25, 'OP-ACOUS-L',   JSON_OBJECT('snType','LEFT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','mic_sensitivity','op','ge','value',-38)))),
(2, 30, 'OP-BURN-R',    JSON_OBJECT('snType','RIGHT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','firmware','op','eq','value','1.2.5')))),
(2, 35, 'OP-ACOUS-R',   JSON_OBJECT('snType','RIGHT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','mic_sensitivity','op','ge','value',-38)))),
(2, 40, 'OP-RF-L',      JSON_OBJECT('snType','LEFT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','tx_power','op','ge','value',-2)))),
(2, 50, 'OP-RF-R',      JSON_OBJECT('snType','RIGHT','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','tx_power','op','ge','value',-2)))),
(2, 60, 'OP-CASE-ASSY', JSON_OBJECT('snType','CASE','requirePrev',true,'requireLoading',true)),
(2, 70, 'OP-CASE-TEST', JSON_OBJECT('snType','CASE','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','charge_cur','op','ge','value',100)))),
(2, 80, 'OP-COUPLE',    JSON_OBJECT('snType','MACHINE','requirePrev',false,'needBinding',JSON_ARRAY('LEFT','RIGHT','CASE'))),
(2, 90, 'OP-AGING',     JSON_OBJECT('snType','MACHINE','requirePrev',true)),
(2, 100,'OP-FCT',       JSON_OBJECT('snType','MACHINE','requirePrev',true,'testItems',JSON_ARRAY(JSON_OBJECT('key','firmware','op','eq','value','1.2.5')))),
(2, 110,'OP-APP',       JSON_OBJECT('snType','MACHINE','requirePrev',true)),
(2, 120,'OP-BOXING',    JSON_OBJECT('snType','MACHINE','requirePrev',true)),
(2, 130,'OP-CARTON',    JSON_OBJECT('snType','MACHINE','requirePrev',true)),
(2, 140,'OP-WEIGH',     JSON_OBJECT('snType','MACHINE','requirePrev',true));

-- ---------------- BOM ----------------
INSERT INTO md_bom (id, bom_code, product_material_id, version, status) VALUES
(1, 'BOM-TWSX1', 1, 'V1.0', 1);
INSERT INTO md_bom_item (bom_id, child_material_id, quantity) VALUES
(1,2,1),(1,3,1),(1,4,1),(1,5,4),(1,6,2),(1,7,2),(1,8,3),(1,9,1),(1,10,1),(1,11,0.05);

-- ---------------- 产线与工位 ----------------
INSERT INTO md_line (id, line_code, line_name, workshop) VALUES
(1,'L1','整机装配一线','总装车间'),
(2,'L2','整机装配二线','总装车间');

INSERT INTO md_station (id, station_code, station_name, line_id, operation_code) VALUES
(1, 'L1-IQC',       'IQC检验站',       1, 'OP-IQC'),
(2, 'L1-BURN-L',    '烧录站(左耳)',    1, 'OP-BURN-L'),
(3, 'L1-BURN-R',    '烧录站(右耳)',    1, 'OP-BURN-R'),
(4, 'L1-ACOUS-L',   '声学测试站(左耳)',1, 'OP-ACOUS-L'),
(5, 'L1-ACOUS-R',   '声学测试站(右耳)',1, 'OP-ACOUS-R'),
(6, 'L1-RF-L',      'RF测试站(左耳)',  1, 'OP-RF-L'),
(7, 'L1-RF-R',      'RF测试站(右耳)',  1, 'OP-RF-R'),
(8, 'L1-CASE-ASSY', '充电盒组装站',    1, 'OP-CASE-ASSY'),
(9, 'L1-CASE-TEST', '充放电测试站',    1, 'OP-CASE-TEST'),
(10,'L1-COUPLE',    '三码绑定站',      1, 'OP-COUPLE'),
(11,'L1-AGING',     '老化间',          1, 'OP-AGING'),
(12,'L1-FCT',       'FCT全检站',       1, 'OP-FCT'),
(13,'L1-APP',       '外观检验站',      1, 'OP-APP'),
(14,'L1-BOXING',    '彩盒包装站',      1, 'OP-BOXING'),
(15,'L1-CARTON',    '中箱装箱站',      1, 'OP-CARTON'),
(16,'L1-WEIGH',     '称重入库站',      1, 'OP-WEIGH'),
-- 二线只布关键站,演示"同一工序多工位"
(17,'L2-BURN-L',    '二线烧录站(左耳)',2, 'OP-BURN-L'),
(18,'L2-COUPLE',    '二线三码绑定站',  2, 'OP-COUPLE'),
(19,'L2-FCT',       '二线FCT全检站',   2, 'OP-FCT');

-- ---------------- 不良代码 ----------------
INSERT INTO md_defect_code (id, defect_code, defect_name, category) VALUES
(1, 'D01','外观划伤','APPEARANCE'),
(2, 'D02','壳体脏污','APPEARANCE'),
(3, 'D03','麦克风无声','ACOUSTIC'),
(4, 'D04','麦克风灵敏度偏低','ACOUSTIC'),
(5, 'D05','烧录失败','FUNC'),
(6, 'D06','RF功率不足','FUNC'),
(7, 'D07','耦合失败','FUNC'),
(8, 'D08','电池充放电异常','FUNC'),
(9, 'D09','FCT通讯失败','FUNC'),
(10,'D10','称重超差','OTHER'),
(11,'D11','耳机异音','ACOUSTIC');

-- ---------------- 工单 ----------------
INSERT INTO plan_work_order (id, wo_no, product_material_id, bom_id, routing_id, routing_version,
  plan_qty, ok_qty, ng_qty, sn_generated, status, plan_start_date, plan_end_date, create_by) VALUES
(1,'WO20260801001',1,1,1,1, 500,79,4,1,'COMPLETED','2026-08-01','2026-08-08','planner1'),
(2,'WO20260816002',1,1,2,2, 1000,20,0,1,'IN_PROGRESS','2026-08-16','2026-08-23','planner1'),
(3,'WO20260817003',1,1,1,1, 300,0,0,0,'CREATED','2026-08-18','2026-08-25','planner1');
-- 工单1 的工序快照与历史流水由下方存储过程生成(含快照本身)

-- ============================================================
-- 存储过程: 生成历史数据(工单快照/部件SN/整机SN/过站/绑定/不良/维修)
-- ============================================================
DELIMITER $$

-- 通用: 为工单生成工序快照(从 md_routing_operation 复制, 冗余工序名/类型)
DROP PROCEDURE IF EXISTS seed_wo_snapshot $$
CREATE PROCEDURE seed_wo_snapshot(IN p_wo_id BIGINT, IN p_routing_id BIGINT)
BEGIN
  DELETE FROM plan_wo_operation WHERE work_order_id = p_wo_id;
  INSERT INTO plan_wo_operation (work_order_id, seq, operation_code, operation_name, operation_type, check_rules)
  SELECT p_wo_id, r.seq, r.operation_code, o.operation_name, o.operation_type, r.check_rules
  FROM md_routing_operation r JOIN md_operation o ON o.operation_code = r.operation_code
  WHERE r.routing_id = p_routing_id;
END $$

-- 单机完整过站辅助: 插入一条 CHECKIN 流水并推进 SN 状态
DROP PROCEDURE IF EXISTS seed_pass $$
CREATE PROCEDURE seed_pass(IN p_sn VARCHAR(64), IN p_wo BIGINT, IN p_station VARCHAR(50),
  IN p_op VARCHAR(50), IN p_seq INT, IN p_result VARCHAR(5), IN p_ngcode VARCHAR(50),
  IN p_test JSON, IN p_round INT, IN p_time DATETIME, IN p_operator VARCHAR(50))
BEGIN
  INSERT INTO station_log (sn, work_order_id, station_code, operation_code, seq, record_type,
    result, ng_code, test_data, retest_round, checkin_key, operator, create_time)
  VALUES (p_sn, p_wo, p_station, p_op, p_seq, 'CHECKIN', p_result, p_ngcode, p_test, p_round,
    CONCAT(p_sn,':',p_station,':',p_round), p_operator, p_time);
END $$

-- 工单1(WO20260801001, V1路线, 80台, 已完工): FPY 95%, 最终良率 98.75%
DROP PROCEDURE IF EXISTS seed_wo1 $$
CREATE PROCEDURE seed_wo1()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE v_m VARCHAR(64); DECLARE v_l VARCHAR(64); DECLARE v_r VARCHAR(64); DECLARE v_c VARCHAR(64);
  DECLARE v_lbatch VARCHAR(50);
  DECLARE v_time DATETIME;
  DECLARE v_ng INT;
  CALL seed_wo_snapshot(1, 1);
  WHILE i <= 80 DO
    -- 8台/天 x 10天, 报表按日才有数据
    SET v_time = DATE_ADD(DATE_ADD('2026-08-02 08:30:00', INTERVAL FLOOR((i-1)/8) DAY), INTERVAL ((i-1)%8)*60 MINUTE);
    SET v_m = CONCAT('WO20260801001-', LPAD(i,4,'0'));
    -- 问题批次 PCLB240801 集中在 21~36 号机
    SET v_lbatch = IF(i BETWEEN 21 AND 36, 'PCLB240801', 'PCLA240801');
    SET v_l = CONCAT('L-', v_lbatch, '-', LPAD(i,4,'0'));
    SET v_r = CONCAT('R-PCRA240801-', LPAD(i,4,'0'));
    SET v_c = CONCAT('C-PCCA240801-', LPAD(i,4,'0'));
    -- 注册 4 个 SN
    INSERT INTO sn_registry (sn, sn_type, work_order_id, batch_no, status, create_time)
    VALUES (v_l,'LEFT',1,v_lbatch,'DONE',v_time),(v_r,'RIGHT',1,'PCRA240801','DONE',v_time),
           (v_c,'CASE',1,'PCCA240801','DONE',v_time),(v_m,'MACHINE',1,NULL,'INIT',v_time);
    -- 部件过站(V1: 烧录+RF; 盒组装+充放电)
    CALL seed_pass(v_l,1,'L1-BURN-L','OP-BURN-L',20,'OK',NULL,JSON_OBJECT('firmware','1.2.5','mac',CONCAT('A4C1',LPAD(i,8,'0')), 'station', 'L1-BURN-L'),0,DATE_ADD(v_time,INTERVAL 2 MINUTE),'op1');
    CALL seed_pass(v_l,1,'L1-RF-L','OP-RF-L',40,'OK',NULL,JSON_OBJECT('tx_power',-1.2+i%2*0.5),0,DATE_ADD(v_time,INTERVAL 4 MINUTE),'op1');
    CALL seed_pass(v_r,1,'L1-BURN-R','OP-BURN-R',30,'OK',NULL,JSON_OBJECT('firmware','1.2.5','mac',CONCAT('A4C2',LPAD(i,8,'0'))),0,DATE_ADD(v_time,INTERVAL 3 MINUTE),'op1');
    CALL seed_pass(v_r,1,'L1-RF-R','OP-RF-R',50,'OK',NULL,JSON_OBJECT('tx_power',-1.1+i%3*0.3),0,DATE_ADD(v_time,INTERVAL 5 MINUTE),'op1');
    CALL seed_pass(v_c,1,'L1-CASE-ASSY','OP-CASE-ASSY',60,'OK',NULL,NULL,0,DATE_ADD(v_time,INTERVAL 6 MINUTE),'op1');
    CALL seed_pass(v_c,1,'L1-CASE-TEST','OP-CASE-TEST',70,'OK',NULL,JSON_OBJECT('charge_cur',128),0,DATE_ADD(v_time,INTERVAL 7 MINUTE),'op1');
    -- 绑定(三码绑定站) + 绑定流水
    INSERT INTO sn_binding (parent_sn, child_sn, bind_type, work_order_id, station_code, operator, create_time) VALUES
      (v_m, v_l, 'LEFT',  1, 'L1-COUPLE','op1',DATE_ADD(v_time,INTERVAL 8 MINUTE)),
      (v_m, v_r, 'RIGHT', 1, 'L1-COUPLE','op1',DATE_ADD(v_time,INTERVAL 8 MINUTE)),
      (v_m, v_c, 'CASE',  1, 'L1-COUPLE','op1',DATE_ADD(v_time,INTERVAL 8 MINUTE));
    UPDATE sn_registry SET parent_sn = v_m WHERE sn IN (v_l, v_r, v_c);
    CALL seed_pass(v_m,1,'L1-COUPLE','OP-COUPLE',80,'OK',NULL,JSON_OBJECT('children',JSON_ARRAY(v_l,v_r,v_c)),0,DATE_ADD(v_time,INTERVAL 8 MINUTE),'op1');
    CALL seed_pass(v_m,1,'L1-AGING','OP-AGING',90,'OK',NULL,JSON_OBJECT('aging_hours',9.5),0,DATE_ADD(v_time,INTERVAL 10 MINUTE),'op1');
    -- FCT: 问题批 21/25/29/33 号机 NG(D04 灵敏度偏低)
    SET v_ng = IF(v_lbatch='PCLB240801' AND i IN (21,25,29,33), 1, 0);
    IF v_ng = 1 THEN
      CALL seed_pass(v_m,1,'L1-FCT','OP-FCT',100,'NG','D04',JSON_OBJECT('mic_sensitivity',-41.5),0,DATE_ADD(v_time,INTERVAL 12 MINUTE),'op1');
      INSERT INTO defect_record (sn, work_order_id, station_code, operation_code, defect_code, defect_desc, discover_type, repair_round, status, create_time)
      VALUES (v_m,1,'L1-FCT','OP-FCT','D04','FCT检出麦克风灵敏度-41.5dB(规格-38±3dB)','CHECKIN',0,'OPEN',DATE_ADD(v_time,INTERVAL 12 MINUTE));
      IF i <> 33 THEN
        -- 维修换左耳(换成正常批次)后重测通过
        UPDATE sn_registry SET status='RETEST' WHERE sn = v_m;
        UPDATE defect_record SET status='REPAIRED', update_time=DATE_ADD(v_time,INTERVAL 90 MINUTE)
         WHERE sn=v_m AND status='OPEN';
        INSERT INTO repair_record (defect_id, sn, action, root_cause, change_batch_no, result, repairer, create_time)
        VALUES ((SELECT id FROM defect_record WHERE sn=v_m LIMIT 1), v_m, '更换左耳整机,重新耦合绑定',
                '左耳PCBA问题批次贴装华科问题麦克风(MICB240715)', 'PCLA240801', 'OK', 'qc1', DATE_ADD(v_time,INTERVAL 90 MINUTE));
        CALL seed_pass(v_m,1,'L1-FCT','OP-FCT',100,'OK',NULL,JSON_OBJECT('firmware','1.2.5','mic_sensitivity',-37.8),1,DATE_ADD(v_time,INTERVAL 95 MINUTE),'op1');
        CALL seed_pass(v_m,1,'L1-APP','OP-APP',110,'OK',NULL,NULL,1,DATE_ADD(v_time,INTERVAL 96 MINUTE),'op1');
        CALL seed_pass(v_m,1,'L1-BOXING','OP-BOXING',120,'OK',NULL,NULL,1,DATE_ADD(v_time,INTERVAL 97 MINUTE),'op1');
        CALL seed_pass(v_m,1,'L1-CARTON','OP-CARTON',130,'OK',NULL,NULL,1,DATE_ADD(v_time,INTERVAL 98 MINUTE),'op1');
        CALL seed_pass(v_m,1,'L1-WEIGH','OP-WEIGH',140,'OK',NULL,JSON_OBJECT('weight_g',268.5),1,DATE_ADD(v_time,INTERVAL 99 MINUTE),'op1');
        UPDATE sn_registry SET status='DONE', current_seq=140 WHERE sn = v_m;
      ELSE
        -- 33号机报废
        UPDATE sn_registry SET status='SCRAP' WHERE sn = v_m;
        UPDATE defect_record SET status='SCRAP' WHERE sn=v_m AND status='OPEN';
      END IF;
    ELSE
      CALL seed_pass(v_m,1,'L1-FCT','OP-FCT',100,'OK',NULL,JSON_OBJECT('firmware','1.2.5','mic_sensitivity',-37.6+((i%5)*0.3)),0,DATE_ADD(v_time,INTERVAL 12 MINUTE),'op1');
      CALL seed_pass(v_m,1,'L1-APP','OP-APP',110,'OK',NULL,NULL,0,DATE_ADD(v_time,INTERVAL 13 MINUTE),'op1');
      CALL seed_pass(v_m,1,'L1-BOXING','OP-BOXING',120,'OK',NULL,NULL,0,DATE_ADD(v_time,INTERVAL 14 MINUTE),'op1');
      CALL seed_pass(v_m,1,'L1-CARTON','OP-CARTON',130,'OK',NULL,NULL,0,DATE_ADD(v_time,INTERVAL 15 MINUTE),'op1');
      CALL seed_pass(v_m,1,'L1-WEIGH','OP-WEIGH',140,'OK',NULL,JSON_OBJECT('weight_g',267.9+(i%4)*0.4),0,DATE_ADD(v_time,INTERVAL 16 MINUTE),'op1');
      UPDATE sn_registry SET status='DONE', current_seq=140 WHERE sn = v_m;
    END IF;
    SET i = i + 1;
  END WHILE;
END $$

-- 工单2(WO20260816002, V2路线含声学站, 40台投产中):
--   1~20 完工(全OK); 21~40 部件全部过完(RF/盒测试)等待三码绑定;
--   22/26 号左耳在声学站被 D04 拦截(不良 OPEN, 维修工作台演示用)
DROP PROCEDURE IF EXISTS seed_wo2 $$
CREATE PROCEDURE seed_wo2()
BEGIN
  DECLARE i INT DEFAULT 1;
  DECLARE v_m VARCHAR(64); DECLARE v_l VARCHAR(64); DECLARE v_r VARCHAR(64); DECLARE v_c VARCHAR(64);
  DECLARE v_lbatch VARCHAR(50); DECLARE v_time DATETIME;
  CALL seed_wo_snapshot(2, 2);
  WHILE i <= 40 DO
    SET v_time = DATE_ADD('2026-08-16 08:00:00', INTERVAL (i-1)*20 MINUTE);
    SET v_m = CONCAT('WO20260816002-', LPAD(i,4,'0'));
    -- 问题批次余料装在 1~8 号机(V2 在声学站提前拦截)
    SET v_lbatch = IF(i <= 8, 'PCLB240801', 'PCLA240801');
    SET v_l = CONCAT('L-', v_lbatch, '-W2-', LPAD(i,4,'0'));
    SET v_r = CONCAT('R-PCRA240801-W2-', LPAD(i,4,'0'));
    SET v_c = CONCAT('C-PCCA240801-W2-', LPAD(i,4,'0'));
    INSERT INTO sn_registry (sn, sn_type, work_order_id, batch_no, status, create_time)
    VALUES (v_l,'LEFT',2,v_lbatch,'IN_LINE',v_time),(v_r,'RIGHT',2,'PCRA240801','IN_LINE',v_time),
           (v_c,'CASE',2,'PCCA240801','IN_LINE',v_time),(v_m,'MACHINE',2,NULL,'INIT',v_time);
    -- 左耳: 烧录 -> 声学(问题批 22/26 号 NG 并停线待修)
    CALL seed_pass(v_l,2,'L1-BURN-L','OP-BURN-L',20,'OK',NULL,JSON_OBJECT('firmware','1.2.5'),0,DATE_ADD(v_time,INTERVAL 2 MINUTE),'op1');
    IF i IN (22,26) THEN
      CALL seed_pass(v_l,2,'L1-ACOUS-L','OP-ACOUS-L',25,'NG','D04',JSON_OBJECT('mic_sensitivity',-42.1),0,DATE_ADD(v_time,INTERVAL 3 MINUTE),'op1');
      UPDATE sn_registry SET status='NG', current_seq=25 WHERE sn = v_l;
      INSERT INTO defect_record (sn, work_order_id, station_code, operation_code, defect_code, defect_desc, discover_type, repair_round, status, create_time)
      VALUES (v_l,2,'L1-ACOUS-L','OP-ACOUS-L','D04','声学站拦截:灵敏度-42.1dB 低于下限-38dB','CHECKIN',0,'OPEN',DATE_ADD(v_time,INTERVAL 3 MINUTE));
      UPDATE plan_work_order SET ng_qty = ng_qty + 1 WHERE id = 2;
    ELSE
      CALL seed_pass(v_l,2,'L1-ACOUS-L','OP-ACOUS-L',25,'OK',NULL,JSON_OBJECT('mic_sensitivity',-37.5+(i%6)*0.2),0,DATE_ADD(v_time,INTERVAL 3 MINUTE),'op1');
      UPDATE sn_registry SET current_seq=25 WHERE sn = v_l;
    END IF;
    IF i NOT IN (22,26) THEN
      -- 右耳 + 盒正常过
      CALL seed_pass(v_r,2,'L1-BURN-R','OP-BURN-R',30,'OK',NULL,JSON_OBJECT('firmware','1.2.5'),0,DATE_ADD(v_time,INTERVAL 4 MINUTE),'op1');
      CALL seed_pass(v_r,2,'L1-ACOUS-R','OP-ACOUS-R',35,'OK',NULL,JSON_OBJECT('mic_sensitivity',-37.2+(i%4)*0.2),0,DATE_ADD(v_time,INTERVAL 5 MINUTE),'op1');
      CALL seed_pass(v_l,2,'L1-RF-L','OP-RF-L',40,'OK',NULL,JSON_OBJECT('tx_power',-1.3+(i%3)*0.2),0,DATE_ADD(v_time,INTERVAL 6 MINUTE),'op1');
      CALL seed_pass(v_r,2,'L1-RF-R','OP-RF-R',50,'OK',NULL,JSON_OBJECT('tx_power',-1.2+(i%2)*0.3),0,DATE_ADD(v_time,INTERVAL 7 MINUTE),'op1');
      CALL seed_pass(v_c,2,'L1-CASE-ASSY','OP-CASE-ASSY',60,'OK',NULL,NULL,0,DATE_ADD(v_time,INTERVAL 8 MINUTE),'op1');
      CALL seed_pass(v_c,2,'L1-CASE-TEST','OP-CASE-TEST',70,'OK',NULL,JSON_OBJECT('charge_cur',126+(i%5)),0,DATE_ADD(v_time,INTERVAL 9 MINUTE),'op1');
      UPDATE sn_registry SET current_seq=50 WHERE sn IN (v_l,v_r);
      UPDATE sn_registry SET current_seq=70 WHERE sn = v_c;
      IF i <= 20 THEN
        -- 完工 20 台
        INSERT INTO sn_binding (parent_sn, child_sn, bind_type, work_order_id, station_code, operator, create_time) VALUES
          (v_m, v_l, 'LEFT',  2, 'L1-COUPLE','op1',DATE_ADD(v_time,INTERVAL 10 MINUTE)),
          (v_m, v_r, 'RIGHT', 2, 'L1-COUPLE','op1',DATE_ADD(v_time,INTERVAL 10 MINUTE)),
          (v_m, v_c, 'CASE',  2, 'L1-COUPLE','op1',DATE_ADD(v_time,INTERVAL 10 MINUTE));
        UPDATE sn_registry SET parent_sn = v_m WHERE sn IN (v_l, v_r, v_c);
        CALL seed_pass(v_m,2,'L1-COUPLE','OP-COUPLE',80,'OK',NULL,JSON_OBJECT('children',JSON_ARRAY(v_l,v_r,v_c)),0,DATE_ADD(v_time,INTERVAL 10 MINUTE),'op1');
        CALL seed_pass(v_m,2,'L1-AGING','OP-AGING',90,'OK',NULL,JSON_OBJECT('aging_hours',10.2),0,DATE_ADD(v_time,INTERVAL 12 MINUTE),'op1');
        CALL seed_pass(v_m,2,'L1-FCT','OP-FCT',100,'OK',NULL,JSON_OBJECT('firmware','1.2.5','mic_sensitivity',-37.4+(i%4)*0.2),0,DATE_ADD(v_time,INTERVAL 14 MINUTE),'op1');
        CALL seed_pass(v_m,2,'L1-APP','OP-APP',110,'OK',NULL,NULL,0,DATE_ADD(v_time,INTERVAL 15 MINUTE),'op1');
        CALL seed_pass(v_m,2,'L1-BOXING','OP-BOXING',120,'OK',NULL,NULL,0,DATE_ADD(v_time,INTERVAL 16 MINUTE),'op1');
        CALL seed_pass(v_m,2,'L1-CARTON','OP-CARTON',130,'OK',NULL,NULL,0,DATE_ADD(v_time,INTERVAL 17 MINUTE),'op1');
        CALL seed_pass(v_m,2,'L1-WEIGH','OP-WEIGH',140,'OK',NULL,JSON_OBJECT('weight_g',268.1+(i%3)*0.3),0,DATE_ADD(v_time,INTERVAL 18 MINUTE),'op1');
        UPDATE sn_registry SET status='DONE', current_seq=140 WHERE sn = v_m;
        UPDATE sn_registry SET status='DONE' WHERE sn IN (v_l,v_r,v_c);
      ELSEIF i BETWEEN 21 AND 36 THEN
        -- 到 RF 完成等待绑定
        UPDATE sn_registry SET status='IN_LINE' WHERE sn = v_m;
      ELSE
        UPDATE sn_registry SET status='IN_LINE' WHERE sn = v_m;
      END IF;
    END IF;
    SET i = i + 1;
  END WHILE;
END $$

DELIMITER ;

CALL seed_wo1();
CALL seed_wo2();

-- 工单2 上料记录(演示 LOADING 流水与上料防呆): 总装一线盒组装站上了盒壳与电池批次
INSERT INTO station_log (sn, work_order_id, station_code, operation_code, record_type, batch_no, test_data, operator, create_time) VALUES
('LOADING-0001', 2, 'L1-CASE-ASSY', 'OP-CASE-ASSY', 'LOADING', 'BATA240801', JSON_OBJECT('material_code','BAT-401'), 'op1', '2026-08-16 07:50:00'),
('LOADING-0002', 2, 'L1-CASE-ASSY', 'OP-CASE-ASSY', 'LOADING', 'SPKA240801', JSON_OBJECT('material_code','SPK-301'), 'op1', '2026-08-16 07:52:00');

-- 工单1 一条 FAI 记录
INSERT INTO fai_record (work_order_id, operation_code, sn, result, checker, remark, create_time) VALUES
(1, 'OP-FCT', 'WO20260801001-0001', 'PASS', 'qc1', '换线首件', '2026-08-02 09:10:00');

-- 清理临时过程
DROP PROCEDURE IF EXISTS seed_wo_snapshot;
DROP PROCEDURE IF EXISTS seed_pass;
DROP PROCEDURE IF EXISTS seed_wo1;
DROP PROCEDURE IF EXISTS seed_wo2;

-- 验证: 期望 80 台整机(79 DONE + 1 SCRAP), FPY=76/80=95%
SELECT (SELECT COUNT(*) FROM sn_registry WHERE work_order_id=1 AND sn_type='MACHINE' AND status='DONE') AS wo1_done,
       (SELECT COUNT(*) FROM sn_registry WHERE work_order_id=1 AND sn_type='MACHINE' AND status='SCRAP') AS wo1_scrap,
       (SELECT COUNT(*) FROM station_log) AS total_logs,
       (SELECT COUNT(*) FROM defect_record) AS defects,
       (SELECT COUNT(*) FROM sn_binding) AS bindings;
