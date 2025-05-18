package com.svape.qr.coorapp;

import android.app.Application;
import com.svape.qr.coorapp.di.AppComponent;
import com.svape.qr.coorapp.di.DaggerAppComponent;

public class App extends Application {
    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerAppComponent.builder()
                .application(this)
                .build();
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }
}