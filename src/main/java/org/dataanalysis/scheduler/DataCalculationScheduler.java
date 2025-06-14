package org.dataanalysis.scheduler;

import org.dataanalysis.service.DataStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据计算定时任务
 * 每3秒执行一次所有数据表的计算
 */
@Component
@EnableScheduling
public class DataCalculationScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(DataCalculationScheduler.class);
    
    private final List<DataStatisticsService> dataStatisticsServices;
    
    @Autowired
    public DataCalculationScheduler(List<DataStatisticsService> dataStatisticsServices) {
        this.dataStatisticsServices = dataStatisticsServices;
        logger.info("数据计算调度器初始化完成，找到{}个数据表服务", dataStatisticsServices.size());
    }
    
    /**
     * 每30秒执行一次所有表的数据计算
     */
    @Scheduled(fixedRate = 30000)
    public void calculateAllTablesData() {
        logger.info("=========== 开始定时计算所有{}个数据表的统计数据 ===========", dataStatisticsServices.size());
        
        long startTime = System.currentTimeMillis();
        
        for (DataStatisticsService service : dataStatisticsServices) {
            try {
                logger.info("【{}】表数据计算开始...", service.getServerName());
                // 执行所有计算
                service.calculateAll();
                logger.info("【{}】表数据计算完成", service.getServerName());
            } catch (Exception e) {
                logger.error("【{}】表数据计算错误: {}", service.getServerName(), e.getMessage(), e);
            }
        }
        
        // 收集所有服务器的统计数据，准备显示汇总表
        printServerStatsSummary();
        
        long costTime = System.currentTimeMillis() - startTime;
        logger.info("=========== 所有表数据计算完成，耗时{}毫秒 ===========", costTime);
    }
    
    /**
     * 生成所有服务器的统计数据汇总表并保存为HTML文件
     * 文件保存在项目的static目录下，可以通过浏览器直接访问
     * 不会生成任何控制台输出
     */
    private void printServerStatsSummary() {
        try {
            // 创建HTML内容
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"zh-CN\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>服务器统计数据</title>\n");
            html.append("    <style>\n");
            html.append("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; color: #333; }\n");
            html.append("        h1 { color: #2c3e50; text-align: center; margin-bottom: 20px; }\n");
            html.append("        .stats-container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
            html.append("        .timestamp { text-align: center; margin-bottom: 20px; color: #7f8c8d; font-size: 14px; }\n");
            html.append("        table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
            html.append("        th { background-color: #3498db; color: white; padding: 10px; text-align: center; }\n");
            html.append("        td { padding: 10px; text-align: center; border-bottom: 1px solid #ddd; }\n");
            html.append("        tr:nth-child(even) { background-color: #f2f2f2; }\n");
            html.append("        tr:hover { background-color: #e3f2fd; }\n");
            html.append("        .refresh-controls { display: flex; flex-direction: column; align-items: center; margin: 15px 0; }\n");
            html.append("        .refresh-btn { width: 150px; margin: 10px 0; padding: 10px; background-color: #2ecc71; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }\n");
            html.append("        .refresh-btn:hover { background-color: #27ae60; }\n");
            html.append("        .auto-refresh-panel { display: flex; align-items: center; justify-content: center; margin: 10px 0; gap: 10px; background-color: #f8f9fa; padding: 8px 15px; border-radius: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
            html.append("        #refreshInterval { padding: 5px 10px; border: 1px solid #ddd; border-radius: 4px; background-color: white; }\n");
            html.append("        #nextRefresh { font-size: 14px; color: #7f8c8d; min-width: 150px; }\n");
            html.append("        .server-name { font-weight: bold; }\n");
            html.append("        .positive { color: #27ae60; }\n");
            html.append("        .negative { color: #e74c3c; }\n");
            html.append("        .highlight { font-weight: bold; }\n");
            html.append("    </style>\n");
            // 添加刷新控制的JavaScript脚本
            html.append("    <script>\n");
            html.append("        let refreshTimer;\n");
            html.append("        let secondsLeft = 30;\n");
            html.append("        \n");
            html.append("        // 页面加载时启动定时器\n");
            html.append("        window.onload = function() {\n");
            html.append("            // 检查URL参数中是否包含自定义刷新间隔\n");
            html.append("            const urlParams = new URLSearchParams(window.location.search);\n");
            html.append("            const urlInterval = urlParams.get('refresh');\n");
            html.append("            \n");
            html.append("            if (urlInterval) {\n");
            html.append("                // 如果有URL参数，使用URL中的刷新时间\n");
            html.append("                document.getElementById('refreshInterval').value = urlInterval;\n");
            html.append("            }\n");
            html.append("            \n");
            html.append("            const interval = document.getElementById('refreshInterval').value;\n");
            html.append("            if (interval > 0) {\n");
            html.append("                startRefreshTimer(interval);\n");
            html.append("                console.log('开始刷新计时: ' + interval + '秒');\n");
            html.append("            }\n");
            html.append("        };\n");
            html.append("        \n");
            html.append("        // 设置刷新间隔\n");
            html.append("        function setRefreshInterval(seconds) {\n");
            html.append("            // 清除当前定时器\n");
            html.append("            clearInterval(refreshTimer);\n");
            html.append("            \n");
            html.append("            // 更新URL参数，保存用户的选择\n");
            html.append("            const url = new URL(window.location.href);\n");
            html.append("            url.searchParams.set('refresh', seconds);\n");
            html.append("            window.history.replaceState({}, '', url.toString());\n");
            html.append("            \n");
            html.append("            console.log('设置刷新间隔为: ' + seconds + '秒');\n");
            html.append("            \n");
            html.append("            if (seconds > 0) {\n");
            html.append("                startRefreshTimer(seconds);\n");
            html.append("            } else {\n");
            html.append("                document.getElementById('nextRefresh').textContent = '自动刷新已关闭';\n");
            html.append("            }\n");
            html.append("        }\n");
            html.append("        \n");
            html.append("        // 启动刷新定时器\n");
            html.append("        function startRefreshTimer(seconds) {\n");
            html.append("            secondsLeft = parseInt(seconds);\n");
            html.append("            updateRefreshDisplay();\n");
            html.append("            \n");
            html.append("            refreshTimer = setInterval(function() {\n");
            html.append("                secondsLeft--;\n");
            html.append("                if (secondsLeft <= 0) {\n");
            html.append("                    // 时间到，刷新页面\n");
            html.append("                    location.reload();\n");
            html.append("                } else {\n");
            html.append("                    updateRefreshDisplay();\n");
            html.append("                }\n");
            html.append("            }, 1000);\n");
            html.append("        }\n");
            html.append("        \n");
            html.append("        // 更新刷新显示\n");
            html.append("        function updateRefreshDisplay() {\n");
            html.append("            const display = document.getElementById('nextRefresh');\n");
            html.append("            if (secondsLeft > 60) {\n");
            html.append("                const minutes = Math.floor(secondsLeft / 60);\n");
            html.append("                const seconds = secondsLeft % 60;\n");
            html.append("                display.textContent = '下次刷新: ' + minutes + '分' + (seconds > 0 ? seconds + '秒' : '') + '后';\n");
            html.append("            } else {\n");
            html.append("                display.textContent = '下次刷新: ' + secondsLeft + '秒后';\n");
            html.append("            }\n");
            html.append("        }\n");
            html.append("    </script>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"stats-container\">\n");
            html.append("        <h1>服务器统计数据汇总</h1>\n");
            html.append("        <div class=\"timestamp\">最后更新时间: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "</div>\n");
            
            // 添加自动刷新控制面板
            html.append("        <div class=\"refresh-controls\">\n");
            html.append("            <button class=\"refresh-btn\" onclick=\"location.reload()\">立即刷新</button>\n");
            html.append("            <div class=\"auto-refresh-panel\">\n");
            html.append("                <span>自动刷新: </span>\n");
            html.append("                <select id=\"refreshInterval\" onchange=\"setRefreshInterval(this.value)\">\n");
            html.append("                    <option value=\"0\">关闭</option>\n");
            html.append("                    <option value=\"10\">10秒</option>\n");
            html.append("                    <option value=\"30\" selected>30秒</option>\n");
            html.append("                    <option value=\"60\">1分钟</option>\n");
            html.append("                    <option value=\"300\">5分钟</option>\n");
            html.append("                </select>\n");
            html.append("                <span id=\"nextRefresh\">下次刷新: 30秒后</span>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            html.append("        <table>\n");
            html.append("            <thead>\n");
            html.append("                <tr>\n");
            html.append("                    <th>服务器</th>\n");
            html.append("                    <th>总数/正确</th>\n");
            html.append("                    <th>预测胜率</th>\n");
            html.append("                    <th>实际胜率</th>\n");
            html.append("                    <th>最近100期</th>\n");
            html.append("                    <th>被杀率</th>\n");
            html.append("                    <th>连中/连错</th>\n");
            html.append("                    <th>连盈/连亏</th>\n");
            html.append("                    <th>单率</th>\n");
            html.append("                    <th>双率</th>\n");
            html.append("                    <th>最高连中</th>\n");
            html.append("                    <th>最高连错</th>\n");
            html.append("                </tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");

            // 添加服务器数据行
            for (DataStatisticsService service : dataStatisticsServices) {
                String serverName = service.getServerName().toUpperCase();
                
                // 从服务中获取最新的统计数据
                try {
                    // 使用反射获取服务中的统计数据
                    // 对于实际场景，应该从服务中获取最新的统计数据
                    Object statsData = null;
                    try {
                        // 尝试使用反射或服务提供的获取统计数据的方法
                        java.lang.reflect.Method getStats = service.getClass().getMethod("getStats");
                        if (getStats != null) {
                            statsData = getStats.invoke(service);
                        }
                    } catch (Exception e) {
                        // 反射失败，使用模拟数据
                        // 实际项目中会从服务中获取真实数据
                    }
                    
                    // 获取服务器的详细统计数据
                    // 预设默认值（如果数据获取失败会显示这些默认值）
                    String totalStats = "--/--";
                    String predictRate = "--%";
                    String actualRate = "--%";
                    String recent100Rate = "--%";
                    String killRate = "--%";
                    String consecutiveStats = "--/--";
                    String profitStats = "--/--";
                    String singleRate = "--%";
                    String doubleRate = "--%";
                    String maxConsecutiveHit = "--";
                    String maxConsecutiveMiss = "--";
                    
                    // 获取真实数据
                    try {
                        // 尝试直接从综合统计分析中获取总数和正确数
                        service.calculateComprehensiveRateAnalysis();
                        if (service.getClass().getMethod("getComprehensiveRateAnalysis").getDeclaringClass() != Object.class) {
                            Map<String, Object> rateData = (Map<String, Object>) service.getClass()
                                .getMethod("getComprehensiveRateAnalysis")
                                .invoke(service);
                                
                            if (rateData != null) {
                                // 总记录数 - 字段名为totalPredictionCount
                                int totalRecords = 0;
                                if (rateData.containsKey("totalPredictionCount")) {
                                    Object totalObj = rateData.get("totalPredictionCount");
                                    if (totalObj != null) {
                                        totalRecords = (totalObj instanceof Number) ? 
                                            ((Number)totalObj).intValue() : 
                                            Integer.parseInt(totalObj.toString());
                                    }
                                // 兼容原来的字段名
                                } else if (rateData.containsKey("totalRecords")) {
                                    Object totalObj = rateData.get("totalRecords");
                                    if (totalObj != null) {
                                        totalRecords = (totalObj instanceof Number) ? 
                                            ((Number)totalObj).intValue() : 
                                            Integer.parseInt(totalObj.toString());
                                    }
                                }
                                
                                // 正确预测数 - 字段名为correctPredictionCount
                                int correctCount = 0;
                                if (rateData.containsKey("correctPredictionCount")) {
                                    Object correctObj = rateData.get("correctPredictionCount");
                                    if (correctObj != null) {
                                        correctCount = (correctObj instanceof Number) ? 
                                            ((Number)correctObj).intValue() : 
                                            Integer.parseInt(correctObj.toString());
                                    }
                                }
                                
                                if (totalRecords > 0) {
                                    totalStats = totalRecords + "/" + correctCount;
                                }
                            }
                        }
                        
                        // 1. 获取综合胜率分析数据
                        service.calculateComprehensiveRateAnalysis();
                        if (service.getClass().getMethod("getComprehensiveRateAnalysis").getDeclaringClass() != Object.class) {
                            Map<String, Object> rateData = (Map<String, Object>) service.getClass()
                                .getMethod("getComprehensiveRateAnalysis")
                                .invoke(service);
                                
                            if (rateData != null) {
                                if (rateData.containsKey("predictionWinRate")) {
                                    Object rate = rateData.get("predictionWinRate");
                                    if (rate != null) {
                                        double rateVal;
                                        if (rate instanceof Number) {
                                            rateVal = ((Number)rate).doubleValue();
                                        } else {
                                            String rateStr = rate.toString();
                                            // 如果是百分比形式，移除%符号
                                            if (rateStr.endsWith("%")) {
                                                rateStr = rateStr.substring(0, rateStr.length() - 1);
                                                rateVal = Double.parseDouble(rateStr) / 100;
                                            } else {
                                                rateVal = Double.parseDouble(rateStr);
                                            }
                                        }
                                        predictRate = String.format("%.2f%%", rateVal * 100);
                                    }
                                }
                                
                                if (rateData.containsKey("actualWinRate")) {
                                    Object rate = rateData.get("actualWinRate");
                                    if (rate != null) {
                                        double rateVal;
                                        if (rate instanceof Number) {
                                            rateVal = ((Number)rate).doubleValue();
                                        } else {
                                            String rateStr = rate.toString();
                                            // 如果是百分比形式，移除%符号
                                            if (rateStr.endsWith("%")) {
                                                rateStr = rateStr.substring(0, rateStr.length() - 1);
                                                rateVal = Double.parseDouble(rateStr) / 100;
                                            } else {
                                                rateVal = Double.parseDouble(rateStr);
                                            }
                                        }
                                        actualRate = String.format("%.2f%%", rateVal * 100);
                                    }
                                }
                                
                                if (rateData.containsKey("killedRate")) {
                                    Object rate = rateData.get("killedRate");
                                    if (rate != null) {
                                        double rateVal;
                                        if (rate instanceof Number) {
                                            rateVal = ((Number)rate).doubleValue();
                                        } else {
                                            String rateStr = rate.toString();
                                            // 如果是百分比形式，移除%符号
                                            if (rateStr.endsWith("%")) {
                                                rateStr = rateStr.substring(0, rateStr.length() - 1);
                                                rateVal = Double.parseDouble(rateStr) / 100;
                                            } else {
                                                rateVal = Double.parseDouble(rateStr);
                                            }
                                        }
                                        killRate = String.format("%.2f%%", rateVal * 100);
                                    }
                                }
                            }
                        }
                        
                        // 2. 获取最近100期数据
                        if (service.getClass().getMethod("getRecentWinRateResult", int.class).getDeclaringClass() != Object.class) {
                            Map<String, Object> recentData = (Map<String, Object>) service.getClass()
                                .getMethod("getRecentWinRateResult", int.class)
                                .invoke(service, 100);
                                
                            if (recentData != null && recentData.containsKey("winRate")) {
                                Object rate = recentData.get("winRate");
                                if (rate != null) {
                                    double rateVal;
                                    if (rate instanceof Number) {
                                        rateVal = ((Number)rate).doubleValue();
                                    } else {
                                        String rateStr = rate.toString();
                                        // 如果是百分比形式，移除%符号
                                        if (rateStr.endsWith("%")) {
                                            rateStr = rateStr.substring(0, rateStr.length() - 1);
                                            rateVal = Double.parseDouble(rateStr) / 100;
                                        } else {
                                            rateVal = Double.parseDouble(rateStr);
                                        }
                                    }
                                    recent100Rate = String.format("%.2f%%", rateVal * 100);
                                }
                            }
                        }
                        
                        // 3. 获取当前连续统计
                        service.calculateConsecutiveStats();
                        if (service.getClass().getMethod("getCurrentConsecutiveStats").getDeclaringClass() != Object.class) {
                            Map<String, Object> consecutiveData = (Map<String, Object>) service.getClass()
                                .getMethod("getCurrentConsecutiveStats")
                                .invoke(service);
                                
                            if (consecutiveData != null) {
                                int correct = 0;
                                int incorrect = 0;
                                int profit = 0;
                                int loss = 0;
                                
                                if (consecutiveData.containsKey("consecutiveCorrect")) {
                                    Object val = consecutiveData.get("consecutiveCorrect");
                                    if (val != null) {
                                        correct = (val instanceof Number) ? ((Number)val).intValue() : Integer.parseInt(val.toString());
                                    }
                                }
                                
                                if (consecutiveData.containsKey("consecutiveIncorrect")) {
                                    Object val = consecutiveData.get("consecutiveIncorrect");
                                    if (val != null) {
                                        incorrect = (val instanceof Number) ? ((Number)val).intValue() : Integer.parseInt(val.toString());
                                    }
                                }
                                
                                if (consecutiveData.containsKey("consecutiveProfit")) {
                                    Object val = consecutiveData.get("consecutiveProfit");
                                    if (val != null) {
                                        profit = (val instanceof Number) ? ((Number)val).intValue() : Integer.parseInt(val.toString());
                                    }
                                }
                                
                                if (consecutiveData.containsKey("consecutiveLoss")) {
                                    Object val = consecutiveData.get("consecutiveLoss");
                                    if (val != null) {
                                        loss = (val instanceof Number) ? ((Number)val).intValue() : Integer.parseInt(val.toString());
                                    }
                                }
                                
                                consecutiveStats = correct + "/" + incorrect;
                                profitStats = profit + "/" + loss;
                            }
                        }
                        
                        // 4. 获取历史最高连续统计
                        if (service.getClass().getMethod("getHistoricalConsecutiveStats").getDeclaringClass() != Object.class) {
                            Map<String, Object> historicalData = (Map<String, Object>) service.getClass()
                                .getMethod("getHistoricalConsecutiveStats")
                                .invoke(service);
                                
                            if (historicalData != null) {
                                // 服务中的字段名是maxConsecutiveCorrect和maxConsecutiveIncorrect
                                if (historicalData.containsKey("maxConsecutiveCorrect")) {
                                    Object val = historicalData.get("maxConsecutiveCorrect");
                                    if (val != null) {
                                        int maxHit = (val instanceof Number) ? ((Number)val).intValue() : Integer.parseInt(val.toString());
                                        maxConsecutiveHit = String.valueOf(maxHit);
                                    }
                                }
                                
                                if (historicalData.containsKey("maxConsecutiveIncorrect")) {
                                    Object val = historicalData.get("maxConsecutiveIncorrect");
                                    if (val != null) {
                                        int maxMiss = (val instanceof Number) ? ((Number)val).intValue() : Integer.parseInt(val.toString());
                                        maxConsecutiveMiss = String.valueOf(maxMiss);
                                    }
                                }
                                
                                // 旧的字段名形式也尝试获取
                                if (maxConsecutiveHit.equals("--") && historicalData.containsKey("maxConsecutiveHit")) {
                                    Object val = historicalData.get("maxConsecutiveHit");
                                    if (val != null) {
                                        int maxHit = (val instanceof Number) ? ((Number)val).intValue() : Integer.parseInt(val.toString());
                                        maxConsecutiveHit = String.valueOf(maxHit);
                                    }
                                }
                                
                                if (maxConsecutiveMiss.equals("--") && historicalData.containsKey("maxConsecutiveMiss")) {
                                    Object val = historicalData.get("maxConsecutiveMiss");
                                    if (val != null) {
                                        int maxMiss = (val instanceof Number) ? ((Number)val).intValue() : Integer.parseInt(val.toString());
                                        maxConsecutiveMiss = String.valueOf(maxMiss);
                                    }
                                }
                            }
                        }
                        
                        // 5. 获取结果分析
                        service.analyzeResults();
                        if (service.getClass().getMethod("getResultsAnalysis").getDeclaringClass() != Object.class) {
                            Map<String, Object> resultsData = (Map<String, Object>) service.getClass()
                                .getMethod("getResultsAnalysis")
                                .invoke(service);
                                
                            if (resultsData != null) {
                                if (resultsData.containsKey("singleRate")) {
                                    Object rate = resultsData.get("singleRate");
                                    if (rate != null) {
                                        double rateVal;
                                        if (rate instanceof Number) {
                                            rateVal = ((Number)rate).doubleValue();
                                        } else {
                                            String rateStr = rate.toString();
                                            // 如果是百分比形式，移除%符号
                                            if (rateStr.endsWith("%")) {
                                                rateStr = rateStr.substring(0, rateStr.length() - 1);
                                                rateVal = Double.parseDouble(rateStr) / 100;
                                            } else {
                                                rateVal = Double.parseDouble(rateStr);
                                            }
                                        }
                                        singleRate = String.format("%.2f%%", rateVal * 100);
                                    }
                                }
                                
                                if (resultsData.containsKey("doubleRate")) {
                                    Object rate = resultsData.get("doubleRate");
                                    if (rate != null) {
                                        double rateVal;
                                        if (rate instanceof Number) {
                                            rateVal = ((Number)rate).doubleValue();
                                        } else {
                                            String rateStr = rate.toString();
                                            // 如果是百分比形式，移除%符号
                                            if (rateStr.endsWith("%")) {
                                                rateStr = rateStr.substring(0, rateStr.length() - 1);
                                                rateVal = Double.parseDouble(rateStr) / 100;
                                            } else {
                                                rateVal = Double.parseDouble(rateStr);
                                            }
                                        }
                                        doubleRate = String.format("%.2f%%", rateVal * 100);
                                    }
                                }
                            }
                        }
                        
                    } catch (Exception ex) {
                        logger.error("获取{}服务器详细统计数据时出错: {}", serverName, ex.getMessage());
                    }
                    
                    // 计算胜率是否高于50%以确定CSS类
                    String predictRateClass = predictRate.replace("%", "").trim();
                    double predictRateValue = -1;
                    try {
                        predictRateValue = Double.parseDouble(predictRateClass);
                    } catch (Exception e) {}
                    
                    String predictRateStyle = predictRateValue >= 50 ? "positive highlight" : (predictRateValue < 0 ? "" : "negative");
                    String recent100RateStyle = recent100Rate.startsWith("5") || recent100Rate.startsWith("6") || recent100Rate.startsWith("7") || recent100Rate.startsWith("8") || recent100Rate.startsWith("9") ? "positive highlight" : (recent100Rate.equals("--%") ? "" : "negative");
                    
                    html.append("                <tr>\n");
                    html.append("                    <td class=\"server-name\">" + serverName + "</td>\n");
                    html.append("                    <td>" + totalStats + "</td>\n");
                    html.append("                    <td class=\"" + predictRateStyle + "\">" + predictRate + "</td>\n");
                    html.append("                    <td>" + actualRate + "</td>\n");
                    html.append("                    <td class=\"" + recent100RateStyle + "\">" + recent100Rate + "</td>\n");
                    html.append("                    <td>" + killRate + "</td>\n");
                    html.append("                    <td>" + consecutiveStats + "</td>\n");
                    html.append("                    <td>" + profitStats + "</td>\n");
                    html.append("                    <td>" + singleRate + "</td>\n");
                    html.append("                    <td>" + doubleRate + "</td>\n");
                    html.append("                    <td>" + maxConsecutiveHit + "</td>\n");
                    html.append("                    <td>" + maxConsecutiveMiss + "</td>\n");
                    html.append("                </tr>\n");
                } catch (Exception e) {
                    logger.error("获取{}服务器统计数据时出错: {}", serverName, e.getMessage());
                    // 出错时显示错误信息行
                    html.append("                <tr>\n");
                    html.append("                    <td class=\"server-name\">" + serverName + "</td>\n");
                    html.append("                    <td colspan=\"11\" class=\"negative\">获取数据出错: " + e.getMessage() + "</td>\n");
                    html.append("                </tr>\n");
                }
            }

            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>");

            // 保存HTML文件到静态资源目录
            String outputDir = "src/main/resources/static";
            String fileName = "server-stats.html";
            java.nio.file.Path outputPath = java.nio.file.Paths.get(outputDir, fileName);
            
            // 确保目录存在
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(outputDir));
            
            // 写入文件
            java.nio.file.Files.write(outputPath, html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            logger.info("服务器统计数据已保存到: {}", outputPath.toAbsolutePath());
        } catch (Exception e) {
            logger.error("生成服务器统计数据HTML文件时出错: {}", e.getMessage(), e);
        }
    }
}
