package org.dataanalysis.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistoryRecord {
    private Long id;
    private Integer period;
    private String numbers;
    private String prediction;
    private String outcome;
    private Integer totalNumber;
    private String killNumber;
    private String bettingResult;
    private String openResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String serverName; // 用于标识数据来自哪个服务器
} 