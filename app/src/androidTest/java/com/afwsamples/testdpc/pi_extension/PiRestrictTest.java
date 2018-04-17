package com.afwsamples.testdpc.pi_extension;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.LaunchActivity;
import com.afwsamples.testdpc.R;
import com.afwsamples.testdpc.cosu.CosuUtils;
import com.afwsamples.testdpc.pi_extension.restrictions.RestrictionsForPackage;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import sermk.pipi.pilib.CommandCollection;
import sermk.pipi.pilib.NameFieldCollection;

import static org.junit.Assert.*;

/**
 * Created by ser on 08.04.18.
 */
public class PiRestrictTest {

    final String TAG = this.getClass().getName();

    @Rule
    public ActivityTestRule<LaunchActivity> activityTestRule = new ActivityTestRule<>(LaunchActivity.class);


    private void installTestPackage(Context context, int res){
        InputStream in = context.getResources().openRawResource(res);
        try {
            final boolean ret = CosuUtils.installPackage(
                    context, in, null);
            Log.v(TAG, "ret = " + ret);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    final String testPackageName ="com.arksine.easycam";

    @Test
    public void listHidePackages() throws Exception {
        final Activity act = activityTestRule.getActivity();

        List<ResolveInfo> list = RestrictionsForPackage.getAllLauncherIntentResolversSorted(act);
        int i =0;
        for(ResolveInfo info : list){
            System.out.println(info.activityInfo.packageName);
            i++;
        }
        System.out.println(i);

        final List<PackageInfo> listInstall = act.getPackageManager().getInstalledPackages(0);
        i =0;
        for(PackageInfo info : listInstall){
            System.out.println(info.packageName);
            i++;
        }
        System.out.println(i);

        setHideAndRemoveList(act);
    }


    private void setHideAndRemoveList(Context context) throws Exception {
        DPCSettings.Settings settings = DPCSettings.getSettings(context);
        String json = new Gson().toJson(settings);
        System.out.println("json : " + json);
        String test_json = "{\"HIDE_LIST_PACKAGE_NAME\":["+ testPackageName+"]," +
                "\"REMOVE_LIST_PACKAGE_NAME\":["+testPackageName+"]}";
        assertTrue(DPCSettings.setSettings(context,test_json));
        assertEquals(DPCSettings.getSettings(context).HIDE_LIST_PACKAGE_NAME[0],
                testPackageName);
        assertEquals(DPCSettings.getSettings(context).REMOVE_LIST_PACKAGE_NAME[0],
                testPackageName);

        String empty_json =  "{\"HIDE_LIST_PACKAGE_NAME\":[],\"REMOVE_LIST_PACKAGE_NAME\":[]}";
        DPCSettings.setSettings(context,empty_json);
        assertEquals(DPCSettings.getSettings(context).
                HIDE_LIST_PACKAGE_NAME.length,0);

        Intent intent = new Intent(CommandCollection.ACTION_RECIVER_DPC_SET_SETTINGS);
        intent.putExtra(NameFieldCollection.FIELD_RECIVER_DATA_TEXT,test_json);
        context.sendBroadcast(intent);

        while (DPCSettings.getSettings(context).HIDE_LIST_PACKAGE_NAME.length == 0){
            Thread.sleep(100);
            System.out.println("!testPackageName.equals(DPCSettings.getSettings(context).HIDE_LIST_PACKAGE_NAME[0]");
        }
    }


    @Test
    public void tryRemoveExtraInstalledPackages() throws Exception {

        final Activity act = activityTestRule.getActivity();

        RestrictionsForPackage.init(act);

        setHideAndRemoveList(act);

        installTestPackage(act, R.raw.easycam);

        while (!checkInstallPackage(act, testPackageName)){
            Thread.sleep(100);
            System.out.println("!checkInstallPackage(act, testPackageName)");
        }
        System.out.println("installTestPackage(act, R.raw.easycam)");

        uninstall(act, testPackageName);
        while (checkInstallPackage(act, testPackageName)){
            Thread.sleep(100);
            System.out.println("checkInstallPackage(act, testPackageName)");
        }
        System.out.println("uninstall(act, testPackageName)");

        installTestPackage(act, R.raw.easycam);
        while (!checkInstallPackage(act, testPackageName)){
            Thread.sleep(100);
            System.out.println("!checkInstallPackage(act, testPackageName)");
        }
        System.out.println("installTestPackage(act, R.raw.easycam)");

        int num = RestrictionsForPackage.tryHideExtraInstalledPackages(act);
        System.out.println("tryHideExtraInstalledPackages(act) : " + num);
        //assertTrue(num == DPCSettings.getSettings(act).HIDE_LIST_PACKAGE_NAME.length);

        while (!checkHidePackageName(act, testPackageName)) {
            System.out.println("!checkHidePackageName(act, testPackageName");
            Thread.sleep(100);
        }
        System.out.println(testPackageName + " hide!");

        int rem_num = RestrictionsForPackage.tryRemoveExtraInstalledPackages(act);
        while (checkInstallPackage(act, testPackageName)){
            Thread.sleep(100);
            System.out.println("checkInstallPackage(act, testPackageName)");
        }
        System.out.println("tryRemoveExtraInstalledPackages(act) " + rem_num);
    }

    private boolean checkInstallPackage(Context context, String packageName){
        final List<PackageInfo> list = context.getPackageManager().getInstalledPackages(0);

        for (PackageInfo info :list) {
            if(info.packageName.equals(packageName))
                return true;
        }
        return false;
    }

    void uninstall(Context context, String packageName){
        final PackageManager pm = context.getPackageManager();
        final PackageInstaller packageInstaller = pm.getPackageInstaller();

                /*
                * The client app used to set this to F-Droid, but we need it to be set to
                * this package's package name to be able to uninstall from here.
                */
        //pm.setInstallerPackageName(packageName, "com.afwsamples.testdpc");
        // Create a PendingIntent and use it to generate the IntentSender
        Intent intent = new Intent(context, context.getClass());
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        packageInstaller.uninstall(packageName, sender.getIntentSender());
    }

    @After
    public void releaseResources(){
        System.out.println("---------------------release test ----------------------");
        final Activity act = activityTestRule.getActivity();
        final DevicePolicyManager mDevicePolicyManager =
                (DevicePolicyManager) act.getSystemService(
                        Context.DEVICE_POLICY_SERVICE);

        ComponentName mAdminComponentName = DeviceAdminReceiver.getComponentName(act);

        List<ApplicationInfo> list = getAllInstalledApplicationsSorted(act);
        int restorHideAppNUms = 0;
        for (ApplicationInfo applicationInfo : list) {
            if (mDevicePolicyManager.isApplicationHidden(mAdminComponentName,
                    applicationInfo.packageName)) {
                final String packageName = applicationInfo.packageName;
                mDevicePolicyManager.setApplicationHidden(mAdminComponentName,
                        packageName, false);
                restorHideAppNUms++;
            }
        }
        System.out.println("restorHideAppNUms : " + restorHideAppNUms);

        act.finish();
    }

    private boolean checkHidePackageName(Context context, String packageName){
        final DevicePolicyManager mDevicePolicyManager =
                (DevicePolicyManager) context.getSystemService(
                        Context.DEVICE_POLICY_SERVICE);

        ComponentName mAdminComponentName =
                DeviceAdminReceiver.getComponentName(context);

        return  mDevicePolicyManager.isApplicationHidden(
                mAdminComponentName, packageName);
    }

    private List<ApplicationInfo> getAllInstalledApplicationsSorted(Context context) {
        List<ApplicationInfo> allApps = context.getPackageManager().getInstalledApplications(
                PackageManager.GET_UNINSTALLED_PACKAGES);
        Collections.sort(allApps, new ApplicationInfo.
                DisplayNameComparator(context.getPackageManager()));
        return allApps;
    }

}