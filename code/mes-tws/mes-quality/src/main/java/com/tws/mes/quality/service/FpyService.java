package com.tws.mes.quality.service;

import com.tws.mes.quality.mapper.QualityStatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** FPY 双口径统计：一次直通率（不含返修）与最终良率（含返修、剔除报废）并列展示 */
@Service
@RequiredArgsConstructor
public class FpyService {

    private final QualityStatMapper statMapper;

    /** 按工单的良率表（含汇总行） */
    public Map<String, Object> summary(Long woId) {
        List<Map<String, Object>> rows = statMapper.fpyByWo(woId);
        long finished = 0, firstPass = 0, scrapped = 0;
        for (Map<String, Object> r : rows) {
            finished += toLong(r.get("finished"));
            firstPass += toLong(r.get("firstPass"));
            scrapped += toLong(r.get("scrapped"));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("finished", finished);
        result.put("firstPass", firstPass);
        result.put("scrapped", scrapped);
        result.put("fpy", finished == 0 ? 0 : round(firstPass * 100.0 / finished));
        // 最终良率 = 完工 / (完工 + 报废)
        long denom = finished + scrapped;
        result.put("finalYield", denom == 0 ? 0 : round(finished * 100.0 / denom));
        return result;
    }

    public List<Map<String, Object>> daily() {
        List<Map<String, Object>> rows = statMapper.fpyDaily();
        for (Map<String, Object> r : rows) {
            long f = toLong(r.get("finished"));
            long fp = toLong(r.get("firstPass"));
            r.put("fpy", f == 0 ? 0 : round(fp * 100.0 / f));
        }
        return rows;
    }

    public List<Map<String, Object>> pareto(Long woId) {
        return statMapper.defectPareto(woId);
    }

    private long toLong(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
