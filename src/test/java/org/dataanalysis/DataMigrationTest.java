package org.dataanalysis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class DataMigrationTest {



    // 记录类定义，用于存储历史记录数据
    private record HistoryRecord(int period, String numbers, String prediction, String outcome,
                        String totalNumber, String killNumber, String bettingResult,
                        String openResult, int flag) {}

    // 连续杀序列记录类
    private record ConsecutiveKillSequence(int startPeriod, int endPeriod, List<Integer> periods) {}

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 将sf1_history表数据迁移到sf444_history表
     * 实现完整数据处理流程：
     * 1. 从 sf1_history 获取数据，计算 flag 值
     * 2. 迁移到 sf444_history 并计算 final_result
     * 3. 分析连杀序列
     * 4. 计算杀率百分比
     * 5. 筛选出 final_result="1"且outcome="中"的特定记录
     */
    @Test
    public void migrateSf1ToSf444() {
        try {
            System.out.println("\n==========开始数据处理流程==========\n");

            // ==========================================
            // 第1步: Flag计算流程 - 基于判断前一期是否含有“杀”
            // ==========================================
            System.out.println("\n====== 开始 Flag 计算流程 ======");

            // 1.1 获取sf1_history的数据
            List<HistoryRecord> sf1Records = getSf1HistoryRecords();
            System.out.println("从sf1_history获取到 " + sf1Records.size() + " 条记录");

            if (sf1Records.isEmpty()) {
                System.out.println("没有找到历史记录，跳过Flag计算");
                return;
            }

            // 1.2 计算flag值 - 规则: 如果上一期的kill_number含“杀”，则下一期flag为0；否则为1
            calculateFlags(sf1Records);

            // 验证Flag计算结果 - 显示部分数据
            System.out.println("\nFlag计算完成，部分结果如下(前10条):");
            System.out.println("期号\t\tkill_number\tflag");
            for (int i = 0; i < Math.min(10, sf1Records.size()); i++) {
                HistoryRecord record = sf1Records.get(i);
                System.out.println(record.period() + "\t\t" +
                    (record.killNumber() != null ? record.killNumber() : "null") + "\t\t" +
                    record.flag());
            }

            // ==========================================
            // 第2步: 清空并准备sf444_history表
            // ==========================================
            jdbcTemplate.update("TRUNCATE TABLE sf444_history");
            System.out.println("\nsf444_history表已清空，准备写入数据");

            // ==========================================
            // 第3步: 将带有flag的数据保存到sf444_history
            // ==========================================
            saveToSf444History(sf1Records);

            // ==========================================
            // 第4步: 更新final_result字段
            // ==========================================
            System.out.println("\n====== 开始final_result计算 ======");

            // 4.1 当kill_number="杀"且flag=1时，final_result="杀"
            int count1 = jdbcTemplate.update(
                "UPDATE sf444_history " +
                "SET final_result = '杀' " +
                "WHERE kill_number = '杀' AND flag = 1");

            // 4.2 当kill_number不含"杀"且flag=1时，final_result="中"
            int count2 = jdbcTemplate.update(
                "UPDATE sf444_history " +
                "SET final_result = '中' " +
                "WHERE (kill_number != '杀' OR kill_number IS NULL) AND flag = 1");

            System.out.println("final_result更新结果:");
            System.out.println("- 设置为'杀': " + count1 + "条记录");
            System.out.println("- 设置为'中': " + count2 + "条记录");
            System.out.println("- 总共更新: " + (count1 + count2) + "条记录");

            // ==========================================
            // 第5步: 杀率百分比计算
            // ==========================================
            System.out.println("\n====== 开始计算杀率百分比 ======");
            calculateKillPercentages();

            // ==========================================
            // 第7步: 连杀序列分析 (分析final_result='杀'的连续序列)
            // ==========================================
            System.out.println("\n====== 开始连杀序列分析 ======");
            List<ConsecutiveKillSequence> sequences = findConsecutiveKillSequences();

            if (!sequences.isEmpty()) {
                // 统计各种连杀次数的频率
                analyzeConsecutiveKillFrequency(sequences);

                // 找出最长的连杀序列
                findLongestConsecutiveKillSequence(sequences);
            }

            // ==========================================
            // 第8步: 验证迁移结果
            // ==========================================
            verifyMigrationResult();

            // 9. 验证迁移结果
            verifyMigrationResult();

            // 10. 筛选出特定条件的记录并导出
            filterAndExportSpecificRecords();

            System.out.println("\n数据迁移和处理全部完成！");

        } catch (Exception e) {
            System.err.println("数据处理过程中发生错误:");
            e.printStackTrace();
        }
    }

    /**
     * 从sf1_history获取记录
     * @return 历史记录列表
     */
    private List<HistoryRecord> getSf1HistoryRecords() {
        List<HistoryRecord> records = new ArrayList<>();

        String sql = "SELECT period, numbers, prediction, outcome, total_number, " +
                     "kill_number, betting_result, open_result " +
                     "FROM sf1_history ORDER BY period";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> row : results) {
            records.add(new HistoryRecord(
                ((Number)row.get("period")).intValue(),
                (String)row.get("numbers"),
                (String)row.get("prediction"),
                (String)row.get("outcome"),
                row.get("total_number") != null ? row.get("total_number").toString() : null,
                (String)row.get("kill_number"),
                (String)row.get("betting_result"),
                (String)row.get("open_result"),
                0 // 初始 flag 值
            ));
        }

        return records;
    }

    /**
     * 计算flag值
     * 规则：如果上一期的kill_number含有“杀”，则下一期flag为0；否则为1
     * @param records 所有记录
     */
    private void calculateFlags(List<HistoryRecord> records) {
        if (records.isEmpty()) return;

        // 创建新的记录列表，第一条记录的flag设为1
        List<HistoryRecord> newRecords = new ArrayList<>();
        newRecords.add(new HistoryRecord(
            records.get(0).period(),
            records.get(0).numbers(),
            records.get(0).prediction(),
            records.get(0).outcome(),
            records.get(0).totalNumber(),
            records.get(0).killNumber(),
            records.get(0).bettingResult(),
            records.get(0).openResult(),
            1
        ));

        // 从第二条记录开始计算
        for (int i = 1; i < records.size(); i++) {
            HistoryRecord prevRecord = records.get(i - 1);
            HistoryRecord currentRecord = records.get(i);

            String prevKillNumber = prevRecord.killNumber();
            int newFlag = (prevKillNumber == null || !prevKillNumber.contains("杀")) ? 1 : 0;

            newRecords.add(new HistoryRecord(
                currentRecord.period(),
                currentRecord.numbers(),
                currentRecord.prediction(),
                currentRecord.outcome(),
                currentRecord.totalNumber(),
                currentRecord.killNumber(),
                currentRecord.bettingResult(),
                currentRecord.openResult(),
                newFlag
            ));
        }

        // 用新的记录替换原有列表
        records.clear();
        records.addAll(newRecords);
    }

    /**
     * 保存计算好的flag值到sf444_history表
     * @param records 包含flag的记录列表
     * @return 保存的记录数量
     */
    private int saveToSf444History(List<HistoryRecord> records) {
        System.out.println("开始将" + records.size() + "条带flag的记录保存到sf444_history");

        String sql = "INSERT INTO sf444_history " +
                     "(period, numbers, prediction, outcome, total_number, " +
                     "kill_number, betting_result, open_result, flag, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        int[] batchResult = jdbcTemplate.batchUpdate(
            sql,
            new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    HistoryRecord record = records.get(i);
                    ps.setInt(1, record.period());
                    ps.setString(2, record.numbers());
                    ps.setString(3, record.prediction());
                    ps.setString(4, record.outcome());
                    ps.setObject(5, record.totalNumber());
                    ps.setString(6, record.killNumber());
                    ps.setString(7, record.bettingResult());
                    ps.setString(8, record.openResult());
                    ps.setInt(9, record.flag());
                }

                @Override
                public int getBatchSize() {
                    return records.size();
                }
            }
        );

        int totalInserted = 0;
        for (int count : batchResult) {
            totalInserted += count;
        }

        System.out.println("成功保存 " + totalInserted + " 条记录到sf444_history表");
        return totalInserted;
    }


    /**
     * 计算杀率百分比
     * 基于前n期中kill_number="杀"的比例
     */
    private void calculateKillPercentages() {
        // 收集源表数据以计算杀率
        Map<Integer, String> periodToKillNumber = new HashMap<>();
        List<Integer> periods = new ArrayList<>();

        // 首先获取所有期号和kill_number值
        String collectQuery = "SELECT period, kill_number FROM sf444_history ORDER BY period";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(collectQuery);

        for (Map<String, Object> row : results) {
            int period = ((Number)row.get("period")).intValue();
            String killNumber = (String)row.get("kill_number");
            periods.add(period);
            periodToKillNumber.put(period, killNumber);
        }

        if (periods.isEmpty()) {
            System.out.println("源表sf444_history中没有数据");
            return;
        }

        // 按期号排序
        Collections.sort(periods);
        System.out.println("共收集 " + periods.size() + " 条记录用于杀率计算");

        // 为每条记录计算杀率并更新
        int processedCount = 0;
        for (int currentPeriod : periods) {
            // 计算每个杀率百分比
            BigDecimal percent30 = calculateKillPercentage(currentPeriod, periodToKillNumber, periods, 30);
            BigDecimal percent50 = calculateKillPercentage(currentPeriod, periodToKillNumber, periods, 50);
            BigDecimal percent100 = calculateKillPercentage(currentPeriod, periodToKillNumber, periods, 100);

            // 更新记录
            String updateSql = "UPDATE sf444_history SET percent_30 = ?, percent_50 = ?, percent_100 = ? WHERE period = ?";
            jdbcTemplate.update(updateSql,
                    percent30,
                    percent50,
                    percent100,
                    currentPeriod);

            processedCount++;
            if (processedCount % 100 == 0 || processedCount == periods.size()) {
                System.out.println("已处理 " + processedCount + "/" + periods.size() + " 条记录的杀率计算");
            }
        }

        // 随机选择几条记录验证
        String verifySql = "SELECT period, final_result, percent_30, percent_50, percent_100 " +
                     "FROM sf444_history " +
                     "ORDER BY RAND() LIMIT 5";

    }

    /**
     * 计算指定期数的杀率百分比
     * 基于前 n 期中 kill_number="杀" 的比例
     */
    private BigDecimal calculateKillPercentage(int currentPeriod, Map<Integer, String> periodToKillNumber,
                                           List<Integer> periods, int n) {
        // 获取当前期在periods中的索引
        int currentIndex = periods.indexOf(currentPeriod);
        if (currentIndex < 0) {
            return null; // 找不到当前期数
        }

        int killCount = 0;
        int totalCount = 0;

        // 从当前期数往前查找n场(不包括当前期)
        for (int i = currentIndex - 1; i >= 0 && totalCount < n; i--) {
            int period = periods.get(i);
            String killNumber = periodToKillNumber.get(period);

            if (killNumber != null && "杀".equals(killNumber)) {
                killCount++;
            }
            totalCount++;
        }

        // 如果没有足够的历史数据，返回0
        if (totalCount == 0) {
            return BigDecimal.ZERO;
        }

        // 计算百分比，保留两位小数
        BigDecimal percentage = new BigDecimal(killCount)
            .divide(new BigDecimal(totalCount), 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP);

        return percentage;
    }

    /**
     * 查找连续出现final_result='杀'的序列
     * 包含1连杀(单次出现final_result='杀'的情况)
     */
    private List<ConsecutiveKillSequence> findConsecutiveKillSequences() {
        List<ConsecutiveKillSequence> sequences = new ArrayList<>();

        String sql = "SELECT period, final_result " +
                     "FROM sf444_history " +
                     "WHERE final_result IS NOT NULL " +
                     "ORDER BY period";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        List<Integer> currentSequence = new ArrayList<>();
        int startPeriod = 0;
        boolean inSequence = false;

        for (Map<String, Object> row : results) {
            int period = ((Number)row.get("period")).intValue();
            String finalResult = (String)row.get("final_result");

            if ("杀".equals(finalResult)) {
                // 找到一个final_result='杀'的记录
                if (!inSequence) {
                    // 开始新的序列
                    startPeriod = period;
                    currentSequence.clear();
                    currentSequence.add(period);
                    inSequence = true;
                } else {
                    // 继续现有序列
                    currentSequence.add(period);
                }
            } else {
                // 不是'杀'，检查是否需要结束当前序列
                if (inSequence) {
                    // 记录所有连杀序列，包括1连杀
                    int endPeriod = currentSequence.get(currentSequence.size() - 1);
                    List<Integer> periodsCopy = new ArrayList<>(currentSequence);
                    sequences.add(new ConsecutiveKillSequence(
                        startPeriod,
                        endPeriod,
                        periodsCopy
                    ));
                    inSequence = false;
                }
            }
        }

        // 处理最后一个序列（如果存在）
        if (inSequence) {
            int endPeriod = currentSequence.get(currentSequence.size() - 1);
            List<Integer> periodsCopy = new ArrayList<>(currentSequence);
            sequences.add(new ConsecutiveKillSequence(
                startPeriod,
                endPeriod,
                periodsCopy
            ));
        }

        System.out.println("共找到 " + sequences.size() + " 个连杀序列");
        return sequences;
    }

    /**
     * 统计各种连杀次数的频率
     */
    private void analyzeConsecutiveKillFrequency(List<ConsecutiveKillSequence> sequences) {
        // 初始化计数器
        Map<Integer, Integer> frequencyMap = new HashMap<>();

        // 统计各种长度的连杀序列数量
        for (ConsecutiveKillSequence sequence : sequences) {
            int length = sequence.periods().size();
            frequencyMap.put(length, frequencyMap.getOrDefault(length, 0) + 1);
        }

        // 输出统计结果
        System.out.println("\n连杀次数统计：");
        System.out.println("连杀次数\t\t频次\t百分比");

        double total = sequences.size();

        // 按连杀次数升序排序并显示
        List<Map.Entry<Integer, Integer>> sortedEntries = new ArrayList<>(frequencyMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<Integer, Integer> entry : sortedEntries) {
            int length = entry.getKey();
            int frequency = entry.getValue();
            double percentage = (frequency / total) * 100;
            System.out.printf("%d连杀\t\t%d\t%.2f%%\n", length, frequency, percentage);
        }
    }

    /**
     * 找出所有连杀长度≥6的序列并输出
     */
    private void findLongestConsecutiveKillSequence(List<ConsecutiveKillSequence> sequences) {
        if (sequences.isEmpty()) {
            System.out.println("没有可用的连杀序列");
            return;
        }

        // 筛选出长度≥6的序列
        List<ConsecutiveKillSequence> longSequences = sequences.stream()
            .filter(seq -> seq.periods().size() >= 6)
            .collect(java.util.stream.Collectors.toList());
            
        // 按序列开始期号升序排序（即时间先后顺序）
        longSequences.sort(Comparator.comparing(ConsecutiveKillSequence::startPeriod));
        
        if (longSequences.isEmpty()) {
            System.out.println("\n没有发现6连杀及以上的序列");
            return;
        }
        
        // 查询当前最新期数
        Integer currentPeriod = jdbcTemplate.queryForObject(
            "SELECT MAX(period) FROM sf444_history", Integer.class);
        
        if (currentPeriod == null) {
            System.out.println("无法获取当前最新期数");
        } else {
            // 获取最后一个连杀序列
            ConsecutiveKillSequence lastSequence = longSequences.get(longSequences.size() - 1);
            int lastKillEndPeriod = lastSequence.endPeriod();
            int gapToCurrentPeriod = currentPeriod - lastKillEndPeriod;
            
            // 显示当前最新期数
            System.out.println("\n当前最新期数: " + currentPeriod);
            System.out.println("最后一个>=6连杀序列结束期数: " + lastKillEndPeriod);
            System.out.println("最后一个>=6连杀与当前期数距离: " + gapToCurrentPeriod + " 期");
        }
        
        // 打印表头
        System.out.println("\n========== 连杀长度≥6的序列 ===========");
        System.out.println("连杀期数区间\t\t连杀长度\t\t总跨度期数\t\t与上次间隔\t\t连杀周期");
        
        // 输出所有长序列
        ConsecutiveKillSequence prevSeq = null;
        for (ConsecutiveKillSequence seq : longSequences) {
            int startPeriod = seq.startPeriod();
            int endPeriod = seq.endPeriod();
            int length = seq.periods().size();
            // 计算连杀总跨度期数（从开始期号到结束期号）
            int totalSpan = endPeriod - startPeriod + 1;
            
            // 计算与上一次连杀的间隔
            String gapWithPrev = "N/A";
            if (prevSeq != null) {
                int gap = startPeriod - prevSeq.endPeriod();
                gapWithPrev = String.valueOf(gap);
            }
            
            String cycle = calculateKillCycle(seq.periods());
            
            System.out.printf("%d-%d\t\t%d\t\t%d\t\t%s\t\t\t%s\n", 
                    startPeriod, endPeriod, length, totalSpan, gapWithPrev, cycle);
            
            // 记录当前序列作为下一次的上一次序列
            prevSeq = seq;
        }
        
        // 显示统计信息
        System.out.println("\n共找到 " + longSequences.size() + " 个6连杀及以上的序列");
    }
    
    /**
     * 计算连杀周期（连杀期数之间的间隔）
     * 这个周期表示相邻两个连杀期数直接相差的数值
     * 例如序列[3282872, 3282875, 3282878]的周期是"3,3"，表示相邻期数间隔为3
     */
    private String calculateKillCycle(List<Integer> periods) {
        if (periods.size() < 2) {
            return "N/A";
        }
        
        // 计算相邻期数之间的间隔
        StringBuilder cycleBuilder = new StringBuilder();
        for (int i = 1; i < periods.size(); i++) {
            int diff = periods.get(i) - periods.get(i-1);
            cycleBuilder.append(diff);
            if (i < periods.size() - 1) {
                cycleBuilder.append(",");
            }
        }
        
        return cycleBuilder.toString();
    }

    /**
     * 验证迁移结果
     */
    private void verifyMigrationResult() {
        // 验证迁移结果
        Integer sf1Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sf1_history", Integer.class);
        Integer sf444Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sf444_history", Integer.class);
        Integer killResultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sf444_history WHERE final_result = '杀'", Integer.class);
        Integer flaggedRecordsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sf444_history WHERE flag = 1", Integer.class);


        if (sf1Count.equals(sf444Count)) {
            System.out.println("✅ 数据迁移成功完成！");
        } else {
            System.out.println("❌ 数据迁移异常，源表和目标表记录数不一致！");

            // 查找缺失的记录
            System.out.println("\n开始查找缺失的记录...");

            // 获取源表中存在但目标表中不存在的记录
            String missingRecordsSql =
                "SELECT sf1.period, sf1.numbers, sf1.prediction, sf1.outcome, " +
                "sf1.kill_number, sf1.betting_result, sf1.open_result " +
                "FROM sf1_history sf1 " +
                "LEFT JOIN sf444_history sf444 ON sf1.period = sf444.period " +
                "WHERE sf444.period IS NULL";

            List<Map<String, Object>> missingRecords = jdbcTemplate.queryForList(missingRecordsSql);

            if (missingRecords.isEmpty()) {
                System.out.println("没有发现缺失的记录，可能是由于数据存在重复或其他原因导致计数不一致");

                // 检查源表是否有重复记录
                String duplicateSf1Sql = "SELECT period, COUNT(*) as count FROM sf1_history GROUP BY period HAVING COUNT(*) > 1";
                List<Map<String, Object>> duplicateSf1Records = jdbcTemplate.queryForList(duplicateSf1Sql);

                if (!duplicateSf1Records.isEmpty()) {
                    System.out.println("\n发现sf1_history表中存在重复记录：");
                    for (Map<String, Object> record : duplicateSf1Records) {
                        System.out.println("期号 " + record.get("period") + " 出现 " + record.get("count") + " 次");
                    }
                } else {
                    System.out.println("sf1_history表中没有重复记录");
                }
            } else {
                System.out.println("找到 " + missingRecords.size() + " 条缺失的记录：");
                System.out.println("期号\t号码\t\t预测\t这期outcome\tkill_number");

                for (Map<String, Object> record : missingRecords) {
                    int period = ((Number) record.get("period")).intValue();
                    String numbers = (String) record.get("numbers");
                    String prediction = (String) record.get("prediction");
                    String outcome = (String) record.get("outcome");
                    String killNumber = (String) record.get("kill_number");

                    System.out.printf("%d\t%s\t%s\t%s\t\t%s\n",
                                    period,
                                    (numbers != null ? numbers : "N/A"),
                                    (prediction != null ? prediction : "N/A"),
                                    (outcome != null ? outcome : "N/A"),
                                    (killNumber != null ? killNumber : "N/A"));
                }

                // 修复缺失记录的选项
                System.out.println("\n您可以手动将缺失的记录添加到sf444_history表，或修改数据验证逻辑允许少量记录差异");
            }
        }
    }

    /**
     * 筛选出 final_result="1" 或 final_result="中" 的记录
     * 这些是我们需要关注的特定记录
     */
    private void filterAndExportSpecificRecords() {
        System.out.println("\n--------------筛选特定记录(被杀或中)--------------");

        // 筛选出 final_result="杀" 和 final_result="中" 的记录
        String sql = "SELECT period, numbers, prediction, kill_number, final_result, flag " +
                     "FROM sf444_history " +
                     "WHERE final_result = '杀' OR final_result = '中' " +
                     "ORDER BY period";

        List<Map<String, Object>> filteredRecords = jdbcTemplate.queryForList(sql);

        // 汇总统计数据
        int totalFilteredRecords = filteredRecords.size();
        int killRecordsCount = 0; // final_result = '1'
        int hitRecordsCount = 0;  // final_result = '中'

        for (Map<String, Object> record : filteredRecords) {
            String finalResult = (String) record.get("final_result");
            if ("杀".equals(finalResult)) {
                killRecordsCount++;
            } else if ("中".equals(finalResult)) {
                hitRecordsCount++;
            }
        }

        // 输出统计信息
        System.out.println("满足条件的记录总数: " + totalFilteredRecords);
        System.out.println("final_result='杀' (被杀)记录数: " + killRecordsCount);
        System.out.println("final_result='中' (命中)记录数: " + hitRecordsCount);
        System.out.println("--------------------------------------------------");


        // 限制显示的条数，避免输出过多
        int displayLimit = Math.min(20, totalFilteredRecords);

        for (int i = 0; i < displayLimit; i++) {
            Map<String, Object> record = filteredRecords.get(i);
            int period = ((Number) record.get("period")).intValue();
            String numbers = (String) record.get("numbers");
            String prediction = (String) record.get("prediction");
            String killNumber = (String) record.get("kill_number");
            String finalResult = (String) record.get("final_result");
            int flag = ((Number) record.get("flag")).intValue();

            // 格式化输出，使用Tab分隔以保持对齐
            System.out.printf("%d\t%s\t%s\t\t%s\t\t%s\t\t%d\n",
                            period,
                            (numbers != null ? numbers : "N/A"),
                            (prediction != null ? prediction : "N/A"),
                            (killNumber != null ? killNumber : "N/A"),
                            (finalResult != null ? finalResult : "N/A"),
                            flag);
        }

        // 如果有更多记录未显示，提示用户
        if (totalFilteredRecords > displayLimit) {
            int remaining = totalFilteredRecords - displayLimit;
            System.out.println("...[还有 " + remaining + " 条记录未显示]");
        }

        System.out.println("------------------------------------------");
    }
}
