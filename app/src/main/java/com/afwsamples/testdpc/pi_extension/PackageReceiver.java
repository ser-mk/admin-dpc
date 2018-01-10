package com.afwsamples.testdpc.pi_extension;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afwsamples.testdpc.cosu.CosuUtils;

import java.io.IOException;
import java.io.InputStream;

import sermk.pipi.pilib.CommandCollection;
import sermk.pipi.pilib.ErrorCollector;
import sermk.pipi.pilib.MClient;

public class PackageReceiver extends BroadcastReceiver {

    final String TAG = this.getClass().getName();

    private final ErrorCollector EC = new ErrorCollector();

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Log.v(TAG, "inent: " + intent.toString());
        EC.clear();
        String action;
        try{
            action = intent.getAction().trim();
        } catch (Exception e){
            action = "wrong action!";
            EC.addError(action);
        }
        Log.v(TAG, action);

        String content;
        try{
            content = intent.getStringExtra(Intent.EXTRA_TEXT).trim();
        } catch (Exception e){
            content = "wrong content!";
            EC.addError(content);
        }
        Log.v(TAG, content);

        byte[] bytesArray;
        try{
            bytesArray = intent.getByteArrayExtra(Intent.EXTRA_INITIAL_INTENTS);
            bytesArray.hashCode();
        } catch (Exception e){
            bytesArray = "wrong byte array !".getBytes();
            EC.addError(bytesArray.toString());
            Log.w(TAG, "attached data absent!");
        }

        Uri uri = Uri.EMPTY;
        try {
            uri =  Uri.parse(intent.getStringExtra(Intent.EXTRA_STREAM));
        } catch (Exception e) {
            EC.addError("empty Uri");
        }
        Log.v(TAG, "uri = " + uri);


        boolean success = packageAction(context, content, uri, action);

        if(success){ return; }

        MClient.sendMessage(context,
                ErrorCollector.subjError(TAG,action),
                EC.error);
    }

    private boolean packageAction(Context context, final String content, final Uri uriFile, @NonNull final String action){
        if(action.equals(CommandCollection.ACTION_RECIVER_INSTALL_PACKAGE)){
           return installPackage(context, uriFile);
        } else if (action.equals(CommandCollection.ACTION_RECIVER_REMOVE_PACKAGE)){
            return removePackage(context,content);
        }

        Log.w(TAG, EC.addError("undefined action!"));

        return false;
    }

    private boolean installPackage(Context context, final Uri uriFile){

        try {
            InputStream in = context.getContentResolver().openInputStream(uriFile);
            //InputStream in = new ByteArrayInputStream(apkArray);
            CosuUtils.installPackage(context, in, null);
        } catch (IOException e) {
            e.printStackTrace();
            EC.addError(ErrorCollector.getStackTraceString(e));
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
            EC.addError(e.toString());
            return false;
        }
        return true;
    }

    static private String getFirstLine(final String str){
        return str.split("\r\n|\r|\n")[0]; //bad
    }

}
