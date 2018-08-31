package biz.dealnote.xmpp.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.Toast;

import androidx.core.content.PermissionChecker;
import biz.dealnote.xmpp.R;

public class AppPerms {

    public static final int REQUEST_PERMISSION_WRITE_STORAGE = 8364;
    public static final int REQUEST_PERMISSION_READ_STORAGE = 8365;

    public static boolean hasWriteStoragePermision(Context context) {
        if (!Utils.hasMarshmallow()) {
            return true;
        }

        int hasWritePermission = PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return hasWritePermission == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestWriteStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_STORAGE);
        }
    }

    public static void tryInterceptPermissionResult(Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            //String permission = permissions[i];
            int grantResult = grantResults[i];

            switch (requestCode) {
                case REQUEST_PERMISSION_WRITE_STORAGE:
                case REQUEST_PERMISSION_READ_STORAGE:
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(activity, R.string.permission_granted_text, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(activity, R.string.permission_is_not_granted_text, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

    public static boolean hasReadStroragePermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        int hasWritePermission = PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        return hasWritePermission == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestReadStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ_STORAGE);
        }
    }
}