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

// 导入Spring Boot测试相关的注解，标记该类为Spring Boot测试类


// 标记该类为Spring Boot测试类，会自动加载ApplicationContext
@SpringBootTest
// 定义数据迁移测试类
public class dataMigrationTest1 {

    // 使用Java 16引入的record特性定义不可变的历史记录数据结构
    // 参数说明：
    // period: 期号，整数类型
    // numbers: 号码，字符串类型
    // prediction: 预测结果，字符串类型
    // outcome: 实际结果，字符串类型
    // totalNumber: 总数，字符串类型
    // killNumber: 杀号，字符串类型
    // bettingResult: 投注结果，字符串类型
    // openResult: 开奖结果，字符串类型
    // flag: 标志位，整数类型，用于标记特定条件
    private record HistoryRecord(int period, String numbers, String prediction, String outcome,
                        String totalNumber, String killNumber, String bettingResult,
                        String openResult, int flag) {}

    // 定义连续杀序列记录类，用于存储连续出现"杀"的期数序列信息
    // 参数说明：
    // startPeriod: 序列起始期号
    // endPeriod: 序列结束期号
    // periods: 包含所有期号的列表
    private record ConsecutiveKillSequence(int startPeriod, int endPeriod, List<Integer> periods) {}

    // 通过Spring自动注入JdbcTemplate对象，用于执行SQL操作
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
    // 标记为JUnit测试方法，执行测试时会自动调用
    @Test
    public void migrateSf1ToSf444() {
        try {
            // 输出处理开始提示信息
            System.out.println("\n==========开始数据处理流程==========\n");

            // ==========================================
            // 第1步: Flag计算流程 - 基于判断前一期是否含有"杀"
            // ==========================================
            System.out.println("\n====== 开始 Flag 计算流程 ======");

            // 1.1 调用自定义方法获取sf1_history表的所有历史数据
            List<HistoryRecord> sf1Records = getSf1HistoryRecords();
            // 输出获取到的记录数量
            System.out.println("从sf1_history获取到 " + sf1Records.size() + " 条记录");

            // 检查是否获取到数据，如果没有则提前结束处理
            if (sf1Records.isEmpty()) {
                System.out.println("没有找到历史记录，跳过Flag计算");
                return;
            }

            // 1.2 调用自定义方法计算每条记录的flag值
            // 规则: 如果上一期的kill_number含"杀"，则下一期flag为0；否则为1
            calculateFlags(sf1Records);

            // 输出Flag计算的部分结果，用于验证计算是否正确
            System.out.println("\nFlag计算完成，部分结果如下(前10条):");
            System.out.println("期号\t\tkill_number\tflag");
            // 循环输出前10条记录（或全部记录，如果不足10条）
            for (int i = 0; i < Math.min(10, sf1Records.size()); i++) {
                HistoryRecord record = sf1Records.get(i);
                System.out.println(record.period() + "\t\t" +
                    // 处理killNumber可能为null的情况
                    (record.killNumber() != null ? record.killNumber() : "null") + "\t\t" +
                    record.flag());
            }

            // ==========================================
            // 第2步: 清空并准备sf444_history表
            // ==========================================
            // 执行TRUNCATE操作，清空目标表中的所有数据
            jdbcTemplate.update("TRUNCATE TABLE sf444_history");
            System.out.println("\nsf444_history表已清空，准备写入数据");

            // ==========================================
            // 第3步: 将带有flag的数据保存到sf444_history
            // ==========================================
            // 调用自定义方法将计算好flag的记录保存到目标表
            saveToSf444History(sf1Records);

            // ==========================================
            // 第4步: 更新final_result字段
            // ==========================================
            System.out.println("\n====== 开始final_result计算 ======");

            // 4.1 当kill_number="杀"且flag=1时，final_result="杀"
            // 执行UPDATE操作，满足条件的记录的final_result设为"杀"
            int count1 = jdbcTemplate.update(
                "UPDATE sf444_history " +
                "SET final_result = '杀' " +
                "WHERE kill_number = '杀' AND flag = 1");

            // 4.2 当kill_number不含"杀"且flag=1时，final_result="中"
            // 执行UPDATE操作，满足条件的记录的final_result设为"中"
            int count2 = jdbcTemplate.update(
                "UPDATE sf444_history " +
                "SET final_result = '中' " +
                "WHERE (kill_number != '杀' OR kill_number IS NULL) AND flag = 1");

            // 输出更新结果统计信息
            System.out.println("final_result更新结果:");
            System.out.println("- 设置为'杀': " + count1 + "条记录");
            System.out.println("- 设置为'中': " + count2 + "条记录");
            System.out.println("- 总共更新: " + (count1 + count2) + "条记录");

            // ==========================================
            // 第5步: 杀率百分比计算
            // ==========================================
            System.out.println("\n====== 开始计算杀率百分比 ======");
            // 调用自定义方法计算各期的杀率百分比
            calculateKillPercentages();

            // ==========================================
            // 第6步: 连杀序列分析 (分析final_result='杀'的连续序列)
            // ==========================================
            System.out.println("\n====== 开始连杀序列分析 ======");
            // 调用自定义方法查找连续杀序列
            List<ConsecutiveKillSequence> sequences = findConsecutiveKillSequences();

            // 如果找到连杀序列，进行进一步分析
            if (!sequences.isEmpty()) {
                // 统计各种连杀次数的频率
                analyzeConsecutiveKillFrequency(sequences);

                // 查找并输出长度>=6的连杀序列
                findLongestConsecutiveKillSequence(sequences);
            }

            // ==========================================
            // 第7步: 验证迁移结果
            // ==========================================
            // 调用自定义方法验证数据迁移的完整性
            verifyMigrationResult();

            // ==========================================
            // 第8步: 筛选出特定条件的记录并导出
            // ==========================================
            // 调用自定义方法筛选并输出符合特定条件的记录
            filterAndExportSpecificRecords();

            // 输出处理完成的提示信息
            System.out.println("\n数据迁移和处理全部完成！");

        } catch (Exception e) {
            // 捕获并输出处理过程中可能发生的异常
            System.err.println("数据处理过程中发生错误:");
            e.printStackTrace();
        }
    }

    /**
     * 从sf1_history获取记录
     * @return 历史记录列表
     */
    private List<HistoryRecord> getSf1HistoryRecords() {
        // 创建空列表用于存储从数据库获取的记录
        List<HistoryRecord> records = new ArrayList<>();

        // 定义SQL查询语句，从sf1_history表获取所有记录并按期号排序
        String sql = "SELECT period, numbers, prediction, outcome, total_number, " +
                     "kill_number, betting_result, open_result " +
                     "FROM sf1_history ORDER BY period";

        // 执行SQL查询，结果以Map列表形式返回
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        // 遍历查询结果，将每行数据转换为HistoryRecord对象
        for (Map<String, Object> row : results) {
            records.add(new HistoryRecord(
                // 将period字段转换为int类型
                ((Number)row.get("period")).intValue(),
                // 直接获取字符串字段
                (String)row.get("numbers"),
                (String)row.get("prediction"),
                (String)row.get("outcome"),
                // 处理total_number可能为null的情况
                row.get("total_number") != null ? row.get("total_number").toString() : null,
                (String)row.get("kill_number"),
                (String)row.get("betting_result"),
                (String)row.get("open_result"),
                0 // 初始flag值设为0
            ));
        }

        // 返回转换后的记录列表
        return records;
    }

    /**
     * 计算flag值
     * 规则：如果上一期的kill_number含有"杀"，则下一期flag为0；否则为1
     * @param records 所有记录
     */
    private void calculateFlags(List<HistoryRecord> records) {
        // 检查记录列表是否为空，如果为空则直接返回
        if (records.isEmpty()) return;

        // 创建新的记录列表，用于存储计算flag后的记录
        List<HistoryRecord> newRecords = new ArrayList<>();
        
        // 第一条记录的flag默认设为1（因为没有前一期作为参考）
        newRecords.add(new HistoryRecord(
            records.getFirst ().period(),
            records.getFirst ().numbers(),
            records.getFirst ().prediction(),
            records.getFirst ().outcome(),
            records.getFirst ().totalNumber(),
            records.getFirst ().killNumber(),
            records.getFirst ().bettingResult(),
            records.getFirst ().openResult(),
            1
        ));

        // 从第二条记录开始计算flag值
        for (int i = 1; i < records.size(); i++) {
            // 获取上一期记录
            HistoryRecord prevRecord = records.get(i - 1);
            // 获取当前期记录
            HistoryRecord currentRecord = records.get(i);

            // 获取上一期的杀号
            String prevKillNumber = prevRecord.killNumber();
            // 根据规则计算新的flag值：
            // 如果上一期杀号为null或不包含"杀"，则flag为1；否则为0
            int newFlag = (prevKillNumber == null || !prevKillNumber.contains("杀")) ? 1 : 0;

            // 创建带有新flag值的记录并添加到新列表
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

        // 清空原有记录列表
        records.clear();
        // 将带有新flag值的记录添加到原有列表
        records.addAll(newRecords);
    }

    /**
     * 保存计算好的flag值到sf444_history表
     *
     * @param records
     *         包含flag的记录列表
     */
    private void saveToSf444History(List<HistoryRecord> records) {
        // 输出开始保存数据的提示信息
        System.out.println("开始将" + records.size() + "条带flag的记录保存到sf444_history");

        // 定义INSERT SQL语句，插入多个字段值到sf444_history表
        String sql = "INSERT INTO sf444_history " +
                     "(period, numbers, prediction, outcome, total_number, " +
                     "kill_number, betting_result, open_result, flag, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        // 执行批量更新操作，将多条记录一次性插入到数据库表中
        int[] batchResult = jdbcTemplate.batchUpdate(
            sql, // 使用预先定义的SQL插入语句
            new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                // 重写setValues方法，为每条记录设置PreparedStatement中的参数值
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    // 获取当前索引i对应的记录对象
                    HistoryRecord record = records.get(i);
                    // 设置SQL语句中的第1个参数为期号(period)
                    ps.setInt(1, record.period());
                    // 设置SQL语句中的第2个参数为号码(numbers)
                    ps.setString(2, record.numbers());
                    // 设置SQL语句中的第3个参数为预测(prediction)
                    ps.setString(3, record.prediction());
                    // 设置SQL语句中的第4个参数为结果(outcome)
                    ps.setString(4, record.outcome());
                    // 设置SQL语句中的第5个参数为总数(totalNumber)，使用setObject可处理可能为null的情况
                    ps.setObject(5, record.totalNumber());
                    // 设置SQL语句中的第6个参数为杀号(killNumber)
                    ps.setString(6, record.killNumber());
                    // 设置SQL语句中的第7个参数为投注结果(bettingResult)
                    ps.setString(7, record.bettingResult());
                    // 设置SQL语句中的第8个参数为开奖结果(openResult)
                    ps.setString(8, record.openResult());
                    // 设置SQL语句中的第9个参数为标志位(flag)
                    ps.setInt(9, record.flag());
                }

                // 重写getBatchSize方法，返回批处理的记录总数
                @Override
                public int getBatchSize() {
                    // 返回需要批量处理的记录数量
                    return records.size();
                }
            }
        );

        // 初始化计数器，用于统计成功插入的记录总数
        int totalInserted = 0;
        // 遍历批处理结果数组，每个元素代表对应索引位置成功插入的记录数
        for (int count : batchResult) {
            // 累加每个批次成功插入的记录数
            totalInserted += count;
        }

        // 输出成功插入记录的数量信息
        System.out.println("成功保存 " + totalInserted + " 条记录到sf444_history表");
        // 返回成功插入的记录总数
    }

    /**
     * 计算杀率百分比
     * 基于前n期中kill_number="杀"的比例
     */
    private void calculateKillPercentages() {
        // 创建哈希表，用于存储期号与对应的杀号之间的映射关系
        Map<Integer, String> periodToKillNumber = new HashMap<>();
        // 创建列表，用于存储所有期号
        List<Integer> periods = new ArrayList<>();

        // 定义SQL查询语句，从sf444_history表中获取所有期号和杀号数据
        String collectQuery = "SELECT period, kill_number FROM sf444_history ORDER BY period";
        // 执行SQL查询，获取结果集
        List<Map<String, Object>> results = jdbcTemplate.queryForList(collectQuery);

        // 遍历查询结果，提取每行的期号和杀号数据
        for (Map<String, Object> row : results) {
            // 从当前行提取期号并转换为int类型
            int period = ((Number)row.get("period")).intValue();
            // 从当前行提取杀号
            String killNumber = (String)row.get("kill_number");
            // 将期号添加到期号列表中
            periods.add(period);
            // 在映射表中建立期号到杀号的映射关系
            periodToKillNumber.put(period, killNumber);
        }

        // 检查是否获取到数据
        if (periods.isEmpty()) {
            System.out.println("源表sf444_history中没有数据");
            return;
        }

        // 对期号列表进行升序排序
        Collections.sort(periods);
        System.out.println("共收集 " + periods.size() + " 条记录用于杀率计算");

        // 初始化处理计数器
        int processedCount = 0;
        // 遍历每个期号，计算并更新其对应的杀率百分比
        for (int currentPeriod : periods) {
            // 计算当前期号前30期的杀率百分比
            BigDecimal percent30 = calculateKillPercentage(currentPeriod, periodToKillNumber, periods, 30);
            // 计算当前期号前50期的杀率百分比
            BigDecimal percent50 = calculateKillPercentage(currentPeriod, periodToKillNumber, periods, 50);
            // 计算当前期号前100期的杀率百分比
            BigDecimal percent100 = calculateKillPercentage(currentPeriod, periodToKillNumber, periods, 100);

            // 定义更新SQL语句，更新当前期号的杀率百分比字段
            String updateSql = "UPDATE sf444_history SET percent_30 = ?, percent_50 = ?, percent_100 = ? WHERE period = ?";
            // 执行更新操作，设置三个杀率百分比和期号参数
            jdbcTemplate.update(updateSql,
                    percent30,  // 前30期杀率
                    percent50,  // 前50期杀率
                    percent100, // 前100期杀率
                    currentPeriod); // 当前期号

            // 增加处理计数
            processedCount++;
            // 每处理100条记录或处理完所有记录时输出进度
            if (processedCount % 100 == 0 || processedCount == periods.size()) {
                System.out.println("已处理 " + processedCount + "/" + periods.size() + " 条记录的杀率计算");
            }
        }

        // 定义SQL查询，用于随机选择几条记录进行验证
        // 注意：这个查询语句定义了但实际上没有执行，可能是代码未完成
        String verifySql = "SELECT period, final_result, percent_30, percent_50, percent_100 " +
                     "FROM sf444_history " +
                     "ORDER BY RAND() LIMIT 5";
    }

    /**
     * 计算指定期数的杀率百分比
     * 基于前 n 期中 kill_number="杀" 的比例
     * @param currentPeriod 当前期号
     * @param periodToKillNumber 期号到杀号的映射
     * @param periods 所有期号列表
     * @param n 要计算的历史期数范围
     * @return 计算得到的杀率百分比，保留两位小数
     */
    private BigDecimal calculateKillPercentage(int currentPeriod, Map<Integer, String> periodToKillNumber,
                                           List<Integer> periods, int n) {
        // 获取当前期在periods列表中的索引位置
        int currentIndex = periods.indexOf(currentPeriod);
        // 如果找不到当前期号，返回null
        if (currentIndex < 0) {
            return null; // 找不到当前期数
        }

        // 初始化计数器
        int killCount = 0; // 杀号为"杀"的期数
        int totalCount = 0; // 总查找期数

        // 从当前期数往前查找n场(不包括当前期)
        for (int i = currentIndex - 1; i >= 0 && totalCount < n; i--) {
            // 获取前一期的期号
            int period = periods.get(i);
            // 获取该期的杀号
            String killNumber = periodToKillNumber.get(period);

            // 如果杀号为"杀"，增加杀号计数
            if (killNumber != null && "杀".equals(killNumber)) {
                killCount++;
            }
            // 增加总期数计数
            totalCount++;
        }

        // 如果没有足够的历史数据，返回0
        if (totalCount == 0) {
            return BigDecimal.ZERO;
        }

        // 计算杀率百分比
        // 1. 创建杀号计数的BigDecimal对象
        // 2. 除以总期数，保留4位小数，使用HALF_UP舍入模式
        // 3. 乘以100转换为百分比
        // 4. 设置最终结果保留2位小数
        BigDecimal percentage = new BigDecimal(killCount)
            .divide(new BigDecimal(totalCount), 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal(100))
            .setScale(2, RoundingMode.HALF_UP);

        // 返回计算得到的杀率百分比
        return percentage;
    }

    /**
     * 查找连续出现final_result='杀'的序列
     * 包含1连杀(单次出现final_result='杀'的情况)
     * @return 连杀序列列表
     */
    private List<ConsecutiveKillSequence> findConsecutiveKillSequences() {
        // 创建列表用于存储找到的连杀序列
        List<ConsecutiveKillSequence> sequences = new ArrayList<>();

        // 定义SQL查询语句，获取所有有final_result值的记录
        String sql = "SELECT period, final_result " +
                     "FROM sf444_history " +
                     "WHERE final_result IS NOT NULL " +
                     "ORDER BY period";

        // 执行SQL查询，获取结果集
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        // 用于临时存储当前正在处理的连杀序列中的期号
        List<Integer> currentSequence = new ArrayList<>();
        // 当前连杀序列的起始期号
        int startPeriod = 0;
        // 标记是否正在处理一个连杀序列
        boolean inSequence = false;

        // 遍历每条记录，识别连续的"杀"序列
        for (Map<String, Object> row : results) {
            // 获取当前记录的期号
            int period = ((Number)row.get("period")).intValue();
            // 获取当前记录的final_result值
            String finalResult = (String)row.get("final_result");

            // 如果当前记录的final_result为"杀"
            if ("杀".equals(finalResult)) {
                // 找到一个final_result='杀'的记录
                if (!inSequence) {
                    // 如果不在序列中，开始一个新的序列
                    startPeriod = period;
                    currentSequence.clear();
                    currentSequence.add(period);
                    inSequence = true;
                } else {
                    // 如果已在序列中，继续添加到当前序列
                    currentSequence.add(period);
                }
            } else {
                // 当前记录不是"杀"，检查是否需要结束当前序列
                if (inSequence) {
                    // 如果之前在处理序列，现在需要结束并记录该序列
                    int endPeriod = currentSequence.get(currentSequence.size() - 1);
                    // 创建序列期号的副本，避免引用问题
                    List<Integer> periodsCopy = new ArrayList<>(currentSequence);
                    // 创建连杀序列对象并添加到结果列表
                    sequences.add(new ConsecutiveKillSequence(
                        startPeriod,
                        endPeriod,
                        periodsCopy
                    ));
                    // 重置序列状态
                    inSequence = false;
                }
            }
        }

        // 处理最后一个序列（如果存在且未结束）
        if (inSequence) {
            int endPeriod = currentSequence.get(currentSequence.size() - 1);
            List<Integer> periodsCopy = new ArrayList<>(currentSequence);
            sequences.add(new ConsecutiveKillSequence(
                startPeriod,
                endPeriod,
                periodsCopy
            ));
        }

        // 输出找到的连杀序列数量
        System.out.println("共找到 " + sequences.size() + " 个连杀序列");
        // 返回连杀序列列表
        return sequences;
    }

    /**
     * 统计各种连杀次数的频率
     * @param sequences 连杀序列列表
     */
    private void analyzeConsecutiveKillFrequency(List<ConsecutiveKillSequence> sequences) {
        // 创建哈希表，用于统计各连杀次数的频率
        // key为连杀次数，value为出现的频率
        Map<Integer, Integer> frequencyMap = new HashMap<>();

        // 遍历所有连杀序列，统计各种长度的序列数量
        for (ConsecutiveKillSequence sequence : sequences) {
            // 获取当前序列的长度（包含的期数）
            int length = sequence.periods().size();
            // 更新频率映射表，如果该长度已存在则频率+1，否则设为1
            frequencyMap.put(length, frequencyMap.getOrDefault(length, 0) + 1);
        }

        // 输出统计结果的表头
        System.out.println("\n连杀次数统计：");
        System.out.println("连杀次数\t\t频次\t百分比");

        // 计算序列总数，用于计算百分比
        double total = sequences.size();

        // 将频率映射表转换为列表，便于排序
        List<Map.Entry<Integer, Integer>> sortedEntries = new ArrayList<>(frequencyMap.entrySet());
        // 按连杀次数（键）升序排序
        sortedEntries.sort(Map.Entry.comparingByKey());

        // 遍历排序后的列表，输出每种连杀次数的统计信息
        for (Map.Entry<Integer, Integer> entry : sortedEntries) {
            int length = entry.getKey();     // 连杀次数
            int frequency = entry.getValue(); // 出现频率
            // 计算百分比
            double percentage = (frequency / total) * 100;
            // 格式化输出连杀次数、频率和百分比
            System.out.printf("%d连杀\t\t%d\t%.2f%%\n", length, frequency, percentage);
        }
    }

    /**
     * 找出所有连杀长度≥6的序列并输出
     * @param sequences 所有连杀序列
     */
    private void findLongestConsecutiveKillSequence(List<ConsecutiveKillSequence> sequences) {
        // 检查序列列表是否为空
        if (sequences.isEmpty()) {
            System.out.println("没有可用的连杀序列");
            return;
        }

        // 使用Java 8 Stream API筛选出长度≥6的序列
        List<ConsecutiveKillSequence> longSequences = sequences.stream()
            // 筛选条件：序列中的期数数量大于等于6
            .filter(seq -> seq.periods().size() >= 6)
            // 收集结果到新列表
            .collect(java.util.stream.Collectors.toList());
            
        // 按序列开始期号升序排序（即时间先后顺序）
        longSequences.sort(Comparator.comparing(ConsecutiveKillSequence::startPeriod));
        
        // 如果没有找到长度≥6的序列，输出提示并返回
        if (longSequences.isEmpty()) {
            System.out.println("\n没有发现6连杀及以上的序列");
            return;
        }
        
        // 查询当前最新期数（数据库中的最大期号）
        Integer currentPeriod = jdbcTemplate.queryForObject(
            "SELECT MAX(period) FROM sf444_history", Integer.class);
        
        // 如果无法获取当前最新期数，输出错误信息
        if (currentPeriod == null) {
            System.out.println("无法获取当前最新期数");
        } else {
            // 获取最后一个连杀序列（时间最近的）
            ConsecutiveKillSequence lastSequence = longSequences.get(longSequences.size() - 1);
            // 获取最后一个连杀序列的结束期号
            int lastKillEndPeriod = lastSequence.endPeriod();
            // 计算最后一个连杀序列结束期号与当前最新期号之间的距离
            int gapToCurrentPeriod = currentPeriod - lastKillEndPeriod;
            
            // 输出当前最新期数和与最后连杀序列的距离信息
            System.out.println("\n当前最新期数: " + currentPeriod);
            System.out.println("最后一个>=6连杀序列结束期数: " + lastKillEndPeriod);
            System.out.println("最后一个>=6连杀与当前期数距离: " + gapToCurrentPeriod + " 期");
        }
        
        // 输出表头，准备显示长序列详情
        System.out.println("\n========== 连杀长度≥6的序列 ===========");
        System.out.println("连杀期数区间\t\t连杀长度\t总跨度期数\t与上次间隔\t连杀周期");
        
        // 用于记录前一个序列，初始为null
        ConsecutiveKillSequence prevSeq = null;
        // 遍历所有长度≥6的序列，输出详细信息
        for (ConsecutiveKillSequence seq : longSequences) {
            // 获取当前序列的起始期号
            int startPeriod = seq.startPeriod();
            // 获取当前序列的结束期号
            int endPeriod = seq.endPeriod();
            // 获取当前序列的长度（包含的期数）
            int length = seq.periods().size();
            // 计算连杀总跨度期数（从开始期号到结束期号的差值+1）
            int totalSpan = endPeriod - startPeriod + 1;
            
            // 计算与上一次连杀的间隔
            String gapWithPrev = "N/A"; // 默认为N/A（不适用）
            if (prevSeq != null) {
                // 如果有前一个序列，计算当前序列开始期号与前一序列结束期号的差值
                int gap = startPeriod - prevSeq.endPeriod();
                gapWithPrev = String.valueOf(gap);
            }
            
            // 计算当前序列的连杀周期（相邻期号之间的间隔）
            String cycle = calculateKillCycle(seq.periods());
            
            // 格式化输出当前序列的详细信息
            System.out.printf("%d-%d\t\t%d\t\t%d\t\t%s\t\t\t%s\n", 
                    startPeriod, endPeriod, length, totalSpan, gapWithPrev, cycle);
            
            // 更新前一个序列为当前序列，用于下一次循环计算间隔
            prevSeq = seq;
        }
        
        // 输出长序列总数
        System.out.println("\n共找到 " + longSequences.size() + " 个6连杀及以上的序列");
    }

    /**
     * 计算连杀周期（连杀期数之间的间隔）
     * 这个周期表示相邻两个连杀期数直接相差的数值
     * 例如序列[3282872, 3282875, 3282878]的周期是"3,3"，表示相邻期数间隔为3
     * @param periods 期号列表
     * @return 连杀周期字符串，逗号分隔
     */
    private String calculateKillCycle(List<Integer> periods) {
        // 如果期号列表少于2个元素，无法计算周期，返回N/A
        if (periods.size() < 2) {
            return "N/A";
        }
        
        // 创建StringBuilder用于构建周期字符串
        StringBuilder cycleBuilder = new StringBuilder();
        // 遍历期号列表，计算相邻期号之间的差值
        for (int i = 1; i < periods.size(); i++) {
            // 计算当前期号与前一期号的差值
            int diff = periods.get(i) - periods.get(i-1);
            // 将差值添加到周期字符串中
            cycleBuilder.append(diff);
            // 如果不是最后一个差值，添加逗号分隔符
            if (i < periods.size() - 1) {
                cycleBuilder.append(",");
            }
        }
        
        // 返回计算得到的周期字符串
        return cycleBuilder.toString();
    }

    /**
     * 筛选出 final_result="杀" 或 final_result="中" 的记录
     * 这些是我们需要关注的特定记录
     */
    private void filterAndExportSpecificRecords() {
        try {
            // 直接输出筛选开始的分隔线和标题，移除额外的延迟和刷新操作
            System.out.println("\n--------------筛选特定记录(被杀或中)--------------");

            // 定义SQL查询，筛选出final_result为"杀"或"中"的记录
            String sql = "SELECT period, numbers, prediction, kill_number, final_result, flag " +
                    "FROM sf444_history " +
                    "WHERE final_result = '杀' OR final_result = '中' " +
                    "ORDER BY period";

            // 执行查询，获取符合条件的记录
            List<Map<String, Object>> filteredRecords = jdbcTemplate.queryForList(sql);

            // 初始化统计计数器
            int totalFilteredRecords = filteredRecords.size(); // 符合条件的记录总数
            int killRecordsCount = 0; // final_result = '杀'的记录数
            int hitRecordsCount = 0;  // final_result = '中'的记录数

            // 遍历筛选出的记录，统计各类型记录的数量
            for (Map<String, Object> record : filteredRecords) {
                String finalResult = (String) record.get("final_result");
                if ("杀".equals(finalResult)) {
                    killRecordsCount++; // 杀记录计数+1
                } else if ("中".equals(finalResult)) {
                    hitRecordsCount++;  // 中记录计数+1
                }
            }

            // 输出统计信息
            System.out.println("满足条件的记录总数: " + totalFilteredRecords);
            System.out.println("final_result='杀' (被杀)记录数: " + killRecordsCount);
            System.out.println("final_result='中' (命中)记录数: " + hitRecordsCount);
            System.out.println("--------------------------------------------------");

            // 限制显示的记录数量，避免输出过多
            int displayLimit = Math.min(20, totalFilteredRecords);

            // 遍历并显示部分记录（最多20条）
            for (int i = 0; i < displayLimit; i++) {
                Map<String, Object> record = filteredRecords.get(i);
                // 提取当前记录的各字段值
                int period = ((Number) record.get("period")).intValue();
                String numbers = (String) record.get("numbers");
                String prediction = (String) record.get("prediction");
                String killNumber = (String) record.get("kill_number");
                String finalResult = (String) record.get("final_result");
                int flag = ((Number) record.get("flag")).intValue();

                // 格式化输出每条记录的详细信息，使用Tab分隔以保持对齐
                System.out.printf("%d\t%s\t%s\t\t%s\t\t%s\t\t%d\n",
                        period,
                        (numbers != null ? numbers : "N/A"),
                        (prediction != null ? prediction : "N/A"),
                        (killNumber != null ? killNumber : "N/A"),
                        (finalResult != null ? finalResult : "N/A"),
                        flag);
            }

            // 如果筛选出的记录数量超过显示限制，提示用户还有未显示的记录
            if (totalFilteredRecords > displayLimit) {
                int remaining = totalFilteredRecords - displayLimit;
                System.out.println("...[还有 " + remaining + " 条记录未显示]");
            }

            // 输出筛选结束的分隔线
            System.out.println("------------------------------------------");
        } catch (Exception e) {
            // 捕获并输出处理过程中可能发生的异常
            System.err.println("数据处理过程中发生错误:");
            e.printStackTrace();
        }
    }

    /**
     * 验证迁移结果
     * 检查源表和目标表的记录数是否一致，如有不一致则查找原因
     */
    private void verifyMigrationResult() {
        // 查询sf1_history表（源表）的记录总数
        Integer sf1Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sf1_history", Integer.class);
        // 查询sf444_history表（目标表）的记录总数
        Integer sf444Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sf444_history", Integer.class);
        // 查询sf444_history表中final_result='杀'的记录数
        Integer killResultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sf444_history WHERE final_result = '杀'", Integer.class);
        // 查询sf444_history表中flag=1的记录数
        Integer flaggedRecordsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sf444_history WHERE flag = 1", Integer.class);

        // 比较源表和目标表的记录数是否一致
        if (sf1Count.equals(sf444Count)) {
            // 如果记录数一致，输出成功信息
            System.out.println("✅ 数据迁移成功完成！");
        } else {
            // 如果记录数不一致，输出错误信息
            System.out.println("❌ 数据迁移异常，源表和目标表记录数不一致！");

            // 开始查找缺失的记录
            System.out.println("\n开始查找缺失的记录...");

            // 使用LEFT JOIN查询，找出源表中存在但目标表中不存在的记录
            String missingRecordsSql =
                "SELECT sf1.period, sf1.numbers, sf1.prediction, sf1.outcome, " +
                "sf1.kill_number, sf1.betting_result, sf1.open_result " +
                "FROM sf1_history sf1 " +
                "LEFT JOIN sf444_history sf444 ON sf1.period = sf444.period " +
                "WHERE sf444.period IS NULL";

            // 执行查询，获取缺失的记录
            List<Map<String, Object>> missingRecords = jdbcTemplate.queryForList(missingRecordsSql);

            // 如果没有找到缺失的记录
            if (missingRecords.isEmpty()) {
                System.out.println("没有发现缺失的记录，可能是由于数据存在重复或其他原因导致计数不一致");

                // 检查源表是否有重复记录（同一期号出现多次）
                String duplicateSf1Sql = "SELECT period, COUNT(*) as count FROM sf1_history GROUP BY period HAVING COUNT(*) > 1";
                List<Map<String, Object>> duplicateSf1Records = jdbcTemplate.queryForList(duplicateSf1Sql);

                // 如果找到重复记录
                if (!duplicateSf1Records.isEmpty()) {
                    System.out.println("\n发现sf1_history表中存在重复记录：");
                    // 遍历并输出每个重复的期号及其出现次数
                    for (Map<String, Object> record : duplicateSf1Records) {
                        System.out.println("期号 " + record.get("period") + " 出现 " + record.get("count") + " 次");
                    }
                } else {
                    // 如果没有找到重复记录，说明可能有其他原因
                    System.out.println("sf1_history表中没有重复记录");
                }
            } else {
                // 如果找到缺失的记录，输出这些记录的信息
                System.out.println("找到 " + missingRecords.size() + " 条缺失的记录：");
                System.out.println("期号\t号码\t\t预测\t这期outcome\tkill_number");

                // 遍历缺失的记录，输出详细信息
                for (Map<String, Object> record : missingRecords) {
                    int period = ((Number) record.get("period")).intValue();
                    String numbers = (String) record.get("numbers");
                    String prediction = (String) record.get("prediction");
                    String outcome = (String) record.get("outcome");
                    String killNumber = (String) record.get("kill_number");

                    // 格式化输出每条缺失记录的详细信息
                    System.out.printf("%d\t%s\t%s\t%s\t\t%s\n",
                                    period,
                                    (numbers != null ? numbers : "N/A"),
                                    (prediction != null ? prediction : "N/A"),
                                    (outcome != null ? outcome : "N/A"),
                                    (killNumber != null ? killNumber : "N/A"));
                }

                // 提供修复选项建议
                System.out.println("\n您可以手动将缺失的记录添加到sf444_history表，或修改数据验证逻辑允许少量记录差异");
            }
        }
    }


}
    
