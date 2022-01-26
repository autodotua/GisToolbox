package com.autod.gis.map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;

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
import com.autod.gis.layer.LayerManager;
import com.autod.gis.service.LocationService;
import com.autod.gis.ui.activity.MainActivity;
import com.autod.gis.ui.part.SensorHelper;

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

    public static TrackHelper getInstance()
    {
        if (instance == null)
        {
            instance = new TrackHelper();
        }
        return instance;
    }

    private ShapefileFeatureTable polylineTable;
    private Status status = Status.NotRunning;
    private GraphicsOverlay overlay;
    private List<Point> locationPoints = new ArrayList<>();
    private double length = 0;
    private int count = 0;


    private StringBuilder gpxString;

    public Status getStatus()
    {
        return status;
    }

    private Date startTime;

    public boolean start(Context context)
    {
        try
        {
            locationPoints.clear();
            length = 0;
            count = 0;
            startTime = new Date(System.currentTimeMillis());
            initializeGpx(context);
            overlay = new GraphicsOverlay();
            //SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.parseColor("#64B5F6"), 8);
            SimpleLineSymbol symbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.parseColor("#54A5F6"), 6f);
            overlay.setRenderer(new SimpleRenderer(symbol));
            MapViewHelper.getInstance().mapView.getGraphicsOverlays().add(overlay);


            context.startService(new Intent(context, LocationService.class));

            usePressureAltitude = false;
            if (Config.getInstance().useBarometer)
            {
                if (SensorHelper.getInstance().start(context))
                {
                    usePressureAltitude = true;
                }
            }

            status = Status.Running;
            Toast.makeText(context, "开始记录轨迹", Toast.LENGTH_SHORT).show();

            return true;
        }
        catch (Exception ex)
        {
            Toast.makeText(context, "开启轨迹记录服务失败：n" + ex.getMessage(), Toast.LENGTH_SHORT).show();
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

    private static boolean usePressureAltitude = false;

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

    private Location lastLocation = null;

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
            MapViewHelper.getInstance().mapView.setViewpointCenterAsync(point);
        }

        if (count % 10 == 0)
        {
            saveGpx(context);
        }

    }

    private void updateNotification(Context context)
    {
        int totalS = (int) ((System.currentTimeMillis() - startTime.getTime()) / 1000);
        int h = totalS / 3600;
        int m = (totalS - h * 3600) / 60;
        int s = totalS % 60;
        String text = null;
        switch (status)
        {

            case Running:
                text = context.getString(R.string.track_notification_title_running);
                break;
            case NotRunning:
                text = "";
                break;
            case Pausing:
                context.getString(R.string.track_notification_title_pausing);
                break;
        }
        LocationService.updateNotification(text, context.getString(R.string.track_notification_message, h, m, s, length));
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
        status = Status.NotRunning;
        context.stopService(new Intent(context, LocationService.class));
        if (Config.getInstance().useBarometer)
        {
            SensorHelper.getInstance().stop();
        }

        MapViewHelper.getInstance().mapView.getGraphicsOverlays().remove(overlay);

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
                        //Envelope e = polylineTable.getExtent();
                        PointCollection points = new PointCollection(locationPoints);
                        // feature.setGeometry(geometry);
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
            return FileHelper.createShapefile(GeometryType.POLYLINE, targetPath);
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
    public  int getCount(){
        return  count;
    }

    public  double getLength(){
        return  length;
    }

    public enum Status
    {
        Running,
        NotRunning,
        Pausing,
    }
}
