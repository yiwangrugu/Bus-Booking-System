// 显示/隐藏密码功能
function togglePassword(inputId) {
    const passwordInput = document.getElementById(inputId);
    const toggleButton = passwordInput.nextElementSibling;

    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleButton.textContent = '🙈';
    } else {
        passwordInput.type = 'password';
        toggleButton.textContent = '👁️';
    }
}

// 自定义提示弹窗（自动关闭）
function showNotification(message, isSuccess = true, onCloseCallback = null) {
    const popup = document.getElementById('custom-popup');
    popup.textContent = message;
    popup.className = `popup ${isSuccess ? 'success' : 'error'}`;
    popup.style.display = 'flex';

    // 点击弹窗关闭
    popup.onclick = () => {
        popup.style.display = 'none';
        if (onCloseCallback) {
            onCloseCallback();
        }
    };

    // 3秒后自动关闭
    setTimeout(() => {
        if (popup.style.display === 'flex') {
            popup.style.display = 'none';
            if (onCloseCallback) {
                onCloseCallback();
            }
        }
    }, 3000);
}

// 显示确认弹窗
function showConfirmPopup(message, onConfirm) {
    const popup = document.getElementById('confirm-popup');
    const messageEl = document.getElementById('confirm-text');
    const okBtn = document.getElementById('confirm-ok');
    const cancelBtn = document.getElementById('confirm-cancel');

    messageEl.textContent = message;
    popup.style.display = 'flex';

    okBtn.onclick = () => {
        popup.style.display = 'none';
        onConfirm();
    };

    cancelBtn.onclick = () => {
        popup.style.display = 'none';
    };
}

// 初始化弹窗事件
function initPopups() {
    window.addEventListener('click', (e) => {
        const confirmPopupEl = document.getElementById('confirm-popup');
        if (e.target === confirmPopupEl) {
            confirmPopupEl.style.display = 'none';
        }
    });
}

// 初始化自定义弹窗
function initCustomPopups() {
    initPopups();
}
