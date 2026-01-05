// 注册表单提交处理
document.getElementById('registerForm').addEventListener('submit', function (event) {
    event.preventDefault();

    // 获取表单数据
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    // 表单验证
    if (!username) {
        showNotification('账号不能为空！', false);
        return;
    }

    if (!password) {
        showNotification('密码不能为空！', false);
        return;
    }

    if (password !== confirmPassword) {
        showNotification('两次输入的密码不一致！', false);
        return;
    }

    // 准备注册数据
    const registerData = {
        userName: username,
        password: password,
        power: '用户' // 默认注册为用户
    };

    // 发送注册请求
    fetch('/api/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(registerData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('注册失败');
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                showNotification('注册成功！', true);
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 1000);
            } else {
                showNotification('注册失败！账号已存在！', false);
            }
        })
        .catch(error => {
            console.error('注册错误:', error);
            showNotification('注册失败，请稍后重试！', false);
        });
});