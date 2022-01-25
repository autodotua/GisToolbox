package com.autod.gis.map;

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
import com.esri.arcgisruntime.geometry.Polygon;
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
    private static ShapefileFeatureTable polylineTable;
    private static Status status = Status.NotRunning;
    private static GraphicsOverlay overlay;
    private static List<Point> locationPoints = new ArrayList<>();
    private static double length = 0;
    private static int count = 0;

//    private static GPX.Builder gpxBuilder;
//    private static Track.Builder trackBuilder;
//    private static TrackSegment.Builder segment;

    private static StringBuilder gpxString;

    public static Status getStatus()
    {
        return status;
    }

    private static Date startTime;

    public static boolean start()
    {

        try
        {
            //isFirstPoint = true;
            locationPoints.clear();
            length = 0;
            count = 0;
            startTime = new Date(System.currentTimeMillis());
            loadGpx();
            //getShapefile(GeometryType.POINT);
            if (overlay == null)
            {
                overlay = new GraphicsOverlay();
                //SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.parseColor("#64B5F6"), 8);
                SimpleLineSymbol symbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.parseColor("#54A5F6"), 6f);
                overlay.setRenderer(new SimpleRenderer(symbol));
            }
            else
            {
                overlay.getGraphics().clear();
            }
            MapViewHelper.getInstance().mapView.getGraphicsOverlays().add(overlay);
//
//            pointFeatureTable = new ShapefileFeatureTable(FileHelper.getPointTrackFilePath("Point.shp"));
//
//            pointFeatureTable.loadAsync();
//            pointFeatureTable.addDoneLoadingListener(() -> {
//                if (pointFeatureTable.getLoadStatus() == LoadStatus.LOADED)
//                {
//                    FeatureLayer featureLayer = new FeatureLayer(pointFeatureTable);
//                    SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.parseColor("#64B5F6"), 5);
//                    featureLayer.setRenderer(new SimpleRenderer(symbol));
//                   LayerManager.getInstance().getLayers().add(featureLayer);
//                }
//                else
//                {
//                    String error = "打开空白点文件失败\n: " + pointFeatureTable.getLoadError().toString();
//                    Toast.makeText(MainActivity.getInstance(), error, Toast.LENGTH_LONG).show();
//                }
//            });

            MainActivity.getInstance().startService(new Intent(MainActivity.getInstance(), LocationService.class));
            //LocationService.getInstance().startUpdate();

            usePressureAltitude = false;
            if (Config.getInstance().useBarometer)
            {
                if (SensorHelper.getInstance().start())
                {
                    usePressureAltitude = true;
                }
            }

            status = Status.Running;
            Toast.makeText(MainActivity.getInstance(), "开始记录轨迹", Toast.LENGTH_SHORT).show();

            return true;
        }
        catch (Exception ex)
        {
            Toast.makeText(MainActivity.getInstance(), "开启轨迹记录服务失败：n" + ex.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
//
//    private static void clearFeatures(FeatureTable table)
//    {
//        Envelope envelope = new Envelope(-Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, SpatialReferences.getWgs84());
//        QueryParameters query = new QueryParameters();
//        query.setGeometry(envelope);
//        query.setSpatialRelationship(QueryParameters.SpatialRelationship.WITHIN);
////        query.setMaxFeatures(1);
//        ListenableFuture<FeatureQueryResult> result = table.queryFeaturesAsync(query);
//        try
//        {
//            //虽然会搜索到多个要素，但是这里只取第一个，也就是最有可能的那个
//            table.deleteFeaturesAsync(result.get());
//        }
//        catch (Exception ex)
//        {
//
//        }
//    }
//
//    private static boolean isFirstPoint = true;

    private static void loadGpx()
    {
        String name = getTimeBasedFileName(startTime) + "Track";
        String time = getGpxTime(startTime);
        gpxString = new StringBuilder().append(MainActivity.getInstance().getResources()
                .getString(R.string.gpx_head, name, time


                )
        );

        //Toast.makeText(MainActivity.getInstance(), gpxString.toString(), Toast.LENGTH_SHORT).show();
//        gpxBuilder = GPX.builder().addTrack(Track.builder().build());
//        trackBuilder = Track.builder().addSegment(TrackSegment.builder().build());
//        segment = TrackSegment.builder();

    }

    private static String getGpxTime(Date time)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
        return dateFormat.format(time) + "T" + timeFormat.format(time) + "Z";
    }

    private static boolean usePressureAltitude = false;

    private static void addGpxPoint(Location location)
    {
        gpxString.append(MainActivity.getInstance().getResources()
                .getString(R.string.gpx_point,
                        location.getLatitude(),
                        location.getLongitude(),
                        usePressureAltitude ? SensorHelper.getInstance().getCurrentAltitude() : location.getAltitude(),
                        getGpxTime(new Date(System.currentTimeMillis()))
                )
        );
    }

    private static void saveGpx()
    {
//        trackBuilder.segments().set(0, segment.build());
//        gpxBuilder.tracks().set(0, trackBuilder.build());
//        try
//        {
//           // gpxBuilder.build().
//            String path=FileHelper.getGpxTrackFilePath("test.gpx");
//            //Toast.makeText(MainActivity.getInstance(), gpxBuilder.build()., Toast.LENGTH_SHORT).show();
//            GPX.write(gpxBuilder.build(),path);
//            //Toast.makeText(MainActivity.getInstance(), gpxBuilder.build()., Toast.LENGTH_SHORT).show();
//        }
//        catch (Exception ex)
//        {
//            Toast.makeText(MainActivity.getInstance(), "保存GPX失败\n"+ex.getMessage(), Toast.LENGTH_SHORT).show();
//        }
        FileHelper.writeTextToFile(FileHelper.getGpxTrackFilePath(getTimeBasedFileName(startTime) + ".gpx"),
                gpxString.toString() + MainActivity.getInstance().getResources().getString(R.string.gpx_foot));
    }

    public static void locationChanged(Location location)
    {
        if (status != Status.Running)
        {
            return;
        }
//        if (pointFeatureTable.getLoadStatus() != LoadStatus.LOADED)
//        {
//            return;
//        }

        Point point = new Point(location.getLongitude(),
                location.getLatitude(),
                //location.getAltitude(),
                SpatialReferences.getWgs84());
        addGpxPoint(location);
        //segment.addPoint(p -> p.lat(point.getY()).lon(point.getX()).ele(point.getZ()));

        locationPoints.add(point);
//        Graphic graphic = new Graphic(point);
//        SimpleMarkerSymbol symbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.parseColor("#4495D6"), 12);
//        graphic.setSymbol(symbol);
//        overlay.getGraphics().add(graphic);
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
            //overlay.getGraphics().get(locationPoints.size() - 2).setSymbol(null);
        }
        LocationService.updateNotification(MainActivity.getInstance().getString(R.string.track_notification_title_running), MainActivity.getInstance().getString(R.string.track_notification_message, count, length));
//        Feature feature = pointFeatureTable.createFeature();
//        features.add(feature);
//        feature.setGeometry(point);
//        LocationService.updateNotification("已记录" + features.size() + "个点");
//        pointFeatureTable.addFeatureAsync(feature).addDoneListener(() -> {
//
////            if(isFirstPoint)
////            {
////                MapViewHelper.getInstance().mapView.setViewpointScaleAsync(10000);
////                isFirstPoint=false;
////            }
//        });
        if (Config.getInstance().autoCenterWhenRecording)
        {
            MapViewHelper.getInstance().mapView.setViewpointCenterAsync(point);
        }

        if (count % 10 == 0)
        {
            saveGpx();
        }

        // shapefileFeatureTable.updateFeatureAsync(feature);
    }

    public static void pause()
    {
        status = Status.Pausing;
        LocationService.updateNotification(MainActivity.getInstance().getString(R.string.track_notification_title_pausing), MainActivity.getInstance().getString(R.string.track_notification_message, count, length));
        if (Config.getInstance().useBarometer)
        {
            SensorHelper.getInstance().stop();
        }

    }

    public static void resume()
    {
        status = Status.Running;
        LocationService.updateNotification(MainActivity.getInstance().getString(R.string.track_notification_title_running), MainActivity.getInstance().getString(R.string.track_notification_message, count, length));
        if (Config.getInstance().useBarometer)
        {
            SensorHelper.getInstance().start();
        }
    }

    public static void stop()
    {
        status = Status.NotRunning;
        //LocationService.getInstance().stopUpdate();
        MainActivity.getInstance().stopService(new Intent(MainActivity.getInstance(), LocationService.class));
        if (Config.getInstance().useBarometer)
        {
            SensorHelper.getInstance().stop();
        }

        MapViewHelper.getInstance().mapView.getGraphicsOverlays().remove(overlay);

        saveGpx();

        final String polylineFile = getShapefile();

        if (polylineFile == null)
        {
            return;
        }
//        pointFeatureTable.deleteFeaturesAsync(features).addDoneListener(() ->
//               LayerManager.getInstance().getLayers().remove(pointFeatureTable.getFeatureLayer()));

        if (locationPoints.size() <= 1)
        {
            Toast.makeText(MainActivity.getInstance(), "记录的点太少，无法生成图形", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(MainActivity.getInstance(), "轨迹记录停止", Toast.LENGTH_SHORT).show();

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
                            LayerManager.getInstance().addLayer(polylineFile);

                        });
                    }
                    catch (Exception ex)
                    {
                        Looper.prepare();
                        Toast.makeText(MainActivity.getInstance(), "线图层生成失败\n" + ex.getMessage(), Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                }
                else
                {

                    String error = "写入线轨迹文件失败\n: " + polylineTable.getLoadError().toString();
                    Looper.prepare();
                    Toast.makeText(MainActivity.getInstance(), error, Toast.LENGTH_LONG).show();
                    Looper.loop();

                }
            });
        }).start();

    }


    private static String getShapefile( )
    {
        String targetPath = null;

        targetPath = FileHelper.getPolylineTrackFilePath(getTimeBasedFileName(startTime));

        return FileHelper.createShapefile(GeometryType.POLYLINE, targetPath);
    }

    public enum Status
    {
        Running,
        NotRunning,
        Pausing,
    }
}
