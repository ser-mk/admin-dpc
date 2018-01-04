package com.afwsamples.testdpc.pi_extension;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afwsamples.testdpc.R;

public class PackageReceiver extends BroadcastReceiver {

    final String TAG = this.getClass().getName();

    static final String ACTION_INSTALL = "INSTALL_PACKAGE";
    static final String ACTION_REMOVE = "REMOVE_PACKAGE";

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

        packageAction(context, content, bytesArray, action);
    }

    private boolean packageAction(Context context, final String content, final byte[] bytesArray, @NonNull final String action){
        if(action.equals(ACTION_INSTALL)){
           // return setSettings(content, bytesArray);
        } else if (action.equals(ACTION_REMOVE)){
            //return saveSettings(context);
        }

        Log.w(TAG, "undefined action!");

        return false;
    }
}
