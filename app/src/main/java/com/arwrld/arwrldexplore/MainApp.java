package com.arwrld.arwrldexplore;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.mapbox.mapboxsdk.Mapbox;
import com.parse.Parse;

import io.fabric.sdk.android.Fabric;

public class MainApp extends MultiDexApplication {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        Parse.Configuration.Builder configuration = new Parse.Configuration.Builder(getApplicationContext())
                .applicationId(getString(R.string.app_id))
                .clientKey(getString(R.string.client_key))
                .server(getString(R.string.server))
                .enableLocalDataStore();
        Parse.initialize(configuration.build());
        Mapbox.getInstance(getApplicationContext(),
                "pk.eyJ1IjoiYXJ3cmxkIiwiYSI6ImNqY3llOGx3OTF4cXIyenJ4OXNmZXdldzYifQ.karP7IO67HXd9rNthnagxw");

    }

}
