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
                    rs.close();
                    getRefundPstmt.close();

                    if (!"pending".equals(status)) {
                        JSONObject response = new JSONObject();
                        response.put("success", false);
                        response.put("message", "退票申请已被处理，当前状态: " + status);
                        sendResponse(exchange, 400, response.toString());
                        return;
                    }

                    String getBookSql = "SELECT bt.*, b.staName, b.endName, b.date, b.time, u.userName FROM book_ticket bt LEFT JOIN bus b ON bt.bno = b.bno LEFT JOIN user u ON bt.userName = u.userName WHERE bt.btno = ?";
                    PreparedStatement getBookPstmt = connection.prepareStatement(getBookSql);
                    getBookPstmt.setInt(1, btno);
                    ResultSet bookRs = getBookPstmt.executeQuery();

                    String userName = null;
                    String staName = null;
                    String endName = null;
                    java.sql.Date date = null;
                    java.sql.Time time = null;

                    if (bookRs.next()) {
                        userName = bookRs.getString("userName");
                        staName = bookRs.getString("staName");
                        endName = bookRs.getString("endName");
                        date = bookRs.getDate("date");
                        time = bookRs.getTime("time");
                    }
                    bookRs.close();
                    getBookPstmt.close();

                    connection.setAutoCommit(false);

                    try {
                        RefundDao refundDao = new RefundDao();
                        RefundTicket refundTicket = new RefundTicket();
                        refundTicket.setBtno(btno);
                        refundTicket.setBno(bno);
                        refundTicket.setIdno(idno);
                        refundTicket.setStaName(staName);
                        refundTicket.setEndName(endName);
                        refundTicket.setDate(date);
                        refundTicket.setTime(time);
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
