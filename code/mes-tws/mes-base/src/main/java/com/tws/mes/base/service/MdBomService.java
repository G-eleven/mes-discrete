package com.tws.mes.base.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tws.mes.base.entity.MdBom;
import com.tws.mes.base.entity.MdBomItem;
import com.tws.mes.base.entity.MdMaterial;
import com.tws.mes.base.mapper.MdBomItemMapper;
import com.tws.mes.base.mapper.MdBomMapper;
import com.tws.mes.base.mapper.MdMaterialMapper;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** BOM：主表 + 子件明细（主从整单保存） */
@Service
@RequiredArgsConstructor
public class MdBomService {

    private final MdBomMapper bomMapper;
    private final MdBomItemMapper itemMapper;
    private final MdMaterialMapper materialMapper;

    public PageResult<MdBom> page(long page, long size, String keyword) {
        LambdaQueryWrapper<MdBom> qw = new LambdaQueryWrapper<MdBom>()
                .like(StrUtil.isNotBlank(keyword), MdBom::getBomCode, keyword)
                .orderByDesc(MdBom::getId);
        Page<MdBom> p = bomMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /** 详情：主表 + 子件明细（带物料编码/名称，前端直接可展示） */
    public Map<String, Object> detail(Long id) {
        MdBom bom = bomMapper.selectById(id);
        if (bom == null) throw new BizException("BOM 不存在");
        List<MdBomItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<MdBomItem>().eq(MdBomItem::getBomId, id));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MdBomItem it : items) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", it.getId());
            row.put("childMaterialId", it.getChildMaterialId());
            row.put("quantity", it.getQuantity());
            row.put("operationCode", it.getOperationCode());
            MdMaterial m = materialMapper.selectById(it.getChildMaterialId());
            if (m != null) {
                row.put("materialCode", m.getMaterialCode());
                row.put("materialName", m.getMaterialName());
                row.put("unit", m.getUnit());
            }
            rows.add(row);
        }
        Map<String, Object> r = new HashMap<>();
        r.put("bom", bom);
        r.put("items", rows);
        return r;
    }

    @Transactional
    public void save(MdBom bom, List<MdBomItem> items) {
        if (StrUtil.isBlank(bom.getBomCode())) throw new BizException("BOM 编码不能为空");
        if (items == null || items.isEmpty()) throw new BizException("BOM 至少要有一个子件");
        Long cnt = bomMapper.selectCount(new LambdaQueryWrapper<MdBom>()
                .eq(MdBom::getBomCode, bom.getBomCode())
                .ne(bom.getId() != null, MdBom::getId, bom.getId()));
        if (cnt != null && cnt > 0) throw new BizException("BOM 编码已存在");
        if (bom.getId() == null) {
            bomMapper.insert(bom);
        } else {
            bomMapper.updateById(bom);
            itemMapper.delete(new LambdaQueryWrapper<MdBomItem>().eq(MdBomItem::getBomId, bom.getId()));
        }
        for (MdBomItem it : items) {
            it.setId(null);
            it.setBomId(bom.getId());
            itemMapper.insert(it);
        }
    }

    public void delete(Long id) {
        bomMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<MdBomItem>().eq(MdBomItem::getBomId, id));
    }
}
