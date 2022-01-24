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
//        UniqueValueRenderer render = new UniqueValueRenderer();
//        // Set the field to use for the unique values
//        render.getFieldNames().add("CUNNAME");
//        SimpleFillSymbol defaultFillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.GRAY, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 2));
//        render.setDefaultSymbol(defaultFillSymbol);
//
//        addNewValue(render, "三门村", Color.BLUE);
//        addNewValue(render, "雾溪县", Color.GREEN);
//        addNewValue(render, "后山村", Color.RED);
//        File file=new File(FileHelper.getStylePath("1.style"));
//      FileHelper.writeTextToFile(file,render.toJson());
//        return  render;
        UniqueValueRenderer renderer = null;
        if (new File(Config.getInstance().getUniqueValueStyleFile(name)).exists())
        {
            try
            {
                renderer = (UniqueValueRenderer) Renderer.fromJson(FileHelper.readTextFile(Config.getInstance().getUniqueValueStyleFile(name)));
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

//    private static void addNewValue(UniqueValueRenderer render, String value, int color)
//    {
//        List<Object> values = new ArrayList<>();
//        values.add(value);
//        SimpleFillSymbol symbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, color, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 2));
//        render.getUniqueValues().add(new UniqueValueRenderer.UniqueValue(value, value, symbol, values));
//    }

   private static void setLabelDefinition(FeatureLayer layer, String name)
    {
//        TextSymbol textSymbol = new TextSymbol();
//        textSymbol.setSize(12);
//        textSymbol.setColor(0xFF0000FF);
//        textSymbol.setHaloColor(0xFFFFFF00);
//        textSymbol.setHaloWidth(1);
//
//        JsonObject json = new JsonObject();
//        JsonObject expressionInfo = new JsonObject();
//        expressionInfo.add("expression", new JsonPrimitive("$feature.CUNNAME+\"\\n\"+$feature.MIAN_JI+\"亩\""));
//        json.add("labelExpressionInfo", expressionInfo);
//        //json.add("minScale", new JsonPrimitive("1999"));
//        json.add("symbol", new JsonParser().parse(textSymbol.toJson()));
//
//        File file=new File(FileHelper.getStylePath("2.style"));
//        FileHelper.writeTextToFile(file,json.toString());
//        return LabelDefinition.fromJson(json.toString());
        LabelDefinition label = null;
        if (new File(Config.getInstance().getLabelStyleFile(name)).exists())
        {
            try
            {
                label = LabelDefinition.fromJson(FileHelper.readTextFile(Config.getInstance().getLabelStyleFile(name)));
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
        String jsonStr = FileHelper.readTextFile(Config.getInstance().getMapInfoStyleFile(name));
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
