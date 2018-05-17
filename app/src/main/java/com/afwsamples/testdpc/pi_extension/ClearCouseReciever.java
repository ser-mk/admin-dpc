package com.afwsamples.testdpc.pi_extension;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.afwsamples.testdpc.DeviceAdminReceiver;

import sermk.pipi.pilib.NameFieldCollection;

public class ClearCouseReciever extends BroadcastReceiver {

    final static  String NAME_FIELD = "NOT_START_COSU";

    private static SharedPreferences getShareSettings(Context context){
        return context.getSharedPreferences("COSU", Context.MODE_PRIVATE);
    }

    public static boolean notStartCosu(Context context){
        final boolean flag = getShareSettings(context).
                getBoolean(NAME_FIELD, false);

        return flag;
    }

    public static void clearCosu(Context context){
        Log.i(NAME_FIELD, "cosu mode for anyware!");
        getShareSettings(context).edit().clear().apply();
    }


    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        Log.i(NAME_FIELD, "not cosu mode for one time!!");
        getShareSettings(context).edit().putBoolean(NAME_FIELD,true).apply();

        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        ComponentName mAdminComponentName = DeviceAdminReceiver.getComponentName(context);
        mDevicePolicyManager.reboot(mAdminComponentName);

    }
}
