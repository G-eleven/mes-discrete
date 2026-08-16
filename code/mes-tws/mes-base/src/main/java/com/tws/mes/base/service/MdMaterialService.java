package com.tws.mes.base.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tws.mes.base.entity.MdMaterial;
import com.tws.mes.base.mapper.MdMaterialMapper;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 物料主数据 Service —— 本项目 CRUD 的标准写法模板：
 * 分页查询（LambdaQueryWrapper 拼条件）+ 编码唯一校验 + 新增/修改/删除。
 */
@Service
@RequiredArgsConstructor
public class MdMaterialService {

    private final MdMaterialMapper mapper;

    public PageResult<MdMaterial> page(long page, long size, String keyword, String materialType) {
        LambdaQueryWrapper<MdMaterial> qw = new LambdaQueryWrapper<>();
        qw.and(StrUtil.isNotBlank(keyword), w -> w
                        .like(MdMaterial::getMaterialCode, keyword)
                        .or().like(MdMaterial::getMaterialName, keyword))
                .eq(StrUtil.isNotBlank(materialType), MdMaterial::getMaterialType, materialType)
                .orderByDesc(MdMaterial::getId);
        Page<MdMaterial> p = mapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public List<MdMaterial> list(String materialType) {
        return mapper.selectList(new LambdaQueryWrapper<MdMaterial>()
                .eq(StrUtil.isNotBlank(materialType), MdMaterial::getMaterialType, materialType)
                .orderByAsc(MdMaterial::getMaterialCode));
    }

    public void save(MdMaterial m) {
        checkUnique(m);
        if (m.getId() == null) {
            mapper.insert(m);
        } else {
            mapper.updateById(m);
        }
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private void checkUnique(MdMaterial m) {
        Long cnt = mapper.selectCount(new LambdaQueryWrapper<MdMaterial>()
                .eq(MdMaterial::getMaterialCode, m.getMaterialCode())
                .ne(m.getId() != null, MdMaterial::getId, m.getId()));
        if (cnt != null && cnt > 0) {
            throw new BizException("物料编码已存在: " + m.getMaterialCode());
        }
    }
}
