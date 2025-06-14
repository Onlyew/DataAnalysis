package org.dataanalysis.controller;

import org.dataanalysis.entity.HistoryRecord;
import org.dataanalysis.service.Sf5HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sf5")
public class Sf5HistoryController {
    
    @Autowired
    private Sf5HistoryService sf5HistoryService;
    
    /**
     * 获取所有sf5历史记录
     */
    @GetMapping("/history")
    public List<HistoryRecord> getAllHistory() {
        return sf5HistoryService.getAllRecords();
    }
    
    /**
     * 根据期号获取sf5记录
     */
    @GetMapping("/history/{period}")
    public HistoryRecord getHistoryByPeriod(@PathVariable int period) {
        return sf5HistoryService.getRecordByPeriod(period);
    }
    
    /**
     * 获取最近n条sf5记录
     */
    @GetMapping("/history/recent")
    public List<HistoryRecord> getRecentHistory(@RequestParam(defaultValue = "10") int limit) {
        return sf5HistoryService.getRecentRecords(limit);
    }
    
    /**
     * 获取sf5总胜率
     */
    @GetMapping("/stats/win-rate")
    public Map<String, Object> getWinRate() {
        return sf5HistoryService.getWinRateResult();
    }
    
    /**
     * 获取sf5最近n期胜率
     */
    @GetMapping("/stats/recent-win-rate")
    public Map<String, Object> getRecentWinRate(@RequestParam(defaultValue = "100") int count) {
        return sf5HistoryService.getRecentWinRateResult(count);
    }
    
    /**
     * 获取sf5结果分布分析
     */
    @GetMapping("/stats/result-analysis")
    public Map<String, Object> getResultAnalysis() {
        return sf5HistoryService.getResultsAnalysis();
    }
} 