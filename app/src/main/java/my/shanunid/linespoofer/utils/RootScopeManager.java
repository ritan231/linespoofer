package my.shanunid.linespoofer.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Root Scope Direct-Writer Manager for Vector / LSPosed.
 * 
 * Directly synchronizes assigned target apps and system framework into Vector's
 * modules_config.db via root, bypassing Vector Manager UI crashes on Android 16.
 */
public final class RootScopeManager {

    private static final String TAG = "LineSpoofer-RootScope";
    private static final String MODULE_PKG = "my.shanunid.linespoofer";
    private static final int USER_ID = 0;

    private static final String[] CANDIDATE_DBS = new String[]{
        "/data/adb/lspd/config/modules_config.db",
        "/data/adb/lspd/modules_config.db",
        "/data/adb/modules/zygisk_vector/config/modules_config.db",
        "/data/adb/modules/zygisk_vector/modules_config.db",
        "/data/adb/modules/zygisk_lsposed/config/modules_config.db",
        "/data/adb/modules/zygisk_lsposed/modules_config.db"
    };

    private final Context context;
    private final File stageDir;
    private final File stageDb;
    private String cachedDbPath = null;

    public RootScopeManager(Context context) {
        this.context = context.getApplicationContext();
        this.stageDir = new File(this.context.getFilesDir(), "vector_root_sync");
        this.stageDb = new File(stageDir, "modules_config.db");
    }

    public static final class ShellResult {
        public final int exitCode;
        public final String output;

        public ShellResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public boolean ok() {
            return exitCode == 0;
        }
    }

    public static ShellResult runRoot(String script) {
        java.lang.Process p = null;
        StringBuilder out = new StringBuilder();
        try {
            p = Runtime.getRuntime().exec(new String[]{"su"});
            try (OutputStream os = p.getOutputStream()) {
                os.write((script + "\nexit\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            int code = p.waitFor();
            return new ShellResult(code, out.toString().trim());
        } catch (Throwable t) {
            return new ShellResult(-1, String.valueOf(t.getMessage()));
        } finally {
            if (p != null) {
                try {
                    p.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public boolean isRootAvailable() {
        ShellResult r = runRoot("id");
        return r.ok() && r.output.contains("uid=0");
    }

    public String findDbPath() {
        if (cachedDbPath != null) {
            return cachedDbPath;
        }
        for (String path : CANDIDATE_DBS) {
            ShellResult r = runRoot("[ -f '" + path + "' ] && echo FOUND");
            if (r.ok() && r.output.contains("FOUND")) {
                cachedDbPath = path;
                return cachedDbPath;
            }
        }
        ShellResult fallback = runRoot("find /data/adb -name 'modules_config.db' 2>/dev/null | head -n 1");
        if (fallback.ok() && !fallback.output.trim().isEmpty() && fallback.output.contains("modules_config.db")) {
            cachedDbPath = fallback.output.trim();
            return cachedDbPath;
        }
        return null;
    }

    public boolean syncScope(Set<String> targetPackages) {
        return syncScope(targetPackages, null);
    }

    public boolean syncScope(Set<String> targetPackages, Map<String, Object> configs) {
        // Direct SQLite file overwriting (copying modules_config.db and deleting WAL/SHM files
        // while vectord is actively running) causes SQLite lock contention, database corruption,
        // and deactivates all installed Xposed modules (Core Patch, DisableFlagSecure, etc.) upon reboot.
        // We now safely rely on official LibXposed XposedService RemotePreferences and ConfigProvider.
        AppLogger.info(TAG, "Direct SQLite database modification skipped to preserve Vector integrity and module states.");
        return true;
    }

    public static byte[] serializeObject(Object obj) {
        if (obj == null) return new byte[0];
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            return baos.toByteArray();
        } catch (Throwable t) {
            AppLogger.logError(TAG, "Failed serializing object: " + t.getMessage(), t);
            return new byte[0];
        }
    }

    public static void forceStop(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return;
        }
        new Thread(() -> {
            try {
                String clean = packageName.trim();
                if (clean.contains("#")) {
                    String[] parts = clean.split("#");
                    if (parts.length == 2) {
                        runRoot("am force-stop --user " + parts[1] + " '" + parts[0] + "'\nam force-stop '" + parts[0] + "'\npkill -9 -f '" + parts[0] + "'");
                        return;
                    }
                    clean = parts[0];
                }
                runRoot("am force-stop '" + clean + "'\npkill -9 -f '" + clean + "'");
            } catch (Throwable ignored) {
            }
        }).start();
    }

    public static void forceStop(java.util.Collection<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return;
        }
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                for (String pkg : packages) {
                    if (pkg != null && !pkg.trim().isEmpty() && !"android".equals(pkg) && !MODULE_PKG.equals(pkg)) {
                        String clean = pkg.trim();
                        if (clean.contains("#")) {
                            String[] parts = clean.split("#");
                            if (parts.length == 2) {
                                sb.append("am force-stop --user ").append(parts[1]).append(" '").append(parts[0]).append("'\n");
                            }
                            sb.append("am force-stop '").append(parts[0]).append("'\n");
                            sb.append("pkill -9 -f '").append(parts[0]).append("'\n");
                        } else {
                            sb.append("am force-stop '").append(clean).append("'\n");
                            sb.append("pkill -9 -f '").append(clean).append("'\n");
                        }
                    }
                }
                if (sb.length() > 0) {
                    runRoot(sb.toString());
                }
            } catch (Throwable ignored) {
            }
        }).start();
    }

    public static void syncInBackground(Context context, Set<String> targetPackages) {
        syncInBackground(context, targetPackages, null);
    }

    public static void syncInBackground(Context context, Set<String> targetPackages, Map<String, Object> configs) {
        new Thread(() -> {
            try {
                RootScopeManager mgr = new RootScopeManager(context);
                if (mgr.isRootAvailable()) {
                    mgr.syncScope(targetPackages, configs);
                }
            } catch (Throwable ignored) {
            }
        }).start();
    }
}
