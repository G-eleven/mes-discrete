package com.tws.mes.base.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tws.mes.base.entity.MdMaterialBatch;
import com.tws.mes.base.service.MdMaterialBatchService;
import com.tws.mes.common.result.PageResult;
import com.tws.mes.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/material-batch")
@RequiredArgsConstructor
public class MdMaterialBatchController {

    private final MdMaterialBatchService service;

    @GetMapping("/page")
    public Result<PageResult<MdMaterialBatch>> page(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Long materialId) {
        return Result.ok(service.page(page, size, keyword, materialId));
    }

    @GetMapping("/list")
    public Result<List<MdMaterialBatch>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(service.list(keyword));
    }

    /** 批次登记（IQC/仓库），admin 与 quality 均可 */
    @SaCheckRole(value = {"admin", "quality"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @PostMapping("/save")
    public Result<Void> save(@RequestBody MdMaterialBatch b) {
        service.save(b);
        return Result.ok();
    }
}
