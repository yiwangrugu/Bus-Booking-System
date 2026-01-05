package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.BookDao;
import dao.RefundDao;
import model.BookTicket;
import model.RefundTicket;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Time;

public class OrderRefundHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (isPostRequest(exchange)) {
            Connection connection = null;
            try {
                connection = getConnection();

                JSONObject request = parseRequestBody(exchange);

                int orderId = request.getInt("orderId");

                BookDao bookDao = new BookDao();
                BookTicket bookTicket = bookDao.getBook(connection, orderId);
                if (bookTicket == null) {
                    JSONObject response = createErrorResponse("订单不存在");
                    sendResponse(exchange, 404, response.toString());
                    return;
                }

                connection.setAutoCommit(false);
                boolean success = false;

                try {
                    RefundTicket refundTicket = new RefundTicket();
                    refundTicket.setBno(bookTicket.getBno());
                    refundTicket.setBtno(bookTicket.getBtno());
                    refundTicket.setIdno(bookTicket.getIdno());
                    refundTicket.setRdate(new Date(System.currentTimeMillis()));
                    refundTicket.setRtime(new Time(System.currentTimeMillis()));

                    RefundDao refundDao = new RefundDao();
                    RefundTicket resultTicket = refundDao.addRefund(connection, refundTicket);

                    if (resultTicket != null) {
                        int result = bookDao.refund(connection, bookTicket);

                        if (result > 0) {
                            connection.commit();
                            success = true;
                        } else {
                            connection.rollback();
                        }
                    } else {
                        connection.rollback();
                    }
                } catch (Exception e) {
                    if (connection != null) {
                        connection.rollback();
                    }
                    throw e;
                } finally {
                    if (connection != null) {
                        connection.setAutoCommit(true);
                    }
                }

                JSONObject response = new JSONObject();
                response.put("success", success);
                response.put("message", success ? "退票成功" : "退票失败");

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
