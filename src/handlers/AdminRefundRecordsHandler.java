package handlers;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AdminRefundRecordsHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            Connection connection = null;
            try {
                connection = getConnection();

                String sql = "SELECT ra.*, " +
                        "COALESCE(rt.rdate, bt.bdate) as rdate, " +
                        "COALESCE(rt.rtime, bt.btime) as rtime, " +
                        "COALESCE(b.date, b2.date) as date, " +
                        "COALESCE(b.time, b2.time) as time, " +
                        "COALESCE(b.staName, b2.staName) as staName, " +
                        "COALESCE(b.endName, b2.endName) as endName, " +
                        "COALESCE(b.price, b2.price) as price, " +
                        "p.name, p.tel " +
                        "FROM refund_application ra " +
                        "LEFT JOIN refund_ticket rt ON ra.btno = rt.btno " +
                        "LEFT JOIN bus b ON rt.bno = b.bno " +
                        "LEFT JOIN book_ticket bt ON ra.btno = bt.btno " +
                        "LEFT JOIN bus b2 ON bt.bno = b2.bno " +
                        "LEFT JOIN passenger p ON ra.userName = p.userName AND ra.idno = p.idno " +
                        "WHERE ra.status != 'pending' " +
                        "ORDER BY ra.process_time DESC";

                PreparedStatement pstmt = connection.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery();

                JSONArray recordsArray = new JSONArray();
                while (rs.next()) {
                    JSONObject record = new JSONObject();
                    record.put("btno", rs.getInt("btno"));
                    record.put("bno", rs.getInt("bno"));
                    record.put("idno", rs.getString("idno"));
                    record.put("apply_date", rs.getDate("apply_date").toString());
                    record.put("apply_time", rs.getTime("apply_time").toString());
                    record.put("refund_reason", rs.getString("refund_reason"));
                    record.put("status", rs.getString("status"));

                    java.sql.Date date = rs.getDate("date");
                    java.sql.Time time = rs.getTime("time");
                    record.put("date", date != null ? date.toString() : "");
                    record.put("time", time != null ? time.toString() : "");
                    record.put("staName", rs.getString("staName") != null ? rs.getString("staName") : "");
                    record.put("endName", rs.getString("endName") != null ? rs.getString("endName") : "");
                    record.put("price", rs.getFloat("price"));

                    record.put("passengerName", rs.getString("name") != null ? rs.getString("name") : "");
                    record.put("passengerPhone", rs.getString("tel") != null ? rs.getString("tel") : "");

                    if (rs.getTimestamp("process_time") != null) {
                        record.put("process_time", rs.getTimestamp("process_time").toString());
                    }
                    record.put("reject_reason", rs.getString("reject_reason"));

                    String processedBy = rs.getString("processed_by");
                    record.put("processed_by", processedBy != null ? processedBy : "手动审批");

                    recordsArray.put(record);
                }

                rs.close();
                pstmt.close();
                sendResponse(exchange, 200, recordsArray.toString());
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
