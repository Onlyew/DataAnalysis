package org.dataanalysis.controller;

import org.dataanalysis.entity.Sf444HistoryRecord;
import org.dataanalysis.service.Sf444HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SF444历史数据控制器
 * 提供SF444服务器历史数据的API端点
 */
@RestController
@RequestMapping("/api/sf444")
public class Sf444HistoryController {

    @Autowired
    private Sf444HistoryService sf444HistoryService;

    /**
     * 获取所有历史记录
     * 按期数倒序排列
     */
    @GetMapping("/history")
    public List<Sf444HistoryRecord> getAllHistory() {
        List<Sf444HistoryRecord> records = sf444HistoryService.getAllRecords();
        // 按期数倒序排列
        return records.stream()
                .sorted(Comparator.comparing(Sf444HistoryRecord::getPeriod).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取最近N期历史记录
     */
    @GetMapping("/history/recent")
    public List<Sf444HistoryRecord> getRecentHistory(@RequestParam(defaultValue = "50") int limit) {
        return sf444HistoryService.getRecentRecords(limit);
    }

    /**
     * 根据期号获取历史记录
     */
    @GetMapping("/history/{period}")
    public Sf444HistoryRecord getHistoryByPeriod(@PathVariable int period) {
        return sf444HistoryService.getRecordByPeriod(period);
    }

    /**
     * 获取基本胜率统计
     */
    @GetMapping("/stats/win-rate")
    public Map<String, Object> getWinRateStats() {
        return sf444HistoryService.getWinRateResult();
    }

    /**
     * 获取最近N期胜率统计
     */
    @GetMapping("/stats/recent-win-rate")
    public Map<String, Object> getRecentWinRateStats(@RequestParam(defaultValue = "100") int count) {
        return sf444HistoryService.getRecentWinRateResult(count);
    }

    /**
     * 获取综合胜率分析
     */
    @GetMapping("/stats/comprehensive")
    public Map<String, Object> getComprehensiveStats() {
        return sf444HistoryService.getComprehensiveRateAnalysis();
    }

    /**
     * 获取最近N期综合分析
     */
    @GetMapping("/stats/recent-comprehensive")
    public Map<String, Object> getRecentComprehensiveStats(@RequestParam(defaultValue = "100") int count) {
        return sf444HistoryService.getRecentComprehensiveAnalysis(count);
    }

    /**
     * 获取当前连续统计
     */
    @GetMapping("/stats/consecutive")
    public Map<String, Object> getConsecutiveStats() {
        return sf444HistoryService.getCurrentConsecutiveStats();
    }

    /**
     * 获取历史最高连续统计
     */
    @GetMapping("/stats/historical-consecutive")
    public Map<String, Object> getHistoricalConsecutiveStats() {
        return sf444HistoryService.getHistoricalConsecutiveStats();
    }

    /**
     * 获取结果分布统计
     */
    @GetMapping("/stats/results")
    public Map<String, Object> getResultsStats() {
        return sf444HistoryService.getResultsAnalysis();
    }

    /**
     * 获取详细结果分析
     */
    @GetMapping("/stats/detailed-results")
    public Map<String, Object> getDetailedResultsStats() {
        return sf444HistoryService.getDetailedResultsAnalysis();
    }
}
