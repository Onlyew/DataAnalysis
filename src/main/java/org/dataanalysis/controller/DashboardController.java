package org.dataanalysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 仪表盘控制器
 * 负责处理仪表盘页面的请求
 */
@Controller
public class DashboardController {
    
    /**
     * 现金流量分析仪表盘页面
     * 
     * @return 返回现金流量仪表盘页面
     */
    @GetMapping("/cashflow")
    public String cashflowDashboard() {
        return "cashflow-dashboard";
    }
    
    /**
     * 直接访问仪表盘HTML
     */
    @GetMapping("/cashflow-dashboard")
    public String cashflowDashboardHtml() {
        return "cashflow-dashboard";
    }
    
    /**
     * 主页
     * 
     * @return 返回现金流量分析仪表盘
     */
    @GetMapping("/")
    public String index() {
        return "cashflow-dashboard";
    }
}
