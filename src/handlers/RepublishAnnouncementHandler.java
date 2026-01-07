package handlers;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class RepublishAnnouncementHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (isPostRequest(exchange)) {
            Connection connection = null;
            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
                JSONObject requestJson = new JSONObject(requestBody);

                int announcementId = requestJson.getInt("id");

                connection = getConnection();

                // 先将最新一条公告的published设置为0
                String updateMaxSql = "UPDATE announcements SET published = 0 WHERE id = (SELECT id FROM (SELECT MAX(id) AS id FROM announcements) AS tmp)";
                PreparedStatement updateMaxStmt = connection.prepareStatement(updateMaxSql);
                updateMaxStmt.executeUpdate();
                updateMaxStmt.close();

                // 获取要重新发布的公告的内容和日期
                String selectSql = "SELECT content, announcement_date FROM announcements WHERE id = ?";
                PreparedStatement selectStmt = connection.prepareStatement(selectSql);
                selectStmt.setInt(1, announcementId);
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) {
                    String content = rs.getString("content");

                    // 使用今天的日期
                    String today = LocalDate.now().toString();

                    // 新增公告记录
                    String insertSql = "INSERT INTO announcements (content, announcement_date, publish_time, published) VALUES (?, ?, NOW(), 1)";
                    PreparedStatement insertStmt = connection.prepareStatement(insertSql);
                    insertStmt.setString(1, content);
                    insertStmt.setString(2, today);
                    int rowsAffected = insertStmt.executeUpdate();
                    insertStmt.close();

                    JSONObject responseJson = new JSONObject();
                    if (rowsAffected > 0) {
                        responseJson.put("success", true);
                        responseJson.put("message", "公告重新发布成功");
                        sendResponse(exchange, 200, responseJson.toString());
                    } else {
                        responseJson.put("success", false);
                        responseJson.put("message", "重新发布公告失败");
                        sendResponse(exchange, 500, responseJson.toString());
                    }
                } else {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("success", false);
                    responseJson.put("message", "公告不存在");
                    sendResponse(exchange, 500, responseJson.toString());
                }

                rs.close();
                selectStmt.close();
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
