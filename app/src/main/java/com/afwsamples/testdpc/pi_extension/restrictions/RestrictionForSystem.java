package com.afwsamples.testdpc.pi_extension.restrictions;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.pi_extension.DPCSettings;
import com.afwsamples.testdpc.policy.wifimanagement.WifiConfigUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sermk.pipi.pilib.NameFieldCollection;
import sermk.pipi.pilib.PiUtils;

import static android.os.UserManager.*;

/**
 * Created by ser on 15.04.18.
 */

public class RestrictionForSystem {

    private final static String NAME =  "RestrictionForSystem";
    private static String TAG = NAME;

    public static final String NON_ERROR = "";

    private static final String WIFI_CONFIG_LOCKDOWN_ON = "1";
    private static final String NAME_NET = "pifi2018";
    private static final String NET_KEY = "info@pinect.ru";

    @TargetApi(Build.VERSION_CODES.M)
    public static String init(Context context){
        TAG = context.getClass().getName() + " - " + NAME;
        Log.v(TAG,"---------------------------------------------------------------");
        final ComponentName mAdminComponentName = DeviceAdminReceiver.getComponentName(context);
        final DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);

        mDevicePolicyManager.setCameraDisabled(mAdminComponentName, true);
        System.out.println("@setCameraDisabled!");

        String error = NON_ERROR;

        if (mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, true)){
            System.out.println("@setKeyguardDisabled!");
        } else {
            error += "keyguard disable\r\n";
        }

        String val = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        System.out.println("DEFAULT_INPUT_METHOD = " + val);

        mDevicePolicyManager.setScreenCaptureDisabled(mAdminComponentName, true);
        System.out.println("@setScreenCaptureDisabled!");

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);


        if (wifiManager != null) {
            if (!wifiManager.setWifiEnabled(true)){
                error += "wifi turn on\r\n";
            }

            if(!addWifiWPAConfig(context, NAME_NET, NET_KEY)){
                error += "wifi config\r\n";
            }

            mDevicePolicyManager.setGlobalSetting(mAdminComponentName,
                    Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN,
                            WIFI_CONFIG_LOCKDOWN_ON);

        } else {
            error += "wifiManager\r\n";
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!bluetoothAdapter.disable()){
            error += "bluetoothAdapter";
        }

        denyPermissionForAllApps(context);

        return error;
    }

    private static String getQuotedString(String string) {
        return "\"" + string + "\"";
    }

    public static  boolean addWifiWPAConfig(Context context, String name, String password){
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = getQuotedString(name);

        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        if (password.matches("[0-9A-Fa-f]{64}")) {
            config.preSharedKey = password;
        } else {
            config.preSharedKey = getQuotedString(password);
        }

        return WifiConfigUtil.saveWifiConfiguration(context, config);
    }
    
    public static void denyPermissionForAllApps(Context context){
        List<String> managedAppList = getInstalledOrLaunchableApps(context);
        boolean white_app = false;
        final String[] default_white_list = NameFieldCollection.DEFAULT_WHITE_LIST_APPS;
        final String[] external_white_list = DPCSettings.getSettings(context).EXTERNAL_WHITE_PERMISSION_LIST_APPS;
        for (String packageName : managedAppList) {
            white_app |= PiUtils.contains2(default_white_list, packageName);
            white_app |= PiUtils.contains2(external_white_list, packageName);
            if(white_app){
                setAllPermission4App(context, packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
            } else {
                setAllPermission4App(context, packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean setAllPermission4App(Context context, String packageName, int state){

        final DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final ComponentName admincomponentname = DeviceAdminReceiver.getComponentName(context);
        final PackageManager pm = context.getPackageManager();

        List<String> permissions = new ArrayList<String>();

        PackageInfo info = null;
        try {
            info = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not retrieve info about the package: " + packageName, e);
            return false;
        }

        if (info == null || info.requestedPermissions == null) {
            return false;
        }


        for (String requestedPerm : info.requestedPermissions) {
            try {
                PermissionInfo pInfo = pm.getPermissionInfo(requestedPerm, 0);
                if (pInfo != null) {
                    if ((pInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE)
                            == PermissionInfo.PROTECTION_DANGEROUS) {
                        permissions.add(pInfo.name);
                        devicePolicyManager.setPermissionGrantState(
                                admincomponentname,
                                packageName,
                                pInfo.name,
                                state);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.i(TAG, "Could not retrieve info about the permission: "
                        + requestedPerm);
            }
        }

        return true;
    }

    private static List<String> getInstalledOrLaunchableApps(Context context) {
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(
                0 /* Default flags */);
        List<String> filteredAppList = new ArrayList<>();
        for (ApplicationInfo applicationInfo : installedApps) {
            if (pm.getLaunchIntentForPackage(applicationInfo.packageName) != null
                    || (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    || WHITELISTED_APPS.contains(applicationInfo.packageName)) {
                filteredAppList.add(applicationInfo.packageName);
            }
        }
        return filteredAppList;
    }

    private static final Set<String> WHITELISTED_APPS = new HashSet<>();
    static {
        // GmsCore
        WHITELISTED_APPS.add("com.google.android.gms");
        WHITELISTED_APPS.add("com.afwsamples.testdpc");
    }

    public static final String[] KIOSK_USER_LIST_RESTRICTIONS = {
            DISALLOW_SAFE_BOOT,
            DISALLOW_FACTORY_RESET,
            DISALLOW_ADD_USER,
            DISALLOW_REMOVE_USER,
            DISALLOW_CONFIG_MOBILE_NETWORKS,
            DISALLOW_CONFIG_BLUETOOTH,
            DISALLOW_CONFIG_WIFI,
            DISALLOW_CONFIG_TETHERING,
            DISALLOW_CONFIG_VPN,

            DISALLOW_DEBUGGING_FEATURES,
            DISALLOW_USB_FILE_TRANSFER,
            DISALLOW_MOUNT_PHYSICAL_MEDIA,

            DISALLOW_SET_WALLPAPER,

            DISALLOW_MODIFY_ACCOUNTS,
            DISALLOW_NETWORK_RESET,
            DISALLOW_OUTGOING_BEAM,
            DISALLOW_OUTGOING_CALLS,
            DISALLOW_SMS,};


    @TargetApi(Build.VERSION_CODES.N)
    public static void setUserRestriction(Context context){
        final DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final ComponentName admincomponentname = DeviceAdminReceiver.getComponentName(context);

        for (String userRestriction : KIOSK_USER_LIST_RESTRICTIONS) {
            devicePolicyManager.addUserRestriction(admincomponentname, userRestriction);
        }

        devicePolicyManager.clearUserRestriction(admincomponentname,DISALLOW_ADJUST_VOLUME);

        devicePolicyManager.setPackagesSuspended(admincomponentname,
                new String[] {"com.android.settings"}, true);
    }
}
