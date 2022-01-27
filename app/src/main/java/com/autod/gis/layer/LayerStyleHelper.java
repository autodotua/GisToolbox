package com.autod.gis.layer;

import com.esri.arcgisruntime.arcgisservices.LabelDefinition;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.symbology.Renderer;
import com.esri.arcgisruntime.symbology.UniqueValueRenderer;
import com.autod.gis.data.Config;
import com.autod.gis.data.FileHelper;

import org.json.JSONObject;

import java.io.File;

class LayerStyleHelper
{
    static void setLayerStyle(FeatureLayer layer, String name)
    {
        setLabelDefinition(layer, name);
        setMapInfo(layer, name);
        setUniqueValueRenderer(layer, name);
    }

    private static void setUniqueValueRenderer(FeatureLayer layer, String name)
    {

        UniqueValueRenderer renderer = null;
        if (new File(FileHelper.getUniqueValueStyleFile(name)).exists())
        {
            try
            {
                renderer = (UniqueValueRenderer) Renderer.fromJson(FileHelper.readTextFile(FileHelper.getUniqueValueStyleFile(name)));
            }
            catch (Exception ex)
            {

            }
        }
        if (renderer != null)
        {
            layer.setRenderer(renderer);
        }
    }

   private static void setLabelDefinition(FeatureLayer layer, String name)
    {
        LabelDefinition label = null;
        if (new File(FileHelper.getLabelStyleFile(name)).exists())
        {
            try
            {
                label = LabelDefinition.fromJson(FileHelper.readTextFile(FileHelper.getLabelStyleFile(name)));
            }
            catch (Exception ex)
            {

            }
        }
        if (label != null)
        {
            layer.getLabelDefinitions().add(label);
            layer.setLabelsEnabled(true);
        }

    }

    private static void setMapInfo(FeatureLayer layer, String name)
    {
        String jsonStr = FileHelper.readTextFile(FileHelper.getMapInfoStyleFile(name));
        try
        {
            JSONObject json = new JSONObject(jsonStr);
            double value = json.getDouble("MinScale");
            layer.setMinScale(value);
        }
        catch (Exception ex)
        {

        }
    }

}
