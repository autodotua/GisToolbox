package com.autod.gis.app;

import android.app.Application;
import android.os.Debug;

/**
 * 能够捕获异常的Application继承类
 */
public class CrashApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        if (!Debug.isDebuggerConnected())
        {
            CrashHandler crashHandler = CrashHandler.getInstance();
            crashHandler.init(getApplicationContext());
        }
    }
}