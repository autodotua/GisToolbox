package com.autod.gis.data;

import android.content.Context;
import android.widget.Toast;

import com.esri.arcgisruntime.layers.Layer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.autod.gis.layer.LayerManager;
import com.autod.gis.ui.activity.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class Config
{
    private static Config instance;

    public transient boolean check = false;

    public static Config getInstance()
    {
        if (instance == null)
        {
            setInstance(null);
        }
        return instance;
    }


    public static void setInstance(String name)
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = name == null ? FileHelper.getConfigJson() : FileHelper.getConfigJson(name);
        if (json == null)
        {
            instance = new Config();

            instance.featureLayerQueryExtentEveryTime = true;
            instance.useBarometer = true;
            instance.baseUrls.add("https://mt1.google.cn/vt/lyrs=s&x={x}&y={y}&z={z}");

            FileHelper.setConfigJson(gson.toJson(instance));
        }
        else
        {
            try
            {
                instance = gson.fromJson(json, Config.class);
            }
            catch (Exception ex)
            {
                instance = new Config();
            }
        }
    }

    public ArrayList<String> layerPath = new ArrayList<String>();
    public HashMap<String, Boolean> layerVisible = new HashMap<String, Boolean>();
    public String styleFolder = FileHelper.getProgramPath() + "/Style";

    public String getUniqueValueStyleFile(String shapeFileName)
    {
        return styleFolder + "/" + shapeFileName.substring(0, shapeFileName.length() - 4) + ".uniqueValue.style";
    }

    public String getLabelStyleFile(String shapeFileName)
    {
        return styleFolder + "/" + shapeFileName.substring(0, shapeFileName.length() - 4) + ".label.style";
    }

    public String getMapInfoStyleFile(String shapeFileName)
    {
        return styleFolder + "/" + shapeFileName.substring(0, shapeFileName.length() - 4) + ".map.style";
    }

    public int animationDuration = 500;
    public boolean canRotate = true;

    public ArrayList<String> baseUrls = new ArrayList<>();

    public double defaultScale = 10000;

    public int gpsMinTime = 1000;

    public int gpsMinDistance = 5;

    public boolean location = true;

    public boolean autoCenterWhenRecording = true;

    public boolean keepLocationBackground = true;

    public boolean featureLayerQueryExtentEveryTime = false;

    public boolean sideButtonsRight = true;

    public boolean showMapCompass = false;

    public boolean useBarometer = false;

    public boolean useRelativeAltitude = false;

    public String lastRegionalStatisticLayerPath = "";
    public String lastRegionalStatisticAttributeName = "";

    public void save()
    {
        save(false);
    }

    public void save(boolean resaveLayer)
    {
        if (resaveLayer)
        {
            saveLayers();
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        FileHelper.setConfigJson(gson.toJson(this));
    }

    public void save(String name)
    {
        save(name, false);
    }

    public void save(String name, boolean resaveLayer)
    {
        if (resaveLayer)
        {
            saveLayers();
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileHelper.setConfigJson(name, gson.toJson(this));
    }

    private void saveLayers()
    {
        layerPath.clear();
        layerVisible.clear();
        for (Layer layer : LayerManager.getInstance().getLayers())
        {
            String path = LayerManager.getInstance().layerFilePath.get(layer);
            layerPath.add(path);
            layerVisible.put(path, layer.isVisible());
        }
    }

    public void trySave()
    {
        Config.getInstance().save(true);
    }
}
