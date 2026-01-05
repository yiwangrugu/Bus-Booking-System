package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.RefundDao;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RefundsHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (method.equals("OPTIONS")) {
            sendResponse(exchange, 200, "");
            return;
        }

        if (method.equals("GET") && path.equals("/api/refunds/all")) {
            handleGetAllRefunds(exchange);
        } else if (method.equals("GET") && path.equals("/api/refunds/search")) {
            handleSearchRefunds(exchange);
        } else if (method.equals("GET")) {
            handleGetUserRefunds(exchange);
        } else {
            JSONObject errorResponse = createErrorResponse("不支持的请求方法或路径");
            sendResponse(exchange, 405, errorResponse.toString());
        }
    }

    private void handleGetAllRefunds(HttpExchange exchange) throws IOException {
        Connection connection = null;
        try {
            connection = getConnection();

            String sql = "select rt.btno,b.bno,b.date,rt.rdate,rt.rtime,MAX(p.idno) as idno,MAX(p.name) as name,MAX(p.tel) as tel,MAX(b.staName) as staName,MAX(b.endName) as endName,MAX(b.price) as price from refund_ticket rt left join bus b on rt.bno=b.bno left join passenger p on rt.idno=p.idno group by rt.btno";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            JSONArray refundsArray = new JSONArray();
            while (rs.next()) {
                JSONObject refundJson = new JSONObject();
                refundJson.put("btno", rs.getInt("btno"));
                refundJson.put("bno", rs.getInt("bno"));

                java.sql.Date busDate = rs.getDate("date");
                refundJson.put("date", busDate != null ? busDate.toString() : "未知");

                refundJson.put("rdate", rs.getDate("rdate").toString());
                refundJson.put("rtime", rs.getTime("rtime").toString());
                refundJson.put("idno", rs.getString("idno"));
                refundJson.put("passengerName", rs.getString("name") != null ? rs.getString("name") : "");
                refundJson.put("passengerPhone", rs.getString("tel") != null ? rs.getString("tel") : "");
                refundJson.put("staName", rs.getString("staName") != null ? rs.getString("staName") : "");
                refundJson.put("endName", rs.getString("endName") != null ? rs.getString("endName") : "");
                refundJson.put("price", rs.getFloat("price"));
                refundsArray.put(refundJson);
            }

            rs.close();
            pstmt.close();
            sendResponse(exchange, 200, refundsArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = createErrorResponse("服务器内部错误");
            sendResponse(exchange, 500, errorResponse.toString());
        } finally {
            closeConnection(connection);
        }
    }

    private void handleSearchRefunds(HttpExchange exchange) throws IOException {
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

            String sql = "select rt.btno,b.bno,b.date,rt.rdate,rt.rtime,MAX(p.idno) as idno,MAX(p.name) as name,MAX(p.tel) as tel,MAX(b.staName) as staName,MAX(b.endName) as endName,MAX(b.price) as price from refund_ticket rt left join bus b on rt.bno=b.bno left join passenger p on rt.idno=p.idno ";
            String whereClause = "";
            String groupBy = " group by rt.btno";

            switch (type) {
                case "订单号":
                    whereClause = "where rt.btno like ?";
                    break;
                case "车次号":
                    whereClause = "where b.bno like ?";
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
                case "退订日期":
                    whereClause = "where rt.rdate = ?";
                    break;
                case "乘客姓名":
                    whereClause = "where p.name like ?";
                    break;
                case "乘客电话":
                    whereClause = "where p.tel like ?";
                    break;
                default:
                    whereClause = "where rt.btno like ? or b.bno like ?";
                    break;
            }

            sql = sql + whereClause + groupBy;
            PreparedStatement pstmt = connection.prepareStatement(sql);

            if (type.equals("订单号") || type.equals("车次号") || type.equals("出发站") ||
                    type.equals("终点站") || type.equals("乘客姓名") || type.equals("乘客电话")) {
                pstmt.setString(1, "%" + keyword + "%");
            } else if (type.equals("发车日期") || type.equals("退订日期")) {
                pstmt.setString(1, keyword);
            } else {
                pstmt.setString(1, "%" + keyword + "%");
                pstmt.setString(2, "%" + keyword + "%");
            }

            ResultSet rs = pstmt.executeQuery();

            JSONArray refundsArray = new JSONArray();
            while (rs.next()) {
                JSONObject refundJson = new JSONObject();
                refundJson.put("btno", rs.getInt("btno"));
                refundJson.put("bno", rs.getInt("bno"));

                java.sql.Date busDate = rs.getDate("date");
                refundJson.put("date", busDate != null ? busDate.toString() : "未知");

                refundJson.put("rdate", rs.getDate("rdate").toString());
                refundJson.put("rtime", rs.getTime("rtime").toString());
                refundJson.put("idno", rs.getString("idno"));
                refundJson.put("passengerName", rs.getString("name") != null ? rs.getString("name") : "");
                refundJson.put("passengerPhone", rs.getString("tel") != null ? rs.getString("tel") : "");
                refundJson.put("staName", rs.getString("staName") != null ? rs.getString("staName") : "");
                refundJson.put("endName", rs.getString("endName") != null ? rs.getString("endName") : "");
                refundJson.put("price", rs.getFloat("price"));
                refundsArray.put(refundJson);
            }

            rs.close();
            pstmt.close();
            sendResponse(exchange, 200, refundsArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = createErrorResponse("服务器内部错误");
            sendResponse(exchange, 500, errorResponse.toString());
        } finally {
            closeConnection(connection);
        }
    }

    private void handleGetUserRefunds(HttpExchange exchange) throws IOException {
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

            String sql = "select r.btno, r.bno, b.staName, b.endName, b.date, b.price, r.rdate, r.rtime, p.name, p.tel "
                    +
                    "from refund_ticket r left join passenger p on r.idno=p.idno and r.userName=p.userName " +
                    "left join bus b on r.bno=b.bno " +
                    "where r.userName=? order by r.btno ASC";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();

            JSONArray refundsArray = new JSONArray();
            while (rs.next()) {
                JSONObject refundJson = new JSONObject();
                refundJson.put("btno", rs.getInt("btno"));
                refundJson.put("bno", rs.getInt("bno"));
                refundJson.put("staName", rs.getString("staName"));
                refundJson.put("endName", rs.getString("endName"));
                refundJson.put("date", rs.getDate("date").toString());
                refundJson.put("price", rs.getFloat("price"));
                refundJson.put("rdate", rs.getDate("rdate").toString());
                refundJson.put("rtime", rs.getTime("rtime").toString());
                refundJson.put("passengerName", rs.getString("name") != null ? rs.getString("name") : "");
                refundJson.put("passengerPhone", rs.getString("tel") != null ? rs.getString("tel") : "");
                refundsArray.put(refundJson);
            }

            rs.close();
            pstmt.close();
            sendResponse(exchange, 200, refundsArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject errorResponse = createErrorResponse("服务器内部错误");
            sendResponse(exchange, 500, errorResponse.toString());
        } finally {
            closeConnection(connection);
        }
    }
}
