package com.ov3rk1ll.kinocast;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.ov3rk1ll.kinocast.api.KinoxParser;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.ui.util.glide.OkHttpViewModelUrlLoader;
import com.ov3rk1ll.kinocast.ui.util.glide.ViewModelGlideRequest;
import com.ov3rk1ll.kinocast.utils.Utils;

import java.io.InputStream;


public class CastApp extends Application {


    private static CastApp sApplication;

    public static CastApp getApplication() {
        return sApplication;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }


    @Override
    public void onCreate() {
        sApplication = this;

        LoadParser();

        String flurry_key = getString(R.string.FLURRY_API_KEY);
        if(!Utils.isStringEmpty(flurry_key)) {
            new com.flurry.android.FlurryAgent.Builder()
                    .withLogEnabled(true)
                    .withCaptureUncaughtExceptions(true)
                    .build(this, getString(R.string.FLURRY_API_KEY));
        }
        //com.google.android.gms.ads.MobileAds.initialize(this, "ca-app-pub-2728479259954125~72137");
        Glide.get(getApplicationContext())
                .register(ViewModelGlideRequest.class, InputStream.class, new OkHttpViewModelUrlLoader.Factory());

        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

        super.onCreate();
    }

    public void LoadParser() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Utils.DisableSSLCheck = preferences.getBoolean("allow_invalid_ssl",false);
        String parser = preferences.getString("parser", Integer.toString(KinoxParser.PARSER_ID));
        Parser.selectParser(this, Integer.parseInt(parser));
        if(Parser.getInstance() == null){
            Parser.selectParser(this, KinoxParser.PARSER_ID);
        }
        Log.i("selectParser", "ID is " + Parser.getInstance().getParserId());
    }



    public static Context GetCheckedContext(Context context){
        if(context != null) return context;
        return getContext();
    }
}
