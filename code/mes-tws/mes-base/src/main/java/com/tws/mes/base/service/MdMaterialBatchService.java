package com.tws.mes.base.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tws.mes.base.entity.MdMaterialBatch;
import com.tws.mes.base.mapper.MdMaterialBatchMapper;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 物料批次：关键料/半成品来料批次登记（IQC 环节），上料与反向追溯的锚点 */
@Service
@RequiredArgsConstructor
public class MdMaterialBatchService {

    private final MdMaterialBatchMapper mapper;

    public PageResult<MdMaterialBatch> page(long page, long size, String keyword, Long materialId) {
        LambdaQueryWrapper<MdMaterialBatch> qw = new LambdaQueryWrapper<MdMaterialBatch>()
                .like(StrUtil.isNotBlank(keyword), MdMaterialBatch::getBatchNo, keyword)
                .eq(materialId != null, MdMaterialBatch::getMaterialId, materialId)
                .orderByDesc(MdMaterialBatch::getId);
        Page<MdMaterialBatch> p = mapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public List<MdMaterialBatch> list(String keyword) {
        return mapper.selectList(new LambdaQueryWrapper<MdMaterialBatch>()
                .like(StrUtil.isNotBlank(keyword), MdMaterialBatch::getBatchNo, keyword)
                .eq(MdMaterialBatch::getStatus, 1)
                .orderByDesc(MdMaterialBatch::getId));
    }

    public void save(MdMaterialBatch b) {
        Long cnt = mapper.selectCount(new LambdaQueryWrapper<MdMaterialBatch>()
                .eq(MdMaterialBatch::getBatchNo, b.getBatchNo())
                .ne(b.getId() != null, MdMaterialBatch::getId, b.getId()));
        if (cnt != null && cnt > 0) throw new BizException("批次号已存在: " + b.getBatchNo());
        if (b.getId() == null) {
            mapper.insert(b);
        } else {
            mapper.updateById(b);
        }
    }
}
