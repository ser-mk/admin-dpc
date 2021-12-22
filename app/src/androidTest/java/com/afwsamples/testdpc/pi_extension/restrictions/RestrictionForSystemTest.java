package com.afwsamples.testdpc.pi_extension.restrictions;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.test.rule.ActivityTestRule;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.LaunchActivity;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Created by ser on 16.04.18.
 */
public class RestrictionForSystemTest {

    final String TAG = this.getClass().getName();

    @Rule
    public ActivityTestRule<LaunchActivity> activityTestRule = new ActivityTestRule<>(LaunchActivity.class);

    @Test
    public void addWifiWPAConfig() throws Exception {

        final Activity act = activityTestRule.getActivity();

        WifiManager wifiManager = (WifiManager) act.getSystemService(Context.WIFI_SERVICE);

        wifiManager.setWifiEnabled(true);
        await().atMost(2, SECONDS).until(wifiManager::isWifiEnabled);

        wifiManager.setWifiEnabled(false);

        await().atMost(2, SECONDS).until(wifiManager::isWifiEnabled,is(false));

        wifiManager.setWifiEnabled(true);

        await().atMost(2, SECONDS).until(wifiManager::isWifiEnabled);
/*
        assertTrue(RestrictionForSystem.
                addWifiWPAConfig(act,
                        "beeline-router9A0412_EXT","0895918032"));
*/
        assertTrue(RestrictionForSystem.
                addWifiWPAConfig(act,
                        "pifi2018","password"));


        String error = RestrictionForSystem.init(act);

        System.out.println("error : " + error);

        assertEquals(error, RestrictionForSystem.NON_ERROR);

    }

    @Test
    public void testPermission() throws Exception {

        final Activity act = activityTestRule.getActivity();

        assertTrue(RestrictionForSystem.setAllPermission4App(act,
                "sermk.pipi.mclient",DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED)
        );

        RestrictionForSystem.denyPermissionForAllApps(act);
    }

    @Test
    public void testUserRestriction() throws Exception {
        RestrictionForSystem.setUserRestriction(activityTestRule.getActivity());
    }


    @After
    public void clear() throws Exception {
        System.out.println("___________________ clear _______________");
        final Activity act = activityTestRule.getActivity();

        final DevicePolicyManager devicePolicyManager = (DevicePolicyManager) act.getSystemService(Context.DEVICE_POLICY_SERVICE);
        final ComponentName admincomponentname = DeviceAdminReceiver.getComponentName(act);

        for (String userRestriction : RestrictionForSystem.KIOSK_USER_LIST_RESTRICTIONS) {
            devicePolicyManager.clearUserRestriction(admincomponentname, userRestriction);
        }
    }

}