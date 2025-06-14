package org.dataanalysis.controller;

import org.dataanalysis.entity.HistoryRecord;
import org.dataanalysis.service.HistoryDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    
    @Autowired
    private HistoryDataService historyDataService;
    
    /**
     * 获取指定服务器的所有历史记录
     * @param serverName 服务器名称 (sf1, sf3, sf4, sf5, sf6, sf7)
     * @return 历史记录列表
     */
    @GetMapping("/server/{serverName}")
    public List<HistoryRecord> getHistoryByServer(@PathVariable String serverName) {
        return historyDataService.getHistoryByServer(serverName);
    }
    
    /**
     * 获取所有服务器的所有历史记录
     * @return 按服务器分组的历史记录
     */
    @GetMapping("/all")
    public Map<String, List<HistoryRecord>> getAllHistory() {
        return historyDataService.getAllHistory();
    }
    
    /**
     * 获取合并后的所有历史记录
     * @return 所有服务器的历史记录合并为一个列表
     */
    @GetMapping("/merged")
    public List<HistoryRecord> getAllHistoryMerged() {
        return historyDataService.getAllHistoryMerged();
    }
    
    /**
     * 根据期号获取所有服务器的数据
     * @param period 期号
     * @return 按服务器分组的该期号记录
     */
    @GetMapping("/period/{period}")
    public Map<String, HistoryRecord> getHistoryByPeriod(@PathVariable int period) {
        return historyDataService.getHistoryByPeriod(period);
    }
    
    /**
     * 获取每个服务器最近的n条记录
     * @param limit 记录数量限制
     * @return 按服务器分组的最近历史记录
     */
    @GetMapping("/recent")
    public Map<String, List<HistoryRecord>> getRecentHistory(@RequestParam(defaultValue = "10") int limit) {
        return historyDataService.getRecentHistory(limit);
    }
} 