package handlers;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RefundApplicationsHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            handleGetRequest(exchange);
        } else if ("DELETE".equals(exchange.getRequestMethod())) {
            handleDeleteRequest(exchange);
        } else {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "不支持的请求方法");
            sendResponse(exchange, 405, errorResponse.toString());
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        Connection connection = null;
        try {
            connection = getConnection();

            String query = exchange.getRequestURI().getQuery();
            String userName = null;

            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        if ("userName".equals(keyValue[0])) {
                            userName = URLDecoder.decode(keyValue[1], "UTF-8");
                        }
                    }
                }
            }

            String sql = "SELECT ra.*, " +
                    "rt.rdate, " +
                    "rt.rtime, " +
                    "COALESCE(b.price, b2.price) as price, " +
                    "COALESCE(rt.date, b.date, b2.date) as date, " +
                    "COALESCE(rt.time, b.time, b2.time) as time, " +
                    "COALESCE(rt.staName, b.staName, b2.staName) as staName, " +
                    "COALESCE(rt.endName, b.endName, b2.endName) as endName " +
                    "FROM refund_application ra " +
                    "LEFT JOIN refund_ticket rt ON ra.btno = rt.btno " +
                    "LEFT JOIN bus b ON rt.bno = b.bno " +
                    "LEFT JOIN book_ticket bt ON ra.btno = bt.btno " +
                    "LEFT JOIN bus b2 ON bt.bno = b2.bno " +
                    "WHERE ra.userName = ? " +
                    "ORDER BY ra.apply_date DESC, ra.apply_time DESC";

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();

            JSONArray refundsArray = new JSONArray();
            while (rs.next()) {
                JSONObject refund = new JSONObject();
                refund.put("btno", rs.getInt("btno"));
                refund.put("bno", rs.getInt("bno"));
                refund.put("idno", rs.getString("idno"));
                refund.put("sno", rs.getInt("sno"));
                refund.put("apply_date", rs.getDate("apply_date").toString());
                refund.put("apply_time", rs.getTime("apply_time").toString());
                refund.put("refund_reason", rs.getString("refund_reason"));
                refund.put("status", rs.getString("status"));

                java.sql.Date date = rs.getDate("date");
                java.sql.Time time = rs.getTime("time");
                refund.put("date", date != null ? date.toString() : "");
                refund.put("time", time != null ? time.toString() : "");
                refund.put("staName", rs.getString("staName") != null ? rs.getString("staName") : "");
                refund.put("endName", rs.getString("endName") != null ? rs.getString("endName") : "");
                refund.put("price", rs.getFloat("price"));

                if (rs.getTimestamp("process_time") != null) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    refund.put("process_time", sdf.format(rs.getTimestamp("process_time")));
                }
                if (rs.getString("processed_by") != null) {
                    refund.put("processed_by", rs.getString("processed_by"));
                }
                refund.put("reject_reason", rs.getString("reject_reason"));

                refundsArray.put(refund);
            }

            rs.close();
            pstmt.close();
            sendResponse(exchange, 200, refundsArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "服务器内部错误: " + e.getMessage());
            sendResponse(exchange, 500, errorResponse.toString());
        } finally {
            closeConnection(connection);
        }
    }

    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        Connection connection = null;
        try {
            connection = getConnection();

            String path = exchange.getRequestURI().getPath();
            String[] pathSegments = path.split("/");
            int btno = Integer.parseInt(pathSegments[pathSegments.length - 1]);

            String sql = "DELETE FROM refund_application WHERE btno = ? AND status = 'pending'";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, btno);

            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();

            JSONObject response = new JSONObject();
            if (rowsAffected > 0) {
                response.put("success", true);
                response.put("message", "退票申请已取消");
                sendResponse(exchange, 200, response.toString());
            } else {
                response.put("success", false);
                response.put("message", "取消退票申请失败，申请可能已被处理或不存在");
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
    }
}
