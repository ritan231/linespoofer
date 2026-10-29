package my.shanunid.linespoofer.webui;

import android.content.Context;

import my.shanunid.linespoofer.utils.AppLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight local embedded HTTP server for on-device LineSpoofer dashboard.
 * Strictly binds to loopback (127.0.0.1:9229) to eliminate external network exposure.
 */
public final class WebUiServer {

    private static final String TAG = "LineSpoofer-WebUI";
    public static final int DEFAULT_PORT = 9229;

    private static volatile boolean isRunning = false;
    private static ServerSocket serverSocket;
    private static Thread serverThread;
    private static ExecutorService workerPool;

    private WebUiServer() {
    }

    public static synchronized void start(Context context) {
        if (isRunning) {
            return;
        }

        final Context appContext = context.getApplicationContext();
        isRunning = true;
        workerPool = Executors.newFixedThreadPool(2);

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), DEFAULT_PORT), 10);
                AppLogger.info(TAG, "WebUI server listening on http://127.0.0.1:" + DEFAULT_PORT);

                while (isRunning && !serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    handleClientAsync(appContext, clientSocket);
                }
            } catch (Exception exception) {
                if (isRunning) {
                    AppLogger.logError(TAG, "WebUI server error: " + exception.getMessage(), exception);
                }
            } finally {
                stop();
            }
        }, "linespoofer-webui-daemon");

        serverThread.setDaemon(true);
        serverThread.start();
    }

    public static synchronized void stop() {
        isRunning = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {
            }
            serverSocket = null;
        }
        if (workerPool != null) {
            try {
                workerPool.shutdownNow();
            } catch (Exception ignored) {
            }
            workerPool = null;
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

    private static void handleClientAsync(Context context, Socket client) {
        ExecutorService pool = workerPool;
        if (pool == null || pool.isShutdown()) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
            return;
        }

        pool.execute(() -> {
            try (Socket socket = client;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 OutputStream output = socket.getOutputStream()) {

                String requestLine = reader.readLine();
                if (requestLine == null || requestLine.isEmpty()) {
                    return;
                }

                String[] parts = requestLine.split(" ");
                if (parts.length < 2) {
                    return;
                }

                String path = parts[1];

                if ("/api/status".equals(path)) {
                    serveJson(output, "{\"status\":\"ok\",\"engine\":\"LineSpoofer\",\"author\":\"@shanun_id\",\"susfs\":\"2.3.0\"}");
                } else if ("/api/logs".equals(path)) {
                    serveLogs(context, output);
                } else {
                    serveIndexHtml(context, output);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private static void serveIndexHtml(Context context, OutputStream output) throws Exception {
        byte[] content;
        try (InputStream inputStream = context.getAssets().open("webui/index.html")) {
            content = new byte[inputStream.available()];
            int read = inputStream.read(content);
        } catch (Exception exception) {
            content = "<h1>LineSpoofer Dashboard</h1><p>Credit: @shanun_id</p>".getBytes(StandardCharsets.UTF_8);
        }

        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "Connection: close\r\n\r\n";

        output.write(headers.getBytes(StandardCharsets.UTF_8));
        output.write(content);
        output.flush();
    }

    private static void serveJson(OutputStream output, String json) throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + bytes.length + "\r\n" +
                "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.flush();
    }

    private static void serveLogs(Context context, OutputStream output) throws Exception {
        File logFile = AppLogger.getLogFile(context);
        byte[] bytes;
        if (logFile != null && logFile.exists()) {
            bytes = new byte[(int) Math.min(logFile.length(), 65536L)];
            try (FileInputStream fis = new FileInputStream(logFile)) {
                int read = fis.read(bytes);
            }
        } else {
            bytes = "[INFO] Log is currently empty.\n".getBytes(StandardCharsets.UTF_8);
        }

        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "Content-Length: " + bytes.length + "\r\n" +
                "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.flush();
    }
}
