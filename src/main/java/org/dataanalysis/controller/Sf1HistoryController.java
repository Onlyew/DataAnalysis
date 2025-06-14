package org.dataanalysis.controller;

import org.dataanalysis.entity.HistoryRecord;
import org.dataanalysis.service.Sf1HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sf1")
public class Sf1HistoryController {
    
    @Autowired
    private Sf1HistoryService sf1HistoryService;
    
    /**
     * 获取所有sf1历史记录
     */
    @GetMapping("/history")
    public List<HistoryRecord> getAllHistory() {
        return sf1HistoryService.getAllRecords();
    }
    
    /**
     * 根据期号获取sf1记录
     */
    @GetMapping("/history/{period}")
    public HistoryRecord getHistoryByPeriod(@PathVariable int period) {
        return sf1HistoryService.getRecordByPeriod(period);
    }
    
    /**
     * 获取最近n条sf1记录
     */
    @GetMapping("/history/recent")
    public List<HistoryRecord> getRecentHistory(@RequestParam(defaultValue = "10") int limit) {
        return sf1HistoryService.getRecentRecords(limit);
    }
    
    /**
     * 获取sf1总胜率
     */
    @GetMapping("/stats/win-rate")
    public Map<String, Object> getWinRate() {
        return sf1HistoryService.getWinRateResult();
    }
    
    /**
     * 获取sf1最近n期胜率
     */
    @GetMapping("/stats/recent-win-rate")
    public Map<String, Object> getRecentWinRate(@RequestParam(defaultValue = "100") int count) {
        return sf1HistoryService.getRecentWinRateResult(count);
    }
    
    /**
     * 获取sf1结果分布分析
     */
    @GetMapping("/stats/result-analysis")
    public Map<String, Object> getResultAnalysis() {
        return sf1HistoryService.getResultsAnalysis();
    }
    
    /**
     * 获取sf1综合胜率分析(包含预测胜率、实际胜率、杀号概率等)
     */
    @GetMapping("/stats/comprehensive")
    public Map<String, Object> getComprehensiveAnalysis() {
        return sf1HistoryService.getComprehensiveRateAnalysis();
    }
    
    /**
     * 获取sf1最近n期的综合胜率分析
     */
    @GetMapping("/stats/recent-comprehensive")
    public Map<String, Object> getRecentComprehensiveAnalysis(@RequestParam(defaultValue = "100") int count) {
        return sf1HistoryService.getRecentComprehensiveAnalysis(count);
    }
    
    /**
     * 获取sf1当前连续统计(连中、连错、连盈利、连亏损)
     */
    @GetMapping("/stats/consecutive")
    public Map<String, Object> getCurrentConsecutiveStats() {
        return sf1HistoryService.getCurrentConsecutiveStats();
    }
    
    /**
     * 获取sf1详细的结果分析(包含单双结果、杀号分布等)
     */
    @GetMapping("/stats/detailed-analysis")
    public Map<String, Object> getDetailedResultAnalysis() {
        return sf1HistoryService.getDetailedResultAnalysis();
    }
    
    /**
     * 获取sf1历史最高连续统计(历史最高连中、连错、连盈利、连亏损)
     */
    @GetMapping("/stats/historical-consecutive")
    public Map<String, Object> getHistoricalConsecutiveStats() {
        return sf1HistoryService.getHistoricalConsecutiveStats();
    }
}