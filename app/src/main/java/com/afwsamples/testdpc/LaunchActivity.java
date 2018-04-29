/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.afwsamples.testdpc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afwsamples.testdpc.common.LaunchIntentUtil;
import com.afwsamples.testdpc.common.ProvisioningStateUtil;
import com.afwsamples.testdpc.pi_extension.DPCSettings;
import com.afwsamples.testdpc.policy.locktask.KioskModeActivity;

import sermk.pipi.pilib.PassGeneration;

/**
 * <p>Application launch activity that decides the most appropriate initial activity for the
 * user.
 *
 * <p>Options include:
 * <ol>
 *     <li>If TestDPC is already managing the device or profile, forward to the policy management
 *         activity.
 *     <li>If TestDPC was launched as part of synchronous authentication, forward all intent extras
 *         to the setup activities and wait for that activity to finish; allows in-line management
 *         setup immediately after an account is added (before the end of the Add Account or Setup
 *         Wizard flows).
 *     <li>Otherwise, present the non-sync-auth setup options.
 * </ol>
 */
public class LaunchActivity extends Activity implements View.OnKeyListener {
    private static final int REQUEST_CODE_SYNC_AUTH = 1;
    private static final String KIOSK_INTENT = "kiosk";

    private final String TAG = this.getClass().getName();
    private final String COUNT_FIELD = "start_count";
    private final int THRESHOLD_COUNT = 1;

    private boolean checkAvalible() {
        final EditText passInput = new EditText(this);
        passInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        passInput.setOnKeyListener(this);
        setContentView(passInput);

        SharedPreferences settings = getSharedPreferences(TAG, Context.MODE_PRIVATE);

        final int count = settings.getInt(COUNT_FIELD, 0);

        boolean run = false;

        if(count < THRESHOLD_COUNT){ run = true; }

        settings.edit().putInt(COUNT_FIELD,count+1).apply();

        if (BuildConfig.DEBUG) {
            // do something for a debug build
            return true;
        }

        return run;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if(event.getAction() != KeyEvent.ACTION_DOWN ||
                (keyCode != KeyEvent.KEYCODE_ENTER)){
            return false;
        }

        EditText editText = (EditText)v;
        // сохраняем текст, введенный до нажатия Enter в переменную
        String pass = editText.getText().toString();
        Log.v(TAG, pass);

        if(PassGeneration.noPassDPC(pass)){
            Log.e(TAG,"unknown pass!");
            return false;
        }

        Log.e(TAG,"clear count!");
        getSharedPreferences(TAG, Context.MODE_PRIVATE)
                .edit().putInt(COUNT_FIELD,0).apply();

        return true;

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            // We should only forward on first time creation.
            finish();
            return;
        }
/*
        if(!checkAvalible()){
            Log.v(TAG,"can't not run DPC");
            MClient.sendMessage(this, ErrorCollector.subjError(TAG, "run"),
                    "warning! clear count run!");
            return;
        }
*/
        final boolean kiosk = this.getIntent().
                getBooleanExtra(KIOSK_INTENT, true);

        if(kiosk) {
            startKioskMode();
            return;
        }

        if (ProvisioningStateUtil.isManaged(this)
                && !ProvisioningStateUtil.isManagedByTestDPC(this)) {
            // Device or profile owner is a different app to TestDPC - abort.
            Toast.makeText(this, getString(R.string.other_owner_already_setup_error),
                    Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        } else if (LaunchIntentUtil.isSynchronousAuthLaunch(getIntent())) {
            // Forward all extras from original launch intent.
            Intent intent = new Intent(this, SetupManagementActivity.class);
            intent.putExtras(getIntent().getExtras());

            // For synchronous auth either Setup Wizard or Add Account will launch this activity
            // with startActivityForResult(), and continue the account/device setup flow once a
            // result is returned - so we need to wait for a result from any activities we launch
            // and return a result based upon the outcome of those activities to whichever activity
            // launched us.
            startActivityForResult(intent, REQUEST_CODE_SYNC_AUTH);
        } else {
            // The default is to display policy management options
            Intent intent = new Intent(this, PolicyManagementActivity.class);
            startActivity(intent);
            finish();
        }
    }
/*
    private void startKioskMode1() {

        if(!ProvisioningStateUtil.isManagedByTestDPC(this)){
            Toast.makeText(this, getString(R.string.other_owner_already_setup_error),
                    Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        }

        // start locked activity
        final String[] lockTaskArray = new String[0];
        Intent launchIntent = new Intent(this, PiKiosk.class);
        launchIntent.putExtra(KioskModeActivity.LOCKED_APP_PACKAGE_LIST, lockTaskArray);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PackageManager mPackageManager = getPackageManager();
        final String mPackageName = getPackageName();
        mPackageManager.setComponentEnabledSetting(
                new ComponentName(mPackageName, PiKiosk.class.getName()),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        startActivity(launchIntent);
        finish();
    }
*/
    private void startKioskMode() {

        if(!ProvisioningStateUtil.isManagedByTestDPC(this)){
            Toast.makeText(this, getString(R.string.other_owner_already_setup_error),
                    Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        }

        // start locked activity
        final String[] lockTaskArray = new String[1];
        lockTaskArray[0] = DPCSettings.getSettings(this).START_PACKAGE_NAME;

        Intent launchIntent = new Intent(this, KioskModeActivity.class);
        launchIntent.putExtra(KioskModeActivity.LOCKED_APP_PACKAGE_LIST, lockTaskArray);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PackageManager mPackageManager = getPackageManager();
        final String mPackageName = getPackageName();
        mPackageManager.setComponentEnabledSetting(
                new ComponentName(mPackageName, KioskModeActivity.class.getName()),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        startActivity(launchIntent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SYNC_AUTH) {
            // Forward result of activity back to launching activity for sync-auth case.
            setResult(resultCode);
            finish();
        }
    }
}
