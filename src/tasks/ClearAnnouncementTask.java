package tasks;

import Util.DbUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClearAnnouncementTask implements Runnable {
    @Override
    public void run() {
        System.out.println("===== 开始执行每天0:00:00自动取消公告任务 =====");
        Connection connection = null;
        try {
            DbUtil dbUtil = new DbUtil();
            connection = dbUtil.getCon();

            String updateSql = "UPDATE announcements SET published = 0 WHERE id = (SELECT id FROM (SELECT MAX(id) AS id FROM announcements) AS tmp)";
            PreparedStatement updateStmt = connection.prepareStatement(updateSql);
            int rowsAffected = updateStmt.executeUpdate();
            updateStmt.close();

            if (rowsAffected > 0) {
                System.out.println("成功取消最新一条公告的发布状态");
            } else {
                System.out.println("没有公告记录，无需取消");
            }

            System.out.println("===== 自动取消公告任务执行完成 =====");
        } catch (Exception e) {
            System.err.println("执行自动取消公告任务时出错：" + e.getMessage());
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
