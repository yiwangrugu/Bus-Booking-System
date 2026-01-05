package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.UserDao;
import model.User;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;

public class LoginHandler extends BaseHandler {
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
                User resultUser = userDao.login(connection, user);

                JSONObject response = new JSONObject();
                if (resultUser != null) {
                    JSONObject userJson = new JSONObject();
                    userJson.put("userName", resultUser.getUserName());
                    userJson.put("password", resultUser.getPassword());
                    userJson.put("power", resultUser.getPower());

                    response.put("success", true);
                    response.put("message", "登录成功");
                    response.put("user", userJson);
                } else {
                    response.put("success", false);
                    response.put("message", "账号或密码错误");
                    response.put("user", JSONObject.NULL);
                }

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
