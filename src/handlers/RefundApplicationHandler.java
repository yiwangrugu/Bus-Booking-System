package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.BookDao;
import model.BookTicket;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RefundApplicationHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            Connection connection = null;
            try {
                connection = getConnection();

                JSONObject request = parseRequestBody(exchange);

                int orderId = request.getInt("orderId");
                String refundReason = request.getString("refundReason");

                BookDao bookDao = new BookDao();
                BookTicket bookTicket = bookDao.getBook(connection, orderId);
                if (bookTicket == null) {
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("message", "订单不存在");
                    sendResponse(exchange, 404, response.toString());
                    return;
                }

                String checkSql = "SELECT * FROM refund_application WHERE btno = ? AND status = 'pending'";
                PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                checkStmt.setInt(1, orderId);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    rs.close();
                    checkStmt.close();
                    JSONObject response = new JSONObject();
                    response.put("success", false);
                    response.put("message", "该订单已有待处理的退票申请");
                    sendResponse(exchange, 400, response.toString());
                    return;
                }
                rs.close();
                checkStmt.close();

                String getPassengerSql = "SELECT passengerName, passengerPhone FROM book_ticket WHERE btno = ?";
                PreparedStatement getPassengerStmt = connection.prepareStatement(getPassengerSql);
                getPassengerStmt.setInt(1, orderId);
                ResultSet passengerRs = getPassengerStmt.executeQuery();
                String passengerName = "";
                String passengerPhone = "";
                if (passengerRs.next()) {
                    passengerName = passengerRs.getString("passengerName");
                    passengerPhone = passengerRs.getString("passengerPhone");
                }
                passengerRs.close();
                getPassengerStmt.close();

                String sql = "INSERT INTO refund_application (btno, userName, bno, idno, apply_date, apply_time, refund_reason, status, passengerName, passengerPhone) VALUES (?, ?, ?, ?, CURDATE(), CURTIME(), ?, 'pending', ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setInt(1, orderId);
                pstmt.setString(2, bookTicket.getUserName());
                pstmt.setInt(3, bookTicket.getBno());
                pstmt.setString(4, bookTicket.getIdno());
                pstmt.setString(5, refundReason);
                pstmt.setString(6, passengerName);
                pstmt.setString(7, passengerPhone);

                int rowsAffected = pstmt.executeUpdate();
                pstmt.close();

                JSONObject response = new JSONObject();
                if (rowsAffected > 0) {
                    response.put("success", true);
                    response.put("message", "退票申请提交成功");

                    sendResponse(exchange, 200, response.toString());
                } else {
                    response.put("success", false);
                    response.put("message", "退票申请提交失败");
                    sendResponse(exchange, 500, response.toString());
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
