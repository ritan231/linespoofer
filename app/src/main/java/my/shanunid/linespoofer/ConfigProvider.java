package my.shanunid.linespoofer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.os.Bundle;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;

import my.shanunid.linespoofer.data.AppProfileStore;
import my.shanunid.linespoofer.utils.AppLogger;

public class ConfigProvider extends ContentProvider {

    private static final String TAG = "ConfigProvider";
    public static final String AUTHORITY = "my.shanunid.linespoofer.configprovider";
    public static final String FILE_NAME = "resolved_profile.conf";
    public static final Uri CONFIG_URI = Uri.parse("content://" + AUTHORITY + "/" + FILE_NAME);
    public static final String COLUMN_CONTENT = "content";
    public static final String METHOD_GET_CONFIG = "get_config";
    public static final String QUERY_PACKAGE = "package";

    @Override
    public boolean onCreate() {
        if (getContext() != null) {
            AppLogger.init(getContext());
        }
        AppLogger.info(TAG, "ConfigProvider initialized (authority=" + AUTHORITY + ")");
        return true;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        String targetPackage = resolvePackageName(uri, null);
        AppLogger.info(TAG, "openFile() requested for package: " + targetPackage + ", uri: " + uri);
        if (getContext() == null || !FILE_NAME.equals(uri.getLastPathSegment()) || !"r".equals(mode)) {
            AppLogger.warn(TAG, "openFile() rejected invalid uri: " + uri + ", mode: " + mode);
            throw new FileNotFoundException("Unknown config uri: " + uri);
        }
        String content = readConfigContent(targetPackage);
        if (content == null) {
            AppLogger.warn(TAG, "openFile(): No profile assigned for package " + targetPackage);
            throw new FileNotFoundException("No profile is assigned to the requested package");
        }
        AppLogger.info(TAG, "openFile(): Serving profile (" + content.length() + " bytes) to " + targetPackage);
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            Thread writer = new Thread(() -> {
                try (ParcelFileDescriptor.AutoCloseOutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                    outputStream.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (IOException ioException) {
                    AppLogger.logError(TAG, "IPC pipe write failed for " + targetPackage, ioException);
                }
            }, "linespoofer-config-provider");
            writer.start();
            return pipe[0];
        } catch (IOException exception) {
            AppLogger.logError(TAG, "createPipe failed for " + targetPackage, exception);
            throw new FileNotFoundException(exception.getMessage());
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "text/plain";
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        String requestedPackage = selectionArgs != null && selectionArgs.length > 0 ? selectionArgs[0] : null;
        String targetPackage = resolvePackageName(uri, requestedPackage);
        AppLogger.info(TAG, "query() received for package: " + targetPackage);
        String content = readConfigContent(targetPackage);
        if (content == null) {
            AppLogger.warn(TAG, "query(): No profile assigned for package " + targetPackage);
            return null;
        }
        AppLogger.info(TAG, "query(): Returning config cursor (" + content.length() + " bytes) for " + targetPackage);
        MatrixCursor cursor = new MatrixCursor(new String[]{COLUMN_CONTENT});
        cursor.addRow(new Object[]{content});
        return cursor;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (!METHOD_GET_CONFIG.equals(method)) {
            AppLogger.warn(TAG, "call(): Unsupported method: " + method);
            return super.call(method, arg, extras);
        }
        String targetPackage = resolvePackageName(CONFIG_URI, arg);
        AppLogger.info(TAG, "call() get_config for package: " + targetPackage);
        String content = readConfigContent(targetPackage);
        if (content == null) {
            AppLogger.warn(TAG, "call(): No profile assigned for package " + targetPackage);
            return null;
        }
        AppLogger.info(TAG, "call(): Successfully served config for " + targetPackage);
        Bundle bundle = new Bundle();
        bundle.putString(COLUMN_CONTENT, content);
        return bundle;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable
    private String readConfigContent(String packageName) {
        int callingUid = android.os.Binder.getCallingUid();
        int userId = callingUid / 100000;
        return AppProfileStore.resolveConfig(getContext(), packageName, userId);
    }

    private String resolvePackageName(Uri uri, String requestedPackage) {
        if (requestedPackage != null && !requestedPackage.trim().isEmpty()) {
            return requestedPackage.trim();
        }
        String queryPackage = uri.getQueryParameter(QUERY_PACKAGE);
        if (queryPackage != null && !queryPackage.trim().isEmpty()) {
            return queryPackage.trim();
        }
        return getCallingPackage();
    }
}
