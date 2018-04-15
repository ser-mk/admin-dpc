package com.afwsamples.testdpc.pi_extension;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import sermk.pipi.pilib.PiUtils;

/**
 * Created by ser on 07.04.18.
 */

public class DPCSettings {

    static public class Settings {
        public static final String[] EMPTY_ARRAY_STRING = new String[0];
        public final String[] HIDE_LIST_PACKAGE_NAME = EMPTY_ARRAY_STRING;
        public final String[] REMOVE_LIST_PACKAGE_NAME = EMPTY_ARRAY_STRING;
    }

    static final String TAG = "DPCSettings";
    static private Settings settings;

    static public boolean setSettings(Context context, String json){
        final Settings temp = new Gson().fromJson(json, Settings.class);

        if(PiUtils.checkHasNullPublicField(temp, Settings.class)){
            Log.v(TAG,"settings object from json has null object!");
            return false;
        }

        settings = temp;
        return true;
    }

    static public Settings getSettings(Context context){

        if(!PiUtils.checkHasNullPublicField(settings, Settings.class)){
            return settings;
        }

        final String json = PiUtils.getJsonFromShared(context);

        if(json.isEmpty()){
            Log.v(TAG,"JSON settings not found!");
            settings = new Settings();
            return settings;
        }

        final Settings temp = new Gson().fromJson(json, Settings.class);
        if(temp == null){
            Log.v(TAG,"settings broken!");
            settings = new Settings();
            return settings;
        }

        settings = temp;
        return settings;
    }

    static public boolean saveJsonSettings(Context context){

        if(PiUtils.checkHasNullPublicField(settings, Settings.class)){
            Log.v(TAG,"object settings has null object!");
            return false;
        }

        final String json = new Gson().toJson(settings);

        PiUtils.saveJson(context, json);

        return true;
    }

}
