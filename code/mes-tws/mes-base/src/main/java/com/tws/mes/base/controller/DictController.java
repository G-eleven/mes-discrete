package com.tws.mes.base.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.base.entity.MdDefectCode;
import com.tws.mes.base.entity.MdLine;
import com.tws.mes.base.entity.MdOperation;
import com.tws.mes.base.entity.MdStation;
import com.tws.mes.base.mapper.MdDefectCodeMapper;
import com.tws.mes.base.mapper.MdLineMapper;
import com.tws.mes.base.mapper.MdOperationMapper;
import com.tws.mes.base.mapper.MdStationMapper;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典类基础数据聚合接口：工序 / 产线 / 工位 / 不良代码。
 * 这些表结构简单（无复杂业务规则），直接在 Controller 组织简单查询即可；
 * 一旦某个域长出业务规则（如工位绑定工序校验），应拆出独立 Service。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DictController {

    private final MdOperationMapper operationMapper;
    private final MdLineMapper lineMapper;
    private final MdStationMapper stationMapper;
    private final MdDefectCodeMapper defectCodeMapper;

    /* ---------- 工序 ---------- */

    @GetMapping("/operation/list")
    public Result<List<MdOperation>> operationList() {
        return Result.ok(operationMapper.selectList(
                new LambdaQueryWrapper<MdOperation>().orderByAsc(MdOperation::getOperationCode)));
    }

    @SaCheckRole("admin")
    @PostMapping("/operation/save")
    public Result<Void> operationSave(@RequestBody MdOperation op) {
        Long cnt = operationMapper.selectCount(new LambdaQueryWrapper<MdOperation>()
                .eq(MdOperation::getOperationCode, op.getOperationCode())
                .ne(op.getId() != null, MdOperation::getId, op.getId()));
        if (cnt != null && cnt > 0) throw new BizException("工序编码已存在");
        if (op.getId() == null) operationMapper.insert(op);
        else operationMapper.updateById(op);
        return Result.ok();
    }

    /* ---------- 产线 ---------- */

    @GetMapping("/line/list")
    public Result<List<MdLine>> lineList() {
        return Result.ok(lineMapper.selectList(new LambdaQueryWrapper<MdLine>().orderByAsc(MdLine::getId)));
    }

    @SaCheckRole("admin")
    @PostMapping("/line/save")
    public Result<Void> lineSave(@RequestBody MdLine line) {
        Long cnt = lineMapper.selectCount(new LambdaQueryWrapper<MdLine>()
                .eq(MdLine::getLineCode, line.getLineCode())
                .ne(line.getId() != null, MdLine::getId, line.getId()));
        if (cnt != null && cnt > 0) throw new BizException("产线编码已存在");
        if (line.getId() == null) lineMapper.insert(line);
        else lineMapper.updateById(line);
        return Result.ok();
    }

    /* ---------- 工位 ---------- */

    @GetMapping("/station/list")
    public Result<List<MdStation>> stationList(@RequestParam(required = false) Long lineId) {
        return Result.ok(stationMapper.selectList(new LambdaQueryWrapper<MdStation>()
                .eq(lineId != null, MdStation::getLineId, lineId)
                .orderByAsc(MdStation::getStationCode)));
    }

    @SaCheckRole("admin")
    @PostMapping("/station/save")
    public Result<Void> stationSave(@RequestBody MdStation st) {
        Long cnt = stationMapper.selectCount(new LambdaQueryWrapper<MdStation>()
                .eq(MdStation::getStationCode, st.getStationCode())
                .ne(st.getId() != null, MdStation::getId, st.getId()));
        if (cnt != null && cnt > 0) throw new BizException("工位编码已存在");
        if (st.getId() == null) stationMapper.insert(st);
        else stationMapper.updateById(st);
        return Result.ok();
    }

    /* ---------- 不良代码 ---------- */

    @GetMapping("/defect-code/list")
    public Result<List<MdDefectCode>> defectCodeList() {
        return Result.ok(defectCodeMapper.selectList(
                new LambdaQueryWrapper<MdDefectCode>().orderByAsc(MdDefectCode::getDefectCode)));
    }

    @SaCheckRole(value = {"admin", "quality"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @PostMapping("/defect-code/save")
    public Result<Void> defectCodeSave(@RequestBody MdDefectCode dc) {
        Long cnt = defectCodeMapper.selectCount(new LambdaQueryWrapper<MdDefectCode>()
                .eq(MdDefectCode::getDefectCode, dc.getDefectCode())
                .ne(dc.getId() != null, MdDefectCode::getId, dc.getId()));
        if (cnt != null && cnt > 0) throw new BizException("不良代码已存在");
        if (dc.getId() == null) defectCodeMapper.insert(dc);
        else defectCodeMapper.updateById(dc);
        return Result.ok();
    }
}
