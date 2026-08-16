package com.tws.mes.quality.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tws.mes.base.service.CurrentUserService;
import com.tws.mes.common.constant.MesConst;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.common.result.PageResult;
import com.tws.mes.execution.entity.DefectRecord;
import com.tws.mes.execution.entity.PlanWorkOrder;
import com.tws.mes.execution.entity.RepairRecord;
import com.tws.mes.execution.entity.SnRegistry;
import com.tws.mes.execution.mapper.DefectRecordMapper;
import com.tws.mes.execution.mapper.PlanWorkOrderMapper;
import com.tws.mes.execution.mapper.RepairRecordMapper;
import com.tws.mes.execution.mapper.SnRegistryMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 质量管理：不良单流转 + 维修闭环。
 *
 * 维修闭环链路（对应面试话术"维修 2 小时闭环"）：
 * 过站 NG 自动开单(OPEN) → 质量员维修登记 →
 *   修好(OK)  → 不良单 REPAIRED，SN 置 RETEST，回原工位重测（retest_round+1，不污染 FPY 口径）
 *   修不好(NG)→ 不良单 SCRAP，SN 报废
 */
@Service
@RequiredArgsConstructor
public class QualityService {

    private final DefectRecordMapper defectMapper;
    private final RepairRecordMapper repairMapper;
    private final SnRegistryMapper snMapper;
    private final PlanWorkOrderMapper woMapper;
    private final CurrentUserService currentUserService;

    public PageResult<Map<String, Object>> defectPage(long page, long size, String sn, Long workOrderId,
                                                      String status, String defectCode) {
        LambdaQueryWrapper<DefectRecord> qw = new LambdaQueryWrapper<DefectRecord>()
                .like(StrUtil.isNotBlank(sn), DefectRecord::getSn, sn)
                .eq(workOrderId != null, DefectRecord::getWorkOrderId, workOrderId)
                .eq(StrUtil.isNotBlank(status), DefectRecord::getStatus, status)
                .eq(StrUtil.isNotBlank(defectCode), DefectRecord::getDefectCode, defectCode)
                .orderByAsc(DefectRecord::getStatus).orderByDesc(DefectRecord::getId);
        Page<DefectRecord> p = defectMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> rows = p.getRecords().stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("defect", d);
            PlanWorkOrder wo = d.getWorkOrderId() == null ? null : woMapper.selectById(d.getWorkOrderId());
            m.put("woNo", wo == null ? null : wo.getWoNo());
            return m;
        }).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), rows);
    }

    @Data
    public static class RepairDTO {
        private Long defectId;
        private String action;
        private String rootCause;
        private String changeBatchNo;
        /** OK 修好回流 / NG 报废 */
        private String result;
    }

    /** 维修登记：闭环不良单并驱动 SN 回流/报废 */
    @Transactional
    public void repair(RepairDTO dto) {
        if (StrUtil.isBlank(dto.getAction())) throw new BizException("维修措施不能为空");
        DefectRecord defect = defectMapper.selectById(dto.getDefectId());
        if (defect == null) throw new BizException("不良单不存在");
        if (!MesConst.DEFECT_OPEN.equals(defect.getStatus())) {
            throw new BizException("该不良单已处理（" + defect.getStatus() + "），不能重复维修");
        }
        boolean fixed = "OK".equals(dto.getResult());
        if (!fixed && !"NG".equals(dto.getResult())) throw new BizException("维修结果必须为 OK 或 NG");

        RepairRecord r = new RepairRecord();
        r.setDefectId(defect.getId());
        r.setSn(defect.getSn());
        r.setAction(dto.getAction());
        r.setRootCause(dto.getRootCause());
        r.setChangeBatchNo(dto.getChangeBatchNo());
        r.setResult(dto.getResult());
        r.setRepairer(currentUserService.currentUsername());
        repairMapper.insert(r);

        defect.setStatus(fixed ? MesConst.DEFECT_REPAIRED : MesConst.DEFECT_SCRAP);
        defectMapper.updateById(defect);

        SnRegistry sn = snMapper.selectOne(new LambdaQueryWrapper<SnRegistry>()
                .eq(SnRegistry::getSn, defect.getSn()));
        if (sn != null) {
            sn.setStatus(fixed ? MesConst.SN_RETEST : MesConst.SN_SCRAP);
            snMapper.updateById(sn);
        }
    }

    /** 复检/抽检手动开单（不是过站产生的不良） */
    public void manualDefect(String sn, String defectCode, String desc, String discoverType) {
        SnRegistry s = snMapper.selectOne(new LambdaQueryWrapper<SnRegistry>().eq(SnRegistry::getSn, sn));
        if (s == null) throw new BizException("SN 不存在: " + sn);
        DefectRecord d = new DefectRecord();
        d.setSn(sn);
        d.setWorkOrderId(s.getWorkOrderId());
        d.setDefectCode(defectCode);
        d.setDefectDesc(desc);
        d.setDiscoverType(StrUtil.blankToDefault(discoverType, "RECHECK"));
        d.setRepairRound(0);
        d.setStatus(MesConst.DEFECT_OPEN);
        defectMapper.insert(d);
    }

    /** 某 SN 的维修历史 */
    public List<RepairRecord> repairsOf(String sn) {
        return repairMapper.selectList(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getSn, sn).orderByDesc(RepairRecord::getId));
    }
}
