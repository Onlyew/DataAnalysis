package org.dataanalysis.controller;

import org.dataanalysis.entity.HistoryRecord;
import org.dataanalysis.service.Sf7HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sf7")
public class Sf7HistoryController {
    
    @Autowired
    private Sf7HistoryService sf7HistoryService;
    
    /**
     * 获取所有sf7历史记录
     */
    @GetMapping("/history")
    public List<HistoryRecord> getAllHistory() {
        return sf7HistoryService.getAllRecords();
    }
    
    /**
     * 根据期号获取sf7记录
     */
    @GetMapping("/history/{period}")
    public HistoryRecord getHistoryByPeriod(@PathVariable int period) {
        return sf7HistoryService.getRecordByPeriod(period);
    }
    
    /**
     * 获取最近n条sf7记录
     */
    @GetMapping("/history/recent")
    public List<HistoryRecord> getRecentHistory(@RequestParam(defaultValue = "10") int limit) {
        return sf7HistoryService.getRecentRecords(limit);
    }
    
    /**
     * 获取sf7总胜率
     */
    @GetMapping("/stats/win-rate")
    public Map<String, Object> getWinRate() {
        return sf7HistoryService.getWinRateResult();
    }
    
    /**
     * 获取sf7最近n期胜率
     */
    @GetMapping("/stats/recent-win-rate")
    public Map<String, Object> getRecentWinRate(@RequestParam(defaultValue = "100") int count) {
        return sf7HistoryService.getRecentWinRateResult(count);
    }
    
    /**
     * 获取sf7结果分布分析
     */
    @GetMapping("/stats/result-analysis")
    public Map<String, Object> getResultAnalysis() {
        return sf7HistoryService.getResultsAnalysis();
    }
} 