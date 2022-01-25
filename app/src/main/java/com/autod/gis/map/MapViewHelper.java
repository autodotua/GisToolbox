package com.autod.gis.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.autod.gis.BuildConfig;
import com.autod.gis.data.Config;
import com.autod.gis.layer.LayerManager;
import com.autod.gis.R;
import com.autod.gis.ui.activity.MainActivity;
import com.autod.gis.ui.fragment.EditFragment;
import com.autod.gis.ui.fragment.FeatureAttributionTableFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapViewHelper
{
    public ImageView imgCompass;
    public ImageView imgMapCompass;
    public TextView tvwLocation;
    public MapView mapView;

    public MapViewHelper()
    {

    }

    private static MapViewHelper instance = new MapViewHelper();

    public static MapViewHelper getInstance()
    {
        return instance;
    }

    public void linkMapAndMapView()
    {
        mapView.setMap(LayerManager.getInstance().map);
    }

    public void unlinkMapAndMapView()
    {
        mapView.setMap(null);
    }

    public void Initialize()
    {
        mapView = MainActivity.getInstance().findViewById(R.id.main_map);
        mapView.setAttributionTextVisible(false);
        linkMapAndMapView();
        mapView.setMagnifierEnabled(true);
        mapView.setCanMagnifierPanMap(true);
        imgCompass = MainActivity.getInstance().findViewById(R.id.main_img_compass);
        //tvwLocation = MainActivity.getInstance().findViewById(R.id.main_tvw_location);
        imgCompass = MainActivity.getInstance().findViewById(R.id.main_img_compass);
        // mapView.addMapRotationChangedListener(mapRotationChangedEvent -> imgCompass.setRotation(-45 - (float) mapView.getMapRotation()));
        setTouchMapView();
    }

    /**
     * 显示位置弹框
     *
     * @param motionEvent
     */
    private void showLocationCallout(MotionEvent motionEvent)
    {
        android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY()));
        // create a map point from screen point
        Point mapPoint = mapView.screenToLocation(screenPoint);
        // convert to WGS84 for lat/lon format
        if (mapPoint != null)
        {
            Point wgs84Point = (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());

            // create a textview for the callout
            TextView calloutContent = new TextView(MainActivity.getInstance());

            calloutContent.setTextColor(Color.BLACK);
            calloutContent.setLines(2);
            // format coordinates to 4 decimal places
            calloutContent.setText(MainActivity.getInstance().getString(R.string.view_location_callout, wgs84Point.getY(), wgs84Point.getY() >= 0 ? "N" : "S", wgs84Point.getX(), wgs84Point.getX() >= 0 ? "E" : "W"));
            Callout callout = mapView.getCallout();
            // get callout, set content and show
            callout.setLocation(mapPoint);
            callout.setContent(calloutContent);
            callout.show();
            // center on tapped point
            // mapView.setViewpointCenterAsync(mapPoint);
            calloutContent.setOnClickListener(v -> mapView.getCallout().dismiss());
        }
    }

    public ArcGISMap getMap()
    {
        return mapView.getMap();
    }
//    public void setLocationTextTimer()
//    {
//        @SuppressLint("HandlerLeak") final Handler handler = new Handler()
//        {
//            @Override
//            public void handleMessage(Message msg)
//            {
//                switch (msg.what)
//                {
//                    case 0:
//                        Point wgs84Point = (Point) msg.obj;
//                        tvwLocation.setText(instance.getString(R.string.main_location, wgs84Point.getY(), wgs84Point.getX()));
//
//                        break;
//                }
//            }
//        };
//
//
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask()
//        {
//            @Override
//            public void run()
//            {
//                if (mapView.getMap() == null)
//                {
//                    return;
//                }
//                Rect outRect1 = new Rect();
//                instance.getWindow().getDecorView().getWindowVisibleDisplayFrame(outRect1);
//
//                Point point = mapView.screenToLocation(new android.graphics.Point(outRect1.width() / 2, outRect1.height() / 2));
//                if (point != null)
//                {
//                    Point wgs84Point = (Point) GeometryEngine.project(point, SpatialReferences.getWgs84());
//                    Message message = new Message();
//                    message.what = 0;
//                    message.obj = wgs84Point;
//                    handler.sendMessage(message);
//                }
//            }
//        }, 0, 100);
//
//
//    }

    /**
     * 当前指南针角度
     */
    private float currentDegree = 0;

    /**
     * 设置指南针
     *
     * @param sensorEvent
     */
    public void setCompass(SensorEvent sensorEvent)
    {
        float degree = sensorEvent.values[0];
        int screenDegree = getScreenDegree();
        RotateAnimation ra = new RotateAnimation(currentDegree - 45 - screenDegree, -degree - 45 - screenDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(200);
        imgCompass.startAnimation(ra);
        currentDegree = -degree;
    }

    /**
     * 设置地图指南针
     */
    public void setMapCompass()
    {
//        int screenDegree = getScreenDegree();
//        RotateAnimation ra = new RotateAnimation(currentDegree - 45 - screenDegree,
//                -(float)angle - 45 - screenDegree, Animation.RELATIVE_TO_SELF,
//                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
//
//        ra.setDuration(200);
//        imgMapCompass.startAnimation(ra);
        imgMapCompass.setRotation(-45 - (float) mapView.getMapRotation());
        //currentDegree = -(float)angle;
    }

    /**
     * 获取当前屏幕旋转角度，用于修正指南针
     *
     * @return
     */
    private int getScreenDegree()
    {

        int angle = ((WindowManager) Objects.requireNonNull(MainActivity.getInstance().getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay().getRotation();
        return angle * 90;
//        switch (angle) {
//            case Surface.ROTATION_0:
//                return 0;
//            case Surface.ROTATION_90:
//                return 90;
//            case Surface.ROTATION_180:
//                return 180;
//            case Surface.ROTATION_270:
//                return 270;
//            default:
//                return 0;

    }

    /**
     * 将屏幕缩放到刚好显示完成的图层
     */
    public void zoomToLayer(boolean toDefaultScale)
    {
        if (mapView.getMap() == null || LayerManager.getInstance().getLayers().size() == 0)
        {
            Toast.makeText(MainActivity.getInstance(), "请先加载地图", Toast.LENGTH_SHORT).show();
            return;
        }
        if (LayerManager.getInstance().currentLayer == null)
        {
            Toast.makeText(MainActivity.getInstance(), "请先选择当前图层", Toast.LENGTH_SHORT).show();
            return;
        }
        try
        {
            //mapView.setViewpointScaleAsync(map.getMaxScale()   );
            Envelope extent = null;
            if (LayerManager.getInstance().currentLayer instanceof FeatureLayer && Config.getInstance().featureLayerQueryExtentEveryTime)
            {
                extent = ((FeatureLayer) LayerManager.getInstance().currentLayer).getFeatureTable().queryExtentAsync(new QueryParameters()).get();
                //mapView.setViewpointGeometryAsync(LayerManager.getInstance().currentLayer.getFullExtent()).addDoneListener(() ->

            }
            else
            {
                extent = LayerManager.getInstance().currentLayer.getFullExtent();
            }

            mapView.setViewpointGeometryAsync(extent, 24).addDoneListener(() ->
            {
                if (toDefaultScale)
                {
                    mapView.setViewpointScaleAsync(Config.getInstance().defaultScale).addDoneListener(() -> MainActivity.getInstance().setScaleText(Config.getInstance().defaultScale));
                }
            });
        }
        catch (Exception ignored)
        {

        }
    }

    /**
     * 设置单击地图事件
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setTouchMapView()
    {

        mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(MainActivity.getInstance(), mapView)
        {

            @SuppressLint("DefaultLocale")
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                if (mapView.getMap() != null)
                {
                    //如果是矢量图层
                    if (LayerManager.getInstance().currentLayer instanceof FeatureLayer)
                    {
                        FeatureLayer featureLayer = ((FeatureLayer) LayerManager.getInstance().currentLayer);
                        ShapefileFeatureTable featureTable = (ShapefileFeatureTable) featureLayer.getFeatureTable();

                        try
                        {
                            final Point point = mapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));

                            getTouchedFuture(e, feature -> {
                                if (feature == null)
                                {
                                    Toast.makeText(MainActivity.getInstance(), "什么也没选到", Toast.LENGTH_SHORT).show();
//                                     return super.onSingleTapConfirmed(e);
                                }

                                try
                                {
                                    selectFeature(EditFragment.getInstance().isMultiSelect(), feature);
                                    FeatureAttributionTableFragment.getInstance().loadTable(featureTable, feature);
                                    //设置当前状态
                                    EditFragment.getInstance().setSelectStatus(true);

                                }

                                catch (Exception ex)
                                {
                                    Toast.makeText(MainActivity.getInstance(), ex.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
//                            Feature feature = getTouchedFuture(e, featureTable);
                            return super.onSingleTapConfirmed(e);
                        }
                        catch (Exception e1)
                        {
                            e1.printStackTrace();
                        }


                        //获取触摸到的要素

                    }
                    else
                    {

                        showLocationCallout(e);
                    }
                }
                return super.onSingleTapConfirmed(e);
            }

            //Feature touchedFeature;

            //            private void identifyTouchedFuture(MotionEvent e, FeatureLayer layer, Runnable complete)
//            {
//                android.graphics.Point screenPoint = new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY()));
//
//                final ListenableFuture<IdentifyLayerResult> identifyLayerResultListenableFuture = mMapView.identifyLayerAsync(
//                        layer, screenPoint, 12, false, 1);
//                identifyLayerResultListenableFuture.addDoneListener(() -> {
//                    try
//                    {
//                        IdentifyLayerResult identifyLayerResult = identifyLayerResultListenableFuture.get();
//                        touchedFeature = (Feature) identifyLayerResult.getElements().get(0);
//
//                        complete.run();
//                    }
//                    catch (Exception ex)
//                    {
//                    }
//                });
//            }
            private void getTouchedFuture(MotionEvent motionEvent, FeatureGot then)
            {
                if (!(LayerManager.getInstance().currentLayer instanceof FeatureLayer))
                {
                    return;
                }
                FeatureLayer layer = (FeatureLayer) LayerManager.getInstance().currentLayer;
                android.graphics.Point position = new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY()));

                ListenableFuture<IdentifyLayerResult> identityTask = mMapView.identifyLayerAsync(layer, position, 2, false, 1);
                identityTask.addDoneListener(() -> {
                    try
                    {
                        IdentifyLayerResult result = identityTask.get();
                        Feature feature = (Feature) result.getElements().get(0);
                        then.get(feature);
                    }
                    catch (Exception ex)
                    {
//                        Toast.makeText(MainActivity.getInstance(), "查询点击区域要素失败\n" + ex.toString(), Toast.LENGTH_SHORT).show();
                    }
                });


            }

            /**
             * 搜索指定区域的要素
             *
             * @param motionEvent
             * @param featureTable
             * @return
             */
            private Feature getTouchedFuture(MotionEvent motionEvent, FeatureTable featureTable)
            {
                try
                {
                    final Point clickPoint = mapView.screenToLocation(new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY())));
                    //宽容度，就是搜索半径
                    double tolerance = 3;
                    double mapTolerance = tolerance * mapView.getUnitsPerDensityIndependentPixel();
                    Envelope envelope = new Envelope(clickPoint.getX() - mapTolerance, clickPoint.getY() - mapTolerance,
                            clickPoint.getX() + mapTolerance, clickPoint.getY() + mapTolerance, mapView.getSpatialReference());
//envelope=(Envelope)GeometryEngine.project(envelope,featureTable.getSpatialReference());
                    QueryParameters query = new QueryParameters();
                    query.setGeometry(envelope);
                    query.setSpatialRelationship(QueryParameters.SpatialRelationship.INTERSECTS);
//        query.setMaxFeatures(1);
                    ListenableFuture<FeatureQueryResult> result = featureTable.queryFeaturesAsync(query);
                    try
                    {
                        //虽然会搜索到多个要素，但是这里只取第一个，也就是最有可能的那个
                        return result.get().iterator().next();
                    }
                    catch (Exception ex)
                    {
                        Toast.makeText(MainActivity.getInstance(), "查询点击区域要素失败\n" + ex.toString(), Toast.LENGTH_SHORT).show();
                        return null;
                    }
                }
                catch (Exception ex)
                {
                    Toast.makeText(MainActivity.getInstance(), "点击事件失败\n" + ex.toString(), Toast.LENGTH_SHORT).show();
                    return null;
                }
            }

            @Override
            public boolean onRotate(MotionEvent event, double rotationAngle)
            {
                if (Config.getInstance().showMapCompass)
                {
                    setMapCompass();
                }
                return (!Config.getInstance().canRotate) || super.onRotate(event, rotationAngle);
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector)
            {
                MainActivity.getInstance().updateScale(1);
                return super.onScale(detector);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e)
            {
                MainActivity.getInstance().updateScale(1);
                return super.onDoubleTap(e);
            }

            @Override
            public boolean onDoubleTouchDrag(MotionEvent event)
            {
                MainActivity.getInstance().updateScale(1);
                return super.onDoubleTouchDrag(event);
            }


            //            @Override
//            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
//            {
//                if(pauseMove)
//                {
//                    return false;
//                }
//                return super.onScroll(e1, e2, distanceX, distanceY);
//            }


        });


    }
//
//    public  boolean pauseMove=false;


    /**
     * 在地图上选取已在选取列表上的图层
     */
    private void selectFeature(boolean multiple, Feature feature)
    {

        FeatureLayer layer = (FeatureLayer) LayerManager.getInstance().currentLayer;
        if (multiple)
        {
            boolean reppeated = false;
            for (Feature f : selectedFeatures)
            {
                if (f.getAttributes().get("FID").equals(feature.getAttributes().get("FID")))
                {
                    reppeated = true;
                    feature = f;
                    break;
                }
            }
            if (reppeated)
            {
                layer.unselectFeature(feature);
                selectedFeatures.remove(feature);
            }
            else
            {
                layer.selectFeature(feature);
                selectedFeatures.add(feature);
            }
        }
        else
        {
            //可能是esri的BUG，unselectFeatures方法的速度非常慢
            if (selectedFeatures.size() > 0)
            {
                layer.unselectFeatures(selectedFeatures);
            }
            layer.selectFeature(feature);
            selectedFeatures.clear();
            selectedFeatures.add(feature);

        }


    }


    private List<Feature> selectedFeatures = new ArrayList<>();

    /**
     * 停止选取并清空
     */
    public void stopSelect()
    {

        if (LayerManager.getInstance().currentLayer instanceof FeatureLayer)
        {
            FeatureLayer featureLayer = (FeatureLayer) LayerManager.getInstance().currentLayer;
            if (selectedFeatures.size() > 0)
            {
                featureLayer.unselectFeatures(selectedFeatures);
            }
            selectedFeatures.clear();
        }

        EditFragment.getInstance().setSelectStatus(false);
    }

    public List<Feature> getSelectedFeatures()
    {
        return selectedFeatures;
    }

    interface FeatureGot
    {
        void get(Feature result);

    }
}

