/*
 * Copyright (C) 2020 Baidu, Inc. All Rights Reserved.
 */
package com.example.map.testapp;

import android.app.Application;
import android.content.Context;

import com.baidu.mapapi.SDKInitializer;


public class BNaviDemoApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        ReflectHelper.unseal(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SDKInitializer.initialize(this);
    }
}
