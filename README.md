# Bus Booking System

一个基于 Java 和 Web 技术的汽车订票系统，支持用户订票、退票申请、管理员管理等功能。

## 功能特性

### 用户端功能
- 用户注册和登录
- 浏览车次信息
- 在线订票
- 查看订单
- 添加乘客信息
- 申请退票
- 查看退票申请状态
- 修改密码
- 查看公告

### 管理员端功能
- 车次管理（增删改查）
- 订单管理
- 退票申请审批（手动/自动）
- 公告管理（发布、取消、重新发布）
- 查看处理记录
- 修改密码

### 特色功能
- 智能退票规则（根据发车时间自动计算退款金额）
- 自动审批超时退票申请
- 实时座位管理
- 余额不足提示
- 响应式界面设计

## 技术栈

### 后端
- Java 11+
- Java HttpServer
- JDBC
- MySQL 数据库
- 线程池管理
- 定时任务

### 前端
- HTML5
- CSS3（渐变、动画、响应式设计）
- JavaScript（ES6+）
- Fetch API

## 项目结构

```
BookingSystem/
├── src/
│   ├── Main/
│   │   └── WebServer.java          # 主服务器入口
│   ├── Util/
│   │   ├── DbPool.java             # 数据库连接池
│   │   ├── DbUtil.java             # 数据库工具类
│   │   ├── LockManager.java        # 锁管理器
│   │   └── StringUtil.java         # 字符串工具类
│   ├── dao/
│   │   ├── BookDao.java            # 订票数据访问
│   │   ├── BusDao.java             # 车次数据访问
│   │   ├── PassDao.java            # 乘客数据访问
│   │   ├── RefundDao.java          # 退票数据访问
│   │   └── UserDao.java            # 用户数据访问
│   ├── handlers/
│   │   ├── BaseHandler.java        # 基础处理器
│   │   ├── LoginHandler.java       # 登录处理
│   │   ├── RegisterHandler.java    # 注册处理
│   │   ├── BookingHandler.java     # 订票处理
│   │   ├── BusesHandler.java       # 车次管理
│   │   ├── OrdersHandler.java      # 订单管理
│   │   ├── PassengersHandler.java  # 乘客管理
│   │   ├── RefundApplicationHandler.java      # 退票申请
│   │   ├── RefundApplicationsHandler.java    # 退票申请列表
│   │   ├── ApproveRefundApplicationHandler.java   # 批准退票
│   │   ├── RejectRefundApplicationHandler.java    # 拒绝退票
│   │   ├── AdminRefundApplicationsHandler.java   # 管理员退票申请
│   │   ├── AdminRefundRecordsHandler.java        # 管理员退票记录
│   │   ├── AnnouncementRecordsHandler.java       # 公告记录
│   │   ├── PublishAnnouncementHandler.java        # 发布公告
│   │   ├── CancelAnnouncementHandler.java        # 取消公告
│   │   ├── RepublishAnnouncementHandler.java    # 重新发布公告
│   │   ├── ChangePasswordHandler.java            # 修改密码
│   │   └── StaticFileHandler.java                # 静态文件处理
│   ├── model/
│   │   ├── User.java                # 用户模型
│   │   ├── Bus.java                 # 车次模型
│   │   ├── Passenger.java           # 乘客模型
│   │   ├── BookTicket.java          # 订票模型
│   │   └── RefundTicket.java        # 退票模型
│   └── tasks/
│       ├── AutoApproveRefundTask.java   # 自动审批退票任务
│       └── ClearAnnouncementTask.java   # 清理过期公告任务
├── web/
│   ├── login.html                   # 登录页面
│   ├── register.html                # 注册页面
│   ├── user.html                    # 用户主页面
│   ├── admin.html                   # 管理员主页面
│   ├── css/
│   │   ├── common.css              # 通用样式
│   │   ├── style.css               # 登录/注册样式
│   │   ├── user.css                # 用户端样式
│   │   └── admin.css               # 管理员端样式
│   ├── js/
│   │   ├── common.js               # 通用脚本
│   │   ├── login.js                # 登录脚本
│   │   ├── register.js             # 注册脚本
│   │   ├── user.js                 # 用户端脚本
│   │   └── admin.js                # 管理员端脚本
│   └── images/
│       └── 系统背景.png             # 背景图片
├── lib/                             # 依赖库
│   └── mysql-connector-j-8.0.33.jar    # MySQL JDBC 驱动
└── .gitignore                        # Git 忽略文件
```

## 安装和运行

### 环境要求
- Java 11 或更高版本
- MySQL 5.7 或更高版本

### 安装步骤

1. 克隆仓库
```bash
git clone https://github.com/yiwangrugu/Bus-Booking-System.git
cd Bus-Booking-System
```

2. 配置数据库
- 创建数据库：`booksystem`
- 导入数据库表结构（参考下面的数据库结构部分）
- 修改 `DbPool.java` 中的数据库连接信息（用户名、密码等）

3. 编译项目
```bash
javac -cp lib/mysql-connector-j-8.0.33.jar -d out src/**/*.java
```

4. 运行服务器
```bash
java -cp out;lib/mysql-connector-j-8.0.33.jar Main.WebServer
```

5. 访问系统
- 用户端：http://localhost:8080/login.html
- 管理员端：http://localhost:8080/admin.html

### 默认账户

**管理员账户：**
- 用户名：001
- 密码：111

**用户账户：**
- 用户名：111
- 密码：111

## 数据库结构

### 主要表结构

**users 表** - 用户信息
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userName TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    power INTEGER NOT NULL DEFAULT 0,
    balance REAL DEFAULT 0.0
);
```

**buses 表** - 车次信息
```sql
CREATE TABLE buses (
    bno INTEGER PRIMARY KEY AUTOINCREMENT,
    departure TEXT NOT NULL,
    destination TEXT NOT NULL,
    departureTime TEXT NOT NULL,
    price REAL NOT NULL,
    totalSeats INTEGER NOT NULL,
    remainingSeats INTEGER NOT NULL
);
```

**booktickets 表** - 订票记录
```sql
CREATE TABLE booktickets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bno INTEGER NOT NULL,
    userName TEXT NOT NULL,
    passengerName TEXT NOT NULL,
    bookingTime TEXT NOT NULL,
    FOREIGN KEY (bno) REFERENCES buses(bno),
    FOREIGN KEY (userName) REFERENCES users(userName)
);
```

**refunds 表** - 退票申请
```sql
CREATE TABLE refunds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bno INTEGER NOT NULL,
    userName TEXT NOT NULL,
    passengerName TEXT NOT NULL,
    applicationTime TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    refundAmount REAL,
    processedBy TEXT,
    FOREIGN KEY (bno) REFERENCES buses(bno),
    FOREIGN KEY (userName) REFERENCES users(userName)
);
```

**passengers 表** - 乘客信息
```sql
CREATE TABLE passengers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userName TEXT NOT NULL,
    name TEXT NOT NULL,
    idCard TEXT NOT NULL,
    phone TEXT NOT NULL,
    FOREIGN KEY (userName) REFERENCES users(userName)
);
```

**announcements 表** - 公告
```sql
CREATE TABLE announcements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    content TEXT NOT NULL,
    announcement_date DATE NOT NULL,
    publish_time DATETIME NOT NULL,
    published BOOLEAN DEFAULT 0
);
```

### 数据库迁移

如果需要添加 `published` 字段到现有的 `announcements` 表，可以执行以下 SQL：

```sql
-- 添加published字段到announcements表
ALTER TABLE announcements ADD COLUMN published BOOLEAN DEFAULT 0;

-- 更新现有记录的published状态
UPDATE announcements SET published = 1 WHERE content IS NOT NULL AND content != '';
```

## 退票规则

系统根据发车时间自动计算退款金额：

- 发车前 24 小时以上：全额退款
- 发车前 12-24 小时：退款 80%
- 发车前 6-12 小时：退款 50%
- 发车前 2-6 小时：退款 20%
- 发车前 2 小时以内：不可退款

## API 接口

### 用户接口
- `POST /api/login` - 用户登录
- `POST /api/register` - 用户注册
- `GET /api/buses` - 获取车次列表
- `POST /api/booking` - 订票
- `GET /api/orders` - 获取订单列表
- `POST /api/passengers` - 添加乘客
- `GET /api/passengers` - 获取乘客列表
- `POST /api/refund-application` - 申请退票
- `GET /api/refund-applications` - 获取退票申请列表
- `POST /api/change-password` - 修改密码
- `GET /api/announcements` - 获取公告

### 管理员接口
- `GET /api/admin/buses` - 获取车次列表
- `POST /api/admin/buses` - 添加车次
- `PUT /api/admin/buses` - 更新车次
- `DELETE /api/admin/buses` - 删除车次
- `GET /api/admin/orders` - 获取订单列表
- `GET /api/admin/refund-applications` - 获取退票申请
- `POST /api/admin/approve-refund` - 批准退票
- `POST /api/admin/reject-refund` - 拒绝退票
- `GET /api/admin/refund-records` - 获取退票记录
- `POST /api/admin/publish-announcement` - 发布公告
- `POST /api/admin/cancel-announcement` - 取消公告
- `POST /api/admin/republish-announcement` - 重新发布公告
- `GET /api/admin/announcement-records` - 获取公告记录
- `POST /api/change-password` - 修改密码

## 开发说明

### 添加新功能
1. 在 `src/handlers/` 创建新的处理器类
2. 在 `WebServer.java` 中注册新的路由
3. 在前端添加相应的 UI 和交互逻辑

### 数据库迁移
如需修改数据库结构，请：
1. 备份现有数据库
2. 编写迁移脚本
3. 更新相应的 DAO 类

### 代码规范
- 遵循 Java 命名规范
- 使用有意义的变量名
- 添加必要的注释
- 保持代码简洁清晰

## 更新日志

### v1.0.0 (2024-01-05)
- 初始版本发布
- 实现基本的订票、退票功能
- 实现管理员管理功能
- 实现自动审批退票功能
- 实现公告功能
- 完善用户界面和交互体验
