package org.dataanalysis.service;

/**
 * 数据统计服务接口
 * 定义了所有数据表统计服务需要实现的方法
 */
public interface DataStatisticsService {
    
    /**
     * 获取服务器名称
     */
    String getServerName();
    
    /**
     * 计算基本胜率
     */
    void calculateWinRate();
    
    /**
     * 计算最近N期的胜率
     * @param count 期数
     */
    void calculateRecentWinRate(int count);
    
    /**
     * 分析结果分布
     */
    void analyzeResults();
    
    /**
     * 计算综合胜率分析
     */
    void calculateComprehensiveRateAnalysis();
    
    /**
     * 计算当前连续统计
     */
    void calculateConsecutiveStats();
    
    /**
     * 分析详细结果
     */
    void analyzeDetailedResults();
    
    /**
     * 查找历史最高连续统计
     */
    void findHistoricalConsecutiveStats();
    
    /**
     * 执行所有计算
     */
    default void calculateAll() {
        calculateWinRate();
        calculateRecentWinRate(100);
        analyzeResults();
        calculateComprehensiveRateAnalysis();
        calculateConsecutiveStats();
        analyzeDetailedResults();
        findHistoricalConsecutiveStats();
    }
}
