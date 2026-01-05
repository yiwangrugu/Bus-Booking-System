package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import Util.DbUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;

public abstract class BaseHandler implements HttpHandler {

    protected void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, response.getBytes("UTF-8").length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes("UTF-8"));
        }
    }

    protected JSONObject parseRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String requestBody = new String(is.readAllBytes(), "UTF-8");
            return new JSONObject(requestBody);
        }
    }

    protected Connection getConnection() throws Exception {
        DbUtil dbUtil = new DbUtil();
        return dbUtil.getCon();
    }

    protected void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                DbUtil dbUtil = new DbUtil();
                dbUtil.closeCon(connection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected JSONObject createErrorResponse(String message) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    protected JSONObject createSuccessResponse(String message) {
        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("message", message);
        return response;
    }

    protected boolean isPostRequest(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }

    protected void handleUnsupportedMethod(HttpExchange exchange) throws IOException {
        JSONObject response = createErrorResponse("不支持的请求方法");
        sendResponse(exchange, 405, response.toString());
    }
}
