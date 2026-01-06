package handlers;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RejectRefundApplicationHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            Connection connection = null;
            try {
                connection = getConnection();

                String path = exchange.getRequestURI().getPath();
                String[] pathSegments = path.split("/");
                int btno = Integer.parseInt(pathSegments[pathSegments.length - 1]);

                JSONObject request = parseRequestBody(exchange);
                String rejectReason = request.getString("reject_reason");

                String getUserSql = "SELECT u.userName FROM refund_application ra JOIN book_ticket bt ON ra.btno = bt.btno JOIN user u ON bt.userName = u.userName WHERE ra.btno = ?";
                PreparedStatement getUserPstmt = connection.prepareStatement(getUserSql);
                getUserPstmt.setInt(1, btno);
                ResultSet rs = getUserPstmt.executeQuery();
                String userName = null;
                if (rs.next()) {
                    userName = rs.getString("userName");
                }
                rs.close();
                getUserPstmt.close();

                String sql = "UPDATE refund_application SET status = \"rejected\", process_time = NOW(), reject_reason = ?, processed_by = \"手动审批\" WHERE btno = ? AND status = \"pending\"";
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setString(1, rejectReason);
                pstmt.setInt(2, btno);

                int rowsAffected = pstmt.executeUpdate();
                pstmt.close();

                JSONObject response = new JSONObject();
                if (rowsAffected > 0) {
                    response.put("success", true);
                    response.put("message", "退票申请已拒绝");
                    sendResponse(exchange, 200, response.toString());
                } else {
                    response.put("success", false);
                    response.put("message", "拒绝退票申请失败，申请可能已被处理或不存在");
                    sendResponse(exchange, 400, response.toString());
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
