/**
 * 登录状态检查和弹窗提示工具
 * 用于统一处理登录异常和用户提示
 */

// 登录检查工具对象
window.LoginChecker = {
    
    /**
     * 显示登录提示弹窗
     * @param {string} message - 提示消息
     * @param {function} onConfirm - 确认回调函数
     * @param {function} onCancel - 取消回调函数
     */
    showLoginDialog: function(message, onConfirm, onCancel) {
        // 创建弹窗HTML
        const dialogHtml = `
            <div id="loginDialog" style="
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                z-index: 9999;
                display: flex;
                align-items: center;
                justify-content: center;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            ">
                <div style="
                    background: white;
                    border-radius: 8px;
                    padding: 24px;
                    max-width: 400px;
                    width: 90%;
                    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
                    text-align: center;
                ">
                    <div style="
                        color: #f56c6c;
                        font-size: 48px;
                        margin-bottom: 16px;
                    ">
                        <i class="fas fa-exclamation-triangle"></i>
                    </div>
                    <h3 style="
                        color: #303133;
                        font-size: 18px;
                        margin-bottom: 8px;
                        font-weight: 600;
                    ">登录提醒</h3>
                    <p style="
                        color: #606266;
                        font-size: 14px;
                        margin-bottom: 24px;
                        line-height: 1.5;
                    ">${message}</p>
                    <div style="
                        display: flex;
                        gap: 12px;
                        justify-content: center;
                    ">
                        <button id="loginDialogCancel" style="
                            padding: 10px 20px;
                            border: 1px solid #dcdfe6;
                            background: white;
                            color: #606266;
                            border-radius: 4px;
                            cursor: pointer;
                            font-size: 14px;
                            transition: all 0.3s;
                        ">稍后再说</button>
                        <button id="loginDialogConfirm" style="
                            padding: 10px 20px;
                            border: none;
                            background: #409eff;
                            color: white;
                            border-radius: 4px;
                            cursor: pointer;
                            font-size: 14px;
                            transition: all 0.3s;
                        ">立即登录</button>
                    </div>
                </div>
            </div>
        `;
        
        // 移除已存在的弹窗
        const existingDialog = document.getElementById('loginDialog');
        if (existingDialog) {
            existingDialog.remove();
        }
        
        // 添加弹窗到页面
        document.body.insertAdjacentHTML('beforeend', dialogHtml);
        
        // 绑定事件
        const dialog = document.getElementById('loginDialog');
        const confirmBtn = document.getElementById('loginDialogConfirm');
        const cancelBtn = document.getElementById('loginDialogCancel');
        
        // 确认按钮事件
        confirmBtn.addEventListener('click', function() {
            dialog.remove();
            if (typeof onConfirm === 'function') {
                onConfirm();
            } else {
                window.location.href = '/login';
            }
        });
        
        // 取消按钮事件
        cancelBtn.addEventListener('click', function() {
            dialog.remove();
            if (typeof onCancel === 'function') {
                onCancel();
            }
        });
        
        // 点击背景关闭
        dialog.addEventListener('click', function(e) {
            if (e.target === dialog) {
                dialog.remove();
                if (typeof onCancel === 'function') {
                    onCancel();
                }
            }
        });
        
        // 按钮悬停效果
        confirmBtn.addEventListener('mouseenter', function() {
            this.style.background = '#66b1ff';
        });
        confirmBtn.addEventListener('mouseleave', function() {
            this.style.background = '#409eff';
        });
        
        cancelBtn.addEventListener('mouseenter', function() {
            this.style.borderColor = '#c0c4cc';
            this.style.color = '#409eff';
        });
        cancelBtn.addEventListener('mouseleave', function() {
            this.style.borderColor = '#dcdfe6';
            this.style.color = '#606266';
        });
    },
    
    /**
     * 处理API请求错误
     * @param {Error} error - 错误对象
     * @param {function} onLoginRequired - 需要登录时的回调
     */
    handleApiError: function(error, onLoginRequired) {
        if (error.response) {
            const data = error.response.data;
            const status = error.response.status;
            
            // 处理登录相关错误
            if (status === 403 || (data && data.code === 403)) {
                const message = data.message || '您的登录状态已过期，为了保护您的账户安全，请重新登录后继续购物';
                this.showLoginDialog(message, onLoginRequired);
                return true; // 表示已处理
            }
        }
        return false; // 表示未处理，需要其他错误处理
    },
    
    /**
     * 检查用户登录状态
     * @param {function} onSuccess - 已登录回调
     * @param {function} onError - 未登录回调
     */
    checkLoginStatus: function(onSuccess, onError) {
        // 检查是否有token
        const token = this.getToken();
        if (!token) {
            if (typeof onError === 'function') {
                onError();
            }
            return;
        }
        
        // 通过API验证token有效性
        if (typeof axios !== 'undefined') {
            axios.get('/getUserInfo', {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Basic@' + token
                }
            }).then(function(response) {
                if (response.data) {
                    if (typeof onSuccess === 'function') {
                        onSuccess(response.data);
                    }
                } else {
                    if (typeof onError === 'function') {
                        onError();
                    }
                }
            }).catch(function(error) {
                if (typeof onError === 'function') {
                    onError();
                }
            });
        }
    },
    
    /**
     * 获取存储的token
     */
    getToken: function() {
        // 优先从Cookie获取
        if (typeof Cookies !== 'undefined') {
            return Cookies.get('SUSAN-TOKEN');
        }
        
        // 备用方案：从localStorage获取
        try {
            return localStorage.getItem('SUSAN-TOKEN');
        } catch (e) {
            return null;
        }
    },
    
    /**
     * 为axios请求添加拦截器
     */
    setupAxiosInterceptor: function() {
        if (typeof axios !== 'undefined') {
            const self = this;
            
            // 响应拦截器
            axios.interceptors.response.use(
                function(response) {
                    return response;
                },
                function(error) {
                    // 自动处理登录错误
                    const handled = self.handleApiError(error, function() {
                        window.location.href = '/login';
                    });
                    
                    // 如果不是登录错误，继续抛出
                    if (!handled) {
                        return Promise.reject(error);
                    }
                }
            );
        }
    }
};

// 页面加载完成后自动设置拦截器
document.addEventListener('DOMContentLoaded', function() {
    LoginChecker.setupAxiosInterceptor();
});