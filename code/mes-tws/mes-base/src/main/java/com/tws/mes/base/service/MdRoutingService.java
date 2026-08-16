package com.tws.mes.base.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tws.mes.base.entity.MdOperation;
import com.tws.mes.base.entity.MdRouting;
import com.tws.mes.base.entity.MdRoutingOperation;
import com.tws.mes.base.mapper.MdOperationMapper;
import com.tws.mes.base.mapper.MdRoutingMapper;
import com.tws.mes.base.mapper.MdRoutingOperationMapper;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工艺路线（版本化）。
 * 核心概念：路线 "发布"(status=2) 后才能被工单引用；工单创建时把工序+防呆规则
 * 快照到 plan_wo_operation —— 之后路线改版/新建版本都不影响已建工单。
 */
@Service
@RequiredArgsConstructor
public class MdRoutingService {

    private final MdRoutingMapper routingMapper;
    private final MdRoutingOperationMapper opMapper;
    private final MdOperationMapper operationMapper;

    public PageResult<MdRouting> page(long page, long size, String keyword) {
        LambdaQueryWrapper<MdRouting> qw = new LambdaQueryWrapper<>();
        qw.and(StrUtil.isNotBlank(keyword), w -> w.like(MdRouting::getRoutingCode, keyword)
                        .or().like(MdRouting::getRoutingName, keyword))
                .orderByDesc(MdRouting::getVersion);
        Page<MdRouting> p = routingMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /** 已发布路线（工单创建时下拉用），同编码取最高版本 */
    public List<MdRouting> releasedList(Long productMaterialId) {
        List<MdRouting> all = routingMapper.selectList(new LambdaQueryWrapper<MdRouting>()
                .eq(MdRouting::getStatus, 2)
                .eq(productMaterialId != null, MdRouting::getProductMaterialId, productMaterialId)
                .orderByDesc(MdRouting::getVersion));
        Map<String, MdRouting> latest = all.stream().collect(Collectors.toMap(
                MdRouting::getRoutingCode, r -> r, (a, b) -> a));
        return new ArrayList<>(latest.values());
    }

    public Map<String, Object> detail(Long id) {
        MdRouting routing = routingMapper.selectById(id);
        if (routing == null) throw new BizException("路线不存在");
        List<MdRoutingOperation> ops = opMapper.selectList(
                new LambdaQueryWrapper<MdRoutingOperation>()
                        .eq(MdRoutingOperation::getRoutingId, id)
                        .orderByAsc(MdRoutingOperation::getSeq));
        Map<String, Object> result = new HashMap<>();
        result.put("routing", routing);
        result.put("operations", ops);
        return result;
    }

    /** 保存（草稿/修改）：整单覆盖式保存工序明细，checkRules 必须是合法 JSON */
    @Transactional
    public void save(MdRouting routing, List<MdRoutingOperation> operations) {
        if (StrUtil.isBlank(routing.getRoutingCode())) throw new BizException("路线编码不能为空");
        if (operations == null || operations.isEmpty()) throw new BizException("至少要有一道工序");
        for (MdRoutingOperation op : operations) {
            if (op.getSeq() == null || StrUtil.isBlank(op.getOperationCode()))
                throw new BizException("工序顺序与工序编码不能为空");
            if (StrUtil.isNotBlank(op.getCheckRules()) && !JSONUtil.isTypeJSON(op.getCheckRules()))
                throw new BizException("工序[" + op.getOperationCode() + "]的防呆规则不是合法 JSON");
        }
        if (routing.getId() == null) {
            // 新建：同编码自动递增版本号，初始为草稿
            Integer maxVer = routingMapper.selectList(new LambdaQueryWrapper<MdRouting>()
                            .eq(MdRouting::getRoutingCode, routing.getRoutingCode())).stream()
                    .map(MdRouting::getVersion).max(Integer::compareTo).orElse(0);
            routing.setVersion(maxVer + 1);
            routing.setStatus(1);
            routingMapper.insert(routing);
        } else {
            MdRouting db = routingMapper.selectById(routing.getId());
            if (db == null) throw new BizException("路线不存在");
            if (db.getStatus() == 2)
                throw new BizException("已发布版本不可修改，请基于它创建新版本（保证历史工单快照可追溯）");
            routing.setStatus(1);
            routingMapper.updateById(routing);
            opMapper.delete(new LambdaQueryWrapper<MdRoutingOperation>()
                    .eq(MdRoutingOperation::getRoutingId, routing.getId()));
        }
        for (MdRoutingOperation op : operations) {
            op.setId(null);
            op.setRoutingId(routing.getId());
            opMapper.insert(op);
        }
    }

    /** 发布：草稿 → 已发布 */
    public void publish(Long id) {
        MdRouting routing = routingMapper.selectById(id);
        if (routing == null) throw new BizException("路线不存在");
        if (routing.getStatus() != 1) throw new BizException("只有草稿状态可发布");
        routing.setStatus(2);
        routingMapper.updateById(routing);
    }

    public void delete(Long id) {
        MdRouting routing = routingMapper.selectById(id);
        if (routing == null) return;
        if (routing.getStatus() == 2) throw new BizException("已发布路线不可删除");
        routingMapper.deleteById(id);
        opMapper.delete(new LambdaQueryWrapper<MdRoutingOperation>()
                .eq(MdRoutingOperation::getRoutingId, id));
    }
}
