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
       // ArcGISRuntimeEnvironment.
        //ArcGISRuntimeEnvironment.setLicense("runtimelite,1000,rud7068690343,none,B5H93PJPXJPSPF002209");
        if (!Debug.isDebuggerConnected())
        {
            CrashHandler crashHandler = CrashHandler.getInstance();
            crashHandler.init(getApplicationContext());
        }
    }
}