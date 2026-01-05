package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.UserDao;
import model.User;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;

public class RegisterHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (isPostRequest(exchange)) {
            Connection connection = null;
            try {
                connection = getConnection();

                JSONObject request = parseRequestBody(exchange);

                User user = new User();
                user.setUserName(request.getString("userName"));
                user.setPassword(request.getString("password"));
                user.setPower(request.getString("power"));

                UserDao userDao = new UserDao();
                int result = userDao.addUser(connection, user);

                JSONObject response = new JSONObject();
                response.put("success", result > 0);
                response.put("message", result > 0 ? "注册成功" : "账号已存在");

                sendResponse(exchange, 200, response.toString());
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
