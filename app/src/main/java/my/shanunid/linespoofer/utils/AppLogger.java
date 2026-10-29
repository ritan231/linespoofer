package my.shanunid.linespoofer.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Centralized, thread-safe, asynchronous file logger for LineSpoofer.
 *
 * Appends diagnostic events to internal storage (`linespoofer.log`)
 * with automatic log rotation at 2 MB (`linespoofer.log.old`).
 * Dual-dispatches to logcat and XposedBridge when available.
 */
public final class AppLogger {

    public enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private static final String LOG_FILE_NAME = "linespoofer.log";
    private static final String OLD_LOG_FILE_NAME = "linespoofer.log.old";
    private static final String FALLBACK_DIR_PRIMARY = "/data/user/0/my.shanunid.linespoofer/files";
    private static final String FALLBACK_DIR_SECONDARY = "/data/data/my.shanunid.linespoofer/files";
    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L; // 2 MB

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "LineSpoofer-Logger");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile Context applicationContext;

    private AppLogger() {}

    /**
     * Initializes the logger with an application context for path resolution.
     */
    public static void init(Context context) {
        if (context != null && applicationContext == null) {
            applicationContext = context.getApplicationContext();
        }
    }

    public static void log(String tag, String message) {
        log(Level.INFO, tag, message, null);
    }

    public static void info(String tag, String message) {
        log(Level.INFO, tag, message, null);
    }

    public static void debug(String tag, String message) {
        log(Level.DEBUG, tag, message, null);
    }

    public static void warn(String tag, String message) {
        log(Level.WARN, tag, message, null);
    }

    public static void error(String tag, String message) {
        log(Level.ERROR, tag, message, null);
    }

    public static void logError(String tag, String message, Throwable throwable) {
        log(Level.ERROR, tag, message, throwable);
    }

    public static void log(Level level, String tag, String message) {
        log(level, tag, message, null);
    }

    public static void log(Level level, String tag, String message, Throwable throwable) {
        final String safeTag = (tag == null || tag.trim().isEmpty()) ? "LineSpoofer" : tag.trim();
        final String safeMessage = message == null ? "" : message;

        // 1. Android Logcat output
        try {
            switch (level) {
                case DEBUG:
                    if (throwable != null) Log.d(safeTag, safeMessage, throwable);
                    else Log.d(safeTag, safeMessage);
                    break;
                case WARN:
                    if (throwable != null) Log.w(safeTag, safeMessage, throwable);
                    else Log.w(safeTag, safeMessage);
                    break;
                case ERROR:
                    if (throwable != null) Log.e(safeTag, safeMessage, throwable);
                    else Log.e(safeTag, safeMessage);
                    break;
                case INFO:
                default:
                    if (throwable != null) Log.i(safeTag, safeMessage, throwable);
                    else Log.i(safeTag, safeMessage);
                    break;
            }
        } catch (Throwable ignored) {
        }

        // 2. LSPosed / XposedBridge log output (if running in hooked environment)
        try {
            String xposedMessage = "[" + level.name() + "] " + safeTag + ": " + safeMessage;
            if (throwable != null) {
                xposedMessage += "\n" + Log.getStackTraceString(throwable);
            }
            de.robv.android.xposed.XposedBridge.log(xposedMessage);
        } catch (Throwable ignored) {
            // XposedBridge not available when running in standalone companion app
        }

        // 3. Asynchronous file logging with rotation
        final long timestamp = System.currentTimeMillis();
        EXECUTOR.submit(() -> appendToFile(level, safeTag, safeMessage, throwable, timestamp));
    }

    private static synchronized void appendToFile(
            Level level,
            String tag,
            String message,
            Throwable throwable,
            long timestamp
    ) {
        File logFile = getLogFile(applicationContext);
        if (logFile == null) {
            return;
        }

        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // Perform 2 MB log rotation
            if (logFile.exists() && logFile.length() >= MAX_FILE_SIZE_BYTES) {
                rotateLog(logFile);
            }

            String formattedTime;
            synchronized (DATE_FORMAT) {
                formattedTime = DATE_FORMAT.format(new Date(timestamp));
            }

            StringBuilder entry = new StringBuilder();
            entry.append(formattedTime)
                 .append(" [").append(level.name()).append("] ")
                 .append("[").append(tag).append("] ")
                 .append(message)
                 .append('\n');

            if (throwable != null) {
                StringWriter stringWriter = new StringWriter();
                throwable.printStackTrace(new PrintWriter(stringWriter));
                entry.append(stringWriter.toString()).append('\n');
            }

            try (FileOutputStream outputStream = new FileOutputStream(logFile, true)) {
                outputStream.write(entry.toString().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        } catch (Throwable ignored) {
            // Sandboxed target apps without write permissions will gracefully bypass file writing
        }
    }

    private static void rotateLog(File currentFile) {
        try {
            File oldFile = new File(currentFile.getParentFile(), OLD_LOG_FILE_NAME);
            if (oldFile.exists()) {
                oldFile.delete();
            }
            currentFile.renameTo(oldFile);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Resolves the log file location using the provided or cached context,
     * falling back to standard internal storage paths if context is unavailable.
     */
    public static File getLogFile(Context context) {
        if (context != null) {
            return new File(context.getFilesDir(), LOG_FILE_NAME);
        }
        if (applicationContext != null) {
            return new File(applicationContext.getFilesDir(), LOG_FILE_NAME);
        }
        File primary = new File(FALLBACK_DIR_PRIMARY, LOG_FILE_NAME);
        if (primary.getParentFile() != null && primary.getParentFile().exists()) {
            return primary;
        }
        File secondary = new File(FALLBACK_DIR_SECONDARY, LOG_FILE_NAME);
        if (secondary.getParentFile() != null && secondary.getParentFile().exists()) {
            return secondary;
        }
        return primary;
    }

    public static String getLogContent() {
        return getLogContent(applicationContext);
    }

    /**
     * Reads all recorded log entries from internal storage.
     */
    public static String getLogContent(Context context) {
        File file = getLogFile(context);
        if (file == null || !file.exists()) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        } catch (Throwable ignored) {
        }
        return content.toString();
    }

    public static boolean clearLog() {
        return clearLog(applicationContext);
    }

    /**
     * Purges current and rotated log files.
     */
    public static boolean clearLog(Context context) {
        File logFile = getLogFile(context);
        boolean cleared = true;
        if (logFile != null && logFile.exists()) {
            cleared = logFile.delete();
        }
        File oldFile = logFile != null ? new File(logFile.getParentFile(), OLD_LOG_FILE_NAME) : null;
        if (oldFile != null && oldFile.exists()) {
            oldFile.delete();
        }
        return cleared;
    }
}
