package handlers;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StaticFileHandler implements HttpHandler {
    private final String webRoot;

    public StaticFileHandler(String webRoot) {
        this.webRoot = webRoot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.startsWith("/api/")) {
            return;
        }

        if (path.equals("/")) {
            exchange.getResponseHeaders().set("Location", "/html/login.html");
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        String fileName = path.substring(1);
        File file = new File(webRoot + File.separator + fileName);

        if (!file.exists() || file.isDirectory()) {
            exchange.sendResponseHeaders(404, -1);
            exchange.getResponseBody().close();
            return;
        }

        String contentType = getContentType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        exchange.sendResponseHeaders(200, file.length());
        try (OutputStream os = exchange.getResponseBody();
                FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else {
            return "application/octet-stream";
        }
    }
}
