package handlers;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminRefundApplicationsHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            Connection connection = null;
            try {
                connection = getConnection();

                String sql = "SELECT ra.*, " +
                        "COALESCE(b.date, b2.date) as date, " +
                        "COALESCE(b.time, b2.time) as time, " +
                        "COALESCE(b.staName, b2.staName) as staName, " +
                        "COALESCE(b.endName, b2.endName) as endName, " +
                        "COALESCE(b.price, b2.price) as price, " +
                        "p.name, p.tel " +
                        "FROM refund_application ra " +
                        "LEFT JOIN book_ticket bt ON ra.btno = bt.btno " +
                        "LEFT JOIN bus b ON bt.bno = b.bno " +
                        "LEFT JOIN refund_ticket rt ON ra.btno = rt.btno " +
                        "LEFT JOIN bus b2 ON rt.bno = b2.bno " +
                        "LEFT JOIN passenger p ON ra.userName = p.userName AND ra.idno = p.idno " +
                        "WHERE ra.status = 'pending' " +
                        "ORDER BY ra.apply_date DESC, ra.apply_time DESC";

                PreparedStatement pstmt = connection.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery();

                JSONArray refundsArray = new JSONArray();
                while (rs.next()) {
                    JSONObject refund = new JSONObject();
                    refund.put("btno", rs.getInt("btno"));
                    refund.put("bno", rs.getInt("bno"));
                    refund.put("idno", rs.getString("idno"));
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

                    refund.put("passengerName", rs.getString("name") != null ? rs.getString("name") : "");
                    refund.put("passengerPhone", rs.getString("tel") != null ? rs.getString("tel") : "");

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
        } else {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("message", "不支持的请求方法");
            sendResponse(exchange, 405, errorResponse.toString());
        }
    }
}
