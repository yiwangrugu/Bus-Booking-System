// 状态变量：当前是否显示已结束车次
let isShowingEndedBuses = false;

// 轮询定时器
let adminPollingTimer = null;

// 保存搜索状态
let isSearchActive = false;
let currentSearchKeyword = '';
let currentSearchType = '';

// 使用轮询代替 SSE，避免服务器崩溃
function startAdminPolling() {
    // 清除旧的定时器
    if (adminPollingTimer) {
        clearInterval(adminPollingTimer);
    }

    // 每 5 秒轮询一次
    adminPollingTimer = setInterval(() => {
        loadPendingRefunds();
    }, 5000);
}

// 公告状态检查定时器
let announcementCheckTimer = null;

// 启动公告状态检查
function startAnnouncementCheck() {
    if (announcementCheckTimer) {
        clearInterval(announcementCheckTimer);
    }

    // 每 10 秒检查一次公告状态
    announcementCheckTimer = setInterval(() => {
        loadAnnouncement();
    }, 10000);
}

// 停止轮询
function stopAdminPolling() {
    if (adminPollingTimer) {
        clearInterval(adminPollingTimer);
        adminPollingTimer = null;
    }
}

// 页面加载完成后执行
window.addEventListener('load', function () {
    // 初始化自定义弹窗
    initCustomPopups();
    // 检查用户是否已登录且是管理员
    const currentUser = sessionStorage.getItem('currentUser');
    if (!currentUser) {
        window.location.href = 'login.html';
        return;
    }

    const user = JSON.parse(currentUser);
    if (user.power !== '管理员') {
        showNotification('只有管理员才能访问此页面！', false);
        window.location.href = 'login.html';
        return;
    }

    // 绑定添加车次表单提交事件
    document.getElementById('addBusForm').addEventListener('submit', addBus);

    // 绑定车次搜索事件
    document.getElementById('bus-search-btn').addEventListener('click', searchBuses);

    // 绑定车次搜索返回按钮事件
    document.getElementById('bus-reset-btn').addEventListener('click', function () {
        // 重置搜索状态
        isSearchActive = false;
        currentSearchKeyword = '';
        currentSearchType = '';
        // 清空搜索框
        document.getElementById('bus-search').value = '';
        // 加载全部车次
        loadBusList();
    });

    // 绑定查看已结束车次按钮事件
    document.getElementById('toggle-ended-buses').addEventListener('click', toggleEndedBuses);

    // 绑定搜索类型变化事件，动态切换输入框类型
    const searchTypeSelect = document.getElementById('bus-search-type');
    const searchInput = document.getElementById('bus-search');

    // 初始设置
    function updateSearchInputType() {
        if (searchTypeSelect.value === '发车日期') {
            searchInput.type = 'date';
            searchInput.placeholder = '请选择发车日期';
        } else {
            searchInput.type = 'text';
            searchInput.placeholder = '请输入搜索内容';
        }
    }

    // 初始加载时设置
    updateSearchInputType();

    // 监听搜索类型变化
    searchTypeSelect.addEventListener('change', updateSearchInputType);

    // 绑定修改密码表单提交事件
    document.getElementById('password-form').addEventListener('submit', changePassword);

    // 绑定订单搜索事件
    document.getElementById('order-search-btn').addEventListener('click', searchOrders);

    // 绑定订单返回按钮事件
    document.getElementById('order-reset-btn').addEventListener('click', loadOrderList);

    // 绑定退订搜索事件
    document.getElementById('refund-search-btn').addEventListener('click', searchRefunds);

    // 绑定退订返回按钮事件
    document.getElementById('refund-reset-btn').addEventListener('click', loadRefundList);

    // 绑定公告内容输入事件，实时预览
    const announcementContent = document.getElementById('announcement-content');
    if (announcementContent) {
        announcementContent.addEventListener('input', updatePreview);
    }

    // 绑定公告日期变化事件，实时预览
    const announcementDate = document.getElementById('announcement-date');
    if (announcementDate) {
        announcementDate.addEventListener('change', updatePreview);
    }

    // 初始化公告日期为今天
    if (announcementDate) {
        announcementDate.value = new Date().toISOString().split('T')[0];
    }

    // 初始化公告时间显示
    updateAnnouncementDateTime();

    // 每秒更新公告日期
    setInterval(updateAnnouncementDateTime, 1000);

    // 启动轮询，每5秒自动刷新待处理退票
    startAdminPolling();

    // 启动公告状态检查
    startAnnouncementCheck();

    // 加载车次列表
    loadBusList();

    // 加载待处理退票申请，更新通知徽章
    loadPendingRefunds();

    // 初始化公告预览
    updatePreview();

    // 绑定侧边栏下拉菜单事件
    const dropdownBtns = document.querySelectorAll('.dropdown-btn');
    dropdownBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            // 切换当前下拉菜单的显示状态
            const dropdownContent = this.nextElementSibling;
            const isActive = this.classList.contains('active');

            // 关闭所有其他下拉菜单
            document.querySelectorAll('.dropdown-content').forEach(content => {
                content.classList.remove('show');
            });
            document.querySelectorAll('.dropdown-btn').forEach(b => {
                b.classList.remove('active');
            });

            // 如果当前菜单没有打开，则打开它
            if (!isActive) {
                dropdownContent.classList.add('show');
                this.classList.add('active');
            }
        });
    });

    // 点击菜单项时关闭所有下拉菜单
    const dropdownItems = document.querySelectorAll('.dropdown-item');
    dropdownItems.forEach(item => {
        item.addEventListener('click', function (event) {
            document.querySelectorAll('.dropdown-content').forEach(content => {
                content.classList.remove('show');
            });
            document.querySelectorAll('.dropdown-btn').forEach(btn => {
                btn.classList.remove('active');
            });
        });
    });

    // 点击页面其他地方关闭所有下拉菜单
    window.addEventListener('click', function (event) {
        if (!event.target.closest('.dropdown')) {
            document.querySelectorAll('.dropdown-content').forEach(content => {
                content.classList.remove('show');
            });
            document.querySelectorAll('.dropdown-btn').forEach(btn => {
                btn.classList.remove('active');
            });
        }
    });
});

// 显示指定的模块
function showSection(sectionId) {
    // 隐藏所有模块
    const sections = document.querySelectorAll('.section');
    sections.forEach(section => section.classList.remove('active'));

    // 移除所有菜单按钮的激活状态
    const menuBtns = document.querySelectorAll('.menu-btn');
    menuBtns.forEach(btn => btn.classList.remove('active'));

    // 显示选中的模块
    const selectedSection = document.getElementById(sectionId);
    selectedSection.classList.add('active');

    // 根据模块ID激活对应的下拉菜单按钮
    const dropdowns = document.querySelectorAll('.dropdown');
    dropdowns.forEach(dropdown => {
        const dropdownItems = dropdown.querySelectorAll('.dropdown-item');
        dropdownItems.forEach(item => {
            const onclickValue = item.getAttribute('onclick');
            if (onclickValue && onclickValue.includes(sectionId)) {
                const dropdownBtn = dropdown.querySelector('.dropdown-btn');
                dropdownBtn.classList.add('active');
            }
        });
    });

    // 如果是车次列表模块，根据搜索状态决定如何加载数据
    if (sectionId === 'bus-list') {
        if (isSearchActive) {
            // 如果有搜索状态，调用搜索函数
            searchBuses();
        } else {
            // 否则加载全部车次
            loadBusList();
        }
    } else if (sectionId === 'order-list') {
        // 如果是订单列表模块，重新加载数据
        loadOrderList();
    } else if (sectionId === 'refund-list') {
        // 如果是退订列表模块，重新加载数据
        loadRefundList();
    } else if (sectionId === 'daily-announcement') {
        // 如果是今日公告模块，更新今日公告预览
        updatePreview();
    } else if (sectionId === 'announcement-records') {
        // 如果是公告记录模块，加载公告记录
        loadAnnouncementRecords();
    } else if (sectionId === 'pending-tasks') {
        // 如果是待办事务模块，加载待处理退票申请
        loadPendingRefunds();
    }
}

// 切换显示已结束车次
function toggleEndedBuses() {
    isShowingEndedBuses = !isShowingEndedBuses;
    const toggleBtn = document.getElementById('toggle-ended-buses');
    toggleBtn.textContent = isShowingEndedBuses ? '查看在售车次' : '查看已结束车次';

    if (isSearchActive) {
        // 如果有搜索状态，调用搜索函数
        searchBuses();
    } else {
        // 否则加载全部车次
        loadBusList();
    }
}

// 显示指定的任务标签页
function showTaskTab(tabName) {
    // 移除所有标签页按钮的激活状态
    const tabBtns = document.querySelectorAll('.task-tabs .tab-btn');
    tabBtns.forEach(btn => btn.classList.remove('active'));

    // 隐藏所有任务列表
    const taskLists = document.querySelectorAll('.task-list');
    taskLists.forEach(list => list.classList.remove('active'));

    // 激活选中的标签页按钮
    const activeBtn = document.querySelector(`.task-tabs .tab-btn[onclick*="${tabName}"]`);
    if (activeBtn) {
        activeBtn.classList.add('active');
    }

    // 显示选中的任务列表
    const selectedList = document.getElementById(tabName);
    if (selectedList) {
        selectedList.classList.add('active');
    }

    // 根据标签页加载数据
    if (tabName === 'pending-refunds') {
        loadPendingRefunds();
    } else if (tabName === 'processed-refunds') {
        loadProcessedRefunds();
    }
}

// 加载待处理退票申请
function loadPendingRefunds() {
    fetch('/api/admin/refund-applications')
        .then(response => response.json())
        .then(data => {
            const tableBody = document.querySelector('#pending-refunds-table tbody');
            tableBody.innerHTML = '';

            if (data.length === 0) {
                const row = document.createElement('tr');
                row.innerHTML = `<td colspan="11" style="text-align: center; color: #999;">暂无待处理的退票申请</td>`;
                tableBody.appendChild(row);
                // 更新通知徽章，即使列表为空
                updateNotificationBadge(0);
                return;
            }

            data.forEach(refund => {
                const row = document.createElement('tr');

                let refundAmount = 0;
                if (refund.price && (refund.departure_date || refund.date) && (refund.departure_time || refund.time)) {
                    const departureDateTime = new Date(`${refund.departure_date || refund.date} ${refund.departure_time || refund.time}`);
                    const applyDateTime = new Date(`${refund.apply_date} ${refund.apply_time}`);
                    const timeDiff = departureDateTime - applyDateTime;
                    const hoursDiff = timeDiff / (1000 * 60 * 60);

                    let refundPercentage = 100;
                    if (hoursDiff >= 5) {
                        refundPercentage = 100;
                    } else if (hoursDiff >= 2) {
                        refundPercentage = 90;
                    } else if (hoursDiff >= 0.5) {
                        refundPercentage = 80;
                    } else if (hoursDiff >= 10 / 60) {
                        refundPercentage = 50;
                    } else {
                        refundPercentage = 0;
                    }

                    refundAmount = (refund.price * refundPercentage / 100).toFixed(2);
                }

                row.innerHTML = `
                    <td>${refund.btno}</td>
                    <td>${refund.bno}</td>
                    <td>${refund.staName || refund.start_station || '未知'}</td>
                    <td>${refund.endName || refund.end_station || '未知'}</td>
                    <td>${refund.departure_date || refund.date || '未知'}</td>
                    <td>${refund.departure_time || refund.time || '未知'}</td>
                    <td>${refund.apply_date} ${refund.apply_time}</td>
                    <td>${refund.refund_reason}</td>
                    <td>${refundAmount > 0 ? refundAmount + '元' : '-'}</td>
                    <td>
                        <button class="approve-btn" onclick="approveRefund(${refund.btno})">同意</button>
                        <button class="reject-btn" onclick="rejectRefund(${refund.btno})">拒绝</button>
                    </td>
                `;
                tableBody.appendChild(row);
            });

            // 更新通知徽章
            updateNotificationBadge(data.length);
        })
        .catch(error => {
            console.error('加载待处理退票申请失败:', error);
            showNotification('加载待处理退票申请失败，请稍后重试！', false);
        });
}

// 加载已处理退票申请
function loadProcessedRefunds() {
    fetch('/api/admin/refund-records')
        .then(response => response.json())
        .then(data => {
            const tableBody = document.querySelector('#processed-refunds-table tbody');
            tableBody.innerHTML = '';

            if (data.length === 0) {
                const row = document.createElement('tr');
                row.innerHTML = `<td colspan="12" style="text-align: center; color: #999;">暂无已处理的退票申请</td>`;
                tableBody.appendChild(row);
                return;
            }

            data.forEach(refund => {
                const row = document.createElement('tr');
                const isBusDeleted = !refund.staName && !refund.start_station && !refund.departure_date && !refund.departure_date;

                let refundAmount = 0;
                if (refund.price && (refund.departure_date || refund.date) && (refund.departure_time || refund.time)) {
                    const departureDateTime = new Date(`${refund.departure_date || refund.date} ${refund.departure_time || refund.time}`);
                    const applyDateTime = new Date(`${refund.apply_date} ${refund.apply_time}`);
                    const timeDiff = departureDateTime - applyDateTime;
                    const hoursDiff = timeDiff / (1000 * 60 * 60);

                    let refundPercentage = 100;
                    if (hoursDiff >= 5) {
                        refundPercentage = 100;
                    } else if (hoursDiff >= 2) {
                        refundPercentage = 90;
                    } else if (hoursDiff >= 0.5) {
                        refundPercentage = 80;
                    } else if (hoursDiff >= 10 / 60) {
                        refundPercentage = 50;
                    } else {
                        refundPercentage = 0;
                    }

                    refundAmount = (refund.price * refundPercentage / 100).toFixed(2);
                }

                row.innerHTML = `
                    <td>${refund.btno}</td>
                    <td>${refund.bno}</td>
                    <td>${isBusDeleted ? '已下架' : (refund.staName || refund.start_station || '未知')}</td>
                    <td>${isBusDeleted ? '已下架' : (refund.endName || refund.end_station || '未知')}</td>
                    <td>${isBusDeleted ? '已下架' : (refund.departure_date || refund.date || '未知')}</td>
                    <td>${isBusDeleted ? '已下架' : (refund.departure_time || refund.time || '未知')}</td>
                    <td>${refund.apply_date} ${refund.apply_time}</td>
                    <td>${refund.process_time || '未处理'}</td>
                    <td>${refund.refund_reason}</td>
                    <td>${refund.status === 'approved' ? '已通过' : '被拒绝'}</td>
                    <td>${refund.processed_by || '手动审批'}</td>
                    <td>${refundAmount > 0 ? refundAmount + '元' : '-'}</td>
                `;
                tableBody.appendChild(row);
            });
        })
        .catch(error => {
            console.error('加载已处理退票申请失败:', error);
            showNotification('加载已处理退票申请失败，请稍后重试！', false);
        });
}

// 同意退票申请
function approveRefund(btno) {
    showConfirmPopup('确定同意该退票申请吗？', () => {
        const currentUser = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
        const adminName = currentUser.name || '管理员';

        fetch(`/api/admin/refund-applications/approve/${btno}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({})
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showNotification('退票申请已同意！');

                    // 立即更新通知徽章：获取当前徽章计数并减1
                    const badge = document.getElementById('pending-tasks-badge') || document.querySelector('.notification-badge');
                    if (badge && badge.style.display !== 'none') {
                        const currentCount = parseInt(badge.textContent) || 0;
                        const newCount = Math.max(0, currentCount - 1);
                        updateNotificationBadge(newCount);
                    }

                    // 重新加载待处理列表以确保数据准确性
                    loadPendingRefunds();
                    if (document.getElementById('processed-refunds').classList.contains('active')) {
                        loadProcessedRefunds(); // 如果当前显示已处理列表，也重新加载
                    }
                } else {
                    showNotification('操作失败：' + data.message, false);
                }
            })
            .catch(error => {
                console.error('同意退票申请失败:', error);
                showNotification('操作失败，请稍后重试！', false);
            });
    });
}

// 拒绝退票申请
function rejectRefund(btno) {
    showRejectPopup((rejectReason) => {
        showConfirmPopup('确定拒绝该退票申请吗？', () => {
            const currentUser = JSON.parse(sessionStorage.getItem('currentUser') || '{}');
            const adminName = currentUser.name || '管理员';

            fetch(`/api/admin/refund-applications/reject/${btno}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    reject_reason: rejectReason
                })
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        showNotification('退票申请已拒绝！');

                        // 立即更新通知徽章：获取当前徽章计数并减1
                        const badge = document.getElementById('pending-tasks-badge') || document.querySelector('.notification-badge');
                        if (badge && badge.style.display !== 'none') {
                            const currentCount = parseInt(badge.textContent) || 0;
                            const newCount = Math.max(0, currentCount - 1);
                            updateNotificationBadge(newCount);
                        }

                        // 重新加载待处理列表以确保数据准确性
                        loadPendingRefunds();
                        if (document.getElementById('processed-refunds').classList.contains('active')) {
                            loadProcessedRefunds(); // 如果当前显示已处理列表，也重新加载
                        }
                    } else {
                        showNotification('操作失败：' + data.message, false);
                    }
                })
                .catch(error => {
                    console.error('拒绝退票申请失败:', error);
                    showNotification('操作失败，请稍后重试！', false);
                });
        });
    });
}

// 更新通知徽章
function updateNotificationBadge(count) {
    // 优先使用ID选择器查找徽章（HTML中已定义）
    let badge = document.getElementById('pending-tasks-badge');
    const menuBtn = document.querySelector('.menu-btn[onclick*="pending-tasks"]');

    // 如果通过ID没找到，再使用类选择器
    if (!badge) {
        badge = document.querySelector('.notification-badge');
    }

    if (badge) {
        if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.style.display = 'flex';
        } else {
            badge.style.display = 'none';
        }
    } else if (count > 0 && menuBtn) {
        // 如果徽章不存在但需要显示，则创建徽章
        const newBadge = document.createElement('div');
        newBadge.className = 'notification-badge';
        newBadge.id = 'pending-tasks-badge'; // 设置ID，方便后续查找
        newBadge.textContent = count > 99 ? '99+' : count;
        menuBtn.style.position = 'relative';
        menuBtn.appendChild(newBadge);
    }
}

// 初始化自定义弹窗
function initCustomPopups() {
    // 创建拒绝退票弹窗（确认弹窗已经在HTML中定义）
    const rejectPopup = document.createElement('div');
    rejectPopup.className = 'reject-popup';
    rejectPopup.id = 'reject-popup';
    rejectPopup.innerHTML = `
        <div class="reject-content">
            <p>请输入拒绝原因：</p>
            <textarea class="reject-input" id="reject-reason" placeholder="请输入拒绝原因..."></textarea>
            <div class="reject-buttons">
                <button id="reject-submit">提交</button>
                <button id="reject-cancel">取消</button>
            </div>
        </div>
    `;

    // 添加拒绝弹窗到页面
    document.body.appendChild(rejectPopup);

    // 绑定拒绝弹窗事件
    document.getElementById('reject-cancel').addEventListener('click', () => {
        document.getElementById('reject-popup').style.display = 'none';
        document.getElementById('reject-reason').value = '';
    });

    // 绑定确认弹窗事件
    document.getElementById('confirm-cancel').addEventListener('click', () => {
        document.getElementById('confirm-popup').style.display = 'none';
    });

    // 点击弹窗外部关闭
    window.addEventListener('click', (e) => {
        const confirmPopupEl = document.getElementById('confirm-popup');
        const rejectPopupEl = document.getElementById('reject-popup');

        if (e.target === confirmPopupEl) {
            confirmPopupEl.style.display = 'none';
        }

        if (e.target === rejectPopupEl) {
            rejectPopupEl.style.display = 'none';
            document.getElementById('reject-reason').value = '';
        }
    });
}

// 显示确认弹窗
function showConfirmPopup(message, onConfirm) {
    const popup = document.getElementById('confirm-popup');
    const messageEl = document.getElementById('confirm-text');
    const okBtn = document.getElementById('confirm-ok');
    const cancelBtn = document.getElementById('confirm-cancel');

    messageEl.textContent = message;
    popup.style.display = 'flex';

    // 重新绑定确认事件
    okBtn.onclick = () => {
        popup.style.display = 'none';
        onConfirm();
    };

    // 重新绑定取消事件
    cancelBtn.onclick = () => {
        popup.style.display = 'none';
    };
}

// 显示拒绝退票弹窗
function showRejectPopup(onSubmit) {
    const popup = document.getElementById('reject-popup');
    const reasonInput = document.getElementById('reject-reason');
    const submitBtn = document.getElementById('reject-submit');

    popup.style.display = 'flex';
    reasonInput.focus();

    // 重新绑定提交事件
    submitBtn.onclick = () => {
        const reason = reasonInput.value.trim();
        if (reason) {
            popup.style.display = 'none';
            onSubmit(reason);
            reasonInput.value = '';
        } else {
            showNotification('请输入拒绝原因！', false);
        }
    };

    // 回车键提交
    reasonInput.onkeypress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            const reason = reasonInput.value.trim();
            if (reason) {
                popup.style.display = 'none';
                onSubmit(reason);
                reasonInput.value = '';
            } else {
                showNotification('请输入拒绝原因！', false);
            }
        }
    };
}



// 添加车次
function addBus(event) {
    event.preventDefault();

    // 获取表单数据
    const formData = new FormData(event.target);
    const busData = {
        startStation: formData.get('startStation'),
        endStation: formData.get('endStation'),
        departureDate: formData.get('departureDate'),
        departureTime: formData.get('departureTime'),
        totalSeats: parseInt(formData.get('totalSeats')),
        remainSeats: parseInt(formData.get('totalSeats')), // 初始剩余座位等于总座位
        price: parseFloat(formData.get('price'))
    };

    fetch('/api/buses', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(busData)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('车次添加成功！');
                event.target.reset(); // 重置表单
                loadBusList(); // 更新车次列表
            } else {
                showNotification('车次添加失败：' + data.message, false);
            }
        })
        .catch(error => {
            console.error('添加车次失败:', error);
            showNotification('添加车次失败，请稍后重试！', false);
        });
}

// 加载车次列表
function loadBusList() {
    const url = isShowingEndedBuses ? '/api/buses/ended' : '/api/buses';
    fetch(url)
        .then(response => response.json())
        .then(data => {
            const busTable = document.querySelector('#bus-table tbody');
            busTable.innerHTML = '';

            if (data.length === 0) {
                const row = document.createElement('tr');
                row.innerHTML = `<td colspan="9" style="text-align: center; color: #999; padding: 20px;">暂无车次数据</td>`;
                busTable.appendChild(row);
                return;
            }

            data.forEach(bus => {
                const row = document.createElement('tr');
                let actions = '';
                if (isShowingEndedBuses) {
                    // 已结束车次显示再上架按钮
                    actions = `<button class="relist-btn" onclick="relistBus(${JSON.stringify(bus).replace(/"/g, '&quot;')})">再上架</button>`;
                } else {
                    // 在售车次显示编辑和删除按钮
                    actions = `
                        <button class="edit-btn" onclick="editBus(${JSON.stringify(bus).replace(/"/g, '&quot;')})">编辑</button>
                        <button class="delete-btn" onclick="deleteBus(${bus.bno})">删除</button>
                    `;
                }
                row.innerHTML = `
                    <td>${bus.bno}</td>
                    <td>${bus.staName}</td>
                    <td>${bus.endName}</td>
                    <td>${bus.date}</td>
                    <td>${bus.time}</td>
                    <td>${bus.totalSeats}</td>
                    <td>${bus.remainSeats}</td>
                    <td>${bus.price}</td>
                    <td>${actions}</td>
                `;
                busTable.appendChild(row);
            });
        })
        .catch(error => {
            console.error('加载车次列表失败:', error);
            showNotification('加载车次列表失败，请稍后重试！', false);
        });
}



// 搜索车次
function searchBuses() {
    const keyword = document.getElementById('bus-search').value;
    const searchType = document.getElementById('bus-search-type').value;

    if (!keyword) {
        // 如果搜索框为空，取消搜索状态
        isSearchActive = false;
        currentSearchKeyword = '';
        currentSearchType = '';
        loadBusList();
        return;
    }

    // 保存搜索状态
    isSearchActive = true;
    currentSearchKeyword = keyword;
    currentSearchType = searchType;

    const url = isShowingEndedBuses ? `/api/buses/ended?keyword=${keyword}&type=${searchType}` : `/api/buses?keyword=${keyword}&type=${searchType}`;

    fetch(url)
        .then(response => response.json())
        .then(data => {
            const busTable = document.querySelector('#bus-table tbody');
            busTable.innerHTML = '';

            data.forEach(bus => {
                const row = document.createElement('tr');
                let actions = '';
                if (isShowingEndedBuses) {
                    // 已结束车次显示再上架按钮
                    actions = `<button class="relist-btn" onclick="relistBus(${JSON.stringify(bus).replace(/"/g, '&quot;')})">再上架</button>`;
                } else {
                    // 在售车次显示编辑和删除按钮
                    actions = `
                        <button class="edit-btn" onclick="editBus(${JSON.stringify(bus).replace(/"/g, '&quot;')})">编辑</button>
                        <button class="delete-btn" onclick="deleteBus(${bus.bno})">删除</button>
                    `;
                }
                row.innerHTML = `
                    <td>${bus.bno}</td>
                    <td>${bus.staName}</td>
                    <td>${bus.endName}</td>
                    <td>${bus.date}</td>
                    <td>${bus.time}</td>
                    <td>${bus.totalSeats}</td>
                    <td>${bus.remainSeats}</td>
                    <td>${bus.price}</td>
                    <td>${actions}</td>
                `;
                busTable.appendChild(row);
            });
        })
        .catch(error => {
            console.error('搜索车次失败:', error);
            showNotification('搜索车次失败，请稍后重试！', false);
        });
}

// 编辑车次
function editBus(bus) {
    // 检查车次是否已售出
    if (bus.totalSeats !== bus.remainSeats) {
        showNotification('该车次已有售出记录，无法编辑！', false);
        return;
    }

    // 填充表单数据
    document.getElementById('edit-bno').value = bus.bno;
    document.getElementById('edit-start-station').value = bus.staName;
    document.getElementById('edit-end-station').value = bus.endName;
    document.getElementById('edit-departure-date').value = bus.date;
    document.getElementById('edit-departure-time').value = bus.time;
    document.getElementById('edit-total-seats').value = bus.totalSeats;
    document.getElementById('edit-price').value = bus.price;

    // 显示模态框
    const modal = document.getElementById('edit-bus-modal');
    modal.style.display = 'block';

    // 保存当前编辑的车次信息
    modal.dataset.busId = bus.bno;
}

// 关闭模态框
function closeModal() {
    const modal = document.getElementById('edit-bus-modal');
    modal.style.display = 'none';
}

// 保存编辑的车次信息
function saveBusEdit() {
    const modal = document.getElementById('edit-bus-modal');
    const busId = modal.dataset.busId;

    // 获取表单数据
    const formData = new FormData(document.getElementById('editBusForm'));
    const busData = {
        bno: formData.get('bno'),
        startStation: formData.get('startStation'),
        endStation: formData.get('endStation'),
        departureDate: formData.get('departureDate'),
        departureTime: formData.get('departureTime'),
        totalSeats: parseInt(formData.get('totalSeats')),
        price: parseFloat(formData.get('price'))
    };

    // 时间验证：如果选择的时间早于当前时间，显示错误提示
    const selectedTime = new Date(`${busData.departureDate}T${busData.departureTime}`);
    const currentTime = new Date();
    if (selectedTime < currentTime) {
        showNotification('发车时间不能早于当前时间！', false);
        return;
    }

    fetch(`/api/buses/${busId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(busData)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('车次编辑成功！');
                closeModal();
                loadBusList(); // 更新车次列表
            } else {
                showNotification('车次编辑失败：' + data.message, false);
            }
        })
        .catch(error => {
            console.error('编辑车次失败:', error);
            showNotification('编辑车次失败，请稍后重试！', false);
        });
}

// 添加模态框事件监听
document.addEventListener('DOMContentLoaded', function () {
    // 关闭按钮事件
    const closeBtn = document.querySelector('.close');
    closeBtn.addEventListener('click', closeModal);

    // 取消按钮事件
    const cancelBtn = document.querySelector('.cancel-btn');
    cancelBtn.addEventListener('click', closeModal);

    // 点击模态框外部关闭
    window.addEventListener('click', function (event) {
        const modal = document.getElementById('edit-bus-modal');
        if (event.target === modal) {
            closeModal();
        }
    });

    // 表单提交事件
    const editForm = document.getElementById('editBusForm');
    editForm.addEventListener('submit', function (event) {
        event.preventDefault();
        saveBusEdit();
    });
});

// 删除车次
function deleteBus(busNo) {
    showConfirmPopup('确定要删除这个车次吗？', () => {
        fetch(`/api/buses/${busNo}`, {
            method: 'DELETE'
        })
            .then(response => response.json())
            .then(data => {
                showNotification(data.success ? '车次删除成功！' : '车次删除失败：' + data.message, data.success);
                if (data.success) loadBusList();
            })
            .catch(error => {
                console.error('删除车次失败:', error);
                showNotification('删除车次失败，请稍后重试！', false);
            });
    });
}

// 再上架车次
function relistBus(bus) {
    // 创建模态框
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-header">
                <h2>再上架车次</h2>
                <span class="close">&times;</span>
            </div>
            <div class="modal-body">
                <form id="relistBusForm" class="bus-form">
                    <div class="form-row">
                        <div class="form-group">
                            <label for="relist-date">新的发车日期：</label>
                            <input type="date" id="relist-date" name="departureDate" required>
                        </div>
                        <div class="form-group">
                            <label for="relist-time">新的发车时间：</label>
                            <input type="time" id="relist-time" name="departureTime" required>
                        </div>
                    </div>
                    <div class="form-actions">
                        <button type="submit">确认再上架</button>
                        <button type="button" class="cancel-btn">取消</button>
                    </div>
                </form>
            </div>
        </div>
    `;
    document.body.appendChild(modal);

    // 设置模态框显示
    modal.style.display = 'block';

    // 设置最小日期为今天
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('relist-date').min = today;

    // 关闭模态框
    const closeBtn = modal.querySelector('.close');
    const cancelBtn = modal.querySelector('.cancel-btn');
    const closeModal = () => {
        document.body.removeChild(modal);
    };
    closeBtn.addEventListener('click', closeModal);
    cancelBtn.addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    // 绑定表单提交事件
    document.getElementById('relistBusForm').addEventListener('submit', (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);

        // 获取用户选择的日期和时间
        const departureDate = formData.get('departureDate');
        const departureTime = formData.get('departureTime');

        // 构建完整的时间对象进行比较
        const selectedTime = new Date(`${departureDate}T${departureTime}`);
        const currentTime = new Date();

        // 时间验证：如果选择的时间早于当前时间，显示错误提示
        if (selectedTime < currentTime) {
            showNotification('发车时间不能早于当前时间！', false);
            return;
        }

        const newData = {
            startStation: bus.staName,
            endStation: bus.endName,
            departureDate: departureDate,
            departureTime: departureTime,
            totalSeats: bus.totalSeats,
            price: bus.price
        };

        // 发送请求添加新车次
        fetch('/api/buses', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(newData)
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showNotification('车次再上架成功！');
                    closeModal();
                } else {
                    showNotification('车次再上架失败：' + data.message, false);
                }
            })
            .catch(error => {
                console.error('再上架车次失败:', error);
                showNotification('再上架车次失败，请稍后重试！', false);
            });
    });
}

// 退出登录
function logout() {
    showConfirmPopup('确定要退出登录吗？', () => {
        sessionStorage.removeItem('currentUser');
        window.location.href = 'login.html';
    });
}

// 加载公告记录
function loadAnnouncementRecords(searchDate = '') {
    let url = '/api/announcement-records';
    if (searchDate) {
        url += `?date=${searchDate}`;
    }

    fetch(url)
        .then(response => response.json())
        .then(data => {
            const recordsTable = document.querySelector('#announcement-records-table tbody');
            const recordsCount = document.getElementById('records-count');

            recordsTable.innerHTML = '';
            recordsCount.textContent = data.length;

            if (data.length === 0) {
                const row = document.createElement('tr');
                row.innerHTML = `<td colspan="5" style="text-align: center; color: #999; padding: 20px;">暂无公告记录数据</td>`;
                recordsTable.appendChild(row);
                return;
            }

            // 按发布时间降序排序（后端已排序，这里确保顺序正确）
            // 生成从1开始的顺序编号
            data.forEach((record, index) => {
                const row = document.createElement('tr');

                // 格式化发布时间，只显示到秒
                const publishTime = formatPublishTime(record.publish_time);

                // 使用从1开始的顺序编号
                const sequenceNumber = index + 1;

                // 显示发布状态
                const publishedStatus = record.published ? '<span style="color: green;">已发布</span>' : '<span style="color: red;">已结束</span>';

                row.innerHTML = `
                    <td>${sequenceNumber}</td>
                    <td>${record.announcement_date}</td>
                    <td>${publishTime}</td>
                    <td>${publishedStatus}</td>
                    <td><button class="view-details-btn" onclick="viewAnnouncementDetails(${record.id}, '${record.content.replace(/'/g, "\\'")}', ${record.published})">详细信息</button></td>
                `;
                recordsTable.appendChild(row);
            });
        })
        .catch(error => {
            console.error('加载公告记录失败:', error);
            showNotification('加载公告记录失败，请稍后重试！', false);
        });
}

// 搜索公告记录
function searchAnnouncementRecords() {
    const searchDate = document.getElementById('records-search-date').value;
    loadAnnouncementRecords(searchDate);
    loadAnnouncement();
}

// 清空搜索条件
function clearRecordsSearch() {
    document.getElementById('records-search-date').value = '';
    loadAnnouncementRecords();
    loadAnnouncement();
}

// 格式化发布时间，只显示到秒
function formatPublishTime(publishTime) {
    if (!publishTime) return '';

    // 移除毫秒部分
    const timeWithoutMs = publishTime.replace(/\.\d+$/, '');

    // 如果是完整的日期时间格式，直接返回
    if (timeWithoutMs.includes(' ')) {
        return timeWithoutMs;
    }

    return publishTime;
}

// 查看公告详细信息
function viewAnnouncementDetails(id, content, published) {
    const confirmPopup = document.getElementById('confirm-popup');
    const confirmText = document.getElementById('confirm-text');
    const confirmBtn = document.getElementById('confirm-ok');
    const cancelBtn = document.getElementById('confirm-cancel');

    confirmText.innerHTML = `
                <div style="text-align: left; max-width: 500px;">
                    <div style="background-color: #00bcd4; color: white; padding: 20px; border-radius: 8px 8px 0 0; margin: -20px -20px 20px -20px;">
                        <h3 style="margin: 0; font-size: 18px; font-weight: 600;">📋 公告详细信息</h3>
                    </div>
                    
                    <div style="margin-bottom: 20px;">
                        <div style="display: flex; align-items: center; margin-bottom: 10px;">
                            <span style="font-weight: 600; color: #333; min-width: 80px;">编号：</span>
                            <span style="background: #00bcd4; color: white; padding: 4px 12px; border-radius: 12px; font-size: 14px; font-weight: 500;">#${id}</span>
                        </div>
                
                <div style="display: flex; align-items: center; margin-bottom: 15px;">
                    <span style="font-weight: 600; color: #333; min-width: 80px;">公告内容：</span>
                    <span style="color: #666; font-size: 14px;">${content.length > 50 ? content.substring(0, 50) + '...' : content}</span>
                </div>
            </div>
            
            <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #00bcd4;">
                <h4 style="margin: 0 0 10px 0; color: #333; font-size: 15px;">📝 完整内容：</h4>
                <div style="background: white; padding: 15px; border-radius: 4px; border: 1px solid #e0e0e0; max-height: 200px; overflow-y: auto; line-height: 1.6; font-size: 14px; color: #555;">
                    ${content.replace(/\n/g, '<br>')}
                </div>
            </div>
            
            <div style="margin-top: 15px; text-align: center; color: #888; font-size: 12px;">
                💡 点击下方按钮关闭此窗口
            </div>
        </div>
    `;

    if (published) {
        confirmBtn.textContent = '取消发布';
        confirmBtn.style.background = '#f44336';
    } else {
        confirmBtn.textContent = '再次发布';
        confirmBtn.style.background = '#4CAF50';
    }
    confirmBtn.style.color = 'white';
    confirmBtn.style.border = 'none';
    confirmBtn.style.padding = '10px 24px';
    confirmBtn.style.borderRadius = '20px';
    confirmBtn.style.cursor = 'pointer';
    confirmBtn.style.marginRight = '10px';

    cancelBtn.textContent = '关闭';
    cancelBtn.style.background = '#00bcd4';
    cancelBtn.style.color = 'white';
    cancelBtn.style.border = 'none';
    cancelBtn.style.padding = '10px 24px';
    cancelBtn.style.borderRadius = '20px';
    cancelBtn.style.cursor = 'pointer';
    cancelBtn.style.display = 'inline-block';

    confirmPopup.style.display = 'flex';

    confirmBtn.onclick = () => {
        if (published) {
            cancelAnnouncement();
        } else {
            republishAnnouncement(id);
        }
    };

    cancelBtn.onclick = () => {
        confirmPopup.style.display = 'none';
    };
}

// 再次发布公告
function republishAnnouncement(id) {
    fetch('/api/republish-announcement', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            id: id
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('公告重新发布成功！');
                document.getElementById('confirm-popup').style.display = 'none';
                updatePreview();
                loadAnnouncementRecords();
            } else {
                showNotification('重新发布公告失败：' + data.message, false);
            }
        })
        .catch(error => {
            console.error('重新发布公告失败:', error);
            showNotification('重新发布公告失败，请稍后重试！', false);
        });
}
confirmBtn.style.fontSize = '14px';
confirmBtn.style.fontWeight = '500';
confirmBtn.style.transition = 'all 0.3s ease';

cancelBtn.style.display = 'none';

confirmPopup.style.display = 'flex';

confirmBtn.onclick = () => {
    confirmPopup.style.display = 'none';
    // 恢复按钮原始样式
    confirmBtn.textContent = originalBtnText;
    confirmBtn.style.background = originalBtnBackground;
    confirmBtn.style.color = originalBtnColor;
    confirmBtn.style.border = '';
    confirmBtn.style.padding = '';
    confirmBtn.style.borderRadius = '';
    confirmBtn.style.fontSize = '';
    confirmBtn.style.fontWeight = '';
    cancelBtn.style.display = originalCancelDisplay;
};

// 修改密码
function changePassword(event) {
    event.preventDefault();

    const oldPassword = document.getElementById('old-password').value;
    const newPassword = document.getElementById('new-password').value;
    const confirmPassword = document.getElementById('confirm-password').value;

    if (newPassword !== confirmPassword) {
        showNotification('两次输入的新密码不一致！', false);
        return;
    }

    const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));

    const passwordData = {
        userName: currentUser.userName,
        oldPassword: oldPassword,
        newPassword: newPassword,
        power: currentUser.power
    };

    fetch('/api/change-password', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(passwordData)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('密码修改成功！');
                document.getElementById('password-form').reset();
            } else {
                showNotification('密码修改失败：' + data.message, false);
            }
        })
        .catch(error => {
            console.error('修改密码失败:', error);
            showNotification('修改密码失败，请稍后重试！', false);
        });
}

// 今日公告功能
// 发布公告
function publishAnnouncement() {
    const content = document.getElementById('announcement-content').value;
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const today = `${year}-${month}-${day}`;

    // 验证内容是否为空
    if (!content.trim()) {
        showNotification('公告内容不能为空！', false);
        return;
    }

    // 发布到服务器
    fetch('/api/publish-announcement', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            content: content.trim(),
            announcement_date: today
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('公告发布成功！');
                updatePreview();

                // 如果当前显示的是公告记录模块，自动刷新列表
                const currentSection = document.querySelector('.section.active');
                if (currentSection && currentSection.id === 'announcement-records') {
                    loadAnnouncementRecords();
                }
            } else {
                showNotification('公告发布失败：' + data.message, false);
            }
        })
        .catch(error => {
            console.error('发布公告失败:', error);
            showNotification('公告发布失败，请稍后重试！', false);
        });
}

// 取消发布公告
function cancelAnnouncement() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const today = `${year}-${month}-${day}`;

    // 调用后端API清空今天的公告
    fetch('/api/cancel-announcement', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            announcement_date: today
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // 从localStorage中移除公告数据
                localStorage.removeItem('dailyAnnouncement');
                // 不清空表单内容，保持输入框内容不变
                updatePreview();
                showNotification('公告已取消发布！用户端将不再显示。');
            } else {
                showNotification('取消公告失败：' + data.message, false);
            }
        })
        .catch(error => {
            console.error('取消公告失败:', error);
            showNotification('取消公告失败，请稍后重试！', false);
        });
}

// 更新公告日期
function updateAnnouncementDateTime() {
    const now = new Date();
    const dateInput = document.getElementById('announcement-date');

    if (dateInput) {
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        dateInput.value = `${year}-${month}-${day}`;
    }
}

// 更新公告预览
function updatePreview() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const today = `${year}-${month}-${day}`;
    const previewContent = document.getElementById('announcement-preview-content');

    fetch(`/api/announcement-records?date=${today}`)
        .then(response => response.json())
        .then(data => {
            let hasPublishedAnnouncement = false;
            let publishedContent = '';

            for (let i = 0; i < data.length; i++) {
                const record = data[i];
                if (record.published && record.content && record.content.trim() !== '') {
                    hasPublishedAnnouncement = true;
                    publishedContent = record.content;
                    break;
                }
            }

            if (hasPublishedAnnouncement) {
                previewContent.innerHTML = `
                    <p><strong>日期：</strong>${today}</p>
                    <p><strong>内容：</strong>${publishedContent.replace(/\n/g, '<br>')}</p>
                `;
            } else {
                previewContent.innerHTML = '<p style="color: #999;">今日暂无公告</p>';
            }
        })
        .catch(error => {
            console.error('加载公告数据失败:', error);
            previewContent.innerHTML = '<p style="color: #999;">今日暂无公告</p>';
        });
}

// 加载已保存的公告
function loadAnnouncement() {
    updatePreview();
}

// 加载所有订单列表
function loadOrderList() {
    fetch('/api/orders/all')
        .then(response => response.json())
        .then(data => {
            const orderTable = document.querySelector('#order-table tbody');
            orderTable.innerHTML = '';

            if (data.length === 0) {
                const row = document.createElement('tr');
                row.innerHTML = `<td colspan="12" style="text-align: center; color: #999; padding: 20px;">暂无订单数据</td>`;
                orderTable.appendChild(row);
                return;
            }

            data.forEach(order => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${order.orderId}</td>
                    <td>${order.busId}</td>
                    <td>${order.staName}</td>
                    <td>${order.endName}</td>
                    <td>${order.date}</td>
                    <td>${order.time}</td>
                    <td>${order.price}</td>
                    <td>${order.bookDate}</td>
                    <td>${order.bookTime}</td>
                    <td>${order.passengerName || '-'}</td>
                    <td>${order.passengerPhone || '-'}</td>
                `;
                orderTable.appendChild(row);
            });
        })
        .catch(error => {
            console.error('加载订单列表失败:', error);
            showNotification('加载订单列表失败，请稍后重试！', false);
        });
}

// 处理订单搜索类型改变事件
function handleOrderSearchTypeChange() {
    const searchType = document.getElementById('order-search-type').value;
    const searchInput = document.getElementById('order-search');

    if (searchType === '发车日期') {
        searchInput.type = 'date';
        searchInput.placeholder = '请选择发车日期';
    } else {
        searchInput.type = 'text';
        searchInput.placeholder = '请输入搜索内容';
    }

    searchInput.value = '';
}

// 处理退订搜索类型改变事件
function handleRefundSearchTypeChange() {
    const searchType = document.getElementById('refund-search-type').value;
    const searchInput = document.getElementById('refund-search');

    if (searchType === '发车日期' || searchType === '退订日期') {
        searchInput.type = 'date';
        searchInput.placeholder = '请选择日期';
    } else {
        searchInput.type = 'text';
        searchInput.placeholder = '请输入搜索内容';
    }

    searchInput.value = '';
}

// 搜索订单
function searchOrders() {
    const searchType = document.getElementById('order-search-type').value;
    const searchInput = document.getElementById('order-search').value;

    if (!searchInput) {
        loadOrderList();
        return;
    }

    fetch(`/api/orders/search?keyword=${searchInput}&type=${searchType}`)
        .then(response => response.json())
        .then(data => {
            const orderTable = document.querySelector('#order-table tbody');
            orderTable.innerHTML = '';

            if (data.length === 0) {
                const row = document.createElement('tr');
                row.innerHTML = `<td colspan="12" style="text-align: center; color: #999; padding: 20px;">没有找到匹配的订单数据</td>`;
                orderTable.appendChild(row);
                return;
            }

            data.forEach(order => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${order.orderId}</td>
                    <td>${order.busId}</td>
                    <td>${order.staName}</td>
                    <td>${order.endName}</td>
                    <td>${order.date}</td>
                    <td>${order.time}</td>
                    <td>${order.price}</td>
                    <td>${order.bookDate}</td>
                    <td>${order.bookTime}</td>
                    <td>${order.passengerName || '-'}</td>
                    <td>${order.passengerPhone || '-'}</td>
                `;
                orderTable.appendChild(row);
            });
        })
        .catch(error => {
            console.error('搜索订单失败:', error);
            showNotification('搜索订单失败，请稍后重试！', false);
        });
}

// 搜索退订记录
function searchRefunds() {
    const searchType = document.getElementById('refund-search-type').value;
    const searchInput = document.getElementById('refund-search').value;

    if (!searchInput) {
        loadRefundList();
        return;
    }

    fetch(`/api/refunds/search?keyword=${searchInput}&type=${searchType}`)
        .then(response => response.json())
        .then(data => {
            const refundTable = document.querySelector('#refund-table tbody');
            refundTable.innerHTML = '';

            if (data.length === 0) {
                const row = document.createElement('tr');
                row.innerHTML = `<td colspan="9" style="text-align: center; color: #999; padding: 20px;">没有找到匹配的退订记录数据</td>`;
                refundTable.appendChild(row);
                return;
            }

            data.forEach(refund => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${refund.btno}</td>
                    <td>${refund.bno}</td>
                    <td>${refund.staName}</td>
                    <td>${refund.endName}</td>
                    <td>${refund.date}</td>
                    <td>${refund.rdate}</td>
                    <td>${refund.passengerName}</td>
                    <td>${refund.passengerPhone}</td>
                    <td>${refund.price}元</td>
                `;
                refundTable.appendChild(row);
            });
        })
        .catch(error => {
            console.error('搜索退订记录失败:', error);
            showNotification('搜索退订记录失败，请稍后重试！', false);
        });
}

// 加载退订列表
function loadRefundList() {
    fetch('/api/refunds/all')
        .then(response => response.json())
        .then(data => {
            const refundTable = document.querySelector('#refund-table tbody');
            refundTable.innerHTML = '';

            if (data.length === 0) {
                const row = document.createElement('tr');
                row.innerHTML = `<td colspan="9" style="text-align: center; color: #999; padding: 20px;">暂无退订记录数据</td>`;
                refundTable.appendChild(row);
                return;
            }

            data.forEach(refund => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${refund.btno}</td>
                    <td>${refund.bno}</td>
                    <td>${refund.staName}</td>
                    <td>${refund.endName}</td>
                    <td>${refund.date}</td>
                    <td>${refund.rdate}</td>
                    <td>${refund.passengerName}</td>
                    <td>${refund.passengerPhone}</td>
                    <td>${refund.price}元</td>
                `;
                refundTable.appendChild(row);
            });
        })
        .catch(error => {
            console.error('加载退订列表失败:', error);
            showNotification('加载退订列表失败，请稍后重试！', false);
        });
}

// 初始化管理员SSE连接
let adminSSEConnection = null;

function initAdminSSE() {
    // 如果已经有连接，先关闭
    if (adminSSEConnection) {
        adminSSEConnection.close();
        adminSSEConnection = null;
    }

    // 建立新的SSE连接
    adminSSEConnection = new EventSource('/api/sse/admin');

    adminSSEConnection.onmessage = function (event) {
        // 当收到新退票申请通知时，刷新待处理退票列表并显示提示
        if (event.data === 'new_refund_application') {
            loadPendingRefunds();
            showNotification('有新的退票申请需要处理！', true);
        }
    };

    adminSSEConnection.onerror = function (error) {
        // 关闭连接
        if (adminSSEConnection) {
            adminSSEConnection.close();
            adminSSEConnection = null;
        }
        // 5秒后尝试重新连接
        setTimeout(() => {
            initAdminSSE();
        }, 5000);
    };
}

// 页面卸载时关闭管理员SSE连接
window.addEventListener('beforeunload', function () {
    if (adminSSEConnection) {
        adminSSEConnection.close();
        adminSSEConnection = null;
    }
});



