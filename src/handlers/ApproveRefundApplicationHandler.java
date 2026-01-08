package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.BookDao;
import dao.RefundDao;
import model.BookTicket;
import model.RefundTicket;
import org.json.JSONObject;
import Util.LockManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ApproveRefundApplicationHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            Connection connection = null;
            try {
                connection = getConnection();

                String path = exchange.getRequestURI().getPath();
                String[] pathSegments = path.split("/");
                int btno = Integer.parseInt(pathSegments[pathSegments.length - 1]);

                LockManager.getOrderLock(btno).lock();
                try {
                    String getRefundSql = "SELECT * FROM refund_application WHERE btno = ?";
                    PreparedStatement getRefundPstmt = connection.prepareStatement(getRefundSql);
                    getRefundPstmt.setInt(1, btno);
                    ResultSet rs = getRefundPstmt.executeQuery();

                    if (!rs.next()) {
                        rs.close();
                        getRefundPstmt.close();
                        JSONObject response = new JSONObject();
                        response.put("success", false);
                        response.put("message", "退票申请不存在");
                        sendResponse(exchange, 404, response.toString());
                        return;
                    }

                    int bno = rs.getInt("bno");
                    String idno = rs.getString("idno");
                    String status = rs.getString("status");
                    String userName = rs.getString("userName");
                    String passengerName = rs.getString("passengerName");
                    String passengerPhone = rs.getString("passengerPhone");
                    rs.close();
                    getRefundPstmt.close();

                    if (!"pending".equals(status)) {
                        JSONObject response = new JSONObject();
                        response.put("success", false);
                        response.put("message", "退票申请已被处理，当前状态: " + status);
                        sendResponse(exchange, 400, response.toString());
                        return;
                    }

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

                    java.sql.Date applyDate = null;
                    java.sql.Time applyTime = null;
                    if (applyRs.next()) {
                        applyDate = applyRs.getDate("apply_date");
                        applyTime = applyRs.getTime("apply_time");
                    }
                    applyRs.close();
                    getApplyPstmt.close();

                    float refundAmount = 0;
                    if (price > 0 && date != null && time != null && applyDate != null && applyTime != null) {
                        long departureDateTime = date.getTime() + time.getTime();
                        long applyDateTime = applyDate.getTime() + applyTime.getTime();
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

                        String sql = "UPDATE refund_application SET status = \"approved\", process_time = NOW(), processed_by = \"手动审批\" WHERE btno = ?";
                        PreparedStatement pstmt = connection.prepareStatement(sql);
                        pstmt.setInt(1, btno);

                        int rowsAffected = pstmt.executeUpdate();
                        pstmt.close();

                        if (rowsAffected == 0) {
                            connection.rollback();
                            connection.setAutoCommit(true);

                            JSONObject response = new JSONObject();
                            response.put("success", false);
                            response.put("message", "同意退票申请失败，申请可能已被处理或不存在");
                            sendResponse(exchange, 400, response.toString());
                            return;
                        }

                        BookDao bookDao = new BookDao();
                        BookTicket bookTicket = new BookTicket();
                        bookTicket.setBtno(btno);
                        bookDao.refund(connection, bookTicket);

                        connection.commit();
                        connection.setAutoCommit(true);

                        JSONObject response = new JSONObject();
                        response.put("success", true);
                        response.put("message", "退票申请已同意");
                        sendResponse(exchange, 200, response.toString());
                    } catch (Exception e) {
                        connection.rollback();
                        connection.setAutoCommit(true);
                        throw e;
                    }
                } finally {
                    LockManager.releaseOrderLock(btno);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("success", false);
                errorResponse.put("message", "服务器内部错误: " + e.getMessage());
                sendResponse(exchange, 500, errorResponse.toString());
            } finally {
                closeConnection(connection);
            }
        } else {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "不支持的请求方法");
            sendResponse(exchange, 405, errorResponse.toString());
        }
    }
}
