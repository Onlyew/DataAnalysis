package org.dataanalysis.controller;

import org.dataanalysis.entity.HistoryRecord;
import org.dataanalysis.service.Sf4HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sf4")
public class Sf4HistoryController {
    
    @Autowired
    private Sf4HistoryService sf4HistoryService;
    
    /**
     * 获取所有sf4历史记录
     */
    @GetMapping("/history")
    public List<HistoryRecord> getAllHistory() {
        return sf4HistoryService.getAllRecords();
    }
    
    /**
     * 根据期号获取sf4记录
     */
    @GetMapping("/history/{period}")
    public HistoryRecord getHistoryByPeriod(@PathVariable int period) {
        return sf4HistoryService.getRecordByPeriod(period);
    }
    
    /**
     * 获取最近n条sf4记录
     */
    @GetMapping("/history/recent")
    public List<HistoryRecord> getRecentHistory(@RequestParam(defaultValue = "10") int limit) {
        return sf4HistoryService.getRecentRecords(limit);
    }
    
    /**
     * 获取sf4总胜率
     */
    @GetMapping("/stats/win-rate")
    public Map<String, Object> getWinRate() {
        return sf4HistoryService.getWinRateResult();
    }
    
    /**
     * 获取sf4最近n期胜率
     */
    @GetMapping("/stats/recent-win-rate")
    public Map<String, Object> getRecentWinRate(@RequestParam(defaultValue = "100") int count) {
        return sf4HistoryService.getRecentWinRateResult(count);
    }
    
    /**
     * 获取sf4结果分布分析
     */
    @GetMapping("/stats/result-analysis")
    public Map<String, Object> getResultAnalysis() {
        return sf4HistoryService.getResultsAnalysis();
    }
} 