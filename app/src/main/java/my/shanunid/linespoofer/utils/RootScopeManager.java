package my.shanunid.linespoofer.utils;

import android.content.Context;
import android.os.Process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Root Process Manager for LineSpoofer.
 *
 * Safely handles application restarts (force-stop and process kill) for target apps.
 * Direct modification of Vector's SQLite database has been completely eliminated
 * to guarantee that Vector's database integrity and module states (Core Patch,
 * DisableFlagSecure, etc.) remain intact across reboots.
 */
public final class RootScopeManager {

    private static final String TAG = "LineSpoofer-RootProcess";
    private static final String MODULE_PKG = "my.shanunid.linespoofer";

    public RootScopeManager(Context context) {
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

    public boolean syncScope(Set<String> targetPackages) {
        return syncScope(targetPackages, null);
    }

    public boolean syncScope(Set<String> targetPackages, Map<String, Object> configs) {
        // Direct SQLite database modification skipped to preserve Vector integrity and module states.
        return true;
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

    public static void forceStop(Collection<String> packages) {
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
        // Safe no-op: Vector database files are never touched.
    }
}
