package com.afwsamples.testdpc.pi_extension.restrictions;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;


import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.pi_extension.DPCSettings;
import com.afwsamples.testdpc.pi_extension.ListExtraPackages;

import java.util.List;

/**
 * Created by ser on 07.04.18.
 */

public class RestrictionsForPackage {

    private final static String NAME =  "RestrictionsForPackage";
    private static String TAG = NAME;

    private static int tryRemovePackageFromList(Context context, String[] list){
        final PackageManager mPackageManager = context.getPackageManager();
        final PackageInstaller packageInstaller = mPackageManager.getPackageInstaller();

        Intent intent = new Intent(context, context.getClass());
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        int num = 0;
        for (String packageName : list) {
            System.out.println("try remove package :" + packageName);
            try {
                packageInstaller.uninstall(packageName,sender.getIntentSender());
                Thread.sleep(1000);
                num++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e){
                e.printStackTrace();
            }
        }
        return num;
    }

    public static int tryRemoveExtraInstalledPackages(Context context) {
        String[] list = DPCSettings.getSettings(context).REMOVE_LIST_PACKAGE_NAME;
        int num = tryRemovePackageFromList(context, list);
        num += tryRemovePackageFromList(context, ListExtraPackages.getDefaultRemovePackages());
        return num;
    }

    public static List<ResolveInfo> getAllLauncherIntentResolversSorted(Context context) {
        final PackageManager mPackageManager = context.getPackageManager();
        final Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> launcherIntentResolvers = mPackageManager
                .queryIntentActivities(launcherIntent, 0);
        return launcherIntentResolvers;
    }


    public static int tryHideExtraInstalledPackages(Context context){
        String[] list = DPCSettings.getSettings(context).HIDE_LIST_PACKAGE_NAME;
        int num = tryHidePackagesFromList(context, list);
        num += tryHidePackagesFromList(context, ListExtraPackages.getDefaultHidePackages());
        return num;
    }

    public static int tryHidePackagesFromList(Context context, String[] list) {
        final DevicePolicyManager mDevicePolicyManager =
                (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);

        ComponentName mAdminComponentName = DeviceAdminReceiver.getComponentName(context);

        int succesHideAppNums = 0;
        for (String packageName : list) {
            System.out.println("+++ packageName : " + packageName);
            if (!mDevicePolicyManager.isApplicationHidden(mAdminComponentName,
                    packageName)) {
                if(mDevicePolicyManager.setApplicationHidden(mAdminComponentName,
                packageName, true)) {
                    succesHideAppNums++;
                    System.out.println("+++ succesHideAppNums : " + succesHideAppNums);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return succesHideAppNums;
    }



    @TargetApi(Build.VERSION_CODES.M)
    public static void init(Context context){
        RestrictionsForPackage.tryRemoveExtraInstalledPackages(context);
        RestrictionsForPackage.tryHideExtraInstalledPackages(context);
    }

}
