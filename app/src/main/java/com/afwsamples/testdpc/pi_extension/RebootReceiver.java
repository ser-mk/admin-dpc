package com.afwsamples.testdpc.pi_extension;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.afwsamples.testdpc.DeviceAdminReceiver;

public class RebootReceiver extends BroadcastReceiver {

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        ComponentName mAdminComponentName = DeviceAdminReceiver.getComponentName(context);
        mDevicePolicyManager.reboot(mAdminComponentName);
    }
}
