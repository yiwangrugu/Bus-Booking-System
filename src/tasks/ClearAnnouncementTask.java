package tasks;

import Util.DbUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClearAnnouncementTask implements Runnable {
    @Override
    public void run() {
        System.out.println("===== 开始执行每天凌晨0点自动清零公告任务 =====");
        Connection connection = null;
        try {
            DbUtil dbUtil = new DbUtil();
            connection = dbUtil.getCon();

            java.time.LocalDate today = java.time.LocalDate.now();
            String todayStr = today.toString();

            String checkSql = "SELECT id, content FROM announcements WHERE announcement_date = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setString(1, todayStr);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String content = rs.getString("content");

                if (content == null || content.trim().isEmpty()) {
                    System.out.printf("今天(%s)的公告内容已经为空，无需清零\n", todayStr);
                } else {
                    String updateSql = "UPDATE announcements SET content = '' WHERE id = ?";
                    PreparedStatement updateStmt = connection.prepareStatement(updateSql);
                    updateStmt.setInt(1, id);
                    int rowsAffected = updateStmt.executeUpdate();
                    updateStmt.close();

                    if (rowsAffected > 0) {
                        System.out.printf("成功清零今天(%s)的公告内容\n", todayStr);
                    } else {
                        System.out.printf("清零今天(%s)的公告内容失败\n", todayStr);
                    }
                }
            } else {
                System.out.printf("今天(%s)没有公告记录，无需清零\n", todayStr);
            }

            rs.close();
            checkStmt.close();

            System.out.println("===== 自动清零公告任务执行完成 =====");
        } catch (Exception e) {
            System.err.println("执行自动清零公告任务时出错：" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
