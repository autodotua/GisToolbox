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
import android.graphics.Color;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.autod.gis.data.FileHelper;
import com.autod.gis.map.LayerManager;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.map.SensorHelper;
import com.autod.gis.model.TrackInfo;
import com.autod.gis.ui.activity.MainActivity;
import com.autod.gis.util.DateTimeUtility;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.util.ListenableList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.PendingIntent.getActivity;
import static com.autod.gis.data.FileHelper.getTimeBasedFileName;

public class TrackService extends Service
{
    private final int NotificationId = 1024;
    private final IBinder binder = new TrackBinder();
    public boolean isLocationRegistered;
    private boolean useBarometer = false;
    private LocationManager locationManager;
    private Notification.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private ShapefileFeatureTable polylineTable;
    private boolean pausing = false;
    private ListenableList<GraphicsOverlay> oldGraphics = null;
    private GraphicsOverlay overlay;
    private List<Point> locationPoints = new ArrayList<>();
    private double length = 0;
    private int count = 0;
    private StringBuilder gpxString;
    private Date startTime;
    private Location lastLocation = null;
    LocationListener listener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            if (isPausing())
            {
                return;
            }
            lastLocation = location;
            Point point = new Point(location.getLongitude(),
                    location.getLatitude(),
                    SpatialReferences.getWgs84());
            addGpxPoint(location);

            locationPoints.add(point);
            if ((++count) >= 2)
            {
                ArrayList<Point> twoPoints = new ArrayList<Point>()
                {
                    {
                        add(locationPoints.get(count - 2));
                        add(point);
                    }
                };

                Polyline line = new Polyline(new PointCollection(twoPoints));
                length += GeometryEngine.lengthGeodetic(line, null, GeodeticCurveType.NORMAL_SECTION);
                Graphic graphic = new Graphic(line);
                overlay.getGraphics().add(graphic);
            }

            if (Config.getInstance().autoCenterWhenRecording)
            {
                MapViewHelper.getInstance().getMapView().setViewpointCenterAsync(point);
            }

            if (count % 10 == 0)
            {
                saveGpx();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

        @Override
        public void onProviderEnabled(String provider)
        {
            if (ActivityCompat.checkSelfPermission(TrackService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(TrackService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }

        }

        @Override
        public void onProviderDisabled(String provider)
        {

        }
    };
    private Timer trackTimer = new Timer();
    private int lastSatelliteCount = 0;
    private int lastFixedSatelliteCount = 0;
    GnssStatus.Callback gnssCallback = new GnssStatus.Callback()
    {
        @Override
        public void onSatelliteStatusChanged(GnssStatus status)
        {
            lastSatelliteCount = status.getSatelliteCount();
            int fixed = 0;
            for (int i = 0; i < lastSatelliteCount; i++)
            {
                if (status.usedInFix(i))
                {
                    fixed++;
                }
            }
            lastFixedSatelliteCount = fixed;
            super.onSatelliteStatusChanged(status);
        }
    };
    private boolean isRunning = false;
    private List<OnTrackTimerListener> onTrackTimerListeners = new ArrayList<OnTrackTimerListener>();

    public TrackService()
    {
    }

    public TrackInfo getLastTrackInfo()
    {
        return new TrackInfo(startTime, lastLocation, count, length, lastSatelliteCount, lastFixedSatelliteCount);
    }

    @Override
    public void onCreate()
    {
        trackTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                if (!isLocationRegistered || pausing)
                {
                    return;
                }
                notificationBuilder.setContentTitle(pausing ? "暂停记录轨迹" : "正在记录轨迹");
                notificationBuilder.setContentText(getString(R.string.track_notification_message, DateTimeUtility.formatTimeSpan(startTime, new Date()), length));
                if (notificationManager != null)
                {
                    notificationManager.notify(NotificationId, notificationBuilder.build());
                }
                TrackInfo info = getLastTrackInfo();
                for (OnTrackTimerListener listener : onTrackTimerListeners)
                {
                    listener.tick(info);
                }
            }
        }, 0, 1000);
        super.onCreate();
    }

    public void addOnTrackTimerListener(@NonNull OnTrackTimerListener listener)
    {
        onTrackTimerListeners.add(listener);
    }

    public boolean isPausing()
    {
        return pausing;
    }

    /**
     * 用于Service仍在运行、但Activity手动关闭或被杀后重新启动后，重新链接
     */
    public void resumeOverlay()
    {
        oldGraphics.remove(overlay);
        MapViewHelper.getInstance().getMapView().getGraphicsOverlays().add(overlay);
        oldGraphics = MapViewHelper.getInstance().getMapView().getGraphicsOverlays();
    }

    private void initializeFields()
    {
        locationPoints.clear();
        length = 0;
        count = 0;
        startTime = new Date(System.currentTimeMillis());
    }

    private void initializeOverlay()
    {
        overlay = new GraphicsOverlay();
        SimpleLineSymbol symbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.parseColor("#54A5F6"), 6f);
        overlay.setRenderer(new SimpleRenderer(symbol));
        MapViewHelper.getInstance().getMapView().getGraphicsOverlays().add(overlay);
        oldGraphics = MapViewHelper.getInstance().getMapView().getGraphicsOverlays();
    }

    private void initializeBarometer()
    {
        useBarometer = false;
        if (Config.getInstance().useBarometer)
        {
            if (SensorHelper.getInstance().start(this))
            {
                useBarometer = true;
            }
        }
    }

    private void initializeGpx()
    {
        String name = getTimeBasedFileName(startTime) + "Track";
        String time = getGpxTime(startTime);
        gpxString = new StringBuilder().append(getResources()
                .getString(R.string.gpx_head, name, time


                )
        );

    }

    private String getGpxTime(Date time)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
        return dateFormat.format(time) + "T" + timeFormat.format(time) + "Z";
    }

    private void addGpxPoint(Location location)
    {
        gpxString.append(getResources()
                .getString(R.string.gpx_point,
                        location.getLatitude(),
                        location.getLongitude(),
                        useBarometer ? SensorHelper.getInstance().getCurrentAltitude() : location.getAltitude(),
                        getGpxTime(new Date(System.currentTimeMillis()))
                )
        );
    }

    private void saveGpx()
    {
        FileHelper.writeTextToFile(FileHelper.getGpxTrackFilePath(getTimeBasedFileName(startTime) + ".gpx"),
                gpxString.toString() + getResources().getString(R.string.gpx_foot));
    }

    public void pause(Context context)
    {
        pausing = true;
        if (Config.getInstance().useBarometer)
        {
            SensorHelper.getInstance().stop();
        }

    }

    public void resume(Context context)
    {
        pausing = false;
        if (Config.getInstance().useBarometer)
        {
            SensorHelper.getInstance().start(context);
        }
    }

    private String getShapefile()
    {
        String targetPath = null;

        targetPath = FileHelper.getPolylineTrackFilePath(getTimeBasedFileName(startTime));

        try
        {
            return FileHelper.createShapefile(this, GeometryType.POLYLINE, targetPath);
        }
        catch (Exception ex)
        {
            return null;
        }

    }

//    public Date getStartTime()
//    {
//        return startTime;
//    }
//
//    public Location getLastLocation()
//    {
//        return lastLocation;
//    }
//
//    public int getCount()
//    {
//        return count;
//    }
//
//    public double getLength()
//    {
//        return length;
//    }
//
//    public int getLastSatelliteCount()
//    {
//        return lastSatelliteCount;
//    }
//
//    public int getLastFixedSatelliteCount()
//    {
//        return lastFixedSatelliteCount;
//    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        try
        {
            if (isRunning)
            {
                throw new RuntimeException("重复启动");
            }
            isRunning = true;
            Notification notification = getNotification();
            startForeground(NotificationId, notification);
            registerLocationServices();
            //初始化字段
            initializeFields();

            //初始化GPX文件
            initializeGpx();

            //初始化图形覆盖层
            initializeOverlay();

            //初始化气压计
            initializeBarometer();

            Toast.makeText(this, "开始记录轨迹", Toast.LENGTH_SHORT).show();
            return START_NOT_STICKY;
        }
        catch (Exception ex)
        {
            Toast.makeText(this, "开启轨迹记录服务失败：n" + ex.getMessage(), Toast.LENGTH_SHORT).show();
            throw ex;
        }
    }

    private void registerLocationServices()
    {
        if (locationManager == null)
        {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this, "没有定位权限", Toast.LENGTH_SHORT).show();
            throw new RuntimeException("没有定位权限");
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Config.getInstance().gpsMinTime, Config.getInstance().gpsMinDistance, listener);
        locationManager.registerGnssStatusCallback(gnssCallback);
        isLocationRegistered = true;
    }

    private Notification getNotification()
    {
        notificationBuilder = new Notification.Builder(this); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class);

        notificationBuilder
                .setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                .setContentTitle("正在记录轨迹") // 设置下拉列表里的标题
                .setSmallIcon(R.drawable.ic_bottom_track) // 设置状态栏内的小图标
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {

            String channelId = "com.autod.gis";
            String channelName = "轨迹记录";
            NotificationChannel notificationChannel = null;
            notificationChannel = new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableLights(false);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            notificationBuilder.setChannelId(channelId);
        }

        return notificationBuilder.build();
    }

    @Override
    public void onDestroy()
    {
        trackTimer.cancel();
        trackTimer.purge();
        if (notificationManager != null)
        {
            notificationManager.cancel(NotificationId);
        }
        stopForeground(true);
        super.onDestroy();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        try
        {
            locationManager.removeUpdates(listener);
            locationManager.unregisterGnssStatusCallback(gnssCallback);
            isLocationRegistered = false;
        }
        catch (Exception ex)
        {
            Toast.makeText(this, "停止位置更新失败", Toast.LENGTH_SHORT).show();
        }
        SensorHelper.getInstance().stop();

        MapViewHelper.getInstance().getMapView().getGraphicsOverlays().remove(overlay);

        saveGpx();

        final String polylineFile = getShapefile();

        if (polylineFile == null)
        {
            return;
        }
        if (locationPoints.size() <= 1)
        {
            Toast.makeText(this, "记录的点太少，无法生成图形", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "轨迹记录停止", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            if (polylineTable != null && polylineTable.getFeatureLayer() != null)
            {
                LayerManager.getInstance().getLayers().remove(polylineTable.getFeatureLayer());
            }
            polylineTable = new ShapefileFeatureTable(polylineFile);

            polylineTable.loadAsync();
            polylineTable.addDoneLoadingListener(() -> {
                if (polylineTable.getLoadStatus() == LoadStatus.LOADED)
                {
                    try
                    {
                        PointCollection points = new PointCollection(locationPoints);
                        Map<String, Object> map = new HashMap<>();
                        Polyline line = new Polyline(points);
                        map.put("长度", GeometryEngine.lengthGeodetic(line, null, GeodeticCurveType.NORMAL_SECTION));

                        Feature feature = polylineTable.createFeature(map, line);
                        polylineTable.addFeatureAsync(feature).addDoneListener(() -> {
                            polylineTable.close();
                            LayerManager.getInstance().addLayer(this, polylineFile);
                        });
                    }
                    catch (Exception ex)
                    {
                        Looper.prepare();
                        Toast.makeText(this, "线图层生成失败\n" + ex.getMessage(), Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                }
                else
                {

                    String error = "写入线轨迹文件失败\n: " + polylineTable.getLoadError().toString();
                    Looper.prepare();
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    Looper.loop();

                }
            });
        }).start();
    }

    public interface OnTrackTimerListener
    {
        void tick(TrackInfo trackInfo);
    }

    public class TrackBinder extends Binder
    {
        public TrackService getService()
        {
            return TrackService.this;
        }
    }
}
