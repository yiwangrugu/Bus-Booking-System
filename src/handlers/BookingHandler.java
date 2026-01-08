package handlers;

import com.sun.net.httpserver.HttpExchange;
import dao.BookDao;
import dao.BusDao;
import dao.PassDao;
import model.BookTicket;
import model.Bus;
import model.Passenger;
import org.json.JSONObject;
import Util.LockManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;

public class BookingHandler extends BaseHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (isPostRequest(exchange)) {
            Connection connection = null;
            int bno = 0;
            try {
                connection = getConnection();

                JSONObject request = parseRequestBody(exchange);

                String userName = request.getString("userName");

                if (userName == null || userName.trim().isEmpty()) {
                    JSONObject errorResponse = createErrorResponse("缺少userName参数");
                    sendResponse(exchange, 400, errorResponse.toString());
                    return;
                }

                bno = request.getInt("bno");

                LockManager.getBusLock(bno).lock();
                try {
                    BusDao busDao = new BusDao();
                    Bus bus = busDao.getBus(connection, bno);

                    if (bus == null) {
                        JSONObject errorResponse = createErrorResponse("车次不存在");
                        sendResponse(exchange, 400, errorResponse.toString());
                        return;
                    }

                    BookDao bookDao = new BookDao();
                    int bookedSeats = bookDao.getno(connection, bno) - 1;
                    int remainingSeats = bus.getSeat() - bookedSeats;

                    if (remainingSeats <= 0) {
                        JSONObject errorResponse = createErrorResponse("余票不足");
                        sendResponse(exchange, 400, errorResponse.toString());
                        return;
                    }

                    Passenger passenger = new Passenger();
                    passenger.setUserName(userName);
                    passenger.setIdno(request.getString("idno"));
                    passenger.setName(request.getString("passengerName"));
                    passenger.setPhone(request.getString("passengerPhone"));

                    System.out.println("购票数据 - idno: " + request.getString("idno") +
                            ", passengerName: " + request.getString("passengerName") +
                            ", passengerPhone: " + request.getString("passengerPhone"));

                    PassDao passDao = new PassDao();
                    passDao.addPass(connection, passenger);

                    BookTicket bookTicket = new BookTicket();
                    bookTicket.setBno(bno);
                    bookTicket.setUserName(userName);
                    bookTicket.setIdno(request.getString("idno"));
                    bookTicket.setBdate(new Date(System.currentTimeMillis()));
                    bookTicket.setBtime(new Time(System.currentTimeMillis()));
                    bookTicket.setPassengerName(request.getString("passengerName"));
                    bookTicket.setPassengerPhone(request.getString("passengerPhone"));

                    boolean success = bookDao.addBookTicket(connection, bookTicket);

                    JSONObject response = new JSONObject();
                    response.put("success", success);
                    response.put("message", success ? "订票成功" : "订票失败");

                    sendResponse(exchange, 200, response.toString());
                } finally {
                    LockManager.releaseBusLock(bno);
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
