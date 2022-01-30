package com.autod.gis.map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.BackgroundGrid;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapRotationChangedEvent;
import com.esri.arcgisruntime.mapping.view.MapRotationChangedListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.autod.gis.data.Config;
import com.autod.gis.R;
import com.autod.gis.ui.fragment.EditFragment;
import com.autod.gis.ui.fragment.FeatureAttributionTableFragment;
import com.esri.arcgisruntime.mapping.view.SketchCreationMode;
import com.esri.arcgisruntime.mapping.view.SketchEditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapViewHelper
{
    private static MapViewHelper instance = new MapViewHelper();
    private MapView mapView;

    private boolean drawing = false;
    private List<Feature> selectedFeatures = new ArrayList<>();
    private List<OnSelectionStatusChangedEventListener> onSelectionStatusChangedEventListeners = new ArrayList<>();
    private  List<OnSketchEditorStatusChangedEventListener> onSketchEditorStatusChangedEventListeners = new ArrayList<>();

    public MapViewHelper()
    {

    }

    public static MapViewHelper getInstance()
    {
        return instance;
    }

    public MapView getMapView()
    {
        return mapView;
    }

    public void linkMapAndMapView()
    {
        mapView.setMap(LayerManager.getInstance().getMap());
    }

    public void unlinkMapAndMapView()
    {
        mapView.setMap(null);
    }

    public SketchEditor getSketchEditor()
    {
        return mapView.getSketchEditor();
    }

    public void sketchEditorStart(SketchCreationMode mode)
    {
        getSketchEditor().start(mode);
        drawing = true;
        onSketchEditorStatusChangedEventListeners.stream().forEach(p->p.onEvent(true));
    }

    public void sketchEditorStart(Geometry geometry, SketchCreationMode mode)
    {
        getSketchEditor().start(geometry, mode);
        drawing = true;
        onSketchEditorStatusChangedEventListeners.stream().forEach(p->p.onEvent(true));
    }

    public void sketchEditorStop()
    {
        getSketchEditor().stop();
        drawing = false;
        onSketchEditorStatusChangedEventListeners.stream().forEach(p->p.onEvent(false));
    }

    public boolean isDrawing()
    {
        return drawing;
    }

    public void Initialize(Activity activity)
    {
        mapView = activity.findViewById(R.id.main_map);
        mapView.setAttributionTextVisible(false);
        linkMapAndMapView();
        mapView.setMagnifierEnabled(true);
        mapView.setCanMagnifierPanMap(true);
        setTouchMapView(activity);
        mapView.setSketchEditor(new SketchEditor());
        ImageView imgMapCompass = activity.findViewById(R.id.main_img_map_compass);
        imgMapCompass.setVisibility(Config.getInstance().showMapCompass ? View.VISIBLE : View.INVISIBLE);
        imgMapCompass.setOnClickListener(v -> mapView.setViewpointRotationAsync(0));
        mapView.addMapRotationChangedListener(mapRotationChangedEvent ->
                imgMapCompass.setRotation(-45 - (float) mapView.getMapRotation()));
    }

    /**
     * 显示位置弹框
     *
     * @param motionEvent
     */
    private void showLocationCallout(Context context, MotionEvent motionEvent)
    {
        android.graphics.Point screenPoint = new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY()));
        Point mapPoint = mapView.screenToLocation(screenPoint);
        if (mapPoint != null)
        {
            Point wgs84Point = (Point) GeometryEngine.project(mapPoint, SpatialReferences.getWgs84());

            TextView calloutContent = new TextView(context);

            calloutContent.setTextColor(Color.BLACK);
            calloutContent.setLines(2);
            calloutContent.setText(context.getString(R.string.view_location_callout, wgs84Point.getY(), wgs84Point.getY() >= 0 ? "N" : "S", wgs84Point.getX(), wgs84Point.getX() >= 0 ? "E" : "W"));
            Callout callout = mapView.getCallout();
            callout.setLocation(mapPoint);
            callout.setContent(calloutContent);
            callout.show();
            calloutContent.setOnClickListener(v -> mapView.getCallout().dismiss());
        }
    }

    public ArcGISMap getMap()
    {
        return mapView.getMap();
    }

    /**
     * 将屏幕缩放到刚好显示完成的图层
     */
    public void zoomToLayer(Context context, Layer layer)
    {
        if (mapView.getMap() == null || LayerManager.getInstance().getLayers().size() == 0)
        {
            Toast.makeText(context, "请先加载地图", Toast.LENGTH_SHORT).show();
            return;
        }
        try
        {
            Envelope extent;
            if (Config.getInstance().featureLayerQueryExtentEveryTime)
            {
                extent = ((FeatureLayer) layer).getFeatureTable().queryExtentAsync(new QueryParameters()).get();
            }
            else
            {
                extent = layer.getFullExtent();
            }

            mapView.setViewpointGeometryAsync(extent, 24);
        }
        catch (Exception ignored)
        {

        }
    }

    /**
     * 设置单击地图事件
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setTouchMapView(Activity activity)
    {
        mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(activity, mapView)
        {
            @SuppressLint("DefaultLocale")
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                if (mapView.getMap() != null)
                {
                    FeatureLayer featureLayer = LayerManager.getInstance().getCurrentLayer();
                    if (featureLayer != null)
                    {
                        ShapefileFeatureTable featureTable = (ShapefileFeatureTable) featureLayer.getFeatureTable();

                        try
                        {
                            getTouchedFuture(e, feature -> {
                                if (feature == null)
                                {
                                    Toast.makeText(activity, "什么也没选到", Toast.LENGTH_SHORT).show();
                                }

                                try
                                {
                                    selectFeature(((EditFragment) ((FragmentActivity) activity).getSupportFragmentManager().findFragmentById(R.id.main_fgm_edit)).isMultiSelect(), feature);
                                    ((FeatureAttributionTableFragment) ((FragmentActivity) activity).getSupportFragmentManager().findFragmentById(R.id.main_fgm_attri)).loadTable(activity, featureTable, feature);
                                    //设置当前状态
                                    raiseOnSelectionStatusChangedEvent(true);

                                }

                                catch (Exception ex)
                                {
                                    Toast.makeText(activity, ex.toString(), Toast.LENGTH_SHORT).show();
                                }
                            });
                            return super.onSingleTapConfirmed(e);
                        }
                        catch (Exception e1)
                        {
                            e1.printStackTrace();
                        }
                    }

                }
                return super.onSingleTapConfirmed(e);
            }

            private void getTouchedFuture(MotionEvent motionEvent, FeatureGot then)
            {
                FeatureLayer layer = LayerManager.getInstance().getCurrentLayer();
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
                        ex.printStackTrace();
                    }
                });


            }


            @Override
            public boolean onRotate(MotionEvent event, double rotationAngle)
            {
                return (!Config.getInstance().canRotate) || super.onRotate(event, rotationAngle);
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector)
            {
                return super.onScale(detector);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e)
            {
                return super.onDoubleTap(e);
            }

            @Override
            public boolean onDoubleTouchDrag(MotionEvent event)
            {
                return super.onDoubleTouchDrag(event);
            }


        });


    }

    /**
     * 在地图上选取已在选取列表上的图层
     */
    private void selectFeature(boolean multiple, Feature feature)
    {

        FeatureLayer layer = LayerManager.getInstance().getCurrentLayer();
        if (multiple)
        {
            boolean reppeated = false;
            for (Feature f : selectedFeatures)
            {
                if (Objects.equals(f.getAttributes().get("FID"), feature.getAttributes().get("FID")))
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

    /**
     * 停止选取并清空
     */
    public void stopSelect()
    {
        FeatureLayer featureLayer = (FeatureLayer) LayerManager.getInstance().getCurrentLayer();
        if (selectedFeatures.size() > 0)
        {
            featureLayer.unselectFeatures(selectedFeatures);
        }
        selectedFeatures.clear();
        raiseOnSelectionStatusChangedEvent(false);
    }

    public List<Feature> getSelectedFeatures()
    {
        return selectedFeatures;
    }

    private void raiseOnSelectionStatusChangedEvent(boolean selected)
    {
        for (OnSelectionStatusChangedEventListener l : onSelectionStatusChangedEventListeners)
        {
            l.onEvent(selected);
        }
    }

    public void addOnSelectionStatusChangedEventListener(OnSelectionStatusChangedEventListener listener)
    {
        this.onSelectionStatusChangedEventListeners.add(listener);
    }
    public void addOnSketchEditorStatusChangedEventListener(OnSketchEditorStatusChangedEventListener listener)
    {
        this.onSketchEditorStatusChangedEventListeners.add(listener);
    }

    interface FeatureGot
    {
        void get(Feature result);
    }

    public interface OnSelectionStatusChangedEventListener
    {
        void onEvent(boolean hasSelectedFeatures);
    }
    public interface OnSketchEditorStatusChangedEventListener
    {
        void onEvent(boolean isDrawing);
    }
}


