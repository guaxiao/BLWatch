package com.tau.blwatch;

import android.app.Application;
import android.util.Log;

import org.xutils.x;

public class MyApplication extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        //TODO：全工程xUtil化
        //初始化xUtils3框架
        x.Ext.init(this);
        x.Ext.setDebug(true);// 输出debug日志

        Log.i("Application","onCreate");
    }
}
