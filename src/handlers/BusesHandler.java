package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.BusDao;
import model.Bus;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;

public class BusesHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (method.equals("GET")) {
            handleGetRequest(exchange, path);
        } else if (method.equals("POST")) {
            handlePostRequest(exchange);
        } else if (method.equals("DELETE") && path.startsWith("/api/buses/")) {
            handleDeleteRequest(exchange, path);
        } else if (method.equals("PUT") && path.startsWith("/api/buses/")) {
            handlePutRequest(exchange, path);
        } else {
            handleUnsupportedMethod(exchange);
        }
    }

    private void handleGetRequest(HttpExchange exchange, String path) throws IOException {
        Connection connection = null;
        try {
            connection = getConnection();

            if (path.matches("/api/buses/\\d+")) {
                String busNoStr = path.substring(11);
                int busNo = Integer.parseInt(busNoStr);
                handleGetSingleBus(exchange, connection, busNo);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String keyword = null;
            String searchType = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("keyword=")) {
                        keyword = URLDecoder.decode(param.substring(8), "UTF-8");
                    } else if (param.startsWith("type=")) {
                        searchType = URLDecoder.decode(param.substring(5), "UTF-8");
                    }
                }
            }

            BusDao busDao = new BusDao();
            Bus bus = new Bus();
            if (keyword != null) {
                if (searchType != null && searchType.equals("车次号")) {
                    try {
                        int bno = Integer.parseInt(keyword);
                        bus.setBno(bno);
                    } catch (NumberFormatException e) {
                        bus.setStaName(keyword);
                    }
                } else if (searchType != null && searchType.equals("终点站")) {
                    bus.setEndName(keyword);
                } else if (searchType != null && (searchType.equals("发车日期") || searchType.equals("date"))) {
                    try {
                        java.sql.Date date = java.sql.Date.valueOf(keyword);
                        bus.setDate(date);
                    } catch (IllegalArgumentException e) {
                        bus.setStaName(keyword);
                    }
                } else {
                    bus.setStaName(keyword);
                }
            }

            boolean showEnded = path.equals("/api/buses/ended");

            StringBuilder sql;
            sql = new StringBuilder(
                    "SELECT b.bno, b.staName, b.endName, b.date, b.time,b.price,b.seat, COUNT(bt.btno) AS booked  FROM bus b LEFT JOIN book_ticket bt ON b.bno = bt.bno WHERE 1=1 ");

            if (showEnded) {
                sql.append(" AND (b.date < CURRENT_DATE() OR (b.date = CURRENT_DATE() AND b.time < CURRENT_TIME()))");
            } else {
                sql.append(" AND (b.date > CURRENT_DATE() OR (b.date = CURRENT_DATE() AND b.time > CURRENT_TIME()))");
            }

            if (bus.getBno() != 0) {
                sql.append(" AND b.bno = ?");
            }

            if (bus.getDate() != null) {
                sql.append(" AND b.date = ?");
            }

            if (bus.getStaName() != null && !bus.getStaName().isEmpty()) {
                sql.append(" AND b.staName LIKE ?");
            }

            if (bus.getEndName() != null && !bus.getEndName().isEmpty()) {
                sql.append(" AND b.endName LIKE ?");
            }
            sql.append(" GROUP BY b.bno, b.staName, b.endName, b.date, b.time, b.price ORDER BY b.bno;");

            PreparedStatement pstmt = connection.prepareStatement(sql.toString());

            int index = 1;
            if (bus.getBno() != 0) {
                pstmt.setInt(index++, bus.getBno());
            }

            if (bus.getDate() != null) {
                pstmt.setDate(index++, bus.getDate());
            }

            if (bus.getStaName() != null && !bus.getStaName().isEmpty()) {
                pstmt.setString(index++, "%" + bus.getStaName() + "%");
            }

            if (bus.getEndName() != null && !bus.getEndName().isEmpty()) {
                pstmt.setString(index++, "%" + bus.getEndName() + "%");
            }

            ResultSet rs = pstmt.executeQuery();

            JSONArray busesArray = new JSONArray();
            while (rs.next()) {
                JSONObject busJson = new JSONObject();
                busJson.put("bno", rs.getInt("bno"));
                busJson.put("staName", rs.getString("staName"));
                busJson.put("endName", rs.getString("endName"));
                busJson.put("date", rs.getDate("date").toString());
                busJson.put("time", rs.getTime("time").toString());
                busJson.put("price", rs.getFloat("price"));

                int totalSeats;
                int remainSeats;
                try {
                    totalSeats = rs.getInt("seat");
                    int bookedSeats = rs.getInt("booked");
                    remainSeats = totalSeats - bookedSeats;
                } catch (Exception e) {
                    Bus originalBus = busDao.getBus(connection, rs.getInt("bno"));
                    totalSeats = originalBus != null ? originalBus.getSeat() : 0;
                    remainSeats = rs.getInt("res_seat");
                }
                busJson.put("totalSeats", totalSeats);
                busJson.put("remainSeats", remainSeats);
                busesArray.put(busJson);
            }

            rs.close();
            pstmt.close();
            sendResponse(exchange, 200, busesArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = createErrorResponse("服务器内部错误: " + e.getMessage());
            sendResponse(exchange, 500, errorResponse.toString());
        } finally {
            closeConnection(connection);
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        Connection connection = null;
        try {
            connection = getConnection();

            JSONObject request = parseRequestBody(exchange);

            Bus bus = new Bus();
            bus.setStaName(request.getString("startStation"));
            bus.setEndName(request.getString("endStation"));
            bus.setDate(Date.valueOf(request.getString("departureDate")));

            String departureTime = request.getString("departureTime");
            if (departureTime.length() == 5) {
                departureTime += ":00";
            }
            bus.setTime(Time.valueOf(departureTime));

            double price = 0;
            if (request.has("price")) {
                if (request.get("price") instanceof Integer) {
                    price = request.getInt("price");
                } else {
                    price = request.getDouble("price");
                }
            }
            bus.setPrice((float) price);

            int totalSeats = request.getInt("totalSeats");
            bus.setSeat(totalSeats);

            BusDao busDao = new BusDao();
            Bus resultBus = busDao.addBus(connection, bus);

            JSONObject response = new JSONObject();
            response.put("success", resultBus != null);
            response.put("message", resultBus != null ? "车次添加成功" : "车次添加失败");

            sendResponse(exchange, 200, response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = createErrorResponse("服务器内部错误: " + e.getMessage());
            sendResponse(exchange, 500, errorResponse.toString());
        } finally {
            closeConnection(connection);
        }
    }

    private void handleDeleteRequest(HttpExchange exchange, String path) throws IOException {
        Connection connection = null;
        try {
            connection = getConnection();

            String busNoStr = path.substring(11);
            int busNo = Integer.parseInt(busNoStr);

            BusDao busDao = new BusDao();
            Bus bus = new Bus();
            bus.setBno(busNo);

            String sql = "SELECT COUNT(bt.btno) AS booked FROM bus b LEFT JOIN book_ticket bt ON b.bno = bt.bno WHERE b.bno = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, busNo);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int bookedSeats = rs.getInt("booked");
                rs.close();
                pstmt.close();
                if (bookedSeats > 0) {
                    JSONObject errorResponse = createErrorResponse("该车次已有售出记录，无法删除");
                    sendResponse(exchange, 400, errorResponse.toString());
                    return;
                }
            } else {
                rs.close();
                pstmt.close();
                JSONObject errorResponse = createErrorResponse("车次不存在");
                sendResponse(exchange, 404, errorResponse.toString());
                return;
            }

            int result = busDao.delete(connection, bus);

            JSONObject response = new JSONObject();
            response.put("success", result > 0);
            response.put("message", result > 0 ? "车次删除成功" : "车次删除失败");

            sendResponse(exchange, 200, response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = createErrorResponse("服务器内部错误");
            sendResponse(exchange, 500, errorResponse.toString());
        } finally {
            closeConnection(connection);
        }
    }

    private void handlePutRequest(HttpExchange exchange, String path) throws IOException {
        Connection connection = null;
        try {
            connection = getConnection();

            String busNoStr = path.substring(11);
            int busNo = Integer.parseInt(busNoStr);

            BusDao busDao = new BusDao();
            Bus bus = busDao.getBus(connection, busNo);

            if (bus == null) {
                JSONObject errorResponse = createErrorResponse("车次不存在");
                sendResponse(exchange, 404, errorResponse.toString());
                return;
            }

            String sql = "SELECT COUNT(bt.btno) AS booked FROM bus b LEFT JOIN book_ticket bt ON b.bno = bt.bno WHERE b.bno = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, busNo);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int bookedSeats = rs.getInt("booked");
                rs.close();
                pstmt.close();
                if (bookedSeats > 0) {
                    JSONObject errorResponse = createErrorResponse("该车次已有售出记录，无法编辑");
                    sendResponse(exchange, 400, errorResponse.toString());
                    return;
                }
            } else {
                rs.close();
                pstmt.close();
            }

            JSONObject request = parseRequestBody(exchange);

            Bus updateBus = new Bus();
            updateBus.setBno(busNo);
            updateBus.setStaName(request.getString("startStation"));
            updateBus.setEndName(request.getString("endStation"));
            updateBus.setDate(Date.valueOf(request.getString("departureDate")));

            String departureTime = request.getString("departureTime");
            if (departureTime.length() == 5) {
                departureTime += ":00";
            }
            updateBus.setTime(Time.valueOf(departureTime));

            updateBus.setPrice((float) request.getDouble("price"));
            updateBus.setSeat(request.getInt("totalSeats"));

            int result = busDao.update(connection, updateBus);

            JSONObject response = new JSONObject();
            response.put("success", result > 0);
            response.put("message", result > 0 ? "车次编辑成功" : "车次编辑失败");

            sendResponse(exchange, 200, response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = createErrorResponse("服务器内部错误: " + e.getMessage());
            sendResponse(exchange, 500, errorResponse.toString());
        } finally {
            closeConnection(connection);
        }
    }

    private void handleGetSingleBus(HttpExchange exchange, Connection connection, int busNo) throws IOException {
        try {
            BusDao busDao = new BusDao();
            Bus bus = busDao.getBus(connection, busNo);

            if (bus == null) {
                JSONObject errorResponse = createErrorResponse("车次不存在");
                sendResponse(exchange, 404, errorResponse.toString());
                return;
            }

            String sql = "SELECT COUNT(bt.btno) AS booked FROM bus b LEFT JOIN book_ticket bt ON b.bno = bt.bno WHERE b.bno = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, busNo);
            ResultSet rs = pstmt.executeQuery();

            int bookedSeats = 0;
            if (rs.next()) {
                bookedSeats = rs.getInt("booked");
            }
            rs.close();
            pstmt.close();

            int remainSeats = bus.getSeat() - bookedSeats;

            JSONObject busJson = new JSONObject();
            busJson.put("bno", bus.getBno());
            busJson.put("staName", bus.getStaName());
            busJson.put("endName", bus.getEndName());
            busJson.put("date", bus.getDate().toString());
            busJson.put("time", bus.getTime().toString());
            busJson.put("price", bus.getPrice());
            busJson.put("totalSeats", bus.getSeat());
            busJson.put("remainSeats", remainSeats);

            sendResponse(exchange, 200, busJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = createErrorResponse("服务器内部错误: " + e.getMessage());
            sendResponse(exchange, 500, errorResponse.toString());
        }
    }
}
