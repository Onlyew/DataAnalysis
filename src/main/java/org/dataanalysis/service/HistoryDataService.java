package org.dataanalysis.service;

import org.dataanalysis.entity.HistoryRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoryDataService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // 行映射器，将结果集映射为HistoryRecord对象
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
            return record;
        }
    };
    
    // 获取单个表的所有数据
    public List<HistoryRecord> getHistoryByServer(String serverName) {
        String tableName = serverName + "_history";
        String sql = "SELECT * FROM " + tableName + " ORDER BY period DESC";
        
        List<HistoryRecord> records = jdbcTemplate.query(sql, rowMapper);
        
        // 设置服务器名称
        for (HistoryRecord record : records) {
            record.setServerName(serverName);
        }
        
        return records;
    }
    
    // 获取所有表的数据
    public Map<String, List<HistoryRecord>> getAllHistory() {
        String[] servers = {"sf1", "sf3", "sf4", "sf5", "sf6", "sf7"};
        Map<String, List<HistoryRecord>> allHistory = new HashMap<>();
        
        for (String server : servers) {
            List<HistoryRecord> records = getHistoryByServer(server);
            allHistory.put(server, records);
        }
        
        return allHistory;
    }
    
    // 获取所有表的数据合并为一个列表
    public List<HistoryRecord> getAllHistoryMerged() {
        String[] servers = {"sf1", "sf3", "sf4", "sf5", "sf6", "sf7"};
        List<HistoryRecord> allRecords = new ArrayList<>();
        
        for (String server : servers) {
            List<HistoryRecord> records = getHistoryByServer(server);
            allRecords.addAll(records);
        }
        
        return allRecords;
    }
    
    // 根据期号获取所有服务器的数据
    public Map<String, HistoryRecord> getHistoryByPeriod(int period) {
        String[] servers = {"sf1", "sf3", "sf4", "sf5", "sf6", "sf7"};
        Map<String, HistoryRecord> periodRecords = new HashMap<>();
        
        for (String server : servers) {
            String tableName = server + "_history";
            String sql = "SELECT * FROM " + tableName + " WHERE period = ?";
            
            List<HistoryRecord> records = jdbcTemplate.query(sql, new Object[]{period}, rowMapper);
            
            if (!records.isEmpty()) {
                HistoryRecord record = records.get(0);
                record.setServerName(server);
                periodRecords.put(server, record);
            }
        }
        
        return periodRecords;
    }
    
    // 获取最近n条记录
    public Map<String, List<HistoryRecord>> getRecentHistory(int limit) {
        String[] servers = {"sf1", "sf3", "sf4", "sf5", "sf6", "sf7"};
        Map<String, List<HistoryRecord>> recentHistory = new HashMap<>();
        
        for (String server : servers) {
            String tableName = server + "_history";
            String sql = "SELECT * FROM " + tableName + " ORDER BY period DESC LIMIT ?";
            
            List<HistoryRecord> records = jdbcTemplate.query(sql, new Object[]{limit}, rowMapper);
            
            // 设置服务器名称
            for (HistoryRecord record : records) {
                record.setServerName(server);
            }
            
            recentHistory.put(server, records);
        }
        
        return recentHistory;
    }
} 