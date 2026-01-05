package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.UserDao;
import model.User;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;

public class ChangePasswordHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (isPostRequest(exchange)) {
            Connection connection = null;
            try {
                connection = getConnection();

                JSONObject request = parseRequestBody(exchange);

                User user = new User();
                user.setUserName(request.getString("userName"));
                user.setPassword(request.getString("oldPassword"));
                user.setPower(request.getString("power"));

                UserDao userDao = new UserDao();
                int result = userDao.updatePassword(connection, user, request.getString("newPassword"));

                JSONObject response = new JSONObject();
                response.put("success", result > 0);
                response.put("message", result > 0 ? "密码修改成功" : "原密码错误");

                sendResponse(exchange, 200, response.toString());
            } catch (Exception e) {
                e.printStackTrace();
                JSONObject errorResponse = createErrorResponse("服务器内部错误");
                sendResponse(exchange, 500, errorResponse.toString());
            } finally {
                closeConnection(connection);
            }
        } else {
            handleUnsupportedMethod(exchange);
        }
    }
}
