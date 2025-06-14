package org.dataanalysis.controller;

import org.dataanalysis.service.Sf1HistoryService;
import org.dataanalysis.service.Sf3HistoryService;
import org.dataanalysis.service.Sf4HistoryService;
import org.dataanalysis.service.Sf5HistoryService;
import org.dataanalysis.service.Sf6HistoryService;
import org.dataanalysis.service.Sf7HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计数据控制器 - 整合多个服务器的统计信息
 */
@RestController
@RequestMapping("/api/stats")
public class HistoryStatsController {
    
    @Autowired
    private Sf1HistoryService sf1HistoryService;
    
    @Autowired
    private Sf3HistoryService sf3HistoryService;
    
    @Autowired
    private Sf4HistoryService sf4HistoryService;
    
    @Autowired
    private Sf5HistoryService sf5HistoryService;
    
    @Autowired
    private Sf6HistoryService sf6HistoryService;
    
    @Autowired
    private Sf7HistoryService sf7HistoryService;
    
    /**
     * 获取所有服务器的胜率统计
     */
    @GetMapping("/win-rates")
    public List<Map<String, Object>> getAllWinRates() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 添加sf1胜率
        result.add(sf1HistoryService.getWinRateResult());
        
        // 添加sf3胜率
        result.add(sf3HistoryService.getWinRateResult());
        
        // 添加sf4胜率
        result.add(sf4HistoryService.getWinRateResult());
        
        // 添加sf5胜率
        result.add(sf5HistoryService.getWinRateResult());
        
        // 添加sf6胜率
        result.add(sf6HistoryService.getWinRateResult());
        
        // 添加sf7胜率
        result.add(sf7HistoryService.getWinRateResult());
        
        return result;
    }
    
    /**
     * 获取所有服务器的最近n期胜率统计
     */
    @GetMapping("/recent-win-rates")
    public List<Map<String, Object>> getAllRecentWinRates(@RequestParam(defaultValue = "100") int count) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 添加sf1最近胜率
        result.add(sf1HistoryService.getRecentWinRateResult(count));
        
        // 添加sf3最近胜率
        result.add(sf3HistoryService.getRecentWinRateResult(count));
        
        // 添加sf4最近胜率
        result.add(sf4HistoryService.getRecentWinRateResult(count));
        
        // 添加sf5最近胜率
        result.add(sf5HistoryService.getRecentWinRateResult(count));
        
        // 添加sf6最近胜率
        result.add(sf6HistoryService.getRecentWinRateResult(count));
        
        // 添加sf7最近胜率
        result.add(sf7HistoryService.getRecentWinRateResult(count));
        
        return result;
    }
    
    /**
     * 获取所有服务器的胜率对比
     */
    @GetMapping("/win-rate-comparison")
    public Map<String, Object> getWinRateComparison() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> serverRates = new HashMap<>();
        
        // 获取各服务器胜率
        Map<String, Object> sf1Stats = sf1HistoryService.getWinRateResult();
        Map<String, Object> sf3Stats = sf3HistoryService.getWinRateResult();
        Map<String, Object> sf4Stats = sf4HistoryService.getWinRateResult();
        Map<String, Object> sf5Stats = sf5HistoryService.getWinRateResult();
        Map<String, Object> sf6Stats = sf6HistoryService.getWinRateResult();
        Map<String, Object> sf7Stats = sf7HistoryService.getWinRateResult();
        
        // 提取胜率并存入对比表
        serverRates.put("sf1", sf1Stats.get("winRate"));
        serverRates.put("sf3", sf3Stats.get("winRate"));
        serverRates.put("sf4", sf4Stats.get("winRate"));
        serverRates.put("sf5", sf5Stats.get("winRate"));
        serverRates.put("sf6", sf6Stats.get("winRate"));
        serverRates.put("sf7", sf7Stats.get("winRate"));
        
        // 找出最高胜率服务器
        String bestServer = "sf1";
        String bestRate = (String) sf1Stats.get("winRate");
        
        if (bestRate.compareTo((String) sf3Stats.get("winRate")) < 0) {
            bestServer = "sf3";
            bestRate = (String) sf3Stats.get("winRate");
        }
        
        if (bestRate.compareTo((String) sf4Stats.get("winRate")) < 0) {
            bestServer = "sf4";
            bestRate = (String) sf4Stats.get("winRate");
        }
        
        if (bestRate.compareTo((String) sf5Stats.get("winRate")) < 0) {
            bestServer = "sf5";
            bestRate = (String) sf5Stats.get("winRate");
        }
        
        if (bestRate.compareTo((String) sf6Stats.get("winRate")) < 0) {
            bestServer = "sf6";
            bestRate = (String) sf6Stats.get("winRate");
        }
        
        if (bestRate.compareTo((String) sf7Stats.get("winRate")) < 0) {
            bestServer = "sf7";
            bestRate = (String) sf7Stats.get("winRate");
        }
        
        // 构建结果
        result.put("serverRates", serverRates);
        result.put("bestServer", bestServer);
        result.put("bestRate", bestRate);
        
        return result;
    }
} 