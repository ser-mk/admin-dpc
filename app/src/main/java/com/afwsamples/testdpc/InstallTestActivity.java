package com.afwsamples.testdpc;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.afwsamples.testdpc.cosu.CosuUtils;

import java.io.IOException;
import java.io.InputStream;

public class InstallTestActivity extends Activity {

    final String TAG = "InstallTestActivity";


    private boolean hasPrivilegedPermissionsImpl() {
        boolean hasInstallPermission =
                getPackageManager().checkPermission(Manifest.permission.INSTALL_PACKAGES, getPackageName())
                        == PackageManager.PERMISSION_GRANTED;
        boolean hasDeletePermission =
                getPackageManager().checkPermission(Manifest.permission.DELETE_PACKAGES, getPackageName())
                        == PackageManager.PERMISSION_GRANTED;

        return hasInstallPermission && hasDeletePermission;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_test);

        InputStream in = getResources().openRawResource(R.raw.easycam);

        try {
            final boolean ret = CosuUtils.installPackage(this, in, null);
            Log.v(TAG, "ret = " + ret);
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean hasInstallPermission =
                getPackageManager().checkPermission(Manifest.permission.INSTALL_PACKAGES, getPackageName())
                        == PackageManager.PERMISSION_GRANTED;
        Log.v(TAG, "hasInstallPermission = " + hasInstallPermission);

        boolean hasDeletePermission =
                getPackageManager().checkPermission(Manifest.permission.DELETE_PACKAGES, getPackageName())
                        == PackageManager.PERMISSION_GRANTED;
        Log.v(TAG, "hasDeletePermission = " + hasDeletePermission);

        registerReceiver(mInstallReceiver, new IntentFilter(CosuUtils.ACTION_INSTALL_COMPLETE));

        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(BROADCAST_ACTION_UNINSTALL);
        registerReceiver(
                mBroadcastReceiver, intentFilter2);
    }

    static boolean uninstallDo = true;;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(uninstallDo){
            uninstallDo = false;
            Log.v(TAG, "!!!!!!!!!!!");
            uninstall("com.arksine.easycam");
        }
        return super.onTouchEvent(event);
    }

    void uninstall(String packageName){
        final PackageManager pm = getPackageManager();
        final PackageInstaller packageInstaller = pm.getPackageInstaller();

                /*
                * The client app used to set this to F-Droid, but we need it to be set to
                * this package's package name to be able to uninstall from here.
                */
        //pm.setInstallerPackageName(packageName, "com.afwsamples.testdpc");
        // Create a PendingIntent and use it to generate the IntentSender
        Intent broadcastIntent = new Intent(BROADCAST_ACTION_UNINSTALL);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, // context
                0, // arbitary
                broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        packageInstaller.uninstall(packageName, pendingIntent.getIntentSender());
    }


    private static final String BROADCAST_SENDER_PERMISSION =
            "android.permission.INSTALL_PACKAGES";
    private static final String BROADCAST_ACTION_UNINSTALL =
            "com.afwsamples.testdpc.ACTION_UNINSTALL_COMMIT";
    private static final String EXTRA_LEGACY_STATUS = "android.content.pm.extra.LEGACY_STATUS";

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int result = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE);
            String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
            if (CosuUtils.DEBUG) Log.d(CosuUtils.TAG, "PackageInstallerCallback: result=" + result
                    + " packageName=" + packageName);
            switch (result) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION: {
                    // this should not happen in M, but will happen in L and L-MR1
                    Log.e(CosuUtils.TAG, "Install STATUS_PENDING_USER_ACTION.");
                } break;
                case PackageInstaller.STATUS_SUCCESS : {
                    Log.v(TAG, "STATUS_SUCCESS");
                } break;
                default: {
                    Log.e(CosuUtils.TAG, "Install failed.");

                    return;
                }
            }
        }
    };

    private BroadcastReceiver mInstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!CosuUtils.ACTION_INSTALL_COMPLETE.equals(intent.getAction())) {
                return;
            }

            int result = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE);
            String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
            if (CosuUtils.DEBUG) Log.d(CosuUtils.TAG, "PackageInstallerCallback: result=" + result
                    + " packageName=" + packageName);
            switch (result) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION: {
                    // this should not happen in M, but will happen in L and L-MR1
                    startActivity((Intent) intent.getParcelableExtra(Intent.EXTRA_INTENT));
                } break;
                case PackageInstaller.STATUS_SUCCESS: {
                    Log.v(TAG, "STATUS_SUCCESS");
                } break;
                default: {
                    Log.e(CosuUtils.TAG, "Install failed.");

                    return;
                }
            }
        }
    };
}
