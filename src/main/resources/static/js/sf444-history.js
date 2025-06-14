/**
 * SF444历史数据查看页面的JavaScript
 * 实现自动刷新、分页和数据展示功能
 */
$(document).ready(function() {
    // 全局变量
    let currentPage = 0;
    let totalPages = 0;
    let pageSize = $('#pageSize').val();
    let filterType = $('#filterType').val();
    let autoRefreshEnabled = true;
    let refreshInterval = 60000; // 60秒刷新一次
    let refreshTimer = null;
    
    // 初始化页面
    loadData();
    startAutoRefresh();
    
    // 分页大小变更
    $('#pageSize').change(function() {
        pageSize = $(this).val();
        currentPage = 0; // 重置为第一页
        loadData();
    });
    
    // 过滤类型变更
    $('#filterType').change(function() {
        filterType = $(this).val();
        currentPage = 0; // 重置为第一页
        loadData();
    });
    
    // 自动刷新开关
    $('#toggleRefresh').click(function() {
        autoRefreshEnabled = !autoRefreshEnabled;
        
        if (autoRefreshEnabled) {
            $(this).text('停止');
            $('#autoRefreshStatus').removeClass('badge-danger').addClass('badge-success').text('已启用');
            startAutoRefresh();
        } else {
            $(this).text('启动');
            $('#autoRefreshStatus').removeClass('badge-success').addClass('badge-danger').text('已停用');
            stopAutoRefresh();
        }
    });
    
    /**
     * 开始自动刷新
     */
    function startAutoRefresh() {
        if (refreshTimer) {
            clearInterval(refreshTimer);
        }
        
        refreshTimer = setInterval(function() {
            loadData();
        }, refreshInterval);
    }
    
    /**
     * 停止自动刷新
     */
    function stopAutoRefresh() {
        if (refreshTimer) {
            clearInterval(refreshTimer);
            refreshTimer = null;
        }
    }
    
    /**
     * 加载数据
     */
    function loadData() {
        $.ajax({
            url: '/sf444/data',
            data: {
                page: currentPage,
                pageSize: pageSize,
                filterType: filterType
            },
            success: function(response) {
                renderData(response);
                updatePagination(response);
                updateStats(response);
                updateLastUpdateTime();
            },
            error: function(xhr, status, error) {
                console.error('加载数据失败:', error);
                $('#historyTable tbody').html('<tr><td colspan="11" class="text-center text-danger">加载数据失败，请刷新页面重试</td></tr>');
            }
        });
    }
    
    /**
     * 渲染数据表格
     */
    function renderData(data) {
        const records = data.records;
        let html = '';
        
        if (records && records.length > 0) {
            records.forEach(function(record) {
                // 根据final_result设置不同的样式
                let rowClass = '';
                if (record.finalResult === '1') {
                    rowClass = 'table-danger'; // 被杀标红
                } else if (record.finalResult === '中') {
                    rowClass = 'table-success'; // 命中标绿
                }
                
                html += `<tr class="${rowClass}">
                    <td>${record.period}</td>
                    <td>${record.numbers || ''}</td>
                    <td>${record.prediction || ''}</td>
                    <td>${record.outcome || ''}</td>
                    <td>${record.killNumber || ''}</td>
                    <td>${record.flag}</td>
                    <td>${record.finalResult || ''}</td>
                    <td>${record.percent30 || ''}</td>
                    <td>${record.percent50 || ''}</td>
                    <td>${record.percent100 || ''}</td>
                    <td>${record.openResult || ''}</td>
                </tr>`;
            });
        } else {
            html = '<tr><td colspan="11" class="text-center">没有找到记录</td></tr>';
        }
        
        $('#historyTable tbody').html(html);
    }
    
    /**
     * 更新分页控件
     */
    function updatePagination(data) {
        totalPages = data.totalPages;
        const currentDisplayPage = currentPage + 1; // 显示页码从1开始
        
        // 上方分页
        let paginationHtml = createPaginationHtml(currentDisplayPage, totalPages);
        $('#pagination').html(paginationHtml);
        
        // 下方分页（相同内容）
        $('#pagination-bottom').html(paginationHtml);
        
        // 绑定分页点击事件
        $('.page-link').click(function(e) {
            e.preventDefault();
            const targetPage = $(this).data('page');
            
            if (targetPage >= 0 && targetPage < totalPages) {
                currentPage = targetPage;
                loadData();
            }
        });
    }
    
    /**
     * 创建分页HTML
     */
    function createPaginationHtml(currentDisplayPage, totalPages) {
        let html = '';
        
        // 上一页按钮
        html += `<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${currentPage - 1}">上一页</a>
        </li>`;
        
        // 页码按钮
        const maxVisiblePages = 5;
        let startPage = Math.max(1, currentDisplayPage - Math.floor(maxVisiblePages / 2));
        let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
        
        // 调整startPage使页码数量保持在maxVisiblePages
        if (endPage - startPage + 1 < maxVisiblePages && startPage > 1) {
            startPage = Math.max(1, endPage - maxVisiblePages + 1);
        }
        
        // 添加第一页链接
        if (startPage > 1) {
            html += `<li class="page-item">
                <a class="page-link" href="#" data-page="0">1</a>
            </li>`;
            
            if (startPage > 2) {
                html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
        }
        
        // 添加中间页码
        for (let i = startPage; i <= endPage; i++) {
            html += `<li class="page-item ${i === currentDisplayPage ? 'active' : ''}">
                <a class="page-link" href="#" data-page="${i - 1}">${i}</a>
            </li>`;
        }
        
        // 添加最后页链接
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
            }
            
            html += `<li class="page-item">
                <a class="page-link" href="#" data-page="${totalPages - 1}">${totalPages}</a>
            </li>`;
        }
        
        // 下一页按钮
        html += `<li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${currentPage + 1}">下一页</a>
        </li>`;
        
        return html;
    }
    
    /**
     * 更新统计信息
     */
    function updateStats(data) {
        $('#totalRecords').text(data.totalRecords);
        $('#currentDisplayCount').text(data.records.length);
    }
    
    /**
     * 更新最后更新时间
     */
    function updateLastUpdateTime() {
        const now = new Date();
        const timeString = now.toLocaleTimeString();
        $('#lastUpdateTime').text(timeString);
    }
});
