package org.dataanalysis.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * SF1服务器历史记录实体类
 */
@Data
public class Sf1HistoryRecord {
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
}
