package com.afwsamples.testdpc.pi_extension;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.gson.Gson;

import sermk.pipi.pilib.CommandCollection;
import sermk.pipi.pilib.ErrorCollector;
import sermk.pipi.pilib.MClient;
import sermk.pipi.pilib.UniversalReciver;


public class SettingsReciever extends BroadcastReceiver {

    final String TAG = this.getClass().getName();

    private final ErrorCollector EC = new ErrorCollector();

    @Override
    public void onReceive(Context context, Intent intent) {
        EC.clear();

        final UniversalReciver.ReciverVarible rv
                = UniversalReciver.parseIntent(intent, TAG);

        if(settingsAction(context, rv.content, rv.action)){
            return;
        }

        MClient.sendMessage(context,
                ErrorCollector.subjError(TAG,rv.action),
                EC.error);
    }

    private boolean settingsAction(Context context, String content, String action) {
        switch (action){
            case CommandCollection.ACTION_RECIVER_FOR_ALL_QUERY_SETTINGS:
                return getSettings(context,action);
            case CommandCollection.ACTION_RECIVER_DPC_SET_SETTINGS:
                return DPCSettings.setSettings(context,content);
            case CommandCollection.ACTION_RECIVER_DPC_SAVE_SETTINGS:
                return DPCSettings.saveJsonSettings(context);
                default: return false;
        }
    }

    private boolean getSettings(Context context, String action){
        final DPCSettings.Settings settings = DPCSettings.getSettings(context);
        final String json = new Gson().toJson(settings);
        return MClient.sendMessage(context,action,json);
    }
}
