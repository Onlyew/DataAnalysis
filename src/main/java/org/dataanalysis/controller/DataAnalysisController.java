package org.dataanalysis.controller;

import org.dataanalysis.entity.HistoryRecord;
import org.dataanalysis.service.Sf1HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据分析控制器
 * 提供数据分析相关的API端点
 */
@RestController
@RequestMapping("/api")
public class DataAnalysisController {

    @Autowired
    private Sf1HistoryService sf1HistoryService;
    
    /**
     * 获取SF1被杀数字数据
     */
    @GetMapping("/sf1/kill-number-data")
    public Map<String, Object> getSf1KillNumberData() {
        // 获取所有记录
        List<HistoryRecord> records = sf1HistoryService.getAllRecords();
        
        // 按期数倒序排列（最新的在前面）
        List<HistoryRecord> sortedRecords = records.stream()
                .sorted(Comparator.comparing(HistoryRecord::getPeriod).reversed())
                .collect(Collectors.toList());
        
        // 将期数和杀数提取为数组
        List<String> periods = new ArrayList<>();
        List<Integer> killCounts = new ArrayList<>();   // 杀数的个数
        List<Integer> nonKillCounts = new ArrayList<>(); // 未被杀数的个数
        
        // 计算总数据量和总杀数
        int totalRecords = sortedRecords.size();
        int totalKilled = 0;
        
        for (HistoryRecord record : sortedRecords) {
            // 只取期数的后三位
            periods.add(String.valueOf(record.getPeriod() % 1000));
            
            // 计算杀数的个数
            String killNumber = record.getKillNumber();
            int killCount = 0;
            if (killNumber != null && !killNumber.isEmpty()) {
                // 可能有多种分隔符，先尝试逗号分隔
                if (killNumber.contains(",")) {
                    killCount = killNumber.split(",").length;
                } 
                // 空格分隔
                else if (killNumber.contains(" ")) {
                    killCount = killNumber.trim().split("\\s+").length;
                }
                // 无分隔符，每个字符算一个数字
                else {
                    killCount = killNumber.length();
                }
                
                // 确保有数据显示，至少为1
                if (killCount == 0) killCount = 1;
            }
            
            totalKilled += killCount;
            killCounts.add(killCount);
            
            // 计算未被杀数的个数（假设总共有10个数字）
            nonKillCounts.add(10 - killCount);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("periods", periods);
        result.put("killCounts", killCounts);
        result.put("nonKillCounts", nonKillCounts);
        result.put("count", totalRecords);
        result.put("totalKilled", totalKilled);
        result.put("totalNonKilled", totalRecords * 10 - totalKilled); // 假设每期10个数
        
        return result;
    }

    /**
     * 获取SF1表期数和总和数据用于图表显示
     */
    @GetMapping("/sf1/total-number-data")
    public Map<String, Object> getSf1TotalNumberData() {
        // 获取所有记录
        List<HistoryRecord> records = sf1HistoryService.getAllRecords();
        
        // 按期数倒序排列（最新的在前面）
        List<HistoryRecord> sortedRecords = records.stream()
                .sorted(Comparator.comparing(HistoryRecord::getPeriod).reversed())
                .collect(Collectors.toList());
                
        // 移除第一条预测数据（最新的一条）如果存在
        if (!sortedRecords.isEmpty()) {
            sortedRecords.remove(0);
        }
        
        // 将期数和总和提取为两个数组，方便前端使用
        List<String> periods = sortedRecords.stream()
                .map(record -> String.valueOf(record.getPeriod() % 1000)) // 只取后三位
                .collect(Collectors.toList());
                
        List<Integer> totalNumbers = sortedRecords.stream()
                .map(HistoryRecord::getTotalNumber)
                .collect(Collectors.toList());
                
        Map<String, Object> result = new HashMap<>();
        result.put("periods", periods);
        result.put("totalNumbers", totalNumbers);
        result.put("count", records.size());
        
        return result;
    }

    /**
     * 获取最近N期的被杀率数据
     * @return 包含30场到50场、100场等不同场次的被杀率
     */
    @GetMapping("/sf1/recent-kill-rates")
    public Map<String, Object> getRecentKillRates() {
        // 获取所有记录
        List<HistoryRecord> records = sf1HistoryService.getAllRecords();
        
        // 按期数倒序排列（最新的在前面）
        List<HistoryRecord> sortedRecords = records.stream()
                .sorted(Comparator.comparing(HistoryRecord::getPeriod).reversed())
                .collect(Collectors.toList());
        
        // 移除第一条预测数据（最新的一条）如果存在
        if (!sortedRecords.isEmpty()) {
            sortedRecords.remove(0);
        }
        
        Map<String, Object> result = new HashMap<>();
        
        // 计算不同场次的被杀率 - 从30场到650场，每50场为一个间隔
        // 先计算30、50场
        result.put("killRate30", String.format("%.2f%%", getKillRateForLastNPeriods(sortedRecords, 30)));
        result.put("killRate50", String.format("%.2f%%", getKillRateForLastNPeriods(sortedRecords, 50)));
        
        // 计算100到1000场，每50场一个间隔
        for (int period = 100; period <= 1000; period += 50) {
            result.put("killRate" + period, String.format("%.2f%%", getKillRateForLastNPeriods(sortedRecords, period)));
        }
        
        return result;
    }
    
    /**
     * 获取最近N期内的被杀率
     * @param records 按期数降序排序的记录
     * @param periodCount 期数，如100、50、30
     * @return 被杀率（百分比）
     */
    private double getKillRateForLastNPeriods(List<HistoryRecord> records, int periodCount) {
        // 只取最近N期，如果记录不足N期，则取全部
        int actualCount = Math.min(periodCount, records.size());
        List<HistoryRecord> recentRecords = records.stream()
                .limit(actualCount)
                .collect(Collectors.toList());
        
        // 统计有杀号的期数
        long periodsWithKills = recentRecords.stream()
                .filter(record -> record.getKillNumber() != null && !record.getKillNumber().isEmpty())
                .count();
        
        // 计算被杀率
        return (double) periodsWithKills / actualCount * 100;
    }

    /**
     * 获取SF1最大连杀的统计数据
     * 返回历史上最大的连杀次数和对应的期数范围
     * 支持查询所有记录或特定期数范围内的最大连杀
     */
    @GetMapping("/sf1/max-consecutive-kills")
    public Map<String, Object> getMaxConsecutiveKills(@RequestParam(required = false) Integer periodRange) {
        // 获取所有记录
        List<HistoryRecord> records = sf1HistoryService.getAllRecords();
        
        // 按期数倒序排列（最新的在前面）
        List<HistoryRecord> sortedRecords = records.stream()
                .sorted(Comparator.comparing(HistoryRecord::getPeriod).reversed())
                .collect(Collectors.toList());
        
        // 移除第一条预测数据（最新的一条）如果存在
        if (!sortedRecords.isEmpty()) {
            sortedRecords.remove(0);
        }
        
        // 如果指定了期数范围，只取最近N期
        if (periodRange != null && periodRange > 0) {
            int actualCount = Math.min(periodRange, sortedRecords.size());
            sortedRecords = sortedRecords.subList(0, actualCount);
        }
        
        // 计算最大连杀次数
        int maxConsecutiveKills = 0;
        String maxStartPeriod = "";
        String maxEndPeriod = "";
        
        int currentKillCount = 0;
        String currentStartPeriod = "";
        String currentEndPeriod = "";
        
        // 倒序遍历记录，统计连续杀号
        for (HistoryRecord record : sortedRecords) {
            // 如果有杀号
            if (record.getKillNumber() != null && !record.getKillNumber().isEmpty()) {
                // 连续杀号计数增加
                if (currentKillCount == 0) {
                    currentStartPeriod = record.getPeriod().toString();
                }
                currentEndPeriod = record.getPeriod().toString();
                currentKillCount++;
            } else {
                // 重置连续计数
                if (currentKillCount > maxConsecutiveKills) {
                    maxConsecutiveKills = currentKillCount;
                    maxStartPeriod = currentStartPeriod;
                    maxEndPeriod = currentEndPeriod;
                }
                currentKillCount = 0;
            }
        }
        
        // 最后检查一次（如果最后一个连续杀号序列正好是数据的最后部分）
        if (currentKillCount > maxConsecutiveKills) {
            maxConsecutiveKills = currentKillCount;
            maxStartPeriod = currentStartPeriod;
            maxEndPeriod = currentEndPeriod;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("maxConsecutiveKills", maxConsecutiveKills);
        result.put("startPeriod", maxStartPeriod);
        result.put("endPeriod", maxEndPeriod);
        
        return result;
    }
    
    /**
     * 获取多个期数范围内的最大连杀数据
     * 返回100期、200期、300期、400期、500期等范围内的最大连杀统计
     */
    @GetMapping("/sf1/multi-range-max-kills")
    public Map<String, Object> getMultiRangeMaxKills() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有记录
        List<HistoryRecord> records = sf1HistoryService.getAllRecords();
        
        // 记录总数
        System.out.println("Total records: " + records.size());
        
        // 按期数倒序排列（最新的在前面）
        // 问题发现：之前的过滤条件过滤掉了所有记录，可能数据结构或字段变更了
        // 先不过滤，只排序，确保能获取到记录
        List<HistoryRecord> sortedRecords = records.stream()
                .sorted(Comparator.comparing(HistoryRecord::getPeriod).reversed())
                .collect(Collectors.toList());
        
        // 检查数据中是否有prediction字段，以便调试
        if (!sortedRecords.isEmpty()) {
            HistoryRecord firstRecord = sortedRecords.get(0);
            System.out.println("数据示例: " + firstRecord.getPeriod() + ", 是否有预测: " + 
                               (firstRecord.getPrediction() != null ? "是, 值=" + firstRecord.getPrediction() : "否"));
        }
        
        System.out.println("Sorted records (without predictions): " + sortedRecords.size());
        
        // 验证数据
        System.out.println("\n=== 验证数据记录 ===");
        for (int i = 0; i < Math.min(20, sortedRecords.size()); i++) {
            HistoryRecord record = sortedRecords.get(i);
            System.out.println(i + ": Period=" + record.getPeriod() + ", KillNumber=" + record.getKillNumber());
        }
        
        // 获取最新期数
        Integer latestPeriod = null;
        if (!sortedRecords.isEmpty()) {
            latestPeriod = sortedRecords.get(0).getPeriod();
            System.out.println("Latest period: " + latestPeriod);
        }
        
        // 计算整体最大连杀
        int maxConsecutiveKills = 0;
        int currentKillCount = 0;
        Integer maxKillStartPeriod = null;
        Integer maxKillEndPeriod = null;
        Integer currentKillStartPeriod = null;
        
        System.out.println("\n=== 计算整体最大连杀 ===");
        StringBuilder killPattern = new StringBuilder();
        
        // 遍历所有记录计算整体最大连杀
        for (int i = 0; i < sortedRecords.size(); i++) {
            HistoryRecord record = sortedRecords.get(i);
            boolean hasKill = record.getKillNumber() != null && !record.getKillNumber().isEmpty();
            
            killPattern.append(hasKill ? "K" : ".");
            
            if (hasKill) {
                if (currentKillCount == 0) {
                    currentKillStartPeriod = record.getPeriod();
                }
                currentKillCount++;
            } else {
                if (currentKillCount > maxConsecutiveKills) {
                    maxConsecutiveKills = currentKillCount;
                    maxKillStartPeriod = currentKillStartPeriod;
                    maxKillEndPeriod = sortedRecords.get(i-1).getPeriod();
                    System.out.println("Found new max kill streak: " + maxConsecutiveKills + 
                                    ", from " + maxKillStartPeriod + " to " + maxKillEndPeriod);
                }
                currentKillCount = 0;
            }
        }
        
        // 检查最后一次连杀
        if (currentKillCount > maxConsecutiveKills) {
            maxConsecutiveKills = currentKillCount;
            maxKillStartPeriod = currentKillStartPeriod;
            maxKillEndPeriod = sortedRecords.get(sortedRecords.size()-1).getPeriod();
            System.out.println("Found new max kill streak at end: " + maxConsecutiveKills + 
                             ", from " + maxKillStartPeriod + " to " + maxKillEndPeriod);
        }
        
        System.out.println("Kill pattern: " + killPattern.toString());
        System.out.println("Overall max consecutive kills: " + maxConsecutiveKills);
        
        // 设置总体结果
        result.put("maxConsecutiveKills", maxConsecutiveKills);
        result.put("startPeriod", maxKillStartPeriod);
        result.put("endPeriod", maxKillEndPeriod);
        
        // 计算差值
        if (latestPeriod != null && maxKillEndPeriod != null) {
            int periodDiff = latestPeriod - maxKillEndPeriod;
            // 先不设置这个差值，留给后面的getMaxConsecutiveKills来计算
            // result.put("periodDiff", periodDiff);
        }
        
        // 总体最大连杀 - 使用原始方法确保兼容性
        Map<String, Object> allTimeData = getMaxConsecutiveKills(null);
        result.put("maxConsecutiveKills", allTimeData.get("maxConsecutiveKills"));
        result.put("startPeriod", allTimeData.get("startPeriod"));
        result.put("endPeriod", allTimeData.get("endPeriod"));
        
        // 计算历史最大连杀与最新期数的差值 - 最新期数减去连杀区间最后一期的期数
        if (latestPeriod != null && allTimeData.get("endPeriod") != null) {
            try {
                // 修正计算逻辑，按照用户要求处理
                // 1. 在我们的业务中，期数区间呈现为“3281313 至 3281305”这样的形式
                // 2. 新的差值计算逻辑：最新期数 - 区间右边的期数(即较小的期数)
                
                // 从返回数据中获取连杀区间的开始和结束期数
                int endPeriod = Integer.parseInt(allTimeData.get("endPeriod").toString());
                int startPeriod = Integer.parseInt(allTimeData.get("startPeriod").toString());
                
                // 识别哪个是区间右边的期数(较小的期数) - 一般来说就是endPeriod
                int smallerPeriod = Math.min(startPeriod, endPeriod); // 这就是连杀区间右边的期数
                
                // 正确的计算方式: 最新期数 - 连杀区间右边的期数
                int periodDiff = latestPeriod - smallerPeriod;
                
                System.out.println("全局期数差值计算: 最新期数" + latestPeriod + " - 连杀区间右边期数" + smallerPeriod + " = " + periodDiff);
                System.out.println("连杀区间: " + startPeriod + " 至 " + endPeriod + ", 计算差值使用右边期数: " + smallerPeriod);
                // 强制添加期数差值，确保前端能正确显示
                if (periodDiff >= 0) {
                    result.put("periodDiff", periodDiff);
                } else {
                    System.out.println("计算出的期数差值小于0，可能有计算错误，使用默认值");
                    result.put("periodDiff", 0); // 使用默认值而不是null，确保前端能显示
                }
            } catch (NumberFormatException e) {
                System.out.println("期数差值计算失败: " + e.getMessage());
                // 强制添加默认值，确保前端显示
                result.put("periodDiff", 0);
            }
        } else {
            // 如果最新期数或结束期数为空，还是添加默认值
            System.out.println("最新期数或结束期数为空，使用默认差值");
            result.put("periodDiff", 0);
        }
        
        // 处理不同范围(1-100, 101-200等) - 修改排除条件后，确保有数据可用
        System.out.println("\n=== 处理各个区间 ===");
        int[] ranges = {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
        
        for (int i = 0; i < ranges.length; i++) {
            int range = ranges[i];
            int startIdx = (i == 0) ? 0 : ranges[i-1];
            int endIdx = Math.min(range, sortedRecords.size()); // 防止越界
            
            // 即使数据不足也尝试处理可用的部分
            if (startIdx >= sortedRecords.size()) {
                System.out.println("Range " + (startIdx+1) + "-" + range + ": Not enough data");
                result.put("range" + range, 0);
                continue;
            }
            
            // 实际可用的范围
            System.out.println("Range " + (startIdx+1) + "-" + endIdx + ": Processing " + (endIdx - startIdx) + " records");
            
            // 这里已经不需要再检查边界了，因为前面已经检查过
            // 这里直接处理区间内的数据
            
            // 取出区间内的记录
            List<HistoryRecord> rangeRecords = sortedRecords.subList(startIdx, endIdx);
            System.out.println("Range " + (startIdx+1) + "-" + endIdx + ": Processing " + rangeRecords.size() + " records");
            
            // 计算该区间的最大连杀
            int rangeMaxKills = 0;
            int rangeCurrentKills = 0;
            Integer rangeMaxStartPeriod = null;
            Integer rangeMaxEndPeriod = null;
            Integer rangeCurrentStartPeriod = null;
            
            // 全新的连杀判断逻辑
            StringBuilder rangeKillPattern = new StringBuilder();
            
            for (int j = 0; j < rangeRecords.size(); j++) {
                HistoryRecord record = rangeRecords.get(j);
                boolean hasKill = record.getKillNumber() != null && !record.getKillNumber().isEmpty();
                
                rangeKillPattern.append(hasKill ? "K" : ".");
                
                if (hasKill) {
                    if (rangeCurrentKills == 0) {
                        rangeCurrentStartPeriod = record.getPeriod();
                    }
                    rangeCurrentKills++;
                } else {
                    if (rangeCurrentKills > rangeMaxKills) {
                        rangeMaxKills = rangeCurrentKills;
                        rangeMaxStartPeriod = rangeCurrentStartPeriod;
                        rangeMaxEndPeriod = rangeRecords.get(j-1).getPeriod();
                    }
                    rangeCurrentKills = 0;
                }
            }
            
            // 检查最后一次连杀
            if (rangeCurrentKills > rangeMaxKills) {
                rangeMaxKills = rangeCurrentKills;
                rangeMaxStartPeriod = rangeCurrentStartPeriod;
                rangeMaxEndPeriod = rangeRecords.get(rangeRecords.size()-1).getPeriod();
            }
            
            System.out.println("Range " + (startIdx+1) + "-" + endIdx + ": Max Kills = " + rangeMaxKills + 
                           ", Pattern = " + rangeKillPattern.toString());
            
            result.put("range" + range, rangeMaxKills);
            result.put("range" + range + "Start", rangeMaxStartPeriod);
            result.put("range" + range + "End", rangeMaxEndPeriod);
            
            // 计算与最新期数的差值
            if (latestPeriod != null && rangeMaxEndPeriod != null) {
                int periodDiff = latestPeriod - rangeMaxEndPeriod;
                result.put("range" + range + "Diff", periodDiff);
            }
        }
        
        return result;
    }
    
    /**
     * 获取所有至少5连杀以上的历史记录
     * 返回各连杀区间及其与当前距离
     */
    @GetMapping("/sf1/kill-streaks-history")
    public Map<String, Object> getKillStreaksHistory(@RequestParam(required = false, defaultValue = "5") int minStreak) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有记录
        List<HistoryRecord> records = sf1HistoryService.getAllRecords();
        
        // 记录总数
        System.out.println("\n===== 获取连杀历史记录 =====");
        System.out.println("Total records: " + records.size());
        
        // 按期数倒序排列（最新的在前面）
        List<HistoryRecord> sortedRecords = records.stream()
                .sorted(Comparator.comparing(HistoryRecord::getPeriod).reversed())
                .collect(Collectors.toList());
        
        System.out.println("Sorted records: " + sortedRecords.size());
        
        // 获取最新期数，用于计算差值
        Integer latestPeriod = null;
        if (!sortedRecords.isEmpty()) {
            latestPeriod = sortedRecords.get(0).getPeriod();
            System.out.println("Latest period: " + latestPeriod);
        }
        
        // 用于收集所有连杀记录
        List<Map<String, Object>> streaksList = new ArrayList<>();
        
        // 统计连杀情况
        int currentKillCount = 0;
        Integer currentStartPeriod = null;
        Integer currentEndPeriod = null;
        
        // 遍历记录查找连杀
        for (int i = 0; i < sortedRecords.size(); i++) {
            HistoryRecord record = sortedRecords.get(i);
            boolean hasKill = record.getKillNumber() != null && !record.getKillNumber().isEmpty();
            
            if (hasKill) {
                // 如果是新的连杀开始
                if (currentKillCount == 0) {
                    currentStartPeriod = record.getPeriod();
                }
                currentEndPeriod = record.getPeriod();
                currentKillCount++;
            } else {
                // 连杀结束，检查是否达到最小连杀数
                if (currentKillCount >= minStreak) {
                    // 收集连杀记录
                    Map<String, Object> streakInfo = new HashMap<>();
                    streakInfo.put("streakCount", currentKillCount);
                    streakInfo.put("startPeriod", currentStartPeriod);
                    streakInfo.put("endPeriod", currentEndPeriod);
                    
                    // 计算与最新期数的差值 - 最新期数减去连杀区间右边的期数
                    if (latestPeriod != null && currentEndPeriod != null) {
                        // 识别哪个是区间右边的期数(较小的期数)
                        int smallerPeriod = Math.min(currentStartPeriod, currentEndPeriod);
                        int periodDiff = latestPeriod - smallerPeriod;
                        streakInfo.put("periodDiff", periodDiff);
                    }
                    
                    streaksList.add(streakInfo);
                }
                currentKillCount = 0;
            }
        }
        
        // 最后检查一次（如果最后一个连续杀号序列正好是数据的最后部分）
        if (currentKillCount >= minStreak) {
            Map<String, Object> streakInfo = new HashMap<>();
            streakInfo.put("streakCount", currentKillCount);
            streakInfo.put("startPeriod", currentStartPeriod);
            streakInfo.put("endPeriod", currentEndPeriod);
            
            // 计算与最新期数的差值
            if (latestPeriod != null && currentEndPeriod != null) {
                int smallerPeriod = Math.min(currentStartPeriod, currentEndPeriod);
                int periodDiff = latestPeriod - smallerPeriod;
                streakInfo.put("periodDiff", periodDiff);
            }
            
            streaksList.add(streakInfo);
        }
        
        // 按连杀数从高到低排序
        streaksList.sort((a, b) -> {
            Integer streakA = (Integer) a.get("streakCount");
            Integer streakB = (Integer) b.get("streakCount");
            return streakB.compareTo(streakA);
        });
        
        result.put("streaks", streaksList);
        result.put("count", streaksList.size());
        result.put("minStreak", minStreak);
        
        return result;
    }
    
    /**
     * 计算指定记录集内的最大连续杀率
     * @param records 要计算的记录集（已按期数倒序排列）
     * @return 包含最大连杀数和对应期数区间的Map
     */
    private Map<String, Object> calculateMaxConsecutiveKills(List<HistoryRecord> records) {
        Map<String, Object> result = new HashMap<>();
        if (records == null || records.isEmpty()) {
            result.put("maxConsecutiveKills", 0);
            return result;
        }
        
        System.out.println("\n\n*** 计算区间连杀，记录数量: " + records.size() + " ***");
        if (records.size() > 0) {
            System.out.println("First period: " + records.get(0).getPeriod() + ", Last period: " + records.get(records.size()-1).getPeriod());
        }
        
        int maxConsecutiveKills = 0;
        int currentKillCount = 0;
        int maxStartIndex = -1;
        int maxEndIndex = -1;
        int currentStartIndex = -1;
        
        StringBuilder killSequence = new StringBuilder();
        
        for (int i = 0; i < records.size(); i++) {
            HistoryRecord record = records.get(i);
            
            // 判断是否是连杀期 - 有杀号记录为连杀
            boolean isConsecutiveKill = record.getKillNumber() != null && !record.getKillNumber().isEmpty();
            
            // 添加到连杀序列可视化
            killSequence.append(isConsecutiveKill ? "K" : ".");
            
            if (isConsecutiveKill) {
                if (currentKillCount == 0) {
                    currentStartIndex = i; // 记录当前连杀开始索引
                }
                currentKillCount++;
            } else {
                if (currentKillCount > maxConsecutiveKills) {
                    maxConsecutiveKills = currentKillCount;
                    maxStartIndex = currentStartIndex;
                    maxEndIndex = i - 1;
                    System.out.println("Found new max kill streak: " + maxConsecutiveKills + 
                                     ", from period " + records.get(maxStartIndex).getPeriod() + 
                                     " to " + records.get(maxEndIndex).getPeriod());
                }
                currentKillCount = 0;
            }
        }
        
        // 检查最后一次连杀
        if (currentKillCount > maxConsecutiveKills) {
            maxConsecutiveKills = currentKillCount;
            maxStartIndex = currentStartIndex;
            maxEndIndex = records.size() - 1;
            System.out.println("Found new max kill streak at end: " + maxConsecutiveKills + 
                             ", from period " + records.get(maxStartIndex).getPeriod() + 
                             " to " + records.get(maxEndIndex).getPeriod());
        }
        
        System.out.println("Kill sequence: " + killSequence.toString());
        System.out.println("Max consecutive kills: " + maxConsecutiveKills);
        
        result.put("maxConsecutiveKills", maxConsecutiveKills);
        
        // 记录对应的期数区间
        if (maxStartIndex >= 0 && maxEndIndex >= 0) {
            result.put("startPeriod", records.get(maxStartIndex).getPeriod());
            result.put("endPeriod", records.get(maxEndIndex).getPeriod());
            System.out.println("Result - Max kills: " + maxConsecutiveKills + 
                             ", Start: " + records.get(maxStartIndex).getPeriod() + 
                             ", End: " + records.get(maxEndIndex).getPeriod());
        } else {
            System.out.println("No valid kill streak found");
        }
        
        return result;
    }
}
