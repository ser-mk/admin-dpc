package com.afwsamples.testdpc.pi_extension;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.afwsamples.testdpc.PackageMonitorReceiver;
import com.afwsamples.testdpc.R;
import com.afwsamples.testdpc.cosu.CosuUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PackageReceiver extends BroadcastReceiver {

    final String TAG = this.getClass().getName();

    static final String ACTION_INSTALL = "INSTALL_PACKAGE";
    static final String ACTION_REMOVE = "REMOVE_PACKAGE";

    String erorr = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.v(TAG, "inent: " + intent.toString());
        String action;
        try{
            action = intent.getAction();
            action.isEmpty();
        } catch (Exception e){
            action = "wrong action!";
            Log.w(TAG, "action is not exist!");
        }
        Log.v(TAG, action);

        String content;
        try{
            content = intent.getStringExtra(Intent.EXTRA_TEXT);
            content.isEmpty();
        } catch (Exception e){
            content = "wrong content!";
            Log.w(TAG, "content is not exist!");
        }
        Log.v(TAG, content);

        byte[] bytesArray;
        try{
            bytesArray = intent.getByteArrayExtra(Intent.EXTRA_INITIAL_INTENTS);
            bytesArray.hashCode();
        } catch (Exception e){
            bytesArray = "wrong byte array !".getBytes();
            Log.w(TAG, "attached data absent!");
        }

        Uri uri = Uri.EMPTY;
        try {
            uri =  Uri.parse(intent.getStringExtra(Intent.EXTRA_STREAM));
            Log.v(TAG, "uri = " + uri);
            //Log.v(TAG, "return " + readTextFromUri(context, uri));
        } catch (Exception e) {
            e.printStackTrace();
            uri = Uri.EMPTY;
        }

        boolean success = packageAction(context, content, uri, action);

        if(success){ return; }
    }

    private String readTextFromUri(Context context,Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }

        return stringBuilder.toString();
    }



    private boolean packageAction(Context context, final String content, final Uri uriFile, @NonNull final String action){
        if(action.equals(ACTION_INSTALL)){
           return installPackage(context, uriFile);
        } else if (action.equals(ACTION_REMOVE)){
            return removePackage(context,content);
        }

        erorr = "undefined action!";
        Log.w(TAG, erorr);

        return false;
    }

    private boolean installPackage(Context context, final Uri uriFile){

        try {
            InputStream in = context.getContentResolver().openInputStream(uriFile);
            //InputStream in = new ByteArrayInputStream(apkArray);
            CosuUtils.installPackage(context, in, null);
        } catch (IOException e) {
            e.printStackTrace();
            erorr = e.toString();
            return false;
        }

        return true;
    }

    private boolean removePackage(Context context, final String content){
        final PackageManager pm = context.getPackageManager();
        final PackageInstaller packageInstaller = pm.getPackageInstaller();

        PendingIntent nail = PendingIntent.getBroadcast(
                context, // context
                0, // arbitary
                new Intent(CosuUtils.ACTION_UNINSTALL_COMPLETE),
                PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            final String packageName = getFirstLine(content);
            packageInstaller.uninstall(packageName, nail.getIntentSender());
        } catch (Exception e){
            e.printStackTrace();
            erorr = e.toString();
            return false;
        }
        return true;
    }

    static private String getFirstLine(final String str){
        return str.split("\r\n|\r|\n")[0]; //bad
    }

}
