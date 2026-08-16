package com.tws.mes.base.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tws.mes.base.entity.MdMaterial;
import com.tws.mes.base.service.MdMaterialService;
import com.tws.mes.common.result.PageResult;
import com.tws.mes.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 物料主数据接口 —— 其余基础数据 Controller 均参照此模板编写 */
@RestController
@RequestMapping("/api/material")
@RequiredArgsConstructor
public class MdMaterialController {

    private final MdMaterialService service;

    @GetMapping("/page")
    public Result<PageResult<MdMaterial>> page(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String materialType) {
        return Result.ok(service.page(page, size, keyword, materialType));
    }

    @GetMapping("/list")
    public Result<List<MdMaterial>> list(@RequestParam(required = false) String materialType) {
        return Result.ok(service.list(materialType));
    }

    /** 基础数据维护仅 admin 可操作（过站等业务接口另有限制） */
    @SaCheckRole("admin")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody MdMaterial m) {
        service.save(m);
        return Result.ok();
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
