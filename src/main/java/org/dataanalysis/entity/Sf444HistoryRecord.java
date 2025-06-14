package org.dataanalysis.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;


/**
 * SF444服务器历史记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Sf444HistoryRecord extends HistoryRecord {
    /**
     * 标记字段
     */
    private Integer flag;
    
    /**
     * 最终结果
     */
    private String finalResult;
    
    /**
     * 30场百分比
     */
    private BigDecimal percent30;
    
    /**
     * 50场百分比
     */
    private BigDecimal percent50;
    
    /**
     * 100场百分比
     */
    private BigDecimal percent100;
}
