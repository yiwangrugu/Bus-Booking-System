package tasks;

import dao.BookDao;
import dao.RefundDao;
import model.BookTicket;
import model.RefundTicket;
import Util.DbUtil;
import Util.LockManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AutoApproveRefundTask implements Runnable {
    @Override
    public void run() {
        System.out.println("===== 开始执行自动审批超时退票申请任务 =====");
        Connection connection = null;
        try {
            DbUtil dbUtil = new DbUtil();
            connection = dbUtil.getCon();

            String querySql = "SELECT * FROM refund_application WHERE status = \"pending\" AND TIMESTAMP(apply_date, apply_time) < NOW() - INTERVAL 2 HOUR";
            PreparedStatement queryStmt = connection.prepareStatement(querySql);
            ResultSet rs = queryStmt.executeQuery();

            int approvedCount = 0;

            while (rs.next()) {
                int btno = rs.getInt("btno");
                int bno = rs.getInt("bno");
                String idno = rs.getString("idno");
                String userName = rs.getString("userName");
                String passengerName = rs.getString("passengerName");
                String passengerPhone = rs.getString("passengerPhone");
                String applyDate = rs.getString("apply_date");
                String applyTime = rs.getString("apply_time");

                System.out.printf("发现超时未处理的退票申请：btno=%d, 申请时间=%s %s\n", btno, applyDate, applyTime);

                LockManager.getOrderLock(btno).lock();
                try {
                    String getBookSql = "SELECT bt.*, b.staName, b.endName, b.date, b.time, b.price FROM book_ticket bt LEFT JOIN bus b ON bt.bno = b.bno WHERE bt.btno = ?";
                    PreparedStatement getBookPstmt = connection.prepareStatement(getBookSql);
                    getBookPstmt.setInt(1, btno);
                    ResultSet bookRs = getBookPstmt.executeQuery();

                    String staName = null;
                    String endName = null;
                    java.sql.Date date = null;
                    java.sql.Time time = null;
                    float price = 0;

                    if (bookRs.next()) {
                        staName = bookRs.getString("staName");
                        endName = bookRs.getString("endName");
                        date = bookRs.getDate("date");
                        time = bookRs.getTime("time");
                        price = bookRs.getFloat("price");
                    }
                    bookRs.close();
                    getBookPstmt.close();

                    String getApplySql = "SELECT apply_date, apply_time FROM refund_application WHERE btno = ?";
                    PreparedStatement getApplyPstmt = connection.prepareStatement(getApplySql);
                    getApplyPstmt.setInt(1, btno);
                    ResultSet applyRs = getApplyPstmt.executeQuery();

                    java.sql.Date applyDateForCalc = null;
                    java.sql.Time applyTimeForCalc = null;
                    if (applyRs.next()) {
                        applyDateForCalc = applyRs.getDate("apply_date");
                        applyTimeForCalc = applyRs.getTime("apply_time");
                    }
                    applyRs.close();
                    getApplyPstmt.close();

                    float refundAmount = 0;
                    if (price > 0 && date != null && time != null && applyDateForCalc != null
                            && applyTimeForCalc != null) {
                        long departureDateTime = date.getTime() + time.getTime();
                        long applyDateTime = applyDateForCalc.getTime() + applyTimeForCalc.getTime();
                        long timeDiff = departureDateTime - applyDateTime;
                        double hoursDiff = timeDiff / (1000.0 * 60 * 60);

                        double refundPercentage = 100;
                        if (hoursDiff >= 5) {
                            refundPercentage = 100;
                        } else if (hoursDiff >= 2) {
                            refundPercentage = 90;
                        } else if (hoursDiff >= 0.5) {
                            refundPercentage = 80;
                        } else if (hoursDiff >= 10.0 / 60) {
                            refundPercentage = 50;
                        } else {
                            refundPercentage = 0;
                        }

                        refundAmount = (float) (price * refundPercentage / 100);
                    }

                    connection.setAutoCommit(false);
                    try {
                        RefundDao refundDao = new RefundDao();
                        RefundTicket refundTicket = new RefundTicket();
                        refundTicket.setBtno(btno);
                        refundTicket.setUserName(userName);
                        refundTicket.setBno(bno);
                        refundTicket.setIdno(idno);
                        refundTicket.setStaName(staName);
                        refundTicket.setEndName(endName);
                        refundTicket.setDate(date);
                        refundTicket.setTime(time);
                        refundTicket.setPassengerName(passengerName);
                        refundTicket.setPassengerPhone(passengerPhone);
                        refundTicket.setPrice(price);
                        refundTicket.setRefundAmount(refundAmount);
                        refundDao.addRefund(connection, refundTicket);

                        String updateSql = "UPDATE refund_application SET status = \"approved\", process_time = NOW(), processed_by = '自动审批' WHERE btno = ? AND status = \"pending\"";
                        PreparedStatement updateStmt = connection.prepareStatement(updateSql);
                        updateStmt.setInt(1, btno);
                        int updateRowsAffected = updateStmt.executeUpdate();
                        System.out.printf("更新退票申请状态，影响行数：%d\n", updateRowsAffected);

                        if (updateRowsAffected == 0) {
                            connection.rollback();
                            updateStmt.close();
                            System.out.printf("自动同意退票申请失败，无法更新状态：btno=%d\n", btno);
                            continue;
                        }

                        BookDao bookDao = new BookDao();
                        BookTicket bookTicket = new BookTicket();
                        bookTicket.setBtno(btno);
                        int deleteResult = bookDao.refund(connection, bookTicket);
                        System.out.printf("删除book_ticket记录，影响行数：%d\n", deleteResult);

                        updateStmt.close();
                        connection.commit();
                        System.out.printf("自动同意退票申请成功：btno=%d\n", btno);
                        approvedCount++;
                    } catch (Exception e) {
                        connection.rollback();
                        System.err.printf("自动同意退票申请时出错：btno=%d, 错误：%s\n", btno, e.getMessage());
                        e.printStackTrace();
                    } finally {
                        connection.setAutoCommit(true);
                    }
                } finally {
                    LockManager.releaseOrderLock(btno);
                }
            }

            rs.close();
            queryStmt.close();

            System.out.printf("===== 自动审批任务执行完成，共处理 %d 个超时退票申请 =====\n", approvedCount);
        } catch (Exception e) {
            System.err.println("执行自动审批任务时出错：" + e.getMessage());
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
