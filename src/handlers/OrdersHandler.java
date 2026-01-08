package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.BookDao;
import model.BookTicket;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class OrdersHandler extends BaseHandler {
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String method = exchange.getRequestMethod();

    if (method.equals("GET") && path.equals("/api/orders/all")) {
      handleGetAllOrders(exchange);
    } else if (method.equals("GET") && path.equals("/api/orders/search")) {
      handleSearchOrders(exchange);
    } else if (method.equals("DELETE") && path.startsWith("/api/orders/")) {
      handleDeleteOrder(exchange, path);
    } else if (method.equals("GET")) {
      handleGetUserOrders(exchange);
    } else {
      handleUnsupportedMethod(exchange);
    }
  }

  private void handleGetAllOrders(HttpExchange exchange) throws IOException {
    Connection connection = null;
    try {
      connection = getConnection();

      String sql = "select bt.btno,bt.bno,b.staName,b.endName,b.date,b.time,b.price,bt.bdate,bt.btime,bt.idno,bt.passengerName,bt.passengerPhone "
          +
          "from book_ticket bt left join bus b on bt.bno=b.bno "
          +
          "order by bt.btno ASC";
      PreparedStatement pstmt = connection.prepareStatement(sql);
      ResultSet rs = pstmt.executeQuery();

      JSONArray ordersArray = new JSONArray();
      while (rs.next()) {
        JSONObject orderJson = new JSONObject();
        orderJson.put("orderId", rs.getInt("btno"));
        orderJson.put("busId", rs.getInt("bno"));
        orderJson.put("staName", rs.getString("staName"));
        orderJson.put("endName", rs.getString("endName"));
        orderJson.put("date", rs.getDate("date").toString());
        orderJson.put("time", rs.getTime("time").toString());
        orderJson.put("price", rs.getFloat("price"));
        orderJson.put("bookDate", rs.getDate("bdate").toString());
        orderJson.put("bookTime", rs.getTime("btime").toString());
        orderJson.put("idno", rs.getString("idno") != null ? rs.getString("idno") : "");
        orderJson.put("passengerName", rs.getString("passengerName") != null ? rs.getString("passengerName") : "");
        orderJson.put("passengerPhone", rs.getString("passengerPhone") != null ? rs.getString("passengerPhone") : "");
        ordersArray.put(orderJson);
      }

      rs.close();
      pstmt.close();
      sendResponse(exchange, 200, ordersArray.toString());
    } catch (Exception e) {
      e.printStackTrace();
      JSONObject errorResponse = createErrorResponse("服务器内部错误");
      sendResponse(exchange, 500, errorResponse.toString());
    } finally {
      closeConnection(connection);
    }
  }

  private void handleSearchOrders(HttpExchange exchange) throws IOException {
    Connection connection = null;
    try {
      connection = getConnection();

      String query = exchange.getRequestURI().getQuery();
      String keyword = null;
      String type = null;
      if (query != null) {
        for (String param : query.split("&")) {
          if (param.startsWith("keyword=")) {
            keyword = URLDecoder.decode(param.substring(8), "UTF-8");
          } else if (param.startsWith("type=")) {
            type = URLDecoder.decode(param.substring(5), "UTF-8");
          }
        }
      }

      String sql = "select bt.btno,bt.bno,b.staName,b.endName,b.date,b.time,b.price,bt.bdate,bt.btime,bt.idno,bt.passengerName,bt.passengerPhone "
          +
          "from book_ticket bt left join bus b on bt.bno=b.bno ";
      String whereClause = "";
      String orderBy = " order by bt.btno ASC";

      switch (type) {
        case "订单号":
          whereClause = "where bt.btno like ?";
          break;
        case "车次号":
          whereClause = "where bt.bno like ?";
          break;
        case "出发站":
          whereClause = "where b.staName like ?";
          break;
        case "终点站":
          whereClause = "where b.endName like ?";
          break;
        case "发车日期":
          whereClause = "where b.date = ?";
          break;
        case "乘客姓名":
          whereClause = "where bt.passengerName like ?";
          break;
        case "乘客电话":
          whereClause = "where bt.passengerPhone like ?";
          break;
        case "身份证":
          whereClause = "where bt.idno like ?";
          break;
        default:
          whereClause = "where bt.btno like ? or bt.bno like ?";
          break;
      }

      sql = sql + whereClause + orderBy;
      PreparedStatement pstmt = connection.prepareStatement(sql);

      if (type.equals("订单号") || type.equals("车次号") || type.equals("出发站") ||
          type.equals("终点站") || type.equals("乘客姓名") || type.equals("乘客电话") || type.equals("身份证")) {
        pstmt.setString(1, "%" + keyword + "%");
      } else if (type.equals("发车日期")) {
        pstmt.setString(1, keyword);
      } else {
        pstmt.setString(1, "%" + keyword + "%");
        pstmt.setString(2, "%" + keyword + "%");
      }

      ResultSet rs = pstmt.executeQuery();

      JSONArray ordersArray = new JSONArray();
      while (rs.next()) {
        JSONObject orderJson = new JSONObject();
        orderJson.put("orderId", rs.getInt("btno"));
        orderJson.put("busId", rs.getInt("bno"));
        orderJson.put("staName", rs.getString("staName"));
        orderJson.put("endName", rs.getString("endName"));
        orderJson.put("date", rs.getDate("date").toString());
        orderJson.put("time", rs.getTime("time").toString());
        orderJson.put("price", rs.getFloat("price"));
        orderJson.put("bookDate", rs.getDate("bdate").toString());
        orderJson.put("bookTime", rs.getTime("btime").toString());
        orderJson.put("idno", rs.getString("idno") != null ? rs.getString("idno") : "");
        orderJson.put("passengerName", rs.getString("passengerName") != null ? rs.getString("passengerName") : "");
        orderJson.put("passengerPhone", rs.getString("passengerPhone") != null ? rs.getString("passengerPhone") : "");
        ordersArray.put(orderJson);
      }

      rs.close();
      pstmt.close();
      sendResponse(exchange, 200, ordersArray.toString());
    } catch (Exception e) {
      e.printStackTrace();
      JSONObject errorResponse = createErrorResponse("服务器内部错误");
      sendResponse(exchange, 500, errorResponse.toString());
    } finally {
      closeConnection(connection);
    }
  }

  private void handleDeleteOrder(HttpExchange exchange, String path) throws IOException {
    Connection connection = null;
    try {
      connection = getConnection();

      String orderIdStr = path.substring(12);
      int orderId = Integer.parseInt(orderIdStr);

      BookDao bookDao = new BookDao();
      BookTicket bookTicket = new BookTicket();
      bookTicket.setBtno(orderId);

      int result = bookDao.refund(connection, bookTicket);

      JSONObject response = new JSONObject();
      if (result > 0) {
        response.put("success", true);
        response.put("message", "订单删除成功");
        sendResponse(exchange, 200, response.toString());
      } else {
        response.put("success", false);
        response.put("message", "订单删除失败");
        sendResponse(exchange, 404, response.toString());
      }
    } catch (NumberFormatException e) {
      JSONObject errorResponse = createErrorResponse("无效的订单ID");
      sendResponse(exchange, 400, errorResponse.toString());
    } catch (Exception e) {
      e.printStackTrace();
      JSONObject errorResponse = createErrorResponse("服务器内部错误");
      sendResponse(exchange, 500, errorResponse.toString());
    } finally {
      closeConnection(connection);
    }
  }

  private void handleGetUserOrders(HttpExchange exchange) throws IOException {
    Connection connection = null;
    try {
      connection = getConnection();

      String query = exchange.getRequestURI().getQuery();
      String userName = null;
      if (query != null) {
        for (String param : query.split("&")) {
          if (param.startsWith("userName=")) {
            userName = URLDecoder.decode(param.substring(9), "UTF-8");
          }
        }
      }

      if (userName == null || userName.isEmpty()) {
        JSONObject errorResponse = createErrorResponse("缺少userName参数");
        sendResponse(exchange, 400, errorResponse.toString());
        return;
      }

      String sql = "select bt.btno,bt.bno,b.staName,b.endName,b.date,b.time,b.price,bt.bdate,bt.btime,bt.idno,bt.passengerName,bt.passengerPhone "
          +
          "from book_ticket bt left join bus b on bt.bno=b.bno "
          +
          "where bt.userName=? order by bt.btno ASC";
      PreparedStatement pstmt = connection.prepareStatement(sql);
      pstmt.setString(1, userName);
      ResultSet rs = pstmt.executeQuery();

      JSONArray ordersArray = new JSONArray();
      while (rs.next()) {
        JSONObject orderJson = new JSONObject();
        orderJson.put("orderId", rs.getInt("btno"));
        orderJson.put("busId", rs.getInt("bno"));
        orderJson.put("staName", rs.getString("staName"));
        orderJson.put("endName", rs.getString("endName"));
        orderJson.put("date", rs.getDate("date").toString());
        orderJson.put("time", rs.getTime("time").toString());
        orderJson.put("price", rs.getFloat("price"));
        orderJson.put("bookDate", rs.getDate("bdate").toString());
        orderJson.put("bookTime", rs.getTime("btime").toString());
        orderJson.put("idno", rs.getString("idno") != null ? rs.getString("idno") : "");
        orderJson.put("passengerName", rs.getString("passengerName") != null ? rs.getString("passengerName") : "");
        orderJson.put("passengerPhone", rs.getString("passengerPhone") != null ? rs.getString("passengerPhone") : "");
        ordersArray.put(orderJson);
      }

      rs.close();
      pstmt.close();
      sendResponse(exchange, 200, ordersArray.toString());
    } catch (Exception e) {
      e.printStackTrace();
      JSONObject errorResponse = createErrorResponse("服务器内部错误");
      sendResponse(exchange, 500, errorResponse.toString());
    } finally {
      closeConnection(connection);
    }
  }
}
