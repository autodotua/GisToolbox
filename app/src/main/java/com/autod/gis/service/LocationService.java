package com.autod.gis.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.autod.gis.map.TrackHelper;
import com.autod.gis.ui.activity.MainActivity;

import static android.app.PendingIntent.getActivity;

public class LocationService extends Service
{
//    private static LocationService instance;
//
//    public static LocationService getInstance()
//    {
//        return instance;
//    }

    private static LocationManager locationManager;

    public LocationService()
    {
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

//    @Override
//    public void onCreate()
//    {
//        instance = this;
//    }

    private static Notification.Builder notificationBuilder;
    private static NotificationManager notificationManager;
    private   static  final int  NotificationId=1024;

    public static void updateNotification(String title,String message)
    {
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(message);
        if(notificationManager!=null)
        {
            notificationManager.notify(NotificationId, notificationBuilder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        notificationBuilder = new Notification.Builder(this); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class);

        notificationBuilder
                .setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                .setContentTitle("正在记录轨迹") // 设置下拉列表里的标题
                .setSmallIcon(R.drawable.ic_bottom_track) // 设置状态栏内的小图标
                //.setContentText("要显示的内容") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间



        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {

            String channelId = "com.autod.gis";
            String channelName = "轨迹记录";
            NotificationChannel notificationChannel = null;
            notificationChannel = new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(false);
            //notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            notificationBuilder.setChannelId(channelId);
        }

        Notification notification = notificationBuilder.build(); // 获取构建好的Notification
        //notification.defaults = Notification.; //设置为默认的声音
        startForeground(NotificationId, notification);


        if (locationManager == null)
        {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        }
        // Let it continue running until it is stopped.
        //Toast.makeText(this, "服务已经启动", Toast.LENGTH_LONG).show();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this, "没有定位权限", Toast.LENGTH_SHORT).show();
            return START_NOT_STICKY;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Config.getInstance().gpsMinTime, Config.getInstance().gpsMinDistance, listener);

        isLocationRegistered = true;

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {if(notificationManager!=null)
    {
        notificationManager.cancel(NotificationId);}
        stopForeground(true);
        super.onDestroy();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        try
        {
            locationManager.removeUpdates(listener);
            isLocationRegistered = false;
        }
        catch (Exception ex)
        {
            Toast.makeText(this, "停止轮询失败", Toast.LENGTH_SHORT).show();
        }
    }


    public boolean isLocationRegistered;

    LocationListener listener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            // 当GPS定位信息发生改变时，更新定位
            TrackHelper.getInstance().locationChanged(LocationService.this,location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

        @Override
        public void onProviderEnabled(String provider)
        {
            if (ActivityCompat.checkSelfPermission(LocationService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(LocationService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }

        }

        @Override
        public void onProviderDisabled(String provider)
        {

        }
    };
}
