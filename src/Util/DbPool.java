package Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DbPool {
    private static final int MAX_POOL_SIZE = 100;
    private static final int MIN_IDLE = 10;
    private static final int MAX_WAIT_TIME = 30000;

    private static BlockingQueue<Connection> connectionPool;
    private static String dbUrl = "jdbc:mysql://localhost:3306/booksystem?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
    private static String dbUserName = "root";
    private static String dbPassword = "hh2004919";
    private static String jdbcName = "com.mysql.cj.jdbc.Driver";

    static {
        try {
            Class.forName(jdbcName);
            connectionPool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
            for (int i = 0; i < MIN_IDLE; i++) {
                Connection conn = createNewConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                }
            }
            System.out.println("数据库连接池初始化完成，初始连接数：" + MIN_IDLE + "，最大连接数：" + MAX_POOL_SIZE);
        } catch (Exception e) {
            System.err.println("数据库连接池初始化失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Connection createNewConnection() {
        try {
            return DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
        } catch (SQLException e) {
            System.err.println("创建数据库连接失败：" + e.getMessage());
            return null;
        }
    }

    public static Connection getConnection() throws Exception {
        try {
            Connection conn = connectionPool.poll(MAX_WAIT_TIME, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (conn == null || conn.isClosed()) {
                conn = createNewConnection();
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("获取数据库连接超时");
        }
    }

    public static void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    connectionPool.offer(conn);
                } else {
                    Connection newConn = createNewConnection();
                    if (newConn != null) {
                        connectionPool.offer(newConn);
                    }
                }
            } catch (Exception e) {
                System.err.println("释放数据库连接失败：" + e.getMessage());
            }
        }
    }

    public static void closePool() {
        while (!connectionPool.isEmpty()) {
            Connection conn = connectionPool.poll();
            if (conn != null) {
                try {
                    if (!conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    System.err.println("关闭数据库连接失败：" + e.getMessage());
                }
            }
        }
    }
}
