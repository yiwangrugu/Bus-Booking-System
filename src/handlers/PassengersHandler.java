package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.PassDao;
import model.Passenger;
import model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;

public class PassengersHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        Connection connection = null;

        try {
            connection = getConnection();

            if ("OPTIONS".equals(method)) {
                sendResponse(exchange, 200, "");
                return;
            }

            if ("GET".equals(method)) {
                handleGetRequest(exchange, connection);
            } else if ("POST".equals(method)) {
                handlePostRequest(exchange, connection);
            } else if ("DELETE".equals(method)) {
                handleDeleteRequest(exchange, connection);
            } else {
                handleUnsupportedMethod(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = createErrorResponse("服务器内部错误: " + e.getMessage());
            sendResponse(exchange, 500, errorResponse.toString());
        } finally {
            closeConnection(connection);
        }
    }

    private void handleGetRequest(HttpExchange exchange, Connection connection) throws Exception {
        String query = exchange.getRequestURI().getQuery();
        String userName = null;

        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("userName=")) {
                    userName = param.substring(9);
                    break;
                }
            }
        }

        if (userName == null) {
            JSONObject errorResponse = createErrorResponse("用户名参数缺失");
            sendResponse(exchange, 400, errorResponse.toString());
            return;
        }

        PassDao passDao = new PassDao();
        User user = new User();
        user.setUserName(userName);
        List<Passenger> passengers = passDao.list(connection, user);

        JSONArray passengersArray = new JSONArray();
        for (Passenger passenger : passengers) {
            JSONObject passengerJson = new JSONObject();
            passengerJson.put("userName", passenger.getUserName());
            passengerJson.put("idno", passenger.getIdno());
            passengerJson.put("name", passenger.getName());
            passengerJson.put("phone", passenger.getPhone());
            passengersArray.put(passengerJson);
        }

        sendResponse(exchange, 200, passengersArray.toString());
    }

    private void handlePostRequest(HttpExchange exchange, Connection connection) throws Exception {
        JSONObject request = parseRequestBody(exchange);

        Passenger passenger = new Passenger();
        passenger.setUserName(request.getString("userName"));
        passenger.setIdno(request.getString("idno"));
        passenger.setName(request.getString("name"));
        passenger.setPhone(request.getString("phone"));

        PassDao passDao = new PassDao();
        passDao.addPass(connection, passenger);

        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("message", "添加常用乘客成功");

        sendResponse(exchange, 200, response.toString());
    }

    private void handleDeleteRequest(HttpExchange exchange, Connection connection) throws Exception {
        String query = exchange.getRequestURI().getQuery();
        String userName = null;
        String idno = null;

        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("userName=")) {
                    userName = param.substring(9);
                } else if (param.startsWith("idno=")) {
                    idno = param.substring(5);
                }
            }
        }

        if (userName == null || idno == null) {
            JSONObject errorResponse = createErrorResponse("用户名或身份证参数缺失");
            sendResponse(exchange, 400, errorResponse.toString());
            return;
        }

        PassDao passDao = new PassDao();
        Passenger passenger = new Passenger();
        passenger.setUserName(userName);
        passenger.setIdno(idno);
        passDao.deletePass(connection, passenger);

        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("message", "删除常用乘客成功");

        sendResponse(exchange, 200, response.toString());
    }
}
