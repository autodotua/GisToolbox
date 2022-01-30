package com.autod.gis.map;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.autod.gis.R;
import com.autod.gis.ui.activity.MainActivity;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.mapping.view.SketchCreationMode;
import com.esri.arcgisruntime.mapping.view.SketchGeometryChangedEvent;
import com.esri.arcgisruntime.mapping.view.SketchGeometryChangedListener;

import java.util.Map;

public class MeasureHelper
{
    public static void MeasureLength(View view)
    {
        Context context = view.getContext();
        MapViewHelper.getInstance().sketchEditorStart(SketchCreationMode.POLYLINE);
        TextView tvw = view.findViewById(R.id.main_tvw_draw);
        SketchGeometryChangedListener listener = e -> {
            try
            {
                tvw.setText(context.getString(R.string.measure_length_value, GeometryEngine.lengthGeodetic(e.getGeometry(), null, GeodeticCurveType.NORMAL_SECTION)));
            }
            catch (Exception ex)
            {

            }
        };
        MapViewHelper.getInstance().getSketchEditor().addGeometryChangedListener(listener);
        MainActivity.showDrawBar(view.getRootView(), context.getString(R.string.measure_length_value, 0f), () -> {
            MapViewHelper.getInstance().getSketchEditor().removeGeometryChangedListener(listener);
            MapViewHelper.getInstance().sketchEditorStop();
        });
    }

    public static void MeasureArea(View view)
    {
        Context context = view.getContext();
        MapViewHelper.getInstance().sketchEditorStart(SketchCreationMode.POLYGON);
        TextView tvw = view.findViewById(R.id.main_tvw_draw);
        SketchGeometryChangedListener listener = e -> {
            try
            {
                tvw.setText(context.getString(R.string.measure_area_value,
                        GeometryEngine.areaGeodetic(e.getGeometry(), null, GeodeticCurveType.NORMAL_SECTION),
                        GeometryEngine.lengthGeodetic(((Polygon) e.getGeometry()).toPolyline(), null, GeodeticCurveType.NORMAL_SECTION)));
            }
            catch (Exception ex)
            {

            }
        };
        MapViewHelper.getInstance().getSketchEditor().addGeometryChangedListener(listener);
        MainActivity.showDrawBar(view.getRootView(), context.getString(R.string.measure_area_value, 0f, 0f), () -> {
            MapViewHelper.getInstance().getSketchEditor().removeGeometryChangedListener(listener);
            MapViewHelper.getInstance().sketchEditorStop();
        });
    }

}
