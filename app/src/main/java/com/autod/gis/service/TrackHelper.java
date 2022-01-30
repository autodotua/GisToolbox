package com.autod.gis.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;

import com.autod.gis.map.LayerManager;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.map.SensorHelper;
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
import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.autod.gis.data.FileHelper;
import com.esri.arcgisruntime.util.ListenableList;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.autod.gis.data.FileHelper.getTimeBasedFileName;


public class TrackHelper
{
    private static TrackHelper instance;
    private static boolean usePressureAltitude = false;
    private ShapefileFeatureTable polylineTable;
    private Status status = Status.Stop;
    private ListenableList<GraphicsOverlay> oldGraphics = null;
    private GraphicsOverlay overlay;
    private List<Point> locationPoints = new ArrayList<>();
    private double length = 0;
    private int count = 0;


    private StringBuilder gpxString;
    private Date startTime;
    private Location lastLocation = null;

    public static TrackHelper getInstance()
    {
        if (instance == null)
        {
            instance = new TrackHelper();
        }
        return instance;
    }

    public Status getStatus()
    {
        return status;
    }

    public Date getStartTime()
    {
        return startTime;
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

    public boolean start(Context context)
    {
        try
        {
            status = Status.Running;
            //初始化字段
            locationPoints.clear();
            length = 0;
            count = 0;
            startTime = new Date(System.currentTimeMillis());

            //初始化GPX文件
            initializeGpx(context);

            //初始化图形覆盖层
            overlay = new GraphicsOverlay();
            SimpleLineSymbol symbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.parseColor("#54A5F6"), 6f);
            overlay.setRenderer(new SimpleRenderer(symbol));
            MapViewHelper.getInstance().getMapView().getGraphicsOverlays().add(overlay);
            oldGraphics = MapViewHelper.getInstance().getMapView().getGraphicsOverlays();

            //启动服务
            context.startService(new Intent(context.getApplicationContext(), LocationService.class));

            //初始化气压计
            usePressureAltitude = false;
            if (Config.getInstance().useBarometer)
            {
                if (SensorHelper.getInstance().start(context))
                {
                    usePressureAltitude = true;
                }
            }

            Toast.makeText(context, "开始记录轨迹", Toast.LENGTH_SHORT).show();

            return true;
        }
        catch (Exception ex)
        {
            Toast.makeText(context, "开启轨迹记录服务失败：n" + ex.getMessage(), Toast.LENGTH_SHORT).show();
            status = Status.Stop;
            return false;
        }
    }

    private void initializeGpx(Context context)
    {
        String name = getTimeBasedFileName(startTime) + "Track";
        String time = getGpxTime(startTime);
        gpxString = new StringBuilder().append(context.getResources()
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

    private void addGpxPoint(Context context, Location location)
    {
        gpxString.append(context.getResources()
                .getString(R.string.gpx_point,
                        location.getLatitude(),
                        location.getLongitude(),
                        usePressureAltitude ? SensorHelper.getInstance().getCurrentAltitude() : location.getAltitude(),
                        getGpxTime(new Date(System.currentTimeMillis()))
                )
        );
    }

    private void saveGpx(Context context)
    {
        FileHelper.writeTextToFile(FileHelper.getGpxTrackFilePath(getTimeBasedFileName(startTime) + ".gpx"),
                gpxString.toString() + context.getResources().getString(R.string.gpx_foot));
    }

    public void locationChanged(Context context, Location location)
    {
        if (status != Status.Running)
        {
            return;
        }
        lastLocation = location;
        Point point = new Point(location.getLongitude(),
                location.getLatitude(),
                SpatialReferences.getWgs84());
        addGpxPoint(context, location);

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

        updateNotification(context);
        if (Config.getInstance().autoCenterWhenRecording)
        {
            MapViewHelper.getInstance().getMapView().setViewpointCenterAsync(point);
        }

        if (count % 10 == 0)
        {
            saveGpx(context);
        }

    }

    private void updateNotification(Context context)
    {
        String text = null;
        switch (status)
        {
            case Running:
                text = context.getString(R.string.track_notification_title_running);
                break;
            case Stop:
                text = "";
                break;
            case Pausing:
                text = context.getString(R.string.track_notification_title_pausing);
                break;
        }
        LocationService.updateNotification(text, context.getString(R.string.track_notification_message, DateTimeUtility.formatTimeSpan(startTime, new Date()), length));
    }

    public void pause(Context context)
    {
        status = Status.Pausing;
        updateNotification(context);
        if (Config.getInstance().useBarometer)
        {
            SensorHelper.getInstance().stop();
        }

    }

    public void resume(Context context)
    {
        status = Status.Running;
        updateNotification(context);
        if (Config.getInstance().useBarometer)
        {
            SensorHelper.getInstance().start(context);
        }
    }

    public void stop(Context context)
    {
        status = Status.Stop;
        context.stopService(new Intent(context, LocationService.class));
        if (Config.getInstance().useBarometer)
        {
            SensorHelper.getInstance().stop();
        }

        MapViewHelper.getInstance().getMapView().getGraphicsOverlays().remove(overlay);

        saveGpx(context);

        final String polylineFile = getShapefile(context);

        if (polylineFile == null)
        {
            return;
        }
        if (locationPoints.size() <= 1)
        {
            Toast.makeText(context, "记录的点太少，无法生成图形", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(context, "轨迹记录停止", Toast.LENGTH_SHORT).show();

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
                            LayerManager.getInstance().addLayer(context, polylineFile);
                        });
                    }
                    catch (Exception ex)
                    {
                        Looper.prepare();
                        Toast.makeText(context, "线图层生成失败\n" + ex.getMessage(), Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                }
                else
                {

                    String error = "写入线轨迹文件失败\n: " + polylineTable.getLoadError().toString();
                    Looper.prepare();
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                    Looper.loop();

                }
            });
        }).start();

    }


    private String getShapefile(Context context)
    {
        String targetPath = null;

        targetPath = FileHelper.getPolylineTrackFilePath(getTimeBasedFileName(startTime));

        try
        {
            return FileHelper.createShapefile(context, GeometryType.POLYLINE, targetPath);
        }
        catch (Exception ex)
        {
            return null;
        }

    }

    public Location getLastLocation()
    {
        return lastLocation;
    }

    public int getCount()
    {
        return count;
    }

    public double getLength()
    {
        return length;
    }

    public enum Status
    {
        Running,
        Stop,
        Pausing,
    }
}
