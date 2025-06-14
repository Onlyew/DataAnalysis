package org.dataanalysis.repository;

import org.dataanalysis.entity.HistoryRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class Sf7HistoryRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<HistoryRecord> rowMapper = new RowMapper<HistoryRecord>() {
        @Override
        public HistoryRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            HistoryRecord record = new HistoryRecord();
            record.setId(rs.getLong("id"));
            record.setPeriod(rs.getInt("period"));
            record.setNumbers(rs.getString("numbers"));
            record.setPrediction(rs.getString("prediction"));
            record.setOutcome(rs.getString("outcome"));
            record.setTotalNumber(rs.getObject("total_number") != null ? rs.getInt("total_number") : null);
            record.setKillNumber(rs.getString("kill_number"));
            record.setBettingResult(rs.getString("betting_result"));
            record.setOpenResult(rs.getString("open_result"));
            record.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toLocalDateTime() : null);
            record.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                rs.getTimestamp("updated_at").toLocalDateTime() : null);
            record.setServerName("sf7"); // 设置服务器名称
            return record;
        }
    };
    
    /**
     * 获取所有sf7历史记录
     */
    public List<HistoryRecord> findAll() {
        String sql = "SELECT * FROM sf7_history ORDER BY period DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    /**
     * 根据期号查询记录
     */
    public HistoryRecord findByPeriod(int period) {
        String sql = "SELECT * FROM sf7_history WHERE period = ?";
        List<HistoryRecord> records = jdbcTemplate.query(sql, new Object[]{period}, rowMapper);
        return records.isEmpty() ? null : records.get(0);
    }
    
    /**
     * 获取最近n条记录
     */
    public List<HistoryRecord> findRecent(int limit) {
        String sql = "SELECT * FROM sf7_history ORDER BY period DESC LIMIT ?";
        return jdbcTemplate.query(sql, new Object[]{limit}, rowMapper);
    }
    
    /**
     * 统计总记录数
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM sf7_history";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    
    /**
     * 统计结果为中的记录数
     */
    public int countCorrect() {
        String sql = "SELECT COUNT(*) FROM sf7_history WHERE outcome = '中'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    
    /**
     * 统计结果为错的记录数
     */
    public int countIncorrect() {
        String sql = "SELECT COUNT(*) FROM sf7_history WHERE outcome = '错'";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
} 