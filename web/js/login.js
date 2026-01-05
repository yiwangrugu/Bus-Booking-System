// 页面加载时初始化弹窗
window.addEventListener('load', initPopups);

// 登录表单提交处理
document.getElementById('loginForm').addEventListener('submit', function (event) {
    event.preventDefault();

    // 获取表单数据
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const power = document.getElementById('power').value;

    // 表单验证
    if (!username || !password) {
        showNotification('账号或密码不能为空！', false);
        return;
    }

    // 准备登录数据
    const loginData = {
        userName: username,
        password: password,
        power: power
    };

    // 发送登录请求
    fetch('/api/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(loginData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('登录失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                // 保存用户信息到会话存储
                sessionStorage.setItem('currentUser', JSON.stringify(data.user));

                // 根据用户角色跳转到对应页面
                if (data.user.power === '管理员') {
                    window.location.href = 'admin.html';
                } else {
                    window.location.href = 'user.html';
                }
            } else {
                showNotification('账号或密码有误！', false);
            }
        })
        .catch(error => {
            console.error('登录错误:', error);
            showNotification('登录失败，请稍后重试！', false);
        });
});