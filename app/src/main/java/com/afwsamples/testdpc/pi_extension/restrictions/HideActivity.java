package com.afwsamples.testdpc.pi_extension.restrictions;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import com.afwsamples.testdpc.DeviceAdminReceiver;

public class HideActivity extends Activity {

    final String INTENT_PACKAGE_NAME = "package";
    final String INTENT_HIDE = "hide";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String packageName = this.getIntent().
                getStringExtra(INTENT_PACKAGE_NAME);

        final boolean hide = this.getIntent().
                getBooleanExtra(INTENT_HIDE, true);

        final DevicePolicyManager mDevicePolicyManager =
                (DevicePolicyManager) getSystemService(
                        Context.DEVICE_POLICY_SERVICE);

        ComponentName mAdminComponentName =
                DeviceAdminReceiver.getComponentName(this);

        System.out.println("packageName : " + packageName + " | hide :" + hide);

        if(mDevicePolicyManager.setApplicationHidden(mAdminComponentName,
                packageName, hide)) {
            System.out.println("success hide " + packageName);
        }

        finish();
    }
}
