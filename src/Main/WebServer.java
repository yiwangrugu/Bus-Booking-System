package Main;

import com.sun.net.httpserver.HttpServer;
import handlers.*;
import tasks.AutoApproveRefundTask;
import tasks.ClearAnnouncementTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebServer {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new java.net.InetSocketAddress("0.0.0.0", 8080), 0);
            server.setExecutor(Executors.newFixedThreadPool(1000));

            server.createContext("/api/login", new LoginHandler());
            server.createContext("/api/register", new RegisterHandler());
            server.createContext("/api/buses", new BusesHandler());
            server.createContext("/api/booking", new BookingHandler());
            server.createContext("/api/orders", new OrdersHandler());
            server.createContext("/api/change-password", new ChangePasswordHandler());
            server.createContext("/api/orders/refund", new OrderRefundHandler());
            server.createContext("/api/refunds", new RefundsHandler());
            server.createContext("/api/passengers", new PassengersHandler());
            server.createContext("/api/announcement-records", new AnnouncementRecordsHandler());
            server.createContext("/api/publish-announcement", new PublishAnnouncementHandler());
            server.createContext("/api/cancel-announcement", new CancelAnnouncementHandler());
            server.createContext("/api/republish-announcement", new RepublishAnnouncementHandler());
            server.createContext("/api/orders/refund-application", new RefundApplicationHandler());
            server.createContext("/api/refund-applications", new RefundApplicationsHandler());
            server.createContext("/api/admin/refund-applications", new AdminRefundApplicationsHandler());
            server.createContext("/api/admin/refund-records", new AdminRefundRecordsHandler());
            server.createContext("/api/admin/refund-applications/approve/", new ApproveRefundApplicationHandler());
            server.createContext("/api/admin/refund-applications/reject/", new RejectRefundApplicationHandler());

            String userDir = System.getProperty("user.dir");
            java.io.File currentDir = new java.io.File(userDir);
            String webRoot;

            if (currentDir.getName().equalsIgnoreCase("src")) {
                webRoot = currentDir.getParent() + java.io.File.separator + "web";
            } else {
                webRoot = userDir + java.io.File.separator + "web";
            }
            server.createContext("/", new StaticFileHandler(webRoot));

            server.start();
            System.out.println("服务器启动成功，监听端口：" + PORT);
            System.out.println("访问地址：http://localhost:" + PORT);
            System.out.println("最大并发连接数：1000");

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            scheduler.scheduleAtFixedRate(new AutoApproveRefundTask(), 0, 30, TimeUnit.MINUTES);

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime nextMidnight = now.toLocalDate().atTime(0, 0);

            if (now.isAfter(nextMidnight)) {
                nextMidnight = nextMidnight.plusDays(1);
            }

            long initialDelay = java.time.Duration.between(now, nextMidnight).toMillis();

            scheduler.scheduleAtFixedRate(new ClearAnnouncementTask(), initialDelay, 24 * 60 * 60 * 1000,
                    TimeUnit.MILLISECONDS);

            System.out.println("服务器启动完成，正在运行中...");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("服务器正在关闭...");
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        System.err.println("定时任务关闭超时");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                server.stop(0);
                System.out.println("服务器已关闭");
            }));

            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("服务器被中断");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("服务器启动失败：" + e.getMessage());
        }
    }
}
