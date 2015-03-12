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

package com.google.android.testdpc.deviceowner;

import static android.os.UserManager.DISALLOW_ADD_USER;
import static android.os.UserManager.DISALLOW_ADJUST_VOLUME;
import static android.os.UserManager.DISALLOW_CONFIG_CREDENTIALS;
import static android.os.UserManager.DISALLOW_CONFIG_TETHERING;
import static android.os.UserManager.DISALLOW_DEBUGGING_FEATURES;
import static android.os.UserManager.DISALLOW_FACTORY_RESET;
import static android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES;
import static android.os.UserManager.DISALLOW_MODIFY_ACCOUNTS;
import static android.os.UserManager.DISALLOW_REMOVE_USER;
import static android.os.UserManager.DISALLOW_SHARE_LOCATION;
import static android.os.UserManager.DISALLOW_UNMUTE_MICROPHONE;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.testdpc.DeviceAdminReceiver;
import com.google.android.testdpc.R;
import com.google.android.testdpc.deviceowner.accessibility.AccessibilityServiceInfoArrayAdapter;
import com.google.android.testdpc.deviceowner.blockuninstallation.BlockUninstallationInfoArrayAdapter;
import com.google.android.testdpc.deviceowner.inputmethod.InputMethodInfoArrayAdapter;
import com.google.android.testdpc.deviceowner.locktask.LockTaskAppInfoArrayAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides several device management functions.
 * These include
 * 1) {@link DevicePolicyManager#setLockTaskPackages(android.content.ComponentName, String[])}
 * 2) {@link DevicePolicyManager#isLockTaskPermitted(String)}
 * 3) {@link UserManager#DISALLOW_DEBUGGING_FEATURES}
 * 4) {@link UserManager#DISALLOW_INSTALL_UNKNOWN_SOURCES}
 * 5) {@link UserManager#DISALLOW_REMOVE_USER}
 * 6) {@link UserManager#DISALLOW_ADD_USER}
 * 7) {@link UserManager#DISALLOW_FACTORY_RESET}
 * 8) {@link UserManager#DISALLOW_CONFIG_CREDENTIALS}
 * 9) {@link UserManager#DISALLOW_SHARE_LOCATION}
 * 10) {@link UserManager#DISALLOW_CONFIG_TETHERING}
 * 11) {@link UserManager#DISALLOW_ADJUST_VOLUME}
 * 12) {@link UserManager#DISALLOW_UNMUTE_MICROPHONE}
 * 13) {@link UserManager#DISALLOW_MODIFY_ACCOUNTS}
 * 14) {@link DevicePolicyManager#clearDeviceOwnerApp(String)}
 * 15) {@link DevicePolicyManager#getPermittedAccessibilityServices(android.content.ComponentName)}
 * 16) {@link DevicePolicyManager#getPermittedInputMethods(android.content.ComponentName)}
 * 17) {@link DevicePolicyManager#setAccountManagementDisabled(android.content.ComponentName,
 * String, boolean)}
 * 18) {@link DevicePolicyManager#getAccountTypesWithManagementDisabled()}
 * 19) {@link DevicePolicyManager#createAndInitializeUser(android.content.ComponentName, String,
 * String, android.content.ComponentName, android.os.Bundle)}
 * 20) {@link DevicePolicyManager#removeUser(android.content.ComponentName, android.os.UserHandle)}
 * 21) {@link DevicePolicyManager#setUninstallBlocked(android.content.ComponentName, String,
 * boolean)}
 * 22) {@link DevicePolicyManager#isUninstallBlocked(android.content.ComponentName, String)}
 */
public class DevicePolicyManagementFragment extends PreferenceFragment implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private static final String DEVICE_OWNER_STATUS_KEY = "device_owner_status";
    private static final String MANAGE_LOCK_TASK_LIST_KEY = "manage_lock_task";
    private static final String CHECK_LOCK_TASK_PERMITTED_KEY = "check_lock_task_permitted";
    private static final String DISALLOW_INSTALL_DEBUGGING_FEATURE_KEY
            = "disallow_debugging_feature";
    private static final String DISALLOW_INSTALL_UNKNOWN_SOURCES_KEY
            = "disallow_install_unknown_sources";
    private static final String REMOVE_DEVICE_OWNER_KEY = "remove_device_owner";
    private static final String SET_ACCESSIBILITY_SERVICES_KEY = "set_accessibility_services";
    private static final String SET_INPUT_METHODS_KEY = "set_input_methods";
    private static final String SET_DISABLE_ACCOUNT_MANAGEMENT_KEY
            = "set_disable_account_management";
    private static final String GET_DISABLE_ACCOUNT_MANAGEMENT_KEY
            = "get_disable_account_management";
    private static final String CREATE_AND_INITIALIZE_USER_KEY = "create_and_initialize_user";
    private static final String REMOVE_USER_KEY = "remove_user";
    private static final String BLOCK_UNINSTALLATION_BY_PKG_KEY = "block_uninstallation_by_pkg";
    private static final String BLOCK_UNINSTALLATION_LIST_KEY = "block_uninstallation_list";

    private static final String[] PRIMARY_USER_ONLY_RESTRICTIONS = {
            DISALLOW_REMOVE_USER, DISALLOW_ADD_USER, DISALLOW_FACTORY_RESET,
            DISALLOW_CONFIG_TETHERING, DISALLOW_ADJUST_VOLUME, DISALLOW_UNMUTE_MICROPHONE
    };
    private static final String[] ALL_USER_RESTRICTIONS = {
            DISALLOW_DEBUGGING_FEATURES, DISALLOW_INSTALL_UNKNOWN_SOURCES, DISALLOW_REMOVE_USER,
            DISALLOW_ADD_USER, DISALLOW_FACTORY_RESET, DISALLOW_CONFIG_CREDENTIALS,
            DISALLOW_SHARE_LOCATION, DISALLOW_CONFIG_TETHERING, DISALLOW_ADJUST_VOLUME,
            DISALLOW_UNMUTE_MICROPHONE, DISALLOW_MODIFY_ACCOUNTS
    };

    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;
    private String mPackageName;
    private ComponentName mAdminComponentName;
    private UserManager mUserManager;

    private Preference mManageLockTaskPreference;
    private Preference mCheckLockTaskPermittedPreference;
    private Preference mCreateAndInitializeUserPreference;
    private Preference mRemoveUserPreference;
    private SwitchPreference mDisallowDebuggingFeatureSwitchPreference;
    private SwitchPreference mDisallowInstallUnknownSourcesSwitchPreference;
    private SwitchPreference mDisallowRemoveUserSwitchPreference;
    private SwitchPreference mDisallowAddUserSwitchPreference;
    private SwitchPreference mDisallowFactoryResetSwitchPreference;
    private SwitchPreference mDisallowConfigCredentialsSwitchPreference;
    private SwitchPreference mDisallowShareLocationSwitchPreference;
    private SwitchPreference mDisallowConfigTetheringSwitchPreference;
    private SwitchPreference mDisallowAdjustVolumePreference;
    private SwitchPreference mDisallowUnmuteMicrophonePreference;
    private SwitchPreference mDisallowModifyAccountsPreference;
    private GetAccessibilityServicesTask mGetAccessibilityServicesTask = null;
    private GetInputMethodsTask mGetInputMethodsTask = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdminComponentName = DeviceAdminReceiver.getComponentName(getActivity());
        mDevicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        mPackageManager = getActivity().getPackageManager();
        mPackageName = getActivity().getPackageName();

        addPreferencesFromResource(R.xml.device_policy_header);

        mManageLockTaskPreference = findPreference(MANAGE_LOCK_TASK_LIST_KEY);
        mManageLockTaskPreference.setOnPreferenceClickListener(this);

        mCheckLockTaskPermittedPreference = findPreference(CHECK_LOCK_TASK_PERMITTED_KEY);
        mCheckLockTaskPermittedPreference.setOnPreferenceClickListener(this);

        mDisallowDebuggingFeatureSwitchPreference = (SwitchPreference) findPreference(
                DISALLOW_DEBUGGING_FEATURES);
        mDisallowDebuggingFeatureSwitchPreference.setOnPreferenceChangeListener(this);

        mDisallowInstallUnknownSourcesSwitchPreference = (SwitchPreference) findPreference(
                DISALLOW_INSTALL_UNKNOWN_SOURCES);
        mDisallowInstallUnknownSourcesSwitchPreference.setOnPreferenceChangeListener(this);

        mDisallowRemoveUserSwitchPreference = (SwitchPreference) findPreference(
                DISALLOW_REMOVE_USER);
        mDisallowRemoveUserSwitchPreference.setOnPreferenceChangeListener(this);

        mDisallowAddUserSwitchPreference = (SwitchPreference) findPreference(DISALLOW_ADD_USER);
        mDisallowAddUserSwitchPreference.setOnPreferenceChangeListener(this);

        mDisallowFactoryResetSwitchPreference = (SwitchPreference) findPreference(
                DISALLOW_FACTORY_RESET);
        mDisallowFactoryResetSwitchPreference.setOnPreferenceChangeListener(this);

        mDisallowConfigCredentialsSwitchPreference = (SwitchPreference) findPreference(
                DISALLOW_CONFIG_CREDENTIALS);
        mDisallowConfigCredentialsSwitchPreference.setOnPreferenceChangeListener(this);

        mDisallowShareLocationSwitchPreference = (SwitchPreference) findPreference(
                DISALLOW_SHARE_LOCATION);
        mDisallowShareLocationSwitchPreference.setOnPreferenceChangeListener(this);

        mDisallowConfigTetheringSwitchPreference = (SwitchPreference) findPreference(
                DISALLOW_CONFIG_TETHERING);
        mDisallowConfigTetheringSwitchPreference.setOnPreferenceChangeListener(this);

        mDisallowAdjustVolumePreference = (SwitchPreference) findPreference(DISALLOW_ADJUST_VOLUME);
        mDisallowAdjustVolumePreference.setOnPreferenceChangeListener(this);

        mDisallowUnmuteMicrophonePreference = (SwitchPreference) findPreference(
                DISALLOW_UNMUTE_MICROPHONE);
        mDisallowUnmuteMicrophonePreference.setOnPreferenceChangeListener(this);

        mDisallowModifyAccountsPreference = (SwitchPreference) findPreference(
                DISALLOW_MODIFY_ACCOUNTS);
        mDisallowModifyAccountsPreference.setOnPreferenceChangeListener(this);

        findPreference(REMOVE_DEVICE_OWNER_KEY).setOnPreferenceClickListener(this);

        findPreference(SET_ACCESSIBILITY_SERVICES_KEY).setOnPreferenceClickListener(this);

        findPreference(SET_INPUT_METHODS_KEY).setOnPreferenceClickListener(this);

        findPreference(SET_DISABLE_ACCOUNT_MANAGEMENT_KEY).setOnPreferenceClickListener(this);

        findPreference(GET_DISABLE_ACCOUNT_MANAGEMENT_KEY).setOnPreferenceClickListener(this);

        mCreateAndInitializeUserPreference = findPreference(CREATE_AND_INITIALIZE_USER_KEY);
        mCreateAndInitializeUserPreference.setOnPreferenceClickListener(this);

        mRemoveUserPreference = findPreference(REMOVE_USER_KEY);
        mRemoveUserPreference.setOnPreferenceClickListener(this);

        findPreference(BLOCK_UNINSTALLATION_BY_PKG_KEY).setOnPreferenceClickListener(this);

        findPreference(BLOCK_UNINSTALLATION_LIST_KEY).setOnPreferenceClickListener(this);

        updateUserRestrictionUi(ALL_USER_RESTRICTIONS);
        disableIncompatibleManagementOptionsInCurrentProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setTitle(R.string.device_management_title);

        boolean isDeviceOwner = mDevicePolicyManager.isDeviceOwnerApp(mPackageName);
        boolean isProfileOwner = mDevicePolicyManager.isProfileOwnerApp(mPackageName);
        if (!isDeviceOwner && !isProfileOwner) {
            showToast(R.string.not_a_device_owner);
            getActivity().finish();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case MANAGE_LOCK_TASK_LIST_KEY:
                showManageLockTaskListPrompt();
                return true;
            case CHECK_LOCK_TASK_PERMITTED_KEY:
                showCheckLockTaskPermittedPrompt();
                return true;
            case REMOVE_DEVICE_OWNER_KEY:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.remove_device_owner_title)
                        .setMessage(R.string.remove_device_owner_confirmation)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        mDevicePolicyManager.clearDeviceOwnerApp(mPackageName);
                                        if (getActivity() != null && !getActivity().isFinishing()) {
                                            showToast(R.string.device_owner_removed);
                                            getActivity().getFragmentManager().popBackStack();
                                        }
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            case SET_ACCESSIBILITY_SERVICES_KEY:
                // Avoid starting the same task twice.
                if (mGetAccessibilityServicesTask != null && !mGetAccessibilityServicesTask
                        .isCancelled()) {
                    mGetAccessibilityServicesTask.cancel(true);
                }
                mGetAccessibilityServicesTask = new GetAccessibilityServicesTask();
                mGetAccessibilityServicesTask.execute();
                return true;
            case SET_INPUT_METHODS_KEY:
                // Avoid starting the same task twice.
                if (mGetInputMethodsTask != null && !mGetInputMethodsTask.isCancelled()) {
                    mGetInputMethodsTask.cancel(true);
                }
                mGetInputMethodsTask = new GetInputMethodsTask();
                mGetInputMethodsTask.execute();
                return true;
            case SET_DISABLE_ACCOUNT_MANAGEMENT_KEY:
                showSetDisableAccountManagementPrompt();
                return true;
            case GET_DISABLE_ACCOUNT_MANAGEMENT_KEY:
                showDisableAccountTypeList();
                return true;
            case CREATE_AND_INITIALIZE_USER_KEY:
                showCreateUserPrompt();
                return true;
            case REMOVE_USER_KEY:
                showRemoveUserPrompt();
                return true;
            case BLOCK_UNINSTALLATION_BY_PKG_KEY:
                showBlockUninstallationByPackageNamePrompt();
                return true;
            case BLOCK_UNINSTALLATION_LIST_KEY:
                showBlockUninstallationPrompt();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        switch (key) {
            case DISALLOW_DEBUGGING_FEATURES:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_INSTALL_UNKNOWN_SOURCES:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_REMOVE_USER:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_ADD_USER:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_FACTORY_RESET:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_CONFIG_CREDENTIALS:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_SHARE_LOCATION:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_CONFIG_TETHERING:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_ADJUST_VOLUME:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_UNMUTE_MICROPHONE:
                setUserRestriction(key, (Boolean) newValue);
                return true;
            case DISALLOW_MODIFY_ACCOUNTS:
                setUserRestriction(key, (Boolean) newValue);
                return true;
        }
        return false;
    }

    /**
     * Shows a list of primary user apps in a prompt, indicating whether lock task is permitted for
     * that app.
     */
    private void showManageLockTaskListPrompt() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> primaryUserAppList = getActivity().getPackageManager()
                .queryIntentActivities(launcherIntent, 0);
        if (primaryUserAppList.isEmpty()) {
            showToast(R.string.no_primary_app_available);
        } else {
            final LockTaskAppInfoArrayAdapter appInfoArrayAdapter = new LockTaskAppInfoArrayAdapter(
                    getActivity(), R.id.pkg_name, primaryUserAppList);
            ListView listView = new ListView(getActivity());
            listView.setAdapter(appInfoArrayAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    appInfoArrayAdapter.onItemClick(view, position);
                }
            });
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.manage_lock_task))
                    .setView(listView)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String[] lockTaskEnabledArray = appInfoArrayAdapter.getLockTaskList();
                            mDevicePolicyManager.setLockTaskPackages(
                                    DeviceAdminReceiver.getComponentName(getActivity()),
                                    lockTaskEnabledArray);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                    .show();
        }
    }

    /**
     * Shows a prompt to collect a package name and checks whether the lock task for the
     * corresponding app is permitted or not.
     */
    private void showCheckLockTaskPermittedPrompt() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        View view = getActivity().getLayoutInflater().inflate(R.layout.simple_edittext, null);
        final EditText input = (EditText) view.findViewById(R.id.input);
        input.setHint(getString(R.string.input_package_name_hints));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.check_lock_task_permitted))
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String packageName = input.getText().toString();
                        boolean isLockTaskPermitted = mDevicePolicyManager
                                .isLockTaskPermitted(packageName);
                        showToast(isLockTaskPermitted
                                ? R.string.check_lock_task_permitted_result_permitted
                                : R.string.check_lock_task_permitted_result_not_permitted);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void setUserRestriction(String restriction, boolean disallow) {
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction);
        }
        updateUserRestrictionUi(restriction);
    }

    private void updateUserRestrictionUi(String[] userRestrictions) {
        for (String userRestriction : userRestrictions) {
            updateUserRestrictionUi(userRestriction);
        }
    }

    /**
     * Updates the corresponding UI for a given user restriction.
     *
     * @param userRestriction the id of a preference that is going to be updated.
     */
    private void updateUserRestrictionUi(String userRestriction) {
        boolean disallowed = false;
        switch (userRestriction) {
            case DISALLOW_DEBUGGING_FEATURES:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_DEBUGGING_FEATURES);
                mDisallowDebuggingFeatureSwitchPreference.setChecked(disallowed);
                break;
            case DISALLOW_INSTALL_UNKNOWN_SOURCES:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_INSTALL_UNKNOWN_SOURCES);
                mDisallowInstallUnknownSourcesSwitchPreference.setChecked(disallowed);
                break;
            case DISALLOW_REMOVE_USER:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_REMOVE_USER);
                mDisallowRemoveUserSwitchPreference.setChecked(disallowed);
                break;
            case DISALLOW_ADD_USER:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_ADD_USER);
                mDisallowAddUserSwitchPreference.setChecked(disallowed);
                break;
            case DISALLOW_FACTORY_RESET:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_FACTORY_RESET);
                mDisallowFactoryResetSwitchPreference.setChecked(disallowed);
                break;
            case DISALLOW_CONFIG_CREDENTIALS:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_CONFIG_CREDENTIALS);
                mDisallowConfigCredentialsSwitchPreference.setChecked(disallowed);
                break;
            case DISALLOW_SHARE_LOCATION:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_SHARE_LOCATION);
                mDisallowShareLocationSwitchPreference.setChecked(disallowed);
                break;
            case DISALLOW_CONFIG_TETHERING:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_CONFIG_TETHERING);
                mDisallowConfigTetheringSwitchPreference.setChecked(disallowed);
                break;
            case DISALLOW_ADJUST_VOLUME:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_ADJUST_VOLUME);
                mDisallowAdjustVolumePreference.setChecked(disallowed);
                break;
            case DISALLOW_UNMUTE_MICROPHONE:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_UNMUTE_MICROPHONE);
                mDisallowUnmuteMicrophonePreference.setChecked(disallowed);
                break;
            case DISALLOW_MODIFY_ACCOUNTS:
                disallowed = mUserManager.hasUserRestriction(DISALLOW_MODIFY_ACCOUNTS);
                mDisallowModifyAccountsPreference.setChecked(disallowed);
                break;
        }
    }

    /**
     * Some testing UIs in this class can only be run if this app is a device owner and is running
     * in the primary profile. Disable those UIs to avoid confusion.
     */
    private void disableIncompatibleManagementOptionsInCurrentProfile() {
        boolean isProfileOwner = mDevicePolicyManager.isProfileOwnerApp(mPackageName);
        boolean isDeviceOwner = mDevicePolicyManager.isDeviceOwnerApp(mPackageName);
        int deviceOwnerStatusStringId = R.string.not_a_device_owner;
        if (isProfileOwner) {
            // Some of the management options can only be applied in a primary profile.
            for (String primaryUserRestriction : PRIMARY_USER_ONLY_RESTRICTIONS) {
                findPreference(primaryUserRestriction).setEnabled(false);
            }
            // Only the primary profile can remove the device ownership.
            findPreference(REMOVE_DEVICE_OWNER_KEY).setEnabled(false);
            // Only the device owner in the primary profile can create or remove user.
            mCreateAndInitializeUserPreference.setEnabled(false);
            mRemoveUserPreference.setEnabled(false);
            // A device owner running in a managed profile.
            if (isDeviceOwner) {
                deviceOwnerStatusStringId = R.string.a_device_owner_running_in_managed_profile;
            }
            // Not a device owner running in a managed profile.
            else {
                mManageLockTaskPreference.setEnabled(false);
                mCheckLockTaskPermittedPreference.setEnabled(false);
                deviceOwnerStatusStringId = R.string.not_a_device_owner_running_in_managed_profile;
            }
        } else if (isDeviceOwner) {
            // If it's a device owner and running in the primary profile.
            deviceOwnerStatusStringId = R.string.a_device_owner;
        }
        findPreference(DEVICE_OWNER_STATUS_KEY).setSummary(deviceOwnerStatusStringId);
    }

    /**
     * Shows a prompt that allows entering the account type for which account management should be
     * disabled or enabled.
     */
    private void showSetDisableAccountManagementPrompt() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.simple_edittext, null);
        final EditText input = (EditText) view.findViewById(R.id.input);
        input.setHint(R.string.account_type_hint);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.set_disable_account_management)
                .setView(view)
                .setPositiveButton(R.string.disable, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String accountType = input.getText().toString();
                        setDisableAccountManagement(accountType, true);
                    }
                })
                .setNeutralButton(R.string.enable, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String accountType = input.getText().toString();
                        setDisableAccountManagement(accountType, false);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null /* Nothing to do */)
                .show();
    }

    private void setDisableAccountManagement(String accountType, boolean disabled) {
        if (!TextUtils.isEmpty(accountType)) {
            mDevicePolicyManager.setAccountManagementDisabled(mAdminComponentName, accountType,
                    disabled);
            showToast(disabled
                            ? R.string.account_management_disabled
                            : R.string.account_management_enabled,
                    accountType);
            return;
        }
        showToast(R.string.fail_to_set_account_management);
    }

    /**
     * Show a list of account types that is disabled for account management.
     */
    private void showDisableAccountTypeList() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        String[] disabledAccountTypeList = mDevicePolicyManager
                .getAccountTypesWithManagementDisabled();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.list_of_disabled_account_types)
                .setAdapter(new ArrayAdapter<String>(getActivity(),
                                android.R.layout.simple_list_item_1, android.R.id.text1,
                                disabledAccountTypeList), null)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * For user creation:
     * Shows a prompt to ask for the username that would be used for creating a new user.
     */
    private void showCreateUserPrompt() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.simple_edittext, null);
        final EditText input = (EditText) view.findViewById(R.id.input);
        input.setHint(R.string.enter_username_hint);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.create_and_initialize_user)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = input.getText().toString();
                        String ownerName = getString(R.string.app_name);
                        if (!TextUtils.isEmpty(name)) {
                            UserHandle userHandle = mDevicePolicyManager.createAndInitializeUser(
                                    mAdminComponentName, name, ownerName, mAdminComponentName,
                                    new Bundle());
                            if (userHandle != null) {
                                long serialNumber = mUserManager.getSerialNumberForUser(userHandle);
                                showToast(R.string.user_created, serialNumber);
                                return;
                            }
                            showToast(R.string.failed_to_create_user);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * For user removal:
     * Shows a prompt to ask for the user serial number that is going to be removed.
     */
    private void showRemoveUserPrompt() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.remove_user);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.simple_edittext, null);
        final EditText input = (EditText) view.findViewById(R.id.input);
        input.setHint(R.string.enter_user_id);
        input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                boolean success = false;
                long serialNumber = -1;
                try {
                    serialNumber = Long.parseLong(input.getText().toString());
                    UserHandle userHandle = mUserManager.getUserForSerialNumber(serialNumber);
                    if (userHandle != null) {
                        success = mDevicePolicyManager.removeUser(mAdminComponentName, userHandle);
                    }
                } catch (NumberFormatException e) {
                    // Error message is printed in the next few lines.
                }
                showToast(success ? R.string.user_removed : R.string.failed_to_remove_user);
            }
        });
    }

    /**
     * Asks for the package name whose uninstallation should be blocked / unblocked.
     */
    private void showBlockUninstallationByPackageNamePrompt() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        View view = LayoutInflater.from(activity).inflate(R.layout.simple_edittext, null);
        final EditText input = (EditText) view.findViewById(R.id.input);
        input.setHint(getString(R.string.input_package_name_hints));
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.block_uninstallation_title)
                .setView(view)
                .setPositiveButton(R.string.block, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String pkgName = input.getText().toString();
                        if (!TextUtils.isEmpty(pkgName)) {
                            mDevicePolicyManager.setUninstallBlocked(mAdminComponentName, pkgName,
                                    true);
                            showToast(R.string.uninstallation_blocked, pkgName);
                        } else {
                            showToast(R.string.block_uninstallation_failed_invalid_pkgname);
                        }
                    }
                })
                .setNeutralButton(R.string.unblock, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String pkgName = input.getText().toString();
                        if (!TextUtils.isEmpty(pkgName)) {
                            mDevicePolicyManager.setUninstallBlocked(mAdminComponentName, pkgName,
                                    false);
                            showToast(R.string.uninstallation_allowed, pkgName);
                        } else {
                            showToast(R.string.block_uninstallation_failed_invalid_pkgname);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Displays an alert dialog that allows the user to select applications from all non-system
     * applications installed on the current profile. When the user selects an app, this app can't
     * be uninstallation.
     */
    private void showBlockUninstallationPrompt() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }

        List<ApplicationInfo> applicationInfoList
                = mPackageManager.getInstalledApplications(0 /* No flag */);
        List<ResolveInfo> resolveInfoList = new ArrayList<ResolveInfo>();
        for (ApplicationInfo applicationInfo : applicationInfoList) {
            // Ignore system apps because they can't be uninstalled.
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                ResolveInfo resolveInfo = new ResolveInfo();
                resolveInfo.resolvePackageName = applicationInfo.packageName;
                resolveInfoList.add(resolveInfo);
            }
        }

        final BlockUninstallationInfoArrayAdapter blockUninstallationInfoArrayAdapter
                = new BlockUninstallationInfoArrayAdapter(getActivity(), R.id.pkg_name,
                resolveInfoList);
        ListView listview = new ListView(getActivity());
        listview.setAdapter(blockUninstallationInfoArrayAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                blockUninstallationInfoArrayAdapter.onItemClick(view, pos);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.block_uninstallation_title)
                .setView(listview)
                .setPositiveButton(R.string.close, null /* Nothing to do */)
                .show();
    }

    private void showToast(int msgId, Object... args) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (args != null) {
            Toast.makeText(activity, getString(msgId, args), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, msgId, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Gets all the accessibility services. After all the accessibility services are retrieved, the
     * result is displayed in a popup.
     */
    private class GetAccessibilityServicesTask extends
            AsyncTask<Void, Void, List<AccessibilityServiceInfo>> {

        private AccessibilityManager mAccessibilityManager;

        public GetAccessibilityServicesTask() {
            mAccessibilityManager = (AccessibilityManager) getActivity().getSystemService(
                    Context.ACCESSIBILITY_SERVICE);
        }

        @Override
        protected List<AccessibilityServiceInfo> doInBackground(Void... voids) {
            return mAccessibilityManager.getInstalledAccessibilityServiceList();
        }

        @Override
        protected void onPostExecute(List<AccessibilityServiceInfo> accessibilityServicesInfoList) {
            Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.set_accessibility_services);
            List<ResolveInfo> accessibilityServicesResolveInfoList
                    = AccessibilityServiceInfoArrayAdapter
                    .getResolveInfoListFromAccessibilityServiceInfoList(
                            accessibilityServicesInfoList);
            final AccessibilityServiceInfoArrayAdapter accessibilityServiceInfoArrayAdapter
                    = new AccessibilityServiceInfoArrayAdapter(getActivity(), R.id.pkg_name,
                            accessibilityServicesResolveInfoList);
            ListView listview = new ListView(getActivity());
            listview.setAdapter(accessibilityServiceInfoArrayAdapter);
            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                    accessibilityServiceInfoArrayAdapter.onItemClick(view, pos);
                }
            });
            builder.setView(listview);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ArrayList<String> permittedAccessibilityServicesArrayList
                            = accessibilityServiceInfoArrayAdapter
                            .getPermittedAccessibilityServices();
                    boolean result = mDevicePolicyManager.setPermittedAccessibilityServices(
                            DeviceAdminReceiver.getComponentName(getActivity()),
                            permittedAccessibilityServicesArrayList);
                    showToast(result
                            ? R.string.set_accessibility_services_successful
                            : R.string.set_accessibility_services_fail);
                }
            });
            builder.setNeutralButton(R.string.allow_all, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    boolean result = mDevicePolicyManager.setPermittedAccessibilityServices(
                            mAdminComponentName, null);
                    showToast(result
                            ? R.string.all_accessibility_services_enabled
                            : R.string.set_accessibility_services_fail);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    /**
     * Gets all the input methods. After all the input methods are retrieved, the result is displayed
     * in a popup.
     */
    private class GetInputMethodsTask extends AsyncTask<Void, Void, List<InputMethodInfo>> {

        private InputMethodManager mInputMethodManager;

        public GetInputMethodsTask() {
            mInputMethodManager = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
        }

        @Override
        protected List<InputMethodInfo> doInBackground(Void... voids) {
            return mInputMethodManager.getInputMethodList();
        }

        @Override
        protected void onPostExecute(List<InputMethodInfo> inputMethodsInfoList) {
            Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.set_input_methods);
            List<ResolveInfo> inputMethodsResolveInfoList
                    = InputMethodInfoArrayAdapter.getResolveInfoListFromInputMethodsInfoList(
                            inputMethodsInfoList);
            final InputMethodInfoArrayAdapter inputMethodInfoArrayAdapter
                    = new InputMethodInfoArrayAdapter(getActivity(), R.id.pkg_name,
                            inputMethodsResolveInfoList);
            ListView listview = new ListView(getActivity());
            listview.setAdapter(inputMethodInfoArrayAdapter);
            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
                    inputMethodInfoArrayAdapter.onItemClick(view, pos);
                }
            });
            builder.setView(listview);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ArrayList<String> permittedAccessibilityServicesArrayList
                            = inputMethodInfoArrayAdapter.getPermittedAccessibilityServices();
                    boolean result = mDevicePolicyManager.setPermittedInputMethods(
                            DeviceAdminReceiver.getComponentName(getActivity()),
                            permittedAccessibilityServicesArrayList);
                    showToast(result
                            ? R.string.set_input_methods_successful
                            : R.string.set_input_methods_fail);
                }
            });
            builder.setNeutralButton(R.string.allow_all, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    boolean result = mDevicePolicyManager.setPermittedInputMethods(
                            mAdminComponentName, null);
                    showToast(result
                            ? R.string.all_input_methods_enabled
                            : R.string.set_input_methods_fail);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }
}
