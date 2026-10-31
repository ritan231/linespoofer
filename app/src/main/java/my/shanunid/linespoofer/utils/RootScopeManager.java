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
        String dbPath = findDbPath();
        if (dbPath == null) {
            AppLogger.info(TAG, "Vector modules_config.db not found on device");
            return false;
        }

        if (!stageDir.exists() && !stageDir.mkdirs()) {
            return false;
        }

        // Copy db to private directory
        String sdb = stageDb.getAbsolutePath();
        String copyScript =
            "cp '" + dbPath + "' '" + sdb + "'\n" +
            "[ -f '" + dbPath + "-wal' ] && cp '" + dbPath + "-wal' '" + sdb + "-wal'\n" +
            "[ -f '" + dbPath + "-shm' ] && cp '" + dbPath + "-shm' '" + sdb + "-shm'\n" +
            "chmod 666 '" + sdb + "'* 2>/dev/null\n" +
            "chown " + Process.myUid() + ":" + Process.myUid() + " '" + sdb + "'* 2>/dev/null\n" +
            "echo STAGE_OK";
        ShellResult r = runRoot(copyScript);
        if (!r.output.contains("STAGE_OK")) {
            return false;
        }

        boolean edited = false;
        SQLiteDatabase sql = null;
        try {
            sql = SQLiteDatabase.openDatabase(sdb, null, SQLiteDatabase.OPEN_READWRITE);

            // Check schema (mid vs module_pkg_name)
            boolean hasMid = false;
            try (Cursor c = sql.rawQuery("PRAGMA table_info(scope)", null)) {
                int nameIdx = c.getColumnIndex("name");
                while (c.moveToNext()) {
                    if ("mid".equalsIgnoreCase(c.getString(nameIdx))) {
                        hasMid = true;
                        break;
                    }
                }
            }

            Set<String> desired = new LinkedHashSet<>();
            desired.add("system");
            desired.add("android");
            desired.add(MODULE_PKG);
            if (targetPackages != null) {
                desired.addAll(targetPackages);
            }

            if (hasMid) {
                long mid = -1;
                try (Cursor c = sql.rawQuery("SELECT mid FROM modules WHERE module_pkg_name=?", new String[]{MODULE_PKG})) {
                    if (c.moveToFirst()) {
                        mid = c.getLong(0);
                    }
                }

                if (mid <= 0) {
                    String apkPath = context.getPackageResourcePath();
                    sql.execSQL("INSERT INTO modules (module_pkg_name, apk_path, enabled, auto_include) VALUES (?,?,1,0)",
                            new Object[]{MODULE_PKG, apkPath});
                    try (Cursor c = sql.rawQuery("SELECT last_insert_rowid()", null)) {
                        if (c.moveToFirst()) {
                            mid = c.getLong(0);
                        }
                    }
                } else {
                    String apkPath = context.getPackageResourcePath();
                    sql.execSQL("UPDATE modules SET enabled=1, apk_path=? WHERE mid=?", new Object[]{apkPath, mid});
                }

                if (mid > 0) {
                    for (String pkg : desired) {
                        sql.execSQL("INSERT OR IGNORE INTO scope (mid, app_pkg_name, user_id) VALUES (?,?,?)",
                                new Object[]{mid, pkg, USER_ID});
                    }
                    edited = true;
                }
            } else {
                // Legacy schema
                sql.execSQL("INSERT OR REPLACE INTO modules_state (module_pkg_name, user_id, enabled) VALUES (?,?,1)",
                        new Object[]{MODULE_PKG, USER_ID});
                for (String pkg : desired) {
                    sql.execSQL("INSERT OR IGNORE INTO scope (module_pkg_name, app_pkg_name, user_id) VALUES (?,?,?)",
                            new Object[]{MODULE_PKG, pkg, USER_ID});
                }
                edited = true;
            }

            // Write RemotePreferences to Vector's configs table
            if (configs != null && !configs.isEmpty()) {
                boolean hasConfigsTable = false;
                try (Cursor c = sql.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='configs'", null)) {
                    hasConfigsTable = (c != null && c.moveToFirst());
                }
                if (hasConfigsTable) {
                    for (Map.Entry<String, Object> entry : configs.entrySet()) {
                        String key = entry.getKey();
                        Object val = entry.getValue();
                        byte[] serialized = serializeObject(val);
                        if (serialized != null && serialized.length > 0) {
                            sql.execSQL("INSERT OR REPLACE INTO configs (module_pkg_name, user_id, \"group\", \"key\", data) VALUES (?, ?, 'linespoofer_config', ?, ?)",
                                    new Object[]{MODULE_PKG, USER_ID, key, serialized});
                        }
                    }
                    edited = true;
                    AppLogger.info(TAG, "Synchronized " + configs.size() + " configs directly into Vector configs table");
                }
            }

            // Flush WAL checkpoint to main DB file before copying back
            try (Cursor c = sql.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null)) {
                if (c != null && c.moveToFirst()) {
                    AppLogger.debug(TAG, "WAL checkpoint completed");
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            AppLogger.logError(TAG, "Failed editing sqlite db: " + t.getMessage(), t);
            edited = false;
        } finally {
            if (sql != null) {
                try {
                    sql.close();
                } catch (Throwable ignored) {
                }
            }
        }

        if (!edited) {
            runRoot("rm -rf '" + stageDir.getAbsolutePath() + "'");
            return false;
        }

        // Copy back to Vector directory
        String pushScript =
            "cp '" + sdb + "' '" + dbPath + "'\n" +
            "rm -f '" + dbPath + "-wal' '" + dbPath + "-shm'\n" +
            "chmod 660 '" + dbPath + "' 2>/dev/null\n" +
            "chown root:root '" + dbPath + "' 2>/dev/null\n" +
            "restorecon -F '" + dbPath + "' 2>/dev/null\n" +
            "touch '" + dbPath + "'\n" +
            "pkill -HUP -f lspd 2>/dev/null || true\n" +
            "echo PUSH_OK";
        ShellResult pushRes = runRoot(pushScript);
        runRoot("rm -rf '" + stageDir.getAbsolutePath() + "'");

        boolean ok = pushRes.output.contains("PUSH_OK");
        AppLogger.info(TAG, "Vector scope synchronized: " + ok);
        return ok;
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
                runRoot("am force-stop '" + packageName.trim() + "'");
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
                        sb.append("am force-stop '").append(pkg.trim()).append("'\n");
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
