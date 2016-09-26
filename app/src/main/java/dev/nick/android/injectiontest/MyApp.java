package dev.nick.android.injectiontest;

import android.app.Application;

import dev.nick.android.injection.Injector;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Injector.init(this);
    }
}
