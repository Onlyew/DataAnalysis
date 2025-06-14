package org.dataanalysis.repository;

import org.dataanalysis.entity.Sf444HistoryRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * SF444历史记录仓库
 * 提供对sf444_history表的数据访问
 */
@Repository
public class Sf444HistoryRepository {  
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取所有记录
     */
    public List<Sf444HistoryRecord> findAll() {
        String sql = "SELECT *, 'sf444' as server_name FROM sf444_history ORDER BY period DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Sf444HistoryRecord.class));
    }

    /**
     * 根据期号查找记录
     */
    public Sf444HistoryRecord findByPeriod(int period) {
        String sql = "SELECT *, 'sf444' as server_name FROM sf444_history WHERE period = ?";
        List<Sf444HistoryRecord> results = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Sf444HistoryRecord.class), period);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * 获取最近的N条记录
     */
    public List<Sf444HistoryRecord> findRecent(int limit) {
        String sql = "SELECT *, 'sf444' as server_name FROM sf444_history ORDER BY period DESC LIMIT ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Sf444HistoryRecord.class), limit);
    }
    
    /**
     * 获取记录总数
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM sf444_history";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    
    /**
     * 统计正确预测的记录数
     */
    public int countCorrect() {
        String sql = "SELECT COUNT(*) FROM sf444_history WHERE outcome = '中'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    
    /**
     * 统计错误预测的记录数
     */
    public int countIncorrect() {
        String sql = "SELECT COUNT(*) FROM sf444_history WHERE outcome = '错'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    
    /**
     * 保存记录
     */
    public void save(Sf444HistoryRecord record) {
        if (record.getId() == null) {
            insert(record);
        } else {
            update(record);
        }
    }
    
    /**
     * 插入新记录
     */
    private void insert(Sf444HistoryRecord record) {
        String sql = "INSERT INTO sf444_history (period, numbers, prediction, outcome, total_number, " +
                "kill_number, betting_result, open_result, flag, final_result, percent_30, percent_50, percent_100) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
        jdbcTemplate.update(sql, 
                record.getPeriod(),
                record.getNumbers(),
                record.getPrediction(),
                record.getOutcome(),
                record.getTotalNumber(),
                record.getKillNumber(),
                record.getBettingResult(),
                record.getOpenResult(),
                record.getFlag(),
                record.getFinalResult(),
                record.getPercent30(),
                record.getPercent50(),
                record.getPercent100()
        );
    }
    
    /**
     * 修改记录
     */
    private void update(Sf444HistoryRecord record) {
        String sql = "UPDATE sf444_history SET " +
                "numbers = ?, prediction = ?, outcome = ?, total_number = ?, " +
                "kill_number = ?, betting_result = ?, open_result = ?, flag = ?, " +
                "final_result = ?, percent_30 = ?, percent_50 = ?, percent_100 = ? " +
                "WHERE id = ?";
        
        jdbcTemplate.update(sql, 
                record.getNumbers(),
                record.getPrediction(),
                record.getOutcome(),
                record.getTotalNumber(),
                record.getKillNumber(),
                record.getBettingResult(),
                record.getOpenResult(),
                record.getFlag(),
                record.getFinalResult(),
                record.getPercent30(),
                record.getPercent50(),
                record.getPercent100(),
                record.getId()
        );
    }
    
    /**
     * 分页获取数据
     * @param page 页码(从0开始)
     * @param size 每页记录数
     * @return 符合条件的记录
     */
    public List<Sf444HistoryRecord> findPage(int page, int size) {
        int offset = page * size;
        String sql = "SELECT id, period, numbers, prediction, outcome, total_number as totalNumber, " +
                "kill_number as killNumber, betting_result as bettingResult, open_result as openResult, " +
                "flag, final_result as finalResult, " +
                "percent_30 as percent30, percent_50 as percent50, percent_100 as percent100, " +
                "created_at as createdAt, updated_at as updatedAt, " +
                "'sf444' as serverName FROM sf444_history ORDER BY period DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Sf444HistoryRecord.class), size, offset);
    }
    
    /**
     * 根据final_result分页获取数据
     * @param finalResult final_result的值
     * @param page 页码(从0开始)
     * @param size 每页记录数
     * @return 符合条件的记录
     */
    public List<Sf444HistoryRecord> findByFinalResult(String finalResult, int page, int size) {
        int offset = page * size;
        String sql = "SELECT id, period, numbers, prediction, outcome, total_number as totalNumber, " +
                "kill_number as killNumber, betting_result as bettingResult, open_result as openResult, " +
                "flag, final_result as finalResult, " +
                "percent_30 as percent30, percent_50 as percent50, percent_100 as percent100, " +
                "created_at as createdAt, updated_at as updatedAt, " +
                "'sf444' as serverName FROM sf444_history WHERE final_result = ? ORDER BY period DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Sf444HistoryRecord.class), finalResult, size, offset);
    }
    
    /**
     * 获取符合指定final_result的记录总数
     * @param finalResult final_result的值
     * @return 记录数量
     */
    public int countByFinalResult(String finalResult) {
        String sql = "SELECT COUNT(*) FROM sf444_history WHERE final_result = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, finalResult);
    }
    
    /**
     * 获取符合指定flag的记录总数
     * @param flag flag的值
     * @return 记录数量
     */
    public int countByFlag(int flag) {
        String sql = "SELECT COUNT(*) FROM sf444_history WHERE flag = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, flag);
    }
    
    /**
     * 获取数据总页数
     * @param size 每页记录数
     * @return 总页数
     */
    public int getTotalPages(int size) {
        int totalRecords = count();
        return (int) Math.ceil((double) totalRecords / size);
    }
    
    /**
     * 获取指定final_result的数据总页数
     * @param finalResult final_result的值
     * @param size 每页记录数
     * @return 总页数
     */
    public int getTotalPagesByFinalResult(String finalResult, int size) {
        int totalRecords = countByFinalResult(finalResult);
        return (int) Math.ceil((double) totalRecords / size);
    }
}
