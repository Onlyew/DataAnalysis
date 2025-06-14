package org.dataanalysis.service;

import org.dataanalysis.entity.HistoryRecord;
import org.dataanalysis.repository.Sf7HistoryRepository;
import org.dataanalysis.util.WinRateCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Sf7HistoryService implements DataStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(Sf7HistoryService.class);

    // 缓存计算结果
    private final Map<String, Object> calculationCache = new ConcurrentHashMap<>();

    @Autowired
    private Sf7HistoryRepository sf7HistoryRepository;

    @Autowired
    private WinRateCalculator winRateCalculator;

    /**
     * 获取服务器名称
     */
    @Override
    public String getServerName() {
        return "sf7";
    }

    /**
     * 获取所有记录
     */
    public List<HistoryRecord> getAllRecords() {
        return sf7HistoryRepository.findAll();
    }
    
    /**
     * 根据期号获取记录
     */
    public HistoryRecord getRecordByPeriod(int period) {
        return sf7HistoryRepository.findByPeriod(period);
    }
    
    /**
     * 获取最近记录
     */
    public List<HistoryRecord> getRecentRecords(int limit) {
        return sf7HistoryRepository.findRecent(limit);
    }
    
    /**
     * 计算胜率
     */
    @Override
    public void calculateWinRate() {
        logger.info("{}：开始计算基本胜率", getServerName());
        int total = sf7HistoryRepository.count();
        int correct = sf7HistoryRepository.countCorrect();
        int incorrect = sf7HistoryRepository.countIncorrect();
        
        Map<String, Object> result = winRateCalculator.calculateWinRate(getServerName(), total, correct, incorrect);
        calculationCache.put("winRate", result);
        logger.info("{}：基本胜率计算完成 - 总数:{}, 正确:{}, 错误:{}, 胜率:{}", 
                getServerName(), total, correct, incorrect, result.get("winRate"));
    }
    
    /**
     * 获取胜率计算结果
     */
    public Map<String, Object> getWinRateResult() {
        if (!calculationCache.containsKey("winRate")) {
            calculateWinRate();
        }
        return (Map<String, Object>) calculationCache.get("winRate");
    }
    
    /**
     * 计算最近N期的胜率
     */
    @Override
    public void calculateRecentWinRate(int recentCount) {
        logger.info("{}：开始计算最近{}期胜率", getServerName(), recentCount);
        List<HistoryRecord> recentRecords = sf7HistoryRepository.findRecent(recentCount);
        Map<String, Object> result = winRateCalculator.calculateRecentWinRate(getServerName(), recentRecords, recentCount);
        calculationCache.put("recentWinRate", result);
        logger.info("{}：最近{}期胜率计算完成 - 记录数:{}, 胜率:{}", 
                getServerName(), recentCount, result.get("totalCount"), result.get("winRate"));
    }
    
    /**
     * 获取最近胜率计算结果
     */
    public Map<String, Object> getRecentWinRateResult(int count) {
        String key = "recentWinRate";
        if (!calculationCache.containsKey(key)) {
            calculateRecentWinRate(count);
        }
        return (Map<String, Object>) calculationCache.get(key);
    }
    
    /**
     * 分析结果分布
     */
    @Override
    public void analyzeResults() {
        logger.info("{}：开始分析结果分布", getServerName());
        List<HistoryRecord> allRecords = sf7HistoryRepository.findAll();
        Map<String, Object> result = winRateCalculator.analyzeResults(getServerName(), allRecords);
        calculationCache.put("results", result);
        logger.info("{}：结果分布分析完成 - 单率:{}, 双率:{}", 
                getServerName(), result.get("singleRate"), result.get("doubleRate"));
    }
    
    /**
     * 获取结果分析
     */
    public Map<String, Object> getResultsAnalysis() {
        if (!calculationCache.containsKey("results")) {
            analyzeResults();
        }
        return (Map<String, Object>) calculationCache.get("results");
    }
    
    /**
     * 计算综合胜率分析(包含预测胜率、实际胜率、杀号概率等)
     */
    @Override
    public void calculateComprehensiveRateAnalysis() {
        logger.info("{}：开始计算综合胜率分析", getServerName());
        List<HistoryRecord> allRecords = sf7HistoryRepository.findAll();
        Map<String, Object> result = winRateCalculator.comprehensiveRateAnalysis(getServerName(), allRecords);
        calculationCache.put("comprehensiveRate", result);
        logger.info("{}：综合胜率分析计算完成 - 预测胜率:{}, 实际胜率:{}, 被杀率:{}", 
                getServerName(), result.get("predictionWinRate"), result.get("actualWinRate"), result.get("killedRate"));
    }
    
    /**
     * 获取综合胜率分析
     */
    public Map<String, Object> getComprehensiveRateAnalysis() {
        if (!calculationCache.containsKey("comprehensiveRate")) {
            calculateComprehensiveRateAnalysis();
        }
        return (Map<String, Object>) calculationCache.get("comprehensiveRate");
    }
    
    /**
     * 计算当前连续统计(连中、连错、连盈利、连亏损)
     */
    @Override
    public void calculateConsecutiveStats() {
        logger.info("{}：开始计算当前连续统计", getServerName());
        List<HistoryRecord> recentRecords = sf7HistoryRepository.findRecent(100);
        Map<String, Object> result = winRateCalculator.calculateConsecutiveStats(getServerName(), recentRecords);
        calculationCache.put("consecutiveStats", result);
        logger.info("{}：当前连续统计计算完成 - 连中:{}, 连错:{}, 连盈利:{}, 连亏损:{}", 
                getServerName(), result.get("consecutiveCorrect"), result.get("consecutiveIncorrect"), 
                result.get("consecutiveProfit"), result.get("consecutiveLoss"));
    }
    
    /**
     * 获取当前连续统计
     */
    public Map<String, Object> getCurrentConsecutiveStats() {
        if (!calculationCache.containsKey("consecutiveStats")) {
            calculateConsecutiveStats();
        }
        return (Map<String, Object>) calculationCache.get("consecutiveStats");
    }
    
    /**
     * 分析详细结果(包含单双结果、杀号分布等)
     */
    @Override
    public void analyzeDetailedResults() {
        logger.info("{}：开始分析详细结果", getServerName());
        List<HistoryRecord> allRecords = sf7HistoryRepository.findAll();
        Map<String, Object> result = winRateCalculator.analyzeDetailedResults(getServerName(), allRecords);
        calculationCache.put("detailedResults", result);
        logger.info("{}：详细结果分析完成 - 被杀单率:{}, 被杀双率:{}", 
                getServerName(), result.get("killedSingleRate"), result.get("killedDoubleRate"));
    }
    
    /**
     * 获取详细的结果分析
     */
    public Map<String, Object> getDetailedResultAnalysis() {
        if (!calculationCache.containsKey("detailedResults")) {
            analyzeDetailedResults();
        }
        return (Map<String, Object>) calculationCache.get("detailedResults");
    }
    
    /**
     * 查找历史最高连续统计(历史最高连中、连错、连盈利、连亏损)
     */
    @Override
    public void findHistoricalConsecutiveStats() {
        logger.info("{}：开始查找历史最高连续统计", getServerName());
        List<HistoryRecord> allRecords = sf7HistoryRepository.findAll();
        Map<String, Object> result = winRateCalculator.findHistoricalConsecutiveStats(getServerName(), allRecords);
        calculationCache.put("historicalConsecutiveStats", result);
        logger.info("{}：历史最高连续统计查找完成 - 历史最高连中:{}, 历史最高连错:{}", 
                getServerName(), result.get("maxConsecutiveCorrect"), result.get("maxConsecutiveIncorrect"));
    }
    
    /**
     * 获取历史最高连续统计
     */
    public Map<String, Object> getHistoricalConsecutiveStats() {
        if (!calculationCache.containsKey("historicalConsecutiveStats")) {
            findHistoricalConsecutiveStats();
        }
        return (Map<String, Object>) calculationCache.get("historicalConsecutiveStats");
    }
} 