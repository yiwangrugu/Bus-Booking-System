package handlers;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CancelAnnouncementHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (isPostRequest(exchange)) {
            Connection connection = null;
            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
                JSONObject requestJson = new JSONObject(requestBody);

                String announcementDate = requestJson.getString("announcement_date");

                connection = getConnection();

                String updateSql = "UPDATE announcements SET published = 0 WHERE id = (SELECT id FROM (SELECT MAX(id) AS id FROM announcements) AS tmp)";
                PreparedStatement updateStmt = connection.prepareStatement(updateSql);
                int rowsAffected = updateStmt.executeUpdate();
                updateStmt.close();

                JSONObject responseJson = new JSONObject();
                if (rowsAffected > 0) {
                    responseJson.put("success", true);
                    responseJson.put("message", "公告已取消");
                    sendResponse(exchange, 200, responseJson.toString());
                } else {
                    responseJson.put("success", false);
                    responseJson.put("message", "取消公告失败");
                    sendResponse(exchange, 500, responseJson.toString());
                }
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
