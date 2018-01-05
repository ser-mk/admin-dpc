package com.afwsamples.testdpc.pi_extension;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by echormonov on 04.01.18.
 */

public class ClientSender {

    static final String TAG = "ClientSender";

    static String NAME_MC_PACKAGE(){return "sermk.pipi.mclient";}
    static String NAME_MCS_SERVICE(){return "sermk.pipi.mlib.MTransmitterService";}

    public static boolean sendMessage(Context context, final String subject,
                               @NonNull final String data){
        Intent intent = new Intent();
        intent.setClassName(NAME_MC_PACKAGE(), NAME_MCS_SERVICE());
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, data + getVersionInfo(context));

        final ComponentName c = context.startService(intent);
        if(c == null){
            Log.e(TAG, "sent FAILED!");
            return false;
        } else {
            Log.v(TAG, "sent succes");
        }
        return true;
    }

    //get the current version number and name
    private static String getVersionInfo(Context context) {
        String versionName = "error";
        int versionCode = -1;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "\r\n" + "app: " + context.getApplicationContext().getPackageName() + " version: " + versionName + " code: " + String.valueOf(versionCode);
    }
}
