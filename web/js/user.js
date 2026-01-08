// 当前选中的车次信息
let selectedBus = null;

// 是否显示"我的出行"模式（只显示未过发车时间的订单）
let showMyTripsMode = false;

// 轮询定时器
let pollingTimer = null;

// 保存搜索状态
let isSearchActive = false;
let currentSearchKeyword = '';
let currentSearchType = '';

// 使用轮询代替 SSE，避免服务器崩溃
function startPolling() {
    // 清除旧的定时器
    if (pollingTimer) {
        clearInterval(pollingTimer);
    }

    // 每 5 秒轮询一次
    pollingTimer = setInterval(() => {
        // 检查公告状态
        loadAnnouncement();

        // 根据搜索状态决定如何获取车次数据
        if (isSearchActive) {
            // 如果有搜索状态，使用保存的搜索参数
            fetch(`/api/buses?keyword=${currentSearchKeyword}&type=${currentSearchType}`)
                .then(response => response.json())
                .then(data => {
                    const busTable = document.querySelector('#bus-table tbody');
                    busTable.innerHTML = '';

                    if (data.length === 0) {
                        const emptyRow = document.createElement('tr');
                        emptyRow.innerHTML = '<td colspan="8" style="text-align: center; color: #999; padding: 20px;">没有找到匹配的车次数据</td>';
                        busTable.appendChild(emptyRow);
                        return;
                    }

                    data.forEach(bus => {
                        const row = document.createElement('tr');
                        row.innerHTML = `
                            <td>${bus.bno}</td>
                            <td>${bus.staName}</td>
                            <td>${bus.endName}</td>
                            <td>${bus.date}</td>
                            <td>${bus.time}</td>
                            <td>${bus.remainSeats}</td>
                            <td>${bus.price}</td>
                            <td><button class="book-btn" onclick="selectBus(${JSON.stringify(bus).replace(/"/g, '&quot;')})">购买</button></td>
                        `;
                        busTable.appendChild(row);
                    });
                })
                .catch(error => {
                    console.error('加载车次数据失败:', error);
                    showNotification('加载车次数据失败，请稍后重试！', false);
                });
        } else {
            // 如果没有搜索状态，加载全部车次
            loadBuses();
        }
        loadOrders();
        loadMyRefunds();
    }, 5000);
}

// 停止轮询
function stopPolling() {
    if (pollingTimer) {
        clearInterval(pollingTimer);
        pollingTimer = null;
    }
}

// 页面加载完成后执行
window.addEventListener('load', function () {
    // 初始化弹窗
    initPopups();

    // 检查用户是否已登录
    const currentUser = sessionStorage.getItem('currentUser');

    if (!currentUser) {
        window.location.href = 'login.html';
        return;
    }

    const userObj = JSON.parse(currentUser);

    // 初始化页面
    loadBuses();
    loadOrders();
    loadPassengers();
    loadMyRefunds(); // 加载我的退票申请

    // 加载今日公告（页面加载时显示弹窗）
    loadAnnouncement(true);

    // 启动轮询，每5秒自动刷新数据
    startPolling();

    // 绑定搜索按钮事件
    document.getElementById('search-btn').addEventListener('click', searchBuses);

    // 绑定返回全部按钮事件
    document.getElementById('reset-btn').addEventListener('click', function () {
        // 重置搜索状态
        isSearchActive = false;
        currentSearchKeyword = '';
        currentSearchType = '';
        // 清空搜索框
        document.getElementById('search-input').value = '';
        loadBuses();
    });

    // 绑定修改密码表单提交事件
    document.getElementById('password-form').addEventListener('submit', changePassword);

    // 绑定添加常用乘客表单提交事件
    document.getElementById('add-passenger-form').addEventListener('submit', addFrequentPassenger);

    // 绑定选择常用乘客事件
    document.getElementById('select-passenger').addEventListener('change', selectFrequentPassenger);

    // 绑定我要退票按钮事件
    document.getElementById('refund-button').addEventListener('click', refundAction);

    // 绑定我的退票模块事件
    document.getElementById('my-refunds-btn').addEventListener('click', function () {
        showSection('my-refunds');
    });

    // 初始化用户SSE连接
    initUserSSE();
});

// 加载车次数据
function loadBuses() {
    fetch('/api/buses')
        .then(response => response.json())
        .then(data => {
            const busTable = document.querySelector('#bus-table tbody');
            busTable.innerHTML = '';

            if (data.length === 0) {
                const emptyRow = document.createElement('tr');
                emptyRow.innerHTML = '<td colspan="8" style="text-align: center; color: #999; padding: 20px;">暂无车次数据</td>';
                busTable.appendChild(emptyRow);
                return;
            }

            data.forEach(bus => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${bus.bno}</td>
                    <td>${bus.staName}</td>
                    <td>${bus.endName}</td>
                    <td>${bus.date}</td>
                    <td>${bus.time}</td>
                    <td>${bus.remainSeats}</td>
                    <td>${bus.price}</td>
                    <td><button class="book-btn" onclick="selectBus(${JSON.stringify(bus).replace(/"/g, '&quot;')})">购买</button></td>
                `;
                busTable.appendChild(row);
            });
        })
        .catch(error => {
            console.error('加载车次数据失败:', error);
            showNotification('加载车次数据失败，请稍后重试！', false);
        });
}

// 处理输入框焦点事件
function handleInputFocus() {
    const searchType = document.getElementById('search-type').value;
    const searchInput = document.getElementById('search-input');

    if (searchType === '发车日期') {
        searchInput.type = 'date';
        searchInput.placeholder = '请选择发车日期';
    }
}

// 处理输入框失焦事件
function handleInputBlur() {
    const searchType = document.getElementById('search-type').value;
    const searchInput = document.getElementById('search-input');

    // 只有在输入框为空时才恢复为文本类型
    if (searchType === '发车日期' && !searchInput.value) {
        searchInput.type = 'text';
        searchInput.placeholder = '请输入搜索内容';
    }
}

// 处理搜索类型改变事件
function handleSearchTypeChange() {
    const searchType = document.getElementById('search-type').value;
    const searchInput = document.getElementById('search-input');

    if (searchType === '发车日期') {
        searchInput.type = 'date';
        searchInput.placeholder = '请选择发车日期';
    } else {
        searchInput.type = 'text';
        searchInput.placeholder = '请输入搜索内容';
    }

    // 清空输入框内容
    searchInput.value = '';
}

// 搜索车次
function searchBuses() {
    const searchType = document.getElementById('search-type').value;
    const searchInput = document.getElementById('search-input').value;

    if (!searchInput) {
        // 如果搜索框为空，取消搜索状态
        isSearchActive = false;
        currentSearchKeyword = '';
        currentSearchType = '';
        loadBuses();
        return;
    }

    // 保存搜索状态
    isSearchActive = true;
    currentSearchKeyword = searchInput;
    currentSearchType = searchType;

    // 将搜索类型和关键词一起传递给后端
    fetch(`/api/buses?keyword=${searchInput}&type=${searchType}`)
        .then(response => response.json())
        .then(data => {
            const busTable = document.querySelector('#bus-table tbody');
            busTable.innerHTML = '';

            if (data.length === 0) {
                const emptyRow = document.createElement('tr');
                emptyRow.innerHTML = '<td colspan="8" style="text-align: center; color: #999; padding: 20px;">没有找到匹配的车次数据</td>';
                busTable.appendChild(emptyRow);
                return;
            }

            data.forEach(bus => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${bus.bno}</td>
                    <td>${bus.staName}</td>
                    <td>${bus.endName}</td>
                    <td>${bus.date}</td>
                    <td>${bus.time}</td>
                    <td>${bus.remainSeats}</td>
                    <td>${bus.price}</td>
                    <td><button class="book-btn" onclick="selectBus(${JSON.stringify(bus).replace(/"/g, '&quot;')})">购买</button></td>
                `;
                busTable.appendChild(row);
            });
        })
        .catch(error => {
            console.error('搜索车次失败:', error);
            showNotification('搜索车次失败，请稍后重试！', false);
        });
}

// 选择车次
function selectBus(bus) {
    fetch(`/api/buses/${bus.bno}`)
        .then(response => response.json())
        .then(latestBus => {
            if (latestBus.remainSeats <= 0) {
                showNotification('余票不足~', false);
                return;
            }
            selectedBus = latestBus;
            document.getElementById('passenger-form').classList.add('active');
            document.getElementById('passenger-form').scrollIntoView({ behavior: 'smooth' });
        })
        .catch(error => {
            console.error('获取车次信息失败:', error);
            showNotification('获取车次信息失败，请稍后重试！', false);
        });
}

// 取消订票
function cancelBooking() {
    selectedBus = null;
    document.getElementById('passenger-form').classList.remove('active');
    // 重置表单
    document.getElementById('idno').value = '';
    document.getElementById('name').value = '';
    document.getElementById('phone').value = '';
}

// 确认订票
function confirmBooking() {
    if (!selectedBus) return;

    const idno = document.getElementById('idno').value;
    const name = document.getElementById('name').value;
    const phone = document.getElementById('phone').value;

    console.log('确认订票 - idno:', idno, 'name:', name, 'phone:', phone);

    if (!idno || !name || !phone) {
        showNotification('请填写完整的乘客信息！', false);
        return;
    }

    const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));

    // 提示用户要支付的金额
    const confirmPopup = document.getElementById('confirm-popup');
    const confirmText = document.getElementById('confirm-text');
    const confirmBtn = document.getElementById('confirm-ok');
    const cancelBtn = document.getElementById('confirm-cancel');

    confirmText.textContent = `确认支付 ${selectedBus.price} 元？`;
    confirmPopup.style.display = 'flex';

    confirmBtn.onclick = () => {
        confirmPopup.style.display = 'none';
        proceedBooking(idno, name, phone, currentUser);
    };

    cancelBtn.onclick = () => {
        confirmPopup.style.display = 'none';
    };
}

// 执行购票流程
function proceedBooking(idno, name, phone, currentUser) {
    const bookingData = {
        bno: selectedBus.bno,
        userName: currentUser.userName,
        idno: idno,
        passengerName: name,
        passengerPhone: phone
    };

    console.log('发送购票数据:', bookingData);

    // 获取动画元素
    const payAnimation = document.getElementById('pay-animation');
    const passengerForm = document.getElementById('passenger-form');

    // 显示支付动画，隐藏乘客表单
    payAnimation.classList.add('active');
    passengerForm.classList.remove('active');

    // 模拟支付过程，2秒后执行实际的购票请求
    setTimeout(() => {
        fetch('/api/booking', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(bookingData)
        })
            .then(response => {
                return response.json();
            })
            .then(data => {
                // 隐藏支付动画
                payAnimation.classList.remove('active');

                if (data.success) {
                    showNotification('购票成功！');
                    cancelBooking();
                    loadBuses(); // 重新加载车次数据以更新剩余座位
                    loadOrders(); // 重新加载订单
                    loadPassengers(); // 重新加载常用乘客列表，确保新添加的乘客信息显示在常用列表中
                } else {
                    showNotification('购票失败：' + data.message, false);
                    // 如果购票失败，重新显示乘客表单
                    passengerForm.classList.add('active');
                }
            })
            .catch(error => {
                // 隐藏支付动画
                payAnimation.classList.remove('active');
                // 重新显示乘客表单
                passengerForm.classList.add('active');

                console.error('购票失败:', error);
                showNotification('购票失败，请稍后重试！', false);
            });
    }, 2000); // 2秒支付动画
}

// 加载订单数据
function loadOrders() {
    const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));

    // 同时获取订单和退票申请数据
    Promise.all([
        fetch(`/api/orders?userName=${currentUser.userName}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('订单请求失败: ' + response.status);
                }
                return response.json();
            }),
        fetch(`/api/refund-applications?userName=${currentUser.userName}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('退票申请请求失败: ' + response.status);
                }
                return response.json();
            })
    ])
        .then(([ordersData, refundApplications]) => {
            const ordersTable = document.querySelector('#orders-table tbody');
            ordersTable.innerHTML = '';

            // 创建订单ID到退票申请状态的映射
            const refundStatusMap = {};
            if (refundApplications && Array.isArray(refundApplications)) {
                refundApplications.forEach(refund => {
                    // 由于后端订单返回的orderId实际上是btno，所以这里需要使用refund.btno作为键
                    // 这样order.orderId（即btno）就可以正确匹配refund.btno
                    refundStatusMap[refund.btno] = refund.status;
                });
            }

            if (ordersData.length === 0) {
                const emptyRow = document.createElement('tr');
                emptyRow.innerHTML = '<td colspan="13" style="text-align: center; padding: 20px;">暂无订单数据</td>';
                ordersTable.appendChild(emptyRow);
                return;
            }

            ordersData.forEach(order => {

                // 检查是否有退票申请及状态
                const refundStatus = refundStatusMap[order.orderId];

                // 跳过已同意退票的订单
                if (refundStatus === 'approved') {
                    return;
                }

                // 检查是否已过发车时间
                const isPastDeparture = checkIfPastDeparture(order.date, order.time);

                // 如果是"我的出行"模式，跳过已过发车时间的订单
                if (showMyTripsMode && isPastDeparture) {
                    return;
                }

                const row = document.createElement('tr');

                let refundBtnClass, refundBtnText, refundBtnDisabled, refundBtnOnclick;

                if (isPastDeparture) {
                    refundBtnClass = 'refund-btn disabled';
                    refundBtnText = '已过发车时间';
                    refundBtnDisabled = 'disabled';
                    refundBtnOnclick = 'showPastDepartureMessage()';
                } else if (refundStatus === 'rejected') {
                    // 已拒绝退票的订单，按钮不可点击
                    refundBtnClass = 'refund-btn disabled';
                    refundBtnText = '不可退票';
                    refundBtnDisabled = 'disabled';
                    refundBtnOnclick = 'showRefundRejectedMessage()';
                } else if (refundStatus === 'pending') {
                    // 申请退票中的订单
                    refundBtnClass = 'refund-btn disabled';
                    refundBtnText = '申请退票中';
                    refundBtnDisabled = 'disabled';
                    refundBtnOnclick = 'showRefundProcessingMessage()';
                } else {
                    // 正常订单，可以申请退票
                    refundBtnClass = 'refund-btn';
                    refundBtnText = '申请退票';
                    refundBtnDisabled = '';
                    refundBtnOnclick = `refundTicket(${order.orderId}, '${order.date}', '${order.time}')`;
                }

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
                    <td>${order.idno || ''}</td>
                    <td>${order.passengerName}</td>
                    <td>${order.passengerPhone}</td>
                    <td><button class="${refundBtnClass}" onclick="${refundBtnOnclick}" ${refundBtnDisabled}>${refundBtnText}</button></td>
                `;
                ordersTable.appendChild(row);
            });

            // 设置水平滚动条位置到最右侧（使用更长的延迟确保DOM完全渲染）
            setTimeout(scrollToRight, 100);
        })
        .catch(error => {
            console.error('加载订单数据失败:', error);
            showNotification('加载订单数据失败，请稍后重试！', false);
        });
}

// 检查是否已过发车时间
function checkIfPastDeparture(date, time) {
    const departureDateTime = new Date(`${date} ${time}`);
    const currentDateTime = new Date();
    return currentDateTime > departureDateTime;
}

// 显示已过发车时间的提示信息
function showPastDepartureMessage() {
    showNotification('该车次已过发车时间，无法退票！', false);
}

// 显示退票申请处理提示弹窗
function showRefundProcessingPopup() {
    showNotification('退票申请已提交，退票申请会在2小时内处理，请耐心等待~');
}

// 显示退票申请处理提示
function showRefundProcessingMessage() {
    showNotification('退票申请正在处理中，请耐心等待！', false);
}

// 显示退票申请被拒绝提示
function showRefundRejectedMessage() {
    showNotification('该订单退票申请已被拒绝，不可再次申请退票！', false);
}

// 退票
function refundTicket(orderId, departureDate, departureTime) {
    const refundReasonPopup = document.getElementById('refund-reason-popup');
    const refundReasonOk = document.getElementById('refund-reason-ok');
    const refundReasonCancel = document.getElementById('refund-reason-cancel');
    const refundConfirmPopup = document.getElementById('refund-confirm-popup');
    const refundConfirmMessage = document.getElementById('refund-confirm-message');
    const refundConfirmOk = document.getElementById('refund-confirm-ok');
    const refundConfirmCancel = document.getElementById('refund-confirm-cancel');

    // 显示退款原因选择弹窗
    refundReasonPopup.style.display = 'flex';

    // 确定退票按钮点击事件
    refundReasonOk.onclick = () => {
        refundReasonPopup.style.display = 'none';

        // 获取选中的退款原因
        const selectedReason = document.querySelector('input[name="refund-reason"]:checked').value;

        // 计算退票手续费
        const departureDateTime = new Date(`${departureDate} ${departureTime}`);
        const currentDateTime = new Date();
        const timeDiff = departureDateTime - currentDateTime;
        const hoursDiff = timeDiff / (1000 * 60 * 60);

        let refundPercentage = 100;
        let feePercentage = 0;

        if (hoursDiff >= 5) {
            // 提前5个小时(不含)之前退票，免收手续费
            refundPercentage = 100;
            feePercentage = 0;
        } else if (hoursDiff >= 2) {
            // 提前2-5个小时(含)退票，手续费收取10%
            refundPercentage = 90;
            feePercentage = 10;
        } else if (hoursDiff >= 0.5) {
            // 提前30分钟-2个小时(不含)退票，手续费收取20%
            refundPercentage = 80;
            feePercentage = 20;
        } else if (hoursDiff >= 10 / 60) {
            // 提前10分钟-30分钟(含)退票，手续费收取50%
            refundPercentage = 50;
            feePercentage = 50;
        } else {
            // 不足10分钟，不允许退票
            showNotification('距离发车时间不足10分钟，无法申请退票！', false);
            return;
        }

        // 设置确认消息
        refundConfirmMessage.textContent = `按照退款规则，退票成功后将按原路退回${refundPercentage}%原付款金额，确定申请退票吗？`;

        // 显示退票规则确认弹窗
        refundConfirmPopup.style.display = 'flex';

        // 确认按钮点击事件
        refundConfirmOk.onclick = () => {
            refundConfirmPopup.style.display = 'none';

            // 发送退票申请请求
            fetch('/api/orders/refund-application', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    orderId: orderId,
                    refundReason: selectedReason
                })
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        // 显示退票申请提交成功提示
                        showNotification('退票申请已提交，请等待管理员审核！', true);
                        loadOrders(); // 重新加载订单
                        loadMyRefunds(); // 加载我的退票申请
                    } else {
                        showNotification('退票申请失败：' + data.message, false);
                    }
                })
                .catch(error => {
                    console.error('退票申请失败:', error);
                    showNotification('退票申请失败，请稍后重试！', false);
                });
        };

        // 取消按钮点击事件
        refundConfirmCancel.onclick = () => {
            refundConfirmPopup.style.display = 'none';
        };
    };

    // 取消按钮点击事件
    refundReasonCancel.onclick = () => {
        refundReasonPopup.style.display = 'none';
    };
}

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
                // 重置表单
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

// 加载常用乘客列表
function loadPassengers() {
    const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));

    fetch(`/api/passengers?userName=${currentUser.userName}`)
        .then(response => response.json())
        .then(data => {
            // 更新常用乘客列表
            const passengersTable = document.querySelector('#passengers-table tbody');
            passengersTable.innerHTML = '';

            // 更新购票表单中的常用乘客选择框
            const passengerSelect = document.getElementById('select-passenger');
            passengerSelect.innerHTML = '<option value="">请选择常用乘客</option>';

            if (data.length === 0) {
                const emptyRow = document.createElement('tr');
                emptyRow.innerHTML = '<td colspan="4" style="text-align: center; color: #999; padding: 20px;">暂无常用乘客数据</td>';
                passengersTable.appendChild(emptyRow);
            } else {
                data.forEach(passenger => {
                    // 添加到乘客列表
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${passenger.name}</td>
                        <td>${passenger.idno}</td>
                        <td>${passenger.phone}</td>
                        <td><button class="delete-btn" onclick="deleteFrequentPassenger('${passenger.idno}')">删除</button></td>
                    `;
                    passengersTable.appendChild(row);

                    // 添加到选择框
                    const option = document.createElement('option');
                    option.value = JSON.stringify(passenger);
                    option.textContent = passenger.name;
                    passengerSelect.appendChild(option);
                });
            }
        })
        .catch(error => {
            console.error('加载常用乘客失败:', error);
            showNotification('加载常用乘客失败，请稍后重试！', false);
        });
}

// 添加常用乘客
function addFrequentPassenger(event) {
    event.preventDefault();

    const name = document.getElementById('passenger-name').value;
    const idno = document.getElementById('passenger-idno').value;
    const phone = document.getElementById('passenger-phone').value;

    if (!name || !idno || !phone) {
        showNotification('请填写完整的乘客信息！', false);
        return;
    }

    const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));

    const passengerData = {
        userName: currentUser.userName,
        idno: idno,
        name: name,
        phone: phone
    };

    fetch('/api/passengers', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(passengerData)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification('添加常用乘客成功！');
                // 重置表单
                document.getElementById('add-passenger-form').reset();
                // 重新加载乘客列表
                loadPassengers();
            } else {
                showNotification('添加常用乘客失败：' + data.message, false);
            }
        })
        .catch(error => {
            console.error('添加常用乘客失败:', error);
            showNotification('添加常用乘客失败，请稍后重试！', false);
        });
}

// 删除常用乘客
function deleteFrequentPassenger(idno) {
    const confirmPopup = document.getElementById('confirm-popup');
    const confirmText = document.getElementById('confirm-text');
    const confirmBtn = document.getElementById('confirm-ok');
    const cancelBtn = document.getElementById('confirm-cancel');

    confirmText.textContent = '确定要删除该常用乘客吗？';
    confirmPopup.style.display = 'flex';

    confirmBtn.onclick = () => {
        confirmPopup.style.display = 'none';
        const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));

        fetch(`/api/passengers?userName=${currentUser.userName}&idno=${idno}`, {
            method: 'DELETE'
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showNotification('删除常用乘客成功！');
                    // 重新加载乘客列表
                    loadPassengers();
                } else {
                    showNotification('删除常用乘客失败：' + data.message, false);
                }
            })
            .catch(error => {
                console.error('删除常用乘客失败:', error);
                showNotification('删除常用乘客失败，请稍后重试！', false);
            });
    }
}

// 选择常用乘客
function selectFrequentPassenger() {
    const passengerSelect = document.getElementById('select-passenger');
    const selectedPassenger = passengerSelect.value;

    // 只在用户主动选择乘客时才填充表单，避免在 loadPassengers() 清空选择框时触发
    if (selectedPassenger && selectedPassenger !== '') {
        const passenger = JSON.parse(selectedPassenger);
        document.getElementById('idno').value = passenger.idno;
        document.getElementById('name').value = passenger.name;
        document.getElementById('phone').value = passenger.phone;
    }
    // 移除清空表单的逻辑，避免在 loadPassengers() 清空选择框时触发
}

// 退出登录
function logout() {
    const confirmPopup = document.getElementById('confirm-popup');
    const confirmText = document.getElementById('confirm-text');
    const confirmBtn = document.getElementById('confirm-ok');
    const cancelBtn = document.getElementById('confirm-cancel');

    confirmText.textContent = '确定要退出登录吗？';
    confirmPopup.style.display = 'flex';

    confirmBtn.onclick = () => {
        confirmPopup.style.display = 'none';
        sessionStorage.removeItem('currentUser');
        window.location.href = 'login.html';
    };

    cancelBtn.onclick = () => {
        confirmPopup.style.display = 'none';
    };
}

// 滚动到最右侧
function scrollToRight() {
    const tableContainer = document.querySelector('#my-orders .table-container');
    if (tableContainer) {
        // 使用scrollTo实现平滑滚动效果
        tableContainer.scrollTo({
            left: tableContainer.scrollWidth,
            behavior: 'smooth'
        });
    }
}

// 我要退票按钮点击事件
function refundAction() {
    showMyTripsMode = !showMyTripsMode;

    // 更新按钮文本
    const refundButton = document.getElementById('refund-button');
    if (showMyTripsMode) {
        refundButton.textContent = '显示全部订单';
    } else {
        refundButton.textContent = '我的出行';
    }

    loadOrders();
}

// 公告功能
// 加载今日公告
function loadAnnouncement(showPopup = false) {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const today = `${year}-${month}-${day}`;

    // 首先从服务器获取最新公告
    fetch(`/api/announcement-records?date=${today}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('获取公告失败');
            }
            return response.json();
        })
        .then(records => {
            let announcementData = null;

            // 检查是否有今天的公告
            if (records && records.length > 0) {
                let hasPublishedAnnouncement = false;
                let latestContent = '';

                // 检查所有公告记录，找到已发布的公告
                for (let i = 0; i < records.length; i++) {
                    const record = records[i];
                    if (record.published && record.content && record.content.trim() !== '') {
                        hasPublishedAnnouncement = true;
                        latestContent = record.content;
                        break;
                    }
                }

                // 如果有已发布的公告，显示它
                if (hasPublishedAnnouncement) {
                    announcementData = {
                        date: today,
                        content: latestContent,
                        published: true
                    };

                    // 保存到localStorage
                    localStorage.setItem('dailyAnnouncement', JSON.stringify(announcementData));
                } else {
                    // 没有已发布的公告，清除localStorage中的公告
                    localStorage.removeItem('dailyAnnouncement');
                }
            } else {
                // 没有公告，清除localStorage中的公告
                localStorage.removeItem('dailyAnnouncement');
            }

            // 显示公告
            displayAnnouncement(announcementData, showPopup);
        })
        .catch(error => {
            console.error('获取公告时出错:', error);

            // 出错时尝试从localStorage获取
            const savedData = localStorage.getItem('dailyAnnouncement');
            let announcementData = null;
            if (savedData) {
                try {
                    announcementData = JSON.parse(savedData);
                } catch (parseError) {
                    console.error('解析本地公告数据失败:', parseError);
                    localStorage.removeItem('dailyAnnouncement');
                }
            }

            // 显示公告
            displayAnnouncement(announcementData, showPopup);
        });
}

// 显示公告的辅助函数
function displayAnnouncement(data, showPopup = false) {
    const banner = document.getElementById('announcement-banner');
    const marquee = document.getElementById('announcement-marquee');
    const marqueeCopy = document.getElementById('announcement-marquee-copy');
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const today = `${year}-${month}-${day}`;

    // 如果没有公告数据，隐藏横幅
    if (!data || typeof data.content !== 'string' || data.content.trim() === '') {
        if (banner) banner.style.display = 'none';
        return;
    }

    // 显示横幅
    if (banner) {
        banner.style.display = 'block';
    }

    if (marquee) marquee.textContent = data.content;
    if (marqueeCopy) marqueeCopy.textContent = data.content;

    // 只在showPopup为true时显示公告弹窗（页面加载时）
    if (showPopup && data.content.trim() !== '') {
        showAnnouncementPopup(data.content);
    }
}

// 显示公告弹窗
function showAnnouncementPopup(content = null) {
    // 显示公告弹窗
    const announcementPopup = document.getElementById('announcement-popup');
    const announcementText = document.getElementById('announcement-popup-text');
    const announcementBtn = document.getElementById('announcement-ok');
    const announcementCloseBtn = document.getElementById('announcement-close');

    // 确保所有元素都存在
    if (!announcementPopup || !announcementText || !announcementBtn || !announcementCloseBtn) {
        console.error('公告弹窗元素缺失');
        return;
    }

    // 如果没有提供内容，尝试从横幅中获取
    if (!content) {
        const marquee = document.getElementById('announcement-marquee');
        if (marquee && marquee.textContent !== '正在加载公告内容...') {
            content = marquee.textContent;
        } else {
            // 如果没有公告内容，显示默认消息
            content = '暂无公告内容';
        }
    }

    // 更新弹窗内容
    announcementText.innerHTML = content.replace(/\n/g, '<br>');

    // 显示弹窗 - 直接设置样式确保显示
    // 直接设置样式确保弹窗显示
    announcementPopup.style.display = 'flex';
    announcementPopup.style.visibility = 'visible';
    announcementPopup.style.opacity = '1';
    announcementPopup.style.zIndex = '1001';

    // 克隆节点以移除所有旧的事件监听器
    const newBtn = announcementBtn.cloneNode(true);
    const newCloseBtn = announcementCloseBtn.cloneNode(true);
    announcementBtn.parentNode.replaceChild(newBtn, announcementBtn);
    announcementCloseBtn.parentNode.replaceChild(newCloseBtn, announcementCloseBtn);

    // 设置关闭按钮事件
    const closePopup = () => {
        // 直接隐藏弹窗
        announcementPopup.style.display = 'none';
        announcementPopup.style.visibility = 'hidden';
        announcementPopup.style.opacity = '0';
    };

    // 绑定事件
    newBtn.addEventListener('click', closePopup);
    newCloseBtn.addEventListener('click', closePopup);
}

// 我的退票模块功能

// 显示退票处理中消息
function showRefundProcessingMessage() {
    showNotification('退票申请正在处理中，请耐心等待管理员审核！', true);
}

// 加载我的退票申请
function loadMyRefunds() {
    const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));

    fetch(`/api/refund-applications?userName=${currentUser.userName}`)
        .then(response => response.json())
        .then(data => {
            // 按状态分类退票申请
            const pendingRefunds = data.filter(refund => refund.status === 'pending');
            const approvedRefunds = data.filter(refund => refund.status === 'approved');
            const rejectedRefunds = data.filter(refund => refund.status === 'rejected');

            // 填充待通过申请列表
            populateRefundTable('pending-refunds-table', pendingRefunds, true);

            // 填充已通过申请列表
            populateRefundTable('approved-refunds-table', approvedRefunds, false);

            // 填充被拒绝申请列表
            populateRefundTable('rejected-refunds-table', rejectedRefunds, false);
        })
        .catch(error => {
            console.error('加载退票申请数据失败:', error);
            showNotification('加载退票申请数据失败，请稍后重试！', false);
        });
}

// 填充退票申请表格
function populateRefundTable(tableId, refunds, showActions) {
    const table = document.getElementById(tableId);
    const tbody = table.querySelector('tbody');
    tbody.innerHTML = '';

    if (refunds.length === 0) {
        let emptyMessage = '';
        let colspan = 0;

        if (tableId === 'pending-refunds-table') {
            emptyMessage = '暂无待处理的退票申请';
            colspan = 14;
        } else if (tableId === 'approved-refunds-table') {
            emptyMessage = '暂无已通过的退票申请';
            colspan = 14;
        } else if (tableId === 'rejected-refunds-table') {
            emptyMessage = '暂无已拒绝的退票申请';
            colspan = 14;
        }

        const emptyRow = document.createElement('tr');
        emptyRow.innerHTML = `<td colspan="${colspan}" style="text-align: center; color: #999; padding: 20px;">${emptyMessage}</td>`;
        tbody.appendChild(emptyRow);
        return;
    }

    refunds.forEach(refund => {
        const row = document.createElement('tr');

        const isBusDeleted = !refund.staName || !refund.endName || !refund.date || !refund.time;

        let refundAmount = 0;
        if (refund.price && refund.date && refund.time) {
            const departureDateTime = new Date(`${refund.date} ${refund.time}`);
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

        let rowHtml = `
            <td>${refund.btno}</td>
            <td>${refund.bno}</td>
            <td>${isBusDeleted ? '已下架' : (refund.staName || '')}</td>
            <td>${isBusDeleted ? '已下架' : (refund.endName || '')}</td>
            <td>${isBusDeleted ? '已下架' : (refund.date || '')}</td>
            <td>${isBusDeleted ? '已下架' : (refund.time || '')}</td>
            <td>${refund.idno || ''}</td>
            <td>${refund.passengerName || ''}</td>
            <td>${refund.passengerPhone || ''}</td>
            <td>${refund.apply_date} ${refund.apply_time}</td>
        `;

        if (showActions) {
            rowHtml += `
                <td>${refund.refund_reason}</td>
                <td>${refund.price ? refund.price + '元' : '-'}</td>
                <td>${refundAmount > 0 ? refundAmount + '元' : '-'}</td>
                <td><button class="refund-action-btn" onclick="cancelRefundApplication(${refund.btno})">取消申请</button></td>
            `;
        } else if (tableId === 'approved-refunds-table') {
            rowHtml += `
                <td>${refund.process_time}</td>
                <td>${refund.refund_reason}</td>
                <td>${refund.price ? refund.price + '元' : '-'}</td>
                <td>${refundAmount > 0 ? refundAmount + '元' : '-'}</td>
            `;
        } else if (tableId === 'rejected-refunds-table') {
            rowHtml += `
                <td>${refund.process_time}</td>
                <td>${refund.refund_reason}</td>
                <td>${refund.price ? refund.price + '元' : '-'}</td>
                <td><button class="refund-action-btn" onclick="showRejectReasonPopup(${refund.btno})">查看详情</button></td>
            `;
        }

        row.innerHTML = rowHtml;
        tbody.appendChild(row);
    });
}

// 显示退票标签页
function showRefundTab(tabName) {
    // 隐藏所有退票列表
    const refundLists = document.querySelectorAll('.refund-list');
    refundLists.forEach(list => list.classList.remove('active'));

    // 移除所有标签按钮的激活状态
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => btn.classList.remove('active'));

    // 显示选中的标签页
    const selectedTab = document.getElementById(`${tabName}-refunds`);
    selectedTab.classList.add('active');

    // 激活对应的标签按钮
    const selectedBtn = document.querySelector(`.tab-btn[onclick="showRefundTab('${tabName}')"]`);
    selectedBtn.classList.add('active');
}

// 取消退票申请
function cancelRefundApplication(btno) {
    showConfirmPopup('确定要取消退票申请吗？', () => {
        fetch(`/api/refund-applications/${btno}`, {
            method: 'DELETE'
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showNotification('退票申请已取消！', true);
                    loadOrders(); // 重新加载订单
                    loadMyRefunds(); // 重新加载退票申请
                } else {
                    showNotification('取消退票申请失败：' + data.message, false);
                }
            })
            .catch(error => {
                console.error('取消退票申请失败:', error);
                showNotification('取消退票申请失败，请稍后重试！', false);
            });
    });
}

// 显示拒绝理由详情弹窗
function showRejectReasonPopup(btno) {
    const currentUser = JSON.parse(sessionStorage.getItem('currentUser'));
    if (!currentUser) {
        return;
    }

    // 获取退票申请数据
    fetch(`/api/refund-applications?userName=${currentUser.userName}`)
        .then(response => response.json())
        .then(data => {
            const refund = data.find(r => r.btno === btno);
            if (refund) {
                if (refund.reject_reason) {
                    document.getElementById('reject-reason-text').textContent = refund.reject_reason;
                    const rejectReasonPopup = document.getElementById('reject-reason-popup');
                    rejectReasonPopup.style.display = 'flex';
                    rejectReasonPopup.style.visibility = 'visible';
                    rejectReasonPopup.style.opacity = '1';
                    rejectReasonPopup.style.zIndex = '1001';
                } else {
                    showNotification('未找到拒绝理由', false);
                }
            } else {
                showNotification('未找到退票申请记录', false);
            }
        })
        .catch(error => {
            console.error('获取拒绝理由失败:', error);
            showNotification('获取拒绝理由失败，请稍后重试！', false);
        });
}

// 关闭拒绝理由详情弹窗
function closeRejectReasonPopup() {
    const rejectReasonPopup = document.getElementById('reject-reason-popup');
    rejectReasonPopup.style.display = 'none';
    rejectReasonPopup.style.visibility = 'hidden';
    rejectReasonPopup.style.opacity = '0';
}

// 修改showSection函数以支持我的退票模块
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

    // 根据模块激活对应的菜单按钮
    if (sectionId === 'book-ticket') {
        document.querySelector('.menu-btn:first-child').classList.add('active');
    } else if (sectionId === 'my-orders') {
        document.querySelectorAll('.menu-btn')[1].classList.add('active');
        // 重置"我的出行"模式
        showMyTripsMode = false;
        const refundButton = document.getElementById('refund-button');
        refundButton.textContent = '我的出行';
    } else if (sectionId === 'my-refunds') {
        document.querySelectorAll('.menu-btn')[2].classList.add('active');
        // 加载我的退票申请数据
        loadMyRefunds();
    }
}