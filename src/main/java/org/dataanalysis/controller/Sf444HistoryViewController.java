package org.dataanalysis.controller;

import org.dataanalysis.entity.Sf444HistoryRecord;
import org.dataanalysis.repository.Sf444HistoryRepository;
import org.dataanalysis.repository.Sf1HistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/sf444")
public class Sf444HistoryViewController {

    @Autowired
    private Sf444HistoryRepository sf444HistoryRepository;
    
    @Autowired
    private Sf1HistoryRepository sf1HistoryRepository;

    /**
     * 显示历史数据页面
     */
    @GetMapping("/view")
    @ResponseBody
    public String viewHistoryData() {
        System.out.println("====> 访问 /sf444/view 路径, 尝试直接返回HTML");
        int totalRecords = sf444HistoryRepository.count();
        
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("    <title>SF444历史数据查看</title>\n")
            .append("    <link rel=\"stylesheet\" href=\"https://cdn.bootcdn.net/ajax/libs/twitter-bootstrap/4.6.0/css/bootstrap.min.css\">\n")
            .append("    <style>\n")
            .append("        body { font-family: 'Microsoft YaHei', '微软雅黑', sans-serif; background-color: #f5f5f5; }\n")
            .append("        .container-fluid { max-width: 1600px; margin: 0 auto; padding: 0 15px; }\n")
            .append("        .page-header { background: linear-gradient(135deg, #2b5876, #4e4376); color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n")
            .append("        .card { box-shadow: 0 2px 5px rgba(0,0,0,0.05); border: none; margin-bottom: 20px; }\n")
            .append("        .card-header { background-color: #f8f9fa; border-bottom: 1px solid #eee; font-weight: bold; }\n")
            .append("        .table { border-radius: 5px; overflow: hidden; box-shadow: 0 0 10px rgba(0,0,0,0.05); }\n")
            .append("        .table-success { background-color: rgba(40, 167, 69, 0.15); }\n")
            .append("        .table-danger { background-color: rgba(220, 53, 69, 0.15); }\n")
            .append("        .thead-dark { background-color: #343a40; color: white; }\n")
            .append("        /* 表格样式优化 */\n")
            .append("        .table td, .table th { padding: 0.2rem 0.3rem; font-size: 0.85rem; line-height: 1; text-align: center; vertical-align: middle; border: 1px solid #dee2e6; }\n")
            .append("        /* 确保内容不会换行，保持紧凑 */\n")
            .append("        .table td { white-space: nowrap; }\n")
            .append("        /* 设置表格行高度 */\n")
            .append("        .table tr { height: 24px; }\n")
            .append("        /* 分页样式 */\n")
            .append("        .pagination { margin-bottom: 0; }\n")
            .append("        .pagination .page-link { padding: 0.3rem 0.6rem; font-size: 0.85rem; }\n")
            .append("        .badge { font-weight: normal; }\n")
            .append("        #lastUpdateTime { font-size: 0.9em; color: #6c757d; }\n")
            .append("        /* 控制区样式 */\n")
            .append("        .control-panel { background-color: white; border-radius: 5px; padding: 15px; margin-bottom: 20px; }\n")
            .append("        .form-control { font-size: 0.9rem; height: calc(1.5em + 0.5rem + 2px); }\n")
            .append("        /* 状态指示器 */\n")
            .append("        .status-title { font-size: 0.9rem; font-weight: bold; margin-bottom: 0.2rem; }\n")
            .append("        .status-value { font-size: 0.9rem; }\n")
            .append("        /* 按钮组 */\n")
            .append("        .btn-sm { padding: 0.2rem 0.5rem; font-size: 0.8rem; }\n")
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("<div class=\"container-fluid\">\n")
            .append("    <div class=\"page-header d-flex justify-content-between align-items-center\">\n")
            .append("        <h1 class=\"h3 mb-0\">SF444历史数据查看</h1>\n")
            .append("        <div class=\"d-flex align-items-center\">\n")
            .append("            <button id=\"refreshNow\" class=\"btn btn-danger btn-sm mr-3\">刷新动态</button>\n")
            .append("            <div class=\"text-light\">最后更新: <span id=\"headerLastUpdateTime\" class=\"last-update-time\">--</span></div>\n")
            .append("        </div>\n")
            .append("    </div>\n")
            
            // 状态区
            .append("        <div class=\"row mb-3\">\n")
            .append("            <div class=\"col-md-6\">\n")
            .append("                <div class=\"card\">\n")
            .append("                    <div class=\"card-body\">\n")
            .append("                        <h5 class=\"card-title\">数据统计</h5>\n")
            .append("                        <p>总记录数: <span id=\"totalRecords\">" + totalRecords + "</span></p>\n")
            .append("                        <p>当前显示: <span id=\"currentDisplayCount\">0</span> 条记录</p>\n")
            .append("                        <p>自动刷新: <span id=\"autoRefreshStatus\" class=\"badge badge-success\">已启用</span>\n")
            .append("                            <button id=\"toggleRefresh\" class=\"btn btn-sm btn-outline-primary ml-2\">停止</button>\n")
            .append("                        </p>\n")
            .append("                        <p>上次更新: <span id=\"lastUpdateTime\"></span></p>\n")
            .append("                    </div>\n")
            .append("                </div>\n")
            .append("            </div>\n")
            
            // 过滤区
            .append("            <div class=\"col-md-6\">\n")
            .append("                <div class=\"card mb-3\">\n")
            .append("                    <div class=\"card-body\">\n")
            .append("                        <h5 class=\"card-title\">筛选和分页</h5>\n")
            .append("                        <div class=\"row\">\n")
            .append("                            <div class=\"col-md-6\">\n")
            .append("                                <div class=\"form-group\">\n")
            .append("                                    <label for=\"filterType\">显示类型:</label>\n")
            .append("                                    <select id=\"filterType\" class=\"form-control\">\n")
            .append("                                        <option value=\"all\" selected>全部记录</option>\n")
            .append("                                        <option value=\"1\">仅显示\"被杀\"(final_result=1)</option>\n")
            .append("                                        <option value=\"中\">仅显示\"中\"(final_result=中)</option>\n")
            .append("                                    </select>\n")
            .append("                                </div>\n")
            .append("                            </div>\n")
            .append("                            <div class=\"col-md-6\">\n")
            .append("                                <div class=\"form-group\">\n")
            .append("                                    <label for=\"pageSize\">每页记录:</label>\n")
            .append("                                    <select id=\"pageSize\" class=\"form-control\">\n")
            .append("                                        <option value=\"1000\" selected>1000条</option>\n")
            .append("                                        <option value=\"2000\">2000条</option>\n")
            .append("                                        <option value=\"3000\">3000条</option>\n")
            .append("                                        <option value=\"5000\">5000条</option>\n")
            .append("                                        <option value=\"10000\">10000条</option>\n")
            .append("                                        <option value=\"all\">全部数据</option>\n")
            .append("                                    </select>\n")
            .append("                                </div>\n")
            .append("                            </div>\n")
            .append("                        </div>\n")
            .append("                    </div>\n")
            .append("                </div>\n")
            
            .append("                <!-- 统计数据卡片 -->\n")
            .append("                <div class=\"card\">\n")
            .append("                    <div class=\"card-header bg-dark text-white d-flex justify-content-between align-items-center\">\n")
            .append("                        <h5 class=\"mb-0\">全局统计</h5>\n")
            .append("                        <small id=\"statsTime\"></small>\n")
            .append("                    </div>\n")
            .append("                    <div class=\"card-body p-0\">\n")
            .append("                        <table class=\"table table-sm mb-0\">\n")
            .append("                            <tbody>\n")
            .append("                                <tr>\n")
            .append("                                    <td class=\"text-muted\">总记录数:</td>\n")
            .append("                                    <td class=\"text-right font-weight-bold\" id=\"totalCountDisplay\">0</td>\n")
            .append("                                    <td></td>\n")
            .append("                                </tr>\n")
            .append("                                <tr class=\"bg-light\">\n")
            .append("                                    <td class=\"text-muted\">下注推荐数:</td>\n")
            .append("                                    <td class=\"text-right font-weight-bold\" id=\"suggestedBetCount\">0</td>\n")
            .append("                                    <td><span class=\"badge badge-pill badge-info float-right\" id=\"suggestedBetRate\">0%</span></td>\n")
            .append("                                </tr>\n")
            .append("                                <tr class=\"table-success\">\n")
            .append("                                    <td class=\"text-muted\">中奖数量:</td>\n")
            .append("                                    <td class=\"text-right text-success font-weight-bold\" id=\"hitCount\">0</td>\n")
            .append("                                    <td><span class=\"badge badge-pill badge-success float-right\" id=\"hitRate\">0%</span></td>\n")
            .append("                                </tr>\n")
            .append("                                <tr class=\"table-danger\">\n")
            .append("                                    <td class=\"text-muted\">被杀数量:</td>\n")
            .append("                                    <td class=\"text-right text-danger font-weight-bold\" id=\"killedCount\">0</td>\n")
            .append("                                    <td><span class=\"badge badge-pill badge-danger float-right\" id=\"killedRate\">0%</span></td>\n")
            .append("                                </tr>\n")
            .append("                                <tr class=\"bg-light\">\n")
            .append("                                    <td class=\"text-muted\">推荐中奖率:</td>\n")
            .append("                                    <td class=\"text-right font-weight-bold text-primary\" colspan=\"2\">\n")
            .append("                                        <span id=\"suggestedHitRate\" class=\"h5\">0%</span>\n")
            .append("                                    </td>\n")
            .append("                                </tr>\n")
            .append("                            </tbody>\n")
            .append("                        </table>\n")
            .append("                    </div>\n")
            .append("                </div>\n")
            .append("            </div>\n")
            .append("        </div>\n")
            
            // 分页导航
            .append("        <div id=\"pagination-top\" class=\"mb-3 d-flex justify-content-center\"></div>\n")
            
            // 数据表格
            .append("        <div class=\"card\">\n")
            .append("            <div class=\"card-body p-0\">\n")
            .append("                <div class=\"table-responsive\">\n")
            .append("                    <table id=\"historyTable\" class=\"table table-striped table-bordered table-hover mb-0\">\n")
            .append("                        <thead class=\"thead-dark\">\n")
            .append("                            <tr>\n")
            .append("                                <th>期号</th>\n")
            .append("                                <th>号码</th>\n")
            .append("                                <th>预测</th>\n")
            .append("                                <th>outcome</th>\n")
            .append("                                <th>杀号</th>\n")
            .append("                                <th>Flag</th>\n")
            .append("                                <th>Final Result</th>\n")
            .append("                                <th>30场杀率</th>\n")
            .append("                                <th>50场杀率</th>\n")
            .append("                                <th>100场杀率</th>\n")
            .append("                                <th>开奖结果</th>\n")
            .append("                                <th>更新时间</th>\n")
            .append("                            </tr>\n")
            .append("                        </thead>\n")
            .append("                        <tbody id=\"historyData\">\n")
            .append("                            <!-- 数据将通过JavaScript加载 -->\n")
            .append("                        </tbody>\n")
            .append("                    </table>\n")
            .append("                </div>\n")
            .append("            </div>\n")
            .append("        </div>\n")
            
            // 底部分页
            .append("        <div id=\"pagination-bottom\" class=\"mt-3 mb-5 d-flex justify-content-center\"></div>\n")
            .append("    </div>\n");
            
        // JavaScript部分
        html.append("    <script src=\"https://cdn.bootcdn.net/ajax/libs/jquery/3.5.1/jquery.min.js\"></script>\n")
            .append("    <script src=\"https://cdn.bootcdn.net/ajax/libs/twitter-bootstrap/4.6.0/js/bootstrap.bundle.min.js\"></script>\n")
            .append("    <script>\n")
            .append("        $(document).ready(function() {\n")
            .append("            // 刷新动态按钮的点击事件\n")
            .append("            $('#refreshNow').on('click', function() {\n")
            .append("                console.log('手动刷新数据');\n")
            .append("                // 添加加载效果\n")
            .append("                const $btn = $(this);\n")
            .append("                $btn.prop('disabled', true).html('<span class=\"spinner-border spinner-border-sm\" role=\"status\" aria-hidden=\"true\"></span> 加载中...');\n")
            .append("                \n")
            .append("                // 加载数据\n")
            .append("                $.ajax({\n")
            .append("                    url: '/sf444/data',\n")
            .append("                    data: {\n")
            .append("                        page: currentPage,\n")
            .append("                        pageSize: pageSize,\n")
            .append("                        filterType: filterType\n")
            .append("                    },\n")
            .append("                    success: function(response) {\n")
            .append("                        renderData(response);\n")
            .append("                        updatePagination(response);\n")
            .append("                        updateStats(response);\n")
            .append("                        updateLastUpdateTime();\n")
            .append("                    },\n")
            .append("                    error: function(xhr, status, error) {\n")
            .append("                        console.error('加载数据失败:', error);\n")
            .append("                        $('#historyTable tbody').html('<tr><td colspan=\"11\" class=\"text-center text-danger\">加载数据失败，请刷新页面重试</td></tr>');\n")
            .append("                    }\n")
            .append("                });\n")
            .append("                \n")
            .append("                // 500ms后恢复按钮状态\n")
            .append("                setTimeout(function() {\n")
            .append("                    $btn.prop('disabled', false).html('刷新动态');\n")
            .append("                }, 500);\n")
            .append("            });\n")
            .append("        });\n")
            .append("    </script>\n")
            .append("    <script>\n")
            .append("        $(document).ready(function() {\n")
            .append("            // 全局变量\n")
            .append("            let currentPage = 0;\n")
            .append("            let pageSize = $('#pageSize').val();\n")
            .append("            let filterType = $('#filterType').val();\n")
            .append("            let autoRefreshEnabled = true;\n")
            .append("            let refreshInterval = 60000; // 60秒刷新一次\n")
            .append("            let refreshTimer = null;\n")
            
            .append("            // 初始化\n")
            .append("            loadData();\n")
            .append("            // 初始化按钮状态\n")
            .append("            $('#toggleRefresh').text('停止');\n")
            .append("            $('#autoRefreshStatus').removeClass('badge-danger').addClass('badge-success').text('已启用');\n")
            .append("            startAutoRefresh();\n")
            .append("            console.log('页面加载完成，自动刷新已启用');\n")
            
            .append("            // 分页大小变更\n")
            .append("            $('#pageSize').change(function() {\n")
            .append("                pageSize = $(this).val();\n")
            .append("                currentPage = 0;\n")
            .append("                loadData();\n")
            .append("            });\n")
            
            .append("            // 过滤类型变更\n")
            .append("            $('#filterType').change(function() {\n")
            .append("                filterType = $(this).val();\n")
            .append("                currentPage = 0;\n")
            .append("                loadData();\n")
            .append("            });\n")
            
            .append("            // 自动刷新开关\n")
            .append("            $('#toggleRefresh').click(function() {\n")
            .append("                console.log('点击了自动刷新开关按钮, 当前状态: ' + (autoRefreshEnabled ? '已启用' : '已停用'));\n")
            .append("                \n")
            .append("                // 切换自动刷新状态\n")
            .append("                autoRefreshEnabled = !autoRefreshEnabled;\n")
            .append("                console.log('状态已切换为: ' + (autoRefreshEnabled ? '已启用' : '已停用'));\n")
            .append("                \n")
            .append("                if (autoRefreshEnabled) {\n")
            .append("                    // 如果切换为启用状态\n")
            .append("                    $(this).text('停止');\n")
            .append("                    $('#autoRefreshStatus').removeClass('badge-danger').addClass('badge-success').text('已启用');\n")
            .append("                    startAutoRefresh();\n")
            .append("                    console.log('已启用自动刷新');\n")
            .append("                } else {\n")
            .append("                    // 如果切换为停止状态\n")
            .append("                    $(this).text('启动');\n")
            .append("                    $('#autoRefreshStatus').removeClass('badge-success').addClass('badge-danger').text('已停用');\n")
            .append("                    stopAutoRefresh();\n")
            .append("                    console.log('已停止自动刷新');\n")
            .append("                }\n")
            .append("                \n")
            .append("                return false; // 防止事件冒泡\n")
            .append("            });\n\n")
            .append("            // 格式化时间函数\n")
            .append("            function formatDateTime(dateTimeStr) {\n")
            .append("                if (!dateTimeStr) return '';\n")
            .append("                // 处理ISO格式的时间字符串\n")
            .append("                try {\n")
            .append("                    const date = new Date(dateTimeStr);\n")
            .append("                    if (isNaN(date.getTime())) return dateTimeStr; // 如果无效日期则原样返回\n\n")
            .append("                    // 格式化为 YYYY-MM-DD HH:MM:SS\n")
            .append("                    const year = date.getFullYear();\n")
            .append("                    const month = String(date.getMonth() + 1).padStart(2, '0');\n")
            .append("                    const day = String(date.getDate()).padStart(2, '0');\n")
            .append("                    const hours = String(date.getHours()).padStart(2, '0');\n")
            .append("                    const minutes = String(date.getMinutes()).padStart(2, '0');\n")
            .append("                    const seconds = String(date.getSeconds()).padStart(2, '0');\n\n")
            .append("                    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;\n")
            .append("                } catch (e) {\n")
            .append("                    console.error('Error formatting date:', e);\n")
            .append("                    return dateTimeStr; // 如果出错则原样返回\n")
            .append("                }\n")
            .append("            }\n")
            
            .append("            function startAutoRefresh() {\n")
            .append("                if (refreshTimer) {\n")
            .append("                    clearInterval(refreshTimer);\n")
            .append("                }\n")
            .append("                refreshTimer = setInterval(function() {\n")
            .append("                    loadData();\n")
            .append("                }, refreshInterval);\n")
            .append("            }\n")
            
            .append("            function stopAutoRefresh() {\n")
            .append("                console.log('正在停止自动刷新...');\n")
            .append("                if (refreshTimer) {\n")
            .append("                    console.log('清除定时器: ' + refreshTimer);\n")
            .append("                    clearInterval(refreshTimer);\n")
            .append("                    refreshTimer = null;\n")
            .append("                    console.log('定时器已清除，自动刷新已停止');\n")
            .append("                } else {\n")
            .append("                    console.log('没有活动的定时器需要清除');\n")
            .append("                }\n")
            .append("                // 确保全局变量正确设置\n")
            .append("                autoRefreshEnabled = false;\n")
            .append("            }\n")
            
            .append("            function loadData() {\n")
            .append("                $.ajax({\n")
            .append("                    url: '/sf444/data',\n")
            .append("                    data: {\n")
            .append("                        page: currentPage,\n")
            .append("                        pageSize: pageSize,\n")
            .append("                        filterType: filterType\n")
            .append("                    },\n")
            .append("                    success: function(response) {\n")
            .append("                        renderData(response);\n")
            .append("                        updatePagination(response);\n")
            .append("                        updateStats(response);\n")
            .append("                        updateLastUpdateTime();\n")
            .append("                    },\n")
            .append("                    error: function(xhr, status, error) {\n")
            .append("                        console.error('加载数据失败:', error);\n")
            .append("                        $('#historyTable tbody').html('<tr><td colspan=\"11\" class=\"text-center text-danger\">加载数据失败，请刷新页面重试</td></tr>');\n")
            .append("                    }\n")
            .append("                });\n")
            .append("            }\n")
            
            .append("            function renderData(data) {\n")
            .append("                const records = data.records;\n")
            .append("                let html = '';\n")
            .append("                if (records && records.length > 0) {\n")
            .append("                    records.forEach(function(record) {\n")
            .append("                        html += `<tr>\n")
            .append("                            <td>${record.period}</td>\n")
            .append("                            <td>${record.numbers || ''}</td>\n")
            .append("                            <td>${record.prediction || ''}</td>\n")
            .append("                            <td>${record.outcome || ''}</td>\n")
            .append("                            <td ${record.killNumber === '杀' ? 'style=\"background-color:#ffdddd;\"' : ''}>${record.killNumber === '杀' ? '<span style=\"color:#cc0000;font-weight:bold\">杀</span>' : (record.killNumber || '')}</td>\n")
            .append("                            <td>${record.flag}</td>\n")
            .append("                            <td ${record.finalResult === '杀' ? 'style=\"background-color:#ffdddd;color:#cc0000;font-weight:bold\"' : (record.finalResult === '中' ? 'style=\"background-color:#ddffdd;color:#008800;font-weight:bold\"' : '')}>${record.finalResult || ''}</td>\n")
            .append("                            <td>${record.percent30 || ''}</td>\n")
            .append("                            <td>${record.percent50 || ''}</td>\n")
            .append("                            <td>${record.percent100 || ''}</td>\n")
            .append("                            <td>${record.openResult || ''}</td>\n")
            .append("                            <td>${formatDateTime(record.updatedAt) || ''}</td>\n")
            .append("                        </tr>`;\n")
            .append("                    });\n")
            .append("                } else {\n")
            .append("                    html = '<tr><td colspan=\"11\" class=\"text-center\">没有找到记录</td></tr>';\n")
            .append("                }\n")
            .append("                $('#historyTable tbody').html(html);\n")
            .append("            }\n")

            .append("            function updatePagination(data) {\n")
            .append("                const totalPages = data.totalPages;\n")
            .append("                const currentDisplayPage = currentPage + 1;\n")
            .append("                let html = createPaginationHtml(currentDisplayPage, totalPages);\n")
            .append("                $('#pagination-top, #pagination-bottom').html(html);\n")
            .append("                $('.page-link').click(function(e) {\n")
            .append("                    e.preventDefault();\n")
            .append("                    const targetPage = $(this).data('page');\n")
            .append("                    if (targetPage >= 0 && targetPage < totalPages) {\n")
            .append("                        currentPage = targetPage;\n")
            .append("                        loadData();\n")
            .append("                    }\n")
            .append("                });\n")
            .append("            }\n")

            .append("            function createPaginationHtml(currentDisplayPage, totalPages) {\n")
            .append("                let html = '<ul class=\"pagination justify-content-center\">';\n")
            .append("                // 分页逻辑\n")
            .append("                html += `<li class=\"page-item ${currentPage === 0 ? 'disabled' : ''}\">\n")
            .append("                    <a class=\"page-link\" href=\"#\" data-page=\"${currentPage - 1}\">上一页</a></li>`;\n")
            .append("                // 生成页码\n")
            .append("                for (let i = 1; i <= Math.min(totalPages, 5); i++) {\n")
            .append("                    html += `<li class=\"page-item ${i === currentDisplayPage ? 'active' : ''}\">\n")
            .append("                        <a class=\"page-link\" href=\"#\" data-page=\"${i - 1}\">${i}</a></li>`;\n")
            .append("                }\n")
            .append("                html += `<li class=\"page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}\">\n")
            .append("                    <a class=\"page-link\" href=\"#\" data-page=\"${currentPage + 1}\">下一页</a></li>`;\n")
            .append("                html += '</ul>';\n")
            .append("                return html;\n")
            .append("            }\n\n")
            
            .append("            function updateStats(data) {\n")
            .append("                // 更新原有统计\n")
            .append("                $('#totalRecords').text(data.totalRecords);\n")
            .append("                $('#currentDisplayCount').text(data.records ? data.records.length : 0);\n\n")
            .append("                // 更新新添加的统计数据 - 调试\n")
            .append("                console.log('统计数据:', data);\n")
            .append("                console.log('元素检查: #totalCountDisplay 存在?', $('#totalCountDisplay').length > 0);\n")
            .append("                console.log('元素检查: #suggestedBetCount 存在?', $('#suggestedBetCount').length > 0);\n")
            .append("                console.log('元素检查: #hitCount 存在?', $('#hitCount').length > 0);\n")
            .append("                console.log('元素检查: #killedCount 存在?', $('#killedCount').length > 0);\n")
            .append("                $('#totalCountDisplay').text(data.totalCount || 0);\n")
            .append("                $('#suggestedBetCount').text(data.suggestedBetCount || 0);\n")
            .append("                $('#hitCount').text(data.hitCount || 0);\n")
            .append("                $('#killedCount').text(data.killedCount || 0);\n\n")
            .append("                // 更新百分比\n")
            .append("                $('#suggestedBetRate').text((data.totalCount > 0 ? Math.round(data.suggestedBetCount / data.totalCount * 10000) / 100 : 0) + '%');\n")
            .append("                $('#hitRate').text((data.hitRate || 0) + '%');\n")
            .append("                $('#killedRate').text((data.killedRate || 0) + '%');\n")
            .append("                $('#suggestedHitRate').text((data.suggestedHitRate || 0) + '%');\n")
            .append("            }\n\n")
            
            .append("            function updateLastUpdateTime() {\n")
            .append("                const now = new Date();\n")
            .append("                const timeString = formatDateTime(now);\n")
            .append("                // 更新所有需要显示时间的元素\n")
            .append("                $('#statsTime').text('更新于: ' + timeString);\n")
            .append("                $('#lastUpdateTime').text(timeString);\n")
            .append("                $('.last-update-time').text(timeString);\n")
            .append("            }\n")
            
            .append("        });\n")
            .append("    </script>\n")
            .append("</body>\n")
            .append("</html>");
        
        return html.toString();
    }
    
    @GetMapping("/test")
    @ResponseBody
    public String testEndpoint() {
        return "SF444控制器测试成功";
    }
    
    @GetMapping("/testpage")
    public String testPage() {
        System.out.println("====> 访问测试页面");
        return "test";
    }

    /**
     * 提供分页数据的API
     * @param pageSize 页大小(1000,2000,3000,5000,10000,20000或全部)
     * @param page 当前页码(0开始)
     * @param filterType 过滤类型(all,1,中)
     * @return 分页数据和元数据
     */
    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> getHistoryData(
            @RequestParam(defaultValue = "1000") String pageSize,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "all") String filterType) {
        
        System.out.println("API请求: /sf444/data, pageSize=" + pageSize + ", page=" + page + ", filterType=" + filterType);
        
        Map<String, Object> result = new HashMap<>();
        
        // 确定分页大小
        int size = parsePageSize(pageSize);
        int totalRecords;
        List<Sf444HistoryRecord> records;
        int totalPages;
        
        // 根据过滤类型查询数据
        if ("1".equals(filterType)) {
            // 只返回final_result=杀的记录
            records = sf444HistoryRepository.findByFinalResult("杀", page, size);
            totalRecords = sf444HistoryRepository.countByFinalResult("杀");
            totalPages = sf444HistoryRepository.getTotalPagesByFinalResult("杀", size);
        } else if ("中".equals(filterType)) {
            // 只返回final_result=中的记录
            records = sf444HistoryRepository.findByFinalResult("中", page, size);
            totalRecords = sf444HistoryRepository.countByFinalResult("中");
            totalPages = sf444HistoryRepository.getTotalPagesByFinalResult("中", size);
        } else {
            // 返回所有记录
            records = sf444HistoryRepository.findPage(page, size);
            totalRecords = sf444HistoryRepository.count();
            totalPages = sf444HistoryRepository.getTotalPages(size);
        }
        
        // 添加统计数据
        int killedCount = sf444HistoryRepository.countByFinalResult("杀");
        int hitCount = sf444HistoryRepository.countByFinalResult("中");
        int totalCount = sf444HistoryRepository.count();
        // 建议下注总数(flag=1)
        int suggestedBetCount = sf444HistoryRepository.countByFlag(1);
        
        double killedRate = totalCount > 0 ? Math.round((double)killedCount / totalCount * 10000) / 100.0 : 0;
        double hitRate = totalCount > 0 ? Math.round((double)hitCount / totalCount * 10000) / 100.0 : 0;
        // 下注中的比例
        double suggestedHitRate = suggestedBetCount > 0 ? Math.round((double)hitCount / suggestedBetCount * 10000) / 100.0 : 0;
        
        // 调试输出
        System.out.println("===== 统计数据调试信息 =====" );
        System.out.println("总记录数: " + totalCount);
        System.out.println("被杀数量: " + killedCount + " (占比: " + killedRate + "%)");
        System.out.println("中的数量: " + hitCount + " (占比: " + hitRate + "%)");
        System.out.println("下注推荐数: " + suggestedBetCount);
        System.out.println("推荐中奖率: " + suggestedHitRate + "%");
        
        // 为每条记录添加从sf1_history表获取的更新时间
        if (records != null && !records.isEmpty()) {
            for (Sf444HistoryRecord record : records) {
                // 获取期号
                Integer period = record.getPeriod();
                if (period != null) {
                    // 从sf1_history表获取更新时间
                    String sf1UpdatedTime = sf1HistoryRepository.getUpdatedTimeByPeriod(period);
                    if (sf1UpdatedTime != null) {
                        try {
                            // 将字符串类型的时间戳转换为LocalDateTime
                            Timestamp timestamp = Timestamp.valueOf(sf1UpdatedTime);
                            LocalDateTime localDateTime = timestamp.toLocalDateTime();
                            // 设置从sf1_history表获取的更新时间
                            record.setUpdatedAt(localDateTime);
                            System.out.println("期号" + period + "的更新时间已更新为: " + sf1UpdatedTime);
                        } catch (Exception e) {
                            System.out.println("无法转换期号" + period + "的更新时间: " + sf1UpdatedTime + ", 错误: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        // 返回结果
        result.put("records", records);
        result.put("currentPage", page);
        result.put("totalPages", totalPages);
        result.put("totalRecords", totalRecords);
        result.put("pageSize", size);
        result.put("filterType", filterType);
        
        // 添加统计结果
        result.put("killedCount", killedCount);
        result.put("hitCount", hitCount);
        result.put("totalCount", totalCount);
        result.put("suggestedBetCount", suggestedBetCount);
        result.put("killedRate", killedRate);
        result.put("hitRate", hitRate);
        result.put("suggestedHitRate", suggestedHitRate);
        
        return result;
    }
    
    /**
     * 解析页大小参数
     */
    private int parsePageSize(String pageSizeStr) {
        // 处理特殊情况：全部
        if ("all".equalsIgnoreCase(pageSizeStr)) {
            // 返回一个足够大的值来获取所有记录
            return Integer.MAX_VALUE;
        }
        
        // 否则尝试解析数字
        try {
            return Integer.parseInt(pageSizeStr);
        } catch (NumberFormatException e) {
            // 默认值
            return 1000;
        }
    }
}
