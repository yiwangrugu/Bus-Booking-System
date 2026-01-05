package handlers;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AnnouncementRecordsHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            Connection connection = null;
            try {
                connection = getConnection();

                String query = exchange.getRequestURI().getQuery();
                String date = null;
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("date=")) {
                            date = URLDecoder.decode(param.substring(5), "UTF-8");
                            break;
                        }
                    }
                }

                String sql = "SELECT id, content, announcement_date, publish_time FROM announcements";
                if (date != null && !date.isEmpty()) {
                    sql += " WHERE announcement_date = ?";
                }
                sql += " ORDER BY publish_time DESC";

                PreparedStatement pstmt = connection.prepareStatement(sql);
                if (date != null && !date.isEmpty()) {
                    pstmt.setString(1, date);
                }

                ResultSet rs = pstmt.executeQuery();

                JSONArray recordsArray = new JSONArray();
                while (rs.next()) {
                    JSONObject record = new JSONObject();
                    record.put("id", rs.getInt("id"));
                    record.put("content", rs.getString("content"));
                    record.put("announcement_date", rs.getDate("announcement_date").toString());
                    record.put("publish_time", rs.getTimestamp("publish_time").toString());
                    recordsArray.put(record);
                }

                rs.close();
                pstmt.close();
                sendResponse(exchange, 200, recordsArray.toString());
            } catch (Exception e) {
                e.printStackTrace();
                JSONObject errorResponse = createErrorResponse("服务器内部错误: " + e.getMessage());
                sendResponse(exchange, 500, errorResponse.toString());
            } finally {
                closeConnection(connection);
            }
        } else {
            handleUnsupportedMethod(exchange);
        }
    }
}
