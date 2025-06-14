package org.dataanalysis.util;

import org.dataanalysis.entity.HistoryRecord;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 胜率计算工具类，提供通用的胜率计算和数据分析方法
 */
@Component
@Slf4j
public class WinRateCalculator {
    
    /**
     * 计算基本胜率
     * 
     * @param serverName 服务器名称
     * @param total 总记录数
     * @param correct 正确预测数
     * @param incorrect 错误预测数
     * @return 胜率统计结果
     */
    public Map<String, Object> calculateWinRate(String serverName, int total, int correct, int incorrect) {
        double winRate = total > 0 ? (double) correct / total * 100 : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("serverName", serverName);
        result.put("totalCount", total);
        result.put("correctCount", correct);
        result.put("incorrectCount", incorrect);
        result.put("winRate", String.format("%.2f%%", winRate));
        
        return result;
    }
    
    /**
     * 计算最近N期的胜率
     * 
     * @param serverName 服务器名称
     * @param recentRecords 最近N期记录
     * @param recentCount 记录数量
     * @return 最近胜率统计结果
     */
    public Map<String, Object> calculateRecentWinRate(String serverName, List<HistoryRecord> recentRecords, int recentCount) {
        int total = recentRecords.size();
        
        long correct = recentRecords.stream()
                .filter(record -> "中".equals(record.getOutcome()))
                .count();
        
        long incorrect = recentRecords.stream()
                .filter(record -> "错".equals(record.getOutcome()))
                .count();
        
        double winRate = total > 0 ? (double) correct / total * 100 : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("serverName", serverName);
        result.put("recentCount", recentCount);
        result.put("totalCount", total);
        result.put("correctCount", correct);
        result.put("incorrectCount", incorrect);
        result.put("winRate", String.format("%.2f%%", winRate));
        
        return result;
    }
    
    /**
     * 分析历史记录结果分布
     * 
     * @param serverName 服务器名称
     * @param allRecords 所有历史记录
     * @return 结果分析统计
     */
    public Map<String, Object> analyzeResults(String serverName, List<HistoryRecord> allRecords) {
        // 统计单双分布
        long singleCount = allRecords.stream()
                .filter(record -> "单".equals(record.getOpenResult()))
                .count();
        
        long doubleCount = allRecords.stream()
                .filter(record -> "双".equals(record.getOpenResult()))
                .count();
        
        // 统计总和分布
        Map<Integer, Integer> totalNumberDistribution = new HashMap<>();
        allRecords.forEach(record -> {
            Integer totalNumber = record.getTotalNumber();
            if (totalNumber != null) {
                totalNumberDistribution.put(totalNumber, 
                        totalNumberDistribution.getOrDefault(totalNumber, 0) + 1);
            }
        });
        
        Map<String, Object> result = new HashMap<>();
        result.put("serverName", serverName);
        result.put("totalRecords", allRecords.size());
        result.put("singleCount", singleCount);
        result.put("doubleCount", doubleCount);
        result.put("singleRate", String.format("%.2f%%", 
                allRecords.size() > 0 ? (double) singleCount / allRecords.size() * 100 : 0));
        result.put("doubleRate", String.format("%.2f%%", 
                allRecords.size() > 0 ? (double) doubleCount / allRecords.size() * 100 : 0));
        result.put("totalNumberDistribution", totalNumberDistribution);
        
        return result;
    }
    
    /**
     * 综合胜率分析 - 包含各种胜率指标和概率统计
     * 
     * @param serverName 服务器名称
     * @param records 历史记录列表
     * @return 综合胜率分析结果
     */
    public Map<String, Object> comprehensiveRateAnalysis(String serverName, List<HistoryRecord> records) {
        if (records == null || records.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("serverName", serverName);
            emptyResult.put("error", "无有效记录数据");
            return emptyResult;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("serverName", serverName);
        result.put("totalRecords", records.size());
        
        // 基础计数
        int totalCount = records.size();
        
        // 预测正确计数
        long correctCount = records.stream()
                .filter(record -> "中".equals(record.getOutcome()))
                .count();
        
        // 预测错误计数
        long incorrectCount = records.stream()
                .filter(record -> "错".equals(record.getOutcome()))
                .count();
        
        // 被杀记录计数
        long killedCount = records.stream()
                .filter(record -> record.getKillNumber() != null && !record.getKillNumber().isEmpty())
                .count();
        
        // 预测正确但被杀的计数
        long correctButKilledCount = records.stream()
                .filter(record -> "中".equals(record.getOutcome()) 
                        && record.getKillNumber() != null 
                        && !record.getKillNumber().isEmpty())
                .count();
        
        // 命中盈利计数
        long hitProfitCount = records.stream()
                .filter(record -> "命中盈利".equals(record.getBettingResult()))
                .count();
        
        // 未命中盈利计数
        long missButProfitCount = records.stream()
                .filter(record -> "未命中盈利".equals(record.getBettingResult()))
                .count();
        
        // 命中被杀计数
        long hitButKilledCount = records.stream()
                .filter(record -> "命中被杀".equals(record.getBettingResult()))
                .count();
        
        // 未命中亏损计数
        long missAndLossCount = records.stream()
                .filter(record -> "未命中亏损".equals(record.getBettingResult()))
                .count();
        
        // ------- 计算各种胜率和概率 -------
        
        // 1. 总体预测胜率：总命中次数÷总预测次数
        double predictionWinRate = totalCount > 0 ? (double) correctCount / totalCount * 100 : 0;
        
        // 2. 总体胜率：（命中次数-被杀次数）/总预测次数
        double actualWinRate = totalCount > 0 ? (double) (correctCount - correctButKilledCount) / totalCount * 100 : 0;
        
        // 3. 被杀概率：杀的次数/总次数
        double killedRate = totalCount > 0 ? (double) killedCount / totalCount * 100 : 0;
        
        // 4. 预测必杀概率：预测中被杀/预测中次数
        double correctKilledRate = correctCount > 0 ? (double) correctButKilledCount / correctCount * 100 : 0;
        
        // 5. 盈利比：（未命中盈利+命中盈利）/总次数
        double profitRate = totalCount > 0 ? (double) (hitProfitCount + missButProfitCount) / totalCount * 100 : 0;
        
        // 6. 亏损比： （未命中亏损+命中被杀）/总次数
        double lossRate = totalCount > 0 ? (double) (missAndLossCount + hitButKilledCount) / totalCount * 100 : 0;
        
        // 将计算结果添加到返回Map
        result.put("totalPredictionCount", totalCount);
        result.put("correctPredictionCount", correctCount);
        result.put("incorrectPredictionCount", incorrectCount);
        result.put("killedCount", killedCount);
        result.put("correctButKilledCount", correctButKilledCount);
        
        result.put("predictionWinRate", String.format("%.2f%%", predictionWinRate));  // 总体预测胜率
        result.put("actualWinRate", String.format("%.2f%%", actualWinRate));          // 总体实际胜率(排除被杀)
        result.put("killedRate", String.format("%.2f%%", killedRate));                // 被杀概率
        result.put("correctKilledRate", String.format("%.2f%%", correctKilledRate));  // 预测命中被杀概率
        result.put("profitRate", String.format("%.2f%%", profitRate));                // 盈利比例
        result.put("lossRate", String.format("%.2f%%", lossRate));                    // 亏损比例
        
        return result;
    }
    
    /**
     * 计算特定时间段内的胜率分析
     * 
     * @param serverName 服务器名称
     * @param records 指定时间段内的历史记录
     * @return 时间段内的胜率分析结果
     */
    public Map<String, Object> periodRateAnalysis(String serverName, List<HistoryRecord> records) {
        // 复用综合分析方法，但增加标识表明这是特定时间段的分析
        Map<String, Object> result = comprehensiveRateAnalysis(serverName, records);
        result.put("analyzeType", "periodAnalysis");
        return result;
    }
    
    /**
     * 计算连续性统计 - 连中、连错、连盈利、连亏损
     * 
     * @param serverName 服务器名称
     * @param records 按时间倒序排列的历史记录（最新的在前）
     * @return 连续性统计结果
     */
    public Map<String, Object> calculateConsecutiveStats(String serverName, List<HistoryRecord> records) {
        Map<String, Object> result = new HashMap<>();
        result.put("serverName", serverName);
        
        // 默认值设置
        int consecutiveCorrect = 0;
        int consecutiveIncorrect = 0;
        int consecutiveProfit = 0;
        int consecutiveLoss = 0;
        
        // 空记录处理
        if (records == null || records.isEmpty()) {
            log.debug(serverName + ": 没有数据记录");
            result.put("consecutiveCorrect", consecutiveCorrect);       
            result.put("consecutiveIncorrect", consecutiveIncorrect);   
            result.put("consecutiveProfit", consecutiveProfit);      
            result.put("consecutiveLoss", consecutiveLoss);        
            return result;
        }

        log.debug(serverName + ": 收到" + records.size() + "条记录进行连续统计");
        
        // 如果有数据，输出前几条供调试
        int debugCount = Math.min(5, records.size());
        for (int i = 0; i < debugCount; i++) {
            HistoryRecord record = records.get(i);
            log.debug(String.format("%s: 第%d条, period=%d, outcome=%s, bettingResult=%s", 
                    serverName, i, record.getPeriod(), record.getOutcome(), record.getBettingResult()));
        }
        
        // 当period排序检测
        if (records.size() >= 2) {
            Integer firstPeriod = records.get(0).getPeriod();
            Integer secondPeriod = records.get(1).getPeriod();
            if (firstPeriod != null && secondPeriod != null) {
                if (firstPeriod < secondPeriod) {
                    log.debug(serverName + ": 警告 - 数据似乎不是按period降序排列！");
                }
            }
        }
        
        // 判断一个值是否是特殊值还是未知值
        Set<String> specialOutcomes = new HashSet<>(Arrays.asList("---", "未知", "", null));
        
        // 计算连续命中次数 - 跳过特殊值后计算
        int startIndex = 0;
        // 先跳过特殊值
        while (startIndex < records.size() && specialOutcomes.contains(records.get(startIndex).getOutcome())) {
            log.debug(serverName + ": 跳过特殊值记录 - 第" + startIndex + "条, outcome=" + records.get(startIndex).getOutcome());
            startIndex++;
        }
        // 计算连中
        for (int i = startIndex; i < records.size(); i++) {
            String outcome = records.get(i).getOutcome();
            if (outcome != null && "中".equals(outcome)) {
                consecutiveCorrect++;
                log.debug(serverName + ": 第" + i + "条是中，连中计数增加到" + consecutiveCorrect);
            } else if (specialOutcomes.contains(outcome)) {
                log.debug(serverName + ": 第" + i + "条是特殊值(" + outcome + ")，继续计算");
                continue; // 特殊值直接跳过，继续计数
            } else {
                log.debug(serverName + ": 第" + i + "条非中非特殊值(" + outcome + ")，结束连中计数");
                break;
            }
        }
        
        // 计算连续错误次数 - 跳过特殊值后计算
        startIndex = 0;
        // 先跳过特殊值
        while (startIndex < records.size() && specialOutcomes.contains(records.get(startIndex).getOutcome())) {
            startIndex++;
        }
        // 计算连错
        for (int i = startIndex; i < records.size(); i++) {
            String outcome = records.get(i).getOutcome();
            if (outcome != null && "错".equals(outcome)) {
                consecutiveIncorrect++;
                log.debug(serverName + ": 第" + i + "条是错，连错计数增加到" + consecutiveIncorrect);
            } else if (specialOutcomes.contains(outcome)) {
                log.debug(serverName + ": 第" + i + "条是特殊值(" + outcome + ")，继续计算");
                continue; // 特殊值直接跳过，继续计数
            } else {
                log.debug(serverName + ": 第" + i + "条非错非特殊值(" + outcome + ")，结束连错计数");
                break;
            }
        }
        
        // 判断一个值是否是特殊值还是未知值 (与盈亏相关)
        Set<String> specialBettingResults = new HashSet<>(Arrays.asList("未知", "", null));
        
        // 计算连续盈利次数 - 跳过特殊值后计算
        startIndex = 0;
        // 先跳过特殊值
        while (startIndex < records.size() && specialBettingResults.contains(records.get(startIndex).getBettingResult())) {
            startIndex++;
        }
        // 计算连盈利
        for (int i = startIndex; i < records.size(); i++) {
            String bettingResult = records.get(i).getBettingResult();
            if (bettingResult != null && ("命中盈利".equals(bettingResult) || "未命中盈利".equals(bettingResult))) {
                consecutiveProfit++;
                log.debug(serverName + ": 第" + i + "条是盈利，连盈利计数增加到" + consecutiveProfit);
            } else if (specialBettingResults.contains(bettingResult)) {
                log.debug(serverName + ": 第" + i + "条是特殊值(" + bettingResult + ")，继续计算");
                continue; // 特殊值直接跳过，继续计数
            } else {
                log.debug(serverName + ": 第" + i + "条非盈利非特殊值(" + bettingResult + ")，结束连盈利计数");
                break;
            }
        }
        
        // 计算连续亏损次数 - 跳过特殊值后计算
        startIndex = 0;
        // 先跳过特殊值
        while (startIndex < records.size() && specialBettingResults.contains(records.get(startIndex).getBettingResult())) {
            startIndex++;
        }
        // 计算连亏损
        for (int i = startIndex; i < records.size(); i++) {
            String bettingResult = records.get(i).getBettingResult();
            if (bettingResult != null && ("命中被杀".equals(bettingResult) || "未命中亏损".equals(bettingResult))) {
                consecutiveLoss++;
                log.debug(serverName + ": 第" + i + "条是亏损，连亏损计数增加到" + consecutiveLoss);
            } else if (specialBettingResults.contains(bettingResult)) {
                log.debug(serverName + ": 第" + i + "条是特殊值(" + bettingResult + ")，继续计算");
                continue; // 特殊值直接跳过，继续计数
            } else {
                log.debug(serverName + ": 第" + i + "条非亏损非特殊值(" + bettingResult + ")，结束连亏损计数");
                break;
            }
        }
        
        // 统计哪些字段值出现在数据中
        Set<String> outcomeValues = new HashSet<>();
        Set<String> bettingResultValues = new HashSet<>();
        for (HistoryRecord record : records) {
            if (record.getOutcome() != null) {
                outcomeValues.add(record.getOutcome());
            }
            if (record.getBettingResult() != null) {
                bettingResultValues.add(record.getBettingResult());
            }
        }
        log.debug(serverName + ": 数据中出现的outcome值：" + outcomeValues);
        log.debug(serverName + ": 数据中出现的bettingResult值：" + bettingResultValues);
        
        // 存储连续统计结果
        result.put("consecutiveCorrect", consecutiveCorrect);       // 当前连中次数
        result.put("consecutiveIncorrect", consecutiveIncorrect);   // 当前连错次数
        result.put("consecutiveProfit", consecutiveProfit);         // 当前连续盈利次数
        result.put("consecutiveLoss", consecutiveLoss);             // 当前连续亏损次数
        
        log.debug(serverName + ": 计算结果 - 连中:" + consecutiveCorrect + ", 连错:" + 
                    consecutiveIncorrect + ", 连盈利:" + consecutiveProfit + ", 连亏损:" + consecutiveLoss);
        
        return result;
    }
    
    /**
     * 扩展的分析结果分布，增加了单双结果、杀号分布等细节
     * 
     * @param serverName 服务器名称
     * @param allRecords 所有历史记录
     * @return 扩展的结果分析统计
     */
    public Map<String, Object> analyzeDetailedResults(String serverName, List<HistoryRecord> allRecords) {
        Map<String, Object> result = analyzeResults(serverName, allRecords);
        
        // 杀号情况统计
        long killedSingleCount = allRecords.stream()
                .filter(record -> "单".equals(record.getOpenResult()) 
                        && record.getKillNumber() != null 
                        && !record.getKillNumber().isEmpty())
                .count();
        
        long killedDoubleCount = allRecords.stream()
                .filter(record -> "双".equals(record.getOpenResult()) 
                        && record.getKillNumber() != null 
                        && !record.getKillNumber().isEmpty())
                .count();
        
        // 预测结果与开奖结果匹配情况
        long correctSinglePredictions = allRecords.stream()
                .filter(record -> "单".equals(record.getPrediction()) 
                        && "单".equals(record.getOpenResult()))
                .count();
        
        long correctDoublePredictions = allRecords.stream()
                .filter(record -> "双".equals(record.getPrediction()) 
                        && "双".equals(record.getOpenResult()))
                .count();
        
        // 数字分段统计（可根据需要调整区间）
        Map<String, Integer> numberRangeDistribution = new HashMap<>();
        allRecords.forEach(record -> {
            Integer totalNumber = record.getTotalNumber();
            if (totalNumber != null) {
                String range;
                if (totalNumber <= 10) {
                    range = "0-10";
                } else if (totalNumber <= 20) {
                    range = "11-20";
                } else if (totalNumber <= 30) {
                    range = "21-30";
                } else {
                    range = "30+";
                }
                numberRangeDistribution.put(range, 
                        numberRangeDistribution.getOrDefault(range, 0) + 1);
            }
        });
        
        // 添加新的统计数据到结果
        result.put("killedSingleCount", killedSingleCount);
        result.put("killedDoubleCount", killedDoubleCount);
        result.put("killedSingleRate", String.format("%.2f%%", 
                (long)result.get("singleCount") > 0 ? (double)killedSingleCount / (long)result.get("singleCount") * 100 : 0));
        result.put("killedDoubleRate", String.format("%.2f%%", 
                (long)result.get("doubleCount") > 0 ? (double)killedDoubleCount / (long)result.get("doubleCount") * 100 : 0));
        
        result.put("correctSinglePredictions", correctSinglePredictions);
        result.put("correctDoublePredictions", correctDoublePredictions);
        result.put("numberRangeDistribution", numberRangeDistribution);
        
        return result;
    }
    
    /**
     * 查找历史最高连中次数、连错次数等
     * 
     * @param serverName 服务器名称
     * @param allRecords 按时间顺序排列的所有历史记录
     * @return 历史最高连续统计
     */
    public Map<String, Object> findHistoricalConsecutiveStats(String serverName, List<HistoryRecord> allRecords) {
        if (allRecords == null || allRecords.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("serverName", serverName);
            emptyResult.put("error", "无有效记录数据");
            return emptyResult;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("serverName", serverName);
        
        // 初始化最大连续计数
        int maxConsecutiveCorrect = 0;
        int maxConsecutiveIncorrect = 0;
        int maxConsecutiveProfit = 0;
        int maxConsecutiveLoss = 0;
        
        // 当前连续计数
        int currentConsecutiveCorrect = 0;
        int currentConsecutiveIncorrect = 0;
        int currentConsecutiveProfit = 0;
        int currentConsecutiveLoss = 0;
        
        // 记录开始期号
        int correctStartPeriod = 0;
        int incorrectStartPeriod = 0;
        int profitStartPeriod = 0;
        int lossStartPeriod = 0;
        
        // 历史最高记录的开始期号
        int maxCorrectStartPeriod = 0;
        int maxIncorrectStartPeriod = 0;
        int maxProfitStartPeriod = 0;
        int maxLossStartPeriod = 0;
        
        // 遍历所有记录，计算连续性
        for (int i = 0; i < allRecords.size(); i++) {
            HistoryRecord record = allRecords.get(i);
            
            // 检查是否是新的连中序列开始
            if ("中".equals(record.getOutcome())) {
                // 在开始新的连中序列前，检查并更新最大连错记录
                if (currentConsecutiveIncorrect > maxConsecutiveIncorrect) {
                    maxConsecutiveIncorrect = currentConsecutiveIncorrect;
                    maxIncorrectStartPeriod = incorrectStartPeriod;
                }
                
                if (currentConsecutiveCorrect == 0) {
                    correctStartPeriod = record.getPeriod();
                }
                currentConsecutiveCorrect++;
                currentConsecutiveIncorrect = 0; // 重置连错计数
            } else {
                // 更新最大连中记录
                if (currentConsecutiveCorrect > maxConsecutiveCorrect) {
                    maxConsecutiveCorrect = currentConsecutiveCorrect;
                    maxCorrectStartPeriod = correctStartPeriod;
                }
                currentConsecutiveCorrect = 0;
                
                // 检查是否是新的连错序列开始
                if (currentConsecutiveIncorrect == 0) {
                    incorrectStartPeriod = record.getPeriod();
                }
                currentConsecutiveIncorrect++;
            }
            
            // 检查是否是新的连盈利序列开始
            if ("命中盈利".equals(record.getBettingResult()) || "未命中盈利".equals(record.getBettingResult())) {
                if (currentConsecutiveProfit == 0) {
                    profitStartPeriod = record.getPeriod();
                }
                currentConsecutiveProfit++;
                currentConsecutiveLoss = 0; // 重置连亏损计数
            } else if ("命中被杀".equals(record.getBettingResult()) || "未命中亏损".equals(record.getBettingResult())) {
                // 更新最大连盈利记录
                if (currentConsecutiveProfit > maxConsecutiveProfit) {
                    maxConsecutiveProfit = currentConsecutiveProfit;
                    maxProfitStartPeriod = profitStartPeriod;
                }
                currentConsecutiveProfit = 0;
                
                // 检查是否是新的连亏损序列开始
                if (currentConsecutiveLoss == 0) {
                    lossStartPeriod = record.getPeriod();
                }
                currentConsecutiveLoss++;
            } else {
                // 未知结果，重置两个计数
                if (currentConsecutiveProfit > maxConsecutiveProfit) {
                    maxConsecutiveProfit = currentConsecutiveProfit;
                    maxProfitStartPeriod = profitStartPeriod;
                }
                if (currentConsecutiveLoss > maxConsecutiveLoss) {
                    maxConsecutiveLoss = currentConsecutiveLoss;
                    maxLossStartPeriod = lossStartPeriod;
                }
                currentConsecutiveProfit = 0;
                currentConsecutiveLoss = 0;
            }
            
            // 检查最后一条记录
            if (i == allRecords.size() - 1) {
                if (currentConsecutiveCorrect > maxConsecutiveCorrect) {
                    maxConsecutiveCorrect = currentConsecutiveCorrect;
                    maxCorrectStartPeriod = correctStartPeriod;
                }
                if (currentConsecutiveIncorrect > maxConsecutiveIncorrect) {
                    maxConsecutiveIncorrect = currentConsecutiveIncorrect;
                    maxIncorrectStartPeriod = incorrectStartPeriod;
                }
                if (currentConsecutiveProfit > maxConsecutiveProfit) {
                    maxConsecutiveProfit = currentConsecutiveProfit;
                    maxProfitStartPeriod = profitStartPeriod;
                }
                if (currentConsecutiveLoss > maxConsecutiveLoss) {
                    maxConsecutiveLoss = currentConsecutiveLoss;
                    maxLossStartPeriod = lossStartPeriod;
                }
            }
        }
        
        // 存储历史最高连续统计结果
        result.put("maxConsecutiveCorrect", maxConsecutiveCorrect);       // 历史最高连中次数
        result.put("maxConsecutiveIncorrect", maxConsecutiveIncorrect);   // 历史最高连错次数
        result.put("maxConsecutiveProfit", maxConsecutiveProfit);         // 历史最高连续盈利次数
        result.put("maxConsecutiveLoss", maxConsecutiveLoss);             // 历史最高连续亏损次数
        
        // 存储历史最高连续统计的开始期号
        result.put("maxCorrectStartPeriod", maxCorrectStartPeriod);
        result.put("maxIncorrectStartPeriod", maxIncorrectStartPeriod);
        result.put("maxProfitStartPeriod", maxProfitStartPeriod);
        result.put("maxLossStartPeriod", maxLossStartPeriod);
        
        return result;
    }
} 