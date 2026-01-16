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

## 数据库结构

### 完整表结构

基于代码分析，系统包含以下主要数据表：

#### 1. user 表 - 用户信息
```sql
CREATE TABLE user (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    userName VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    power VARCHAR(20) DEFAULT '用户'
);
```

**字段说明：**
- `id`: 用户ID，主键，自增
- `userName`: 用户名，唯一标识
- `password`: 密码
- `power`: 权限（用户/管理员）

#### 2. bus 表 - 车次信息
```sql
CREATE TABLE bus (
    bno INTEGER PRIMARY KEY AUTO_INCREMENT,
    staName VARCHAR(100) NOT NULL,
    endName VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    time TIME NOT NULL,
    price DECIMAL(8,2) NOT NULL,
    seat INTEGER NOT NULL
);
```

**字段说明：**
- `bno`: 车次号，主键，自增
- `staName`: 出发站
- `endName`: 终点站
- `date`: 发车日期
- `time`: 发车时间
- `price`: 票价
- `seat`: 总座位数

#### 3. book_ticket 表 - 订票记录
```sql
CREATE TABLE book_ticket (
    btno INTEGER PRIMARY KEY AUTO_INCREMENT,
    bno INTEGER NOT NULL,
    idno VARCHAR(18) NOT NULL,
    bdate DATE NOT NULL,
    btime TIME NOT NULL,
    userName VARCHAR(50) NOT NULL,
    passengerName VARCHAR(50),
    passengerPhone VARCHAR(20),
    FOREIGN KEY (bno) REFERENCES bus(bno),
    FOREIGN KEY (userName) REFERENCES user(userName)
);
```

**字段说明：**
- `btno`: 订单号，主键，自增
- `bno`: 车次号，外键
- `idno`: 乘客身份证号
- `bdate`: 订票日期
- `btime`: 订票时间
- `userName`: 用户名，外键
- `passengerName`: 乘客姓名
- `passengerPhone`: 乘客电话

#### 4. passenger 表 - 常用乘客信息
```sql
CREATE TABLE passenger (
    userName VARCHAR(50) NOT NULL,
    idno VARCHAR(18) NOT NULL,
    name VARCHAR(50) NOT NULL,
    tel VARCHAR(20),
    PRIMARY KEY (userName, idno),
    FOREIGN KEY (userName) REFERENCES user(userName)
);
```

**字段说明：**
- `userName`: 用户名，外键
- `idno`: 身份证号
- `name`: 乘客姓名
- `tel`: 联系电话

#### 5. refund_application 表 - 退票申请
```sql
CREATE TABLE refund_application (
    btno INTEGER PRIMARY KEY,
    userName VARCHAR(50) NOT NULL,
    bno INTEGER NOT NULL,
    idno VARCHAR(18) NOT NULL,
    apply_date DATE NOT NULL,
    apply_time TIME NOT NULL,
    refund_reason TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    process_time DATETIME,
    reject_reason TEXT,
    processed_by VARCHAR(50),
    passengerName VARCHAR(50),
    passengerPhone VARCHAR(20),
    FOREIGN KEY (btno) REFERENCES book_ticket(btno),
    FOREIGN KEY (userName) REFERENCES user(userName),
    FOREIGN KEY (bno) REFERENCES bus(bno)
);
```

**字段说明：**
- `btno`: 订单号，主键，外键
- `userName`: 用户名，外键
- `bno`: 车次号，外键
- `idno`: 乘客身份证号
- `apply_date`: 申请日期
- `apply_time`: 申请时间
- `refund_reason`: 退票原因
- `status`: 申请状态（pending/approved/rejected）
- `process_time`: 处理时间
- `reject_reason`: 拒绝原因
- `processed_by`: 处理人
- `passengerName`: 乘客姓名
- `passengerPhone`: 乘客电话

#### 6. announcements 表 - 系统公告
```sql
CREATE TABLE announcements (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    content TEXT NOT NULL,
    announcement_date DATE NOT NULL,
    publish_time DATETIME,
    published TINYINT DEFAULT 0
);
```

**字段说明：**
- `id`: 公告ID，主键，自增
- `content`: 公告内容
- `announcement_date`: 公告日期
- `publish_time`: 发布时间
- `published`: 发布状态（0-未发布，1-已发布）

#### 7. refund_ticket 表 - 退票记录（历史表）
```sql
CREATE TABLE refund_ticket (
    btno INTEGER PRIMARY KEY,
    userName VARCHAR(50) NOT NULL,
    bno INTEGER NOT NULL,
    idno VARCHAR(18) NOT NULL,
    rdate DATE NOT NULL,
    rtime TIME NOT NULL,
    staName VARCHAR(100),
    endName VARCHAR(100),
    date DATE,
    time TIME,
    passengerName VARCHAR(50),
    passengerPhone VARCHAR(20),
    price DECIMAL(8,2),
    refundAmount DECIMAL(8,2),
    FOREIGN KEY (btno) REFERENCES book_ticket(btno),
    FOREIGN KEY (bno) REFERENCES bus(bno)
);
```

**字段说明：**
- `btno`: 订单号，主键，外键
- `userName`: 用户名
- `bno`: 车次号，外键
- `idno`: 乘客身份证号
- `rdate`: 退票日期
- `rtime`: 退票时间
- `staName`: 出发站
- `endName`: 终点站
- `date`: 发车日期
- `time`: 发车时间
- `passengerName`: 乘客姓名
- `passengerPhone`: 乘客电话
- `price`: 原票价
- `refundAmount`: 退款金额

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
- 导入数据库表结构
- 修改 `DbPool.java` 中的数据库连接信息（用户名、密码等）

3. 编译项目
```bash
javac -encoding UTF-8 -d bin -cp "bin;lib/*" src/**/*.java
```

4. 运行服务器
```bash
java -cp "bin;lib/*" Main.WebServer
```

5. 访问系统
- 用户端：http://localhost:8080/html/login.html
- 管理员端：http://localhost:8080/html/admin.html

### 默认账户

**管理员账户：**
- 用户名：001
- 密码：111

**用户账户：**
- 用户名：111
- 密码：111

## 功能截图展示

### 用户端功能

#### 登录页面
![登录页面](images/login.png)
- 用户登录界面，支持用户名和密码验证
- 提供注册链接和记住密码功能

#### 注册页面
![注册页面](images/register.png)
- 新用户注册界面
- 包含用户名、密码、确认密码等必填字段

#### 用户主页面
![用户主页面](images/user-home.png)
- 用户登录后的主界面
- 显示用户信息、余额、公告等
- 提供导航菜单访问各功能模块

#### 车次浏览与订票
![车次浏览](images/bus-list.png)
- 显示所有可用车次信息
- 支持按出发地、目的地筛选
- 显示票价、剩余座位数等详细信息

#### 订单管理
![订单管理](images/user-orders.png)
- 查看用户的订票记录
- 显示订单状态、车次信息、乘客信息
- 支持退票申请操作

#### 乘客管理
![乘客管理](images/passengers.png)
- 管理常用乘客信息
- 支持添加、编辑、删除乘客
- 方便快速订票时选择乘客

#### 退票申请
![退票申请](images/refund-application.png)
- 提交退票申请界面
- 显示退款金额计算规则
- 支持查看退票申请状态

### 管理员端功能

#### 管理员登录
![管理员登录](images/admin-login.png)
- 管理员专用登录界面
- 与用户端分离的权限验证

#### 管理员主页面
![管理员主页面](images/admin-home.png)
- 管理员功能主界面
- 显示系统统计信息和快捷操作

#### 车次管理
![车次管理](images/bus-management.png)
- 管理所有车次信息
- 支持添加、编辑、删除车次
- 实时更新座位信息

#### 订单管理
![订单管理](images/admin-orders.png)
- 查看所有用户的订单
- 支持订单状态管理
- 提供订单搜索和筛选功能

#### 退票申请审批
![退票审批](images/refund-approval.png)
- 管理员审批退票申请界面
- 显示申请详情和退款金额
- 支持批准或拒绝操作

#### 公告管理
![公告管理](images/announcement-management.png)
- 发布和管理系统公告
- 支持公告的发布、取消、重新发布
- 公告内容实时推送到用户端


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


### v1.0.0 (2024-01-05)
- 初始版本发布
- 实现基本的订票、退票功能
- 实现管理员管理功能
- 实现自动审批退票功能
- 实现公告功能
- 完善用户界面和交互体验
