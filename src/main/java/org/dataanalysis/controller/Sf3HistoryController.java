package org.dataanalysis.controller;

import org.dataanalysis.entity.HistoryRecord;
import org.dataanalysis.service.Sf3HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sf3")
public class Sf3HistoryController {
    
    @Autowired
    private Sf3HistoryService sf3HistoryService;
    
    /**
     * 获取所有sf3历史记录
     */
    @GetMapping("/history")
    public List<HistoryRecord> getAllHistory() {
        return sf3HistoryService.getAllRecords();
    }
    
    /**
     * 根据期号获取sf3记录
     */
    @GetMapping("/history/{period}")
    public HistoryRecord getHistoryByPeriod(@PathVariable int period) {
        return sf3HistoryService.getRecordByPeriod(period);
    }
    
    /**
     * 获取最近n条sf3记录
     */
    @GetMapping("/history/recent")
    public List<HistoryRecord> getRecentHistory(@RequestParam(defaultValue = "10") int limit) {
        return sf3HistoryService.getRecentRecords(limit);
    }
    
    /**
     * 获取sf3总胜率
     */
    @GetMapping("/stats/win-rate")
    public Map<String, Object> getWinRate() {
        return sf3HistoryService.getWinRateResult();
    }
    
    /**
     * 获取sf3最近n期胜率
     */
    @GetMapping("/stats/recent-win-rate")
    public Map<String, Object> getRecentWinRate(@RequestParam(defaultValue = "100") int count) {
        return sf3HistoryService.getRecentWinRateResult(count);
    }
    
    /**
     * 获取sf3结果分布分析
     */
    @GetMapping("/stats/result-analysis")
    public Map<String, Object> getResultAnalysis() {
        return sf3HistoryService.getResultsAnalysis();
    }
} 