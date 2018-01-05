package com.afwsamples.testdpc.pi_extension;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import com.afwsamples.testdpc.cosu.CosuUtils;

public class StatusReceiver extends BroadcastReceiver {

    final String TAG = this.getClass().getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        int result = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);
        String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
        String action = intent.getAction();
        Log.d(CosuUtils.TAG, "PackageInstallerCallback: action = " + action + " result=" + result
                + " packageName=" + packageName);
        if(result == PackageInstaller.STATUS_SUCCESS){
            Log.v(TAG, "STATUS_SUCCESS");
        } else {
            Log.e(TAG, "Operation failed.");
        }

        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
        Log.v(TAG, "replacing " + replacing);
    }
}
