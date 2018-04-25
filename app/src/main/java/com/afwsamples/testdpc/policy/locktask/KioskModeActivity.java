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

package com.afwsamples.testdpc.policy.locktask;

import static android.os.UserManager.DISALLOW_ADD_USER;
import static android.os.UserManager.DISALLOW_ADJUST_VOLUME;
import static android.os.UserManager.DISALLOW_FACTORY_RESET;
import static android.os.UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA;
import static android.os.UserManager.DISALLOW_SAFE_BOOT;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.os.BuildCompat;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afwsamples.testdpc.BuildConfig;
import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.R;
import com.afwsamples.testdpc.pi_extension.DPCSettings;
import com.afwsamples.testdpc.pi_extension.restrictions.RestrictionForSystem;
import com.afwsamples.testdpc.pi_extension.restrictions.RestrictionsForPackage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import sermk.pipi.pilib.AppRunner;

/**
 * Shows the list of apps passed in the {@link #LOCKED_APP_PACKAGE_LIST} extra (or previously saved
 * in shared preferences if the extra is not found) in single app mode:
 * <ul>
 *   <li> The status bar and keyguard are disabled
 *   <li> Several user restrictions are set to prevent the user from escaping this mode
 *        (e.g. safe boot mode and factory reset are disabled)
 *   <li> This activity is set as the Home intent receiver
 * </ul>
 * If the user taps on one of the apps, it is launched in lock tack mode. Tapping on the back or
 * home buttons will bring the user back to the app list. The list also contains a row to exit
 * single app mode and finish this activity.
 */
@TargetApi(Build.VERSION_CODES.M)
public class KioskModeActivity extends Activity {
    private static final String TAG = "KioskModeActivity";

    private static final String KIOSK_PREFERENCE_FILE = "kiosk_preference_file";
    private static final String KIOSK_APPS_KEY = "kiosk_apps";

    public static final String LOCKED_APP_PACKAGE_LIST
            = "com.afwsamples.testdpc.policy.locktask.LOCKED_APP_PACKAGE_LIST";

    private static final String[] KIOSK_USER_RESTRICTIONS = {
            DISALLOW_SAFE_BOOT,
            DISALLOW_FACTORY_RESET,
            DISALLOW_ADD_USER,
            DISALLOW_MOUNT_PHYSICAL_MEDIA };

    private ComponentName mAdminComponentName;
    private ArrayList<String> mKioskPackages;
    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mPackageManager = getPackageManager();

        // check if a new list of apps was sent, otherwise fall back to saved list
        String[] packageArray = getIntent().getStringArrayExtra(LOCKED_APP_PACKAGE_LIST);
        if (packageArray != null) {
            mKioskPackages = new ArrayList<>();
            for (String pkg : packageArray) {
                mKioskPackages.add(pkg);
            }
            mKioskPackages.remove(getPackageName());
            mKioskPackages.add(getPackageName());

            setDefaultKioskPolicies(true);
        } else {
            // after a reboot there is no need to set the policies again
            SharedPreferences sharedPreferences = getSharedPreferences(KIOSK_PREFERENCE_FILE,
                    MODE_PRIVATE);
            mKioskPackages = new ArrayList<>(sharedPreferences.getStringSet(KIOSK_APPS_KEY,
                    new HashSet<String>()));
        }

        // remove TestDPC package and add to end of list; it will act as back door
        mKioskPackages.remove(getPackageName());

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // start lock task mode if it's not already active
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // ActivityManager.getLockTaskModeState api is not available in pre-M.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (!am.isInLockTaskMode()) {
                startLockTask();
            }
        } else {
            if (am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PackageManager pm = getPackageManager();

        final String packageName = DPCSettings.getSettings(this).START_PACKAGE_NAME;
        final String activityName = DPCSettings.getSettings(this).START_ACTIVITY_NAME;

        if (!AppRunner.run(this,packageName, activityName)){
            Toast.makeText(this, "not found launcher", Toast.LENGTH_LONG).show();
        }
    }

    public void onBackdoorClicked() {
        stopLockTask();
        setDefaultKioskPolicies(false);
        mPackageManager.setComponentEnabledSetting(
                new ComponentName(getPackageName(), getClass().getName()),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP);
        finish();
    }

    private void setUserRestriction(String restriction, boolean disallow) {
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction);
        }
    }

    private void setCustomRestriction(){

        RestrictionForSystem.init(this);
        if (!BuildConfig.DEBUG){
            RestrictionForSystem.setUserRestriction(this);
        }
        RestrictionsForPackage.init(this);
    }

    private void setDefaultKioskPolicies(boolean active) {
        // restore or save previous configuration
        if (active) {
            saveCurrentConfiguration();
            setCustomRestriction();
        } else {
            restorePreviousConfiguration();
        }

        // disable keyguard and status bar
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, active);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        // set lock task packages
        mDevicePolicyManager.setLockTaskPackages(mAdminComponentName,
                active ? mKioskPackages.toArray(new String[]{}) : new String[]{});
        SharedPreferences sharedPreferences = getSharedPreferences(KIOSK_PREFERENCE_FILE,
                MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (active) {
            // set kiosk activity as home intent receiver
            mDevicePolicyManager.addPersistentPreferredActivity(mAdminComponentName, intentFilter,
                    new ComponentName(getPackageName(), KioskModeActivity.class.getName()));
            editor.putStringSet(KIOSK_APPS_KEY, new HashSet<String>(mKioskPackages));
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(mAdminComponentName,
                    getPackageName());
            editor.remove(KIOSK_APPS_KEY);
        }
        editor.commit();
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void saveCurrentConfiguration() {
        if (BuildCompat.isAtLeastN()) {
            Bundle settingsBundle = mDevicePolicyManager.getUserRestrictions(mAdminComponentName);
            SharedPreferences.Editor editor = getSharedPreferences(KIOSK_PREFERENCE_FILE,
                    MODE_PRIVATE).edit();

            for (String userRestriction : KIOSK_USER_RESTRICTIONS) {
                boolean currentSettingValue = settingsBundle.getBoolean(userRestriction);
                editor.putBoolean(userRestriction, currentSettingValue);
            }
            editor.commit();
        }
    }

    private void restorePreviousConfiguration() {
        if (BuildCompat.isAtLeastN()) {
            SharedPreferences sharedPreferences = getSharedPreferences(KIOSK_PREFERENCE_FILE,
                    MODE_PRIVATE);

            for (String userRestriction : KIOSK_USER_RESTRICTIONS) {
                boolean prevSettingValue = sharedPreferences.getBoolean(userRestriction, false);
                setUserRestriction(userRestriction, prevSettingValue);
            }
        }
    }

    private class KioskAppsArrayAdapter extends ArrayAdapter<String> implements
            AdapterView.OnItemClickListener {

        public KioskAppsArrayAdapter(Context context, int resource, List<String > objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ApplicationInfo applicationInfo;
            try {
                applicationInfo = mPackageManager.getApplicationInfo(
                        getItem(position), 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Fail to retrieve application info for the entry: " + position, e);
                return null;
            }

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.kiosk_mode_item,
                        parent, false);
            }
            ImageView iconImageView = (ImageView) convertView.findViewById(R.id.pkg_icon);
            iconImageView.setImageDrawable(applicationInfo.loadIcon(mPackageManager));
            TextView pkgNameTextView = (TextView) convertView.findViewById(R.id.pkg_name);
            if (getPackageName().equals(getItem(position))) {
                // back door
                pkgNameTextView.setText(getString(R.string.stop_kiosk_mode));
            } else {
                pkgNameTextView.setText(applicationInfo.loadLabel(mPackageManager));
            }
            return convertView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (getPackageName().equals(getItem(position))) {
                onBackdoorClicked();
            }
            PackageManager pm = getPackageManager();
            startActivity(pm.getLaunchIntentForPackage(getItem(position)));
        }
    }
}
