package org.dataanalysis.controller;

import org.dataanalysis.entity.HistoryRecord;
import org.dataanalysis.service.Sf6HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sf6")
public class Sf6HistoryController {
    
    @Autowired
    private Sf6HistoryService sf6HistoryService;
    
    /**
     * 获取所有sf6历史记录
     */
    @GetMapping("/history")
    public List<HistoryRecord> getAllHistory() {
        return sf6HistoryService.getAllRecords();
    }
    
    /**
     * 根据期号获取sf6记录
     */
    @GetMapping("/history/{period}")
    public HistoryRecord getHistoryByPeriod(@PathVariable int period) {
        return sf6HistoryService.getRecordByPeriod(period);
    }
    
    /**
     * 获取最近n条sf6记录
     */
    @GetMapping("/history/recent")
    public List<HistoryRecord> getRecentHistory(@RequestParam(defaultValue = "10") int limit) {
        return sf6HistoryService.getRecentRecords(limit);
    }
    
    /**
     * 获取sf6总胜率
     */
    @GetMapping("/stats/win-rate")
    public Map<String, Object> getWinRate() {
        return sf6HistoryService.getWinRateResult();
    }
    
    /**
     * 获取sf6最近n期胜率
     */
    @GetMapping("/stats/recent-win-rate")
    public Map<String, Object> getRecentWinRate(@RequestParam(defaultValue = "100") int count) {
        return sf6HistoryService.getRecentWinRateResult(count);
    }
    
    /**
     * 获取sf6结果分布分析
     */
    @GetMapping("/stats/result-analysis")
    public Map<String, Object> getResultAnalysis() {
        return sf6HistoryService.getResultsAnalysis();
    }
} 