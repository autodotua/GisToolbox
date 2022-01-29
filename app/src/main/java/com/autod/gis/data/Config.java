package com.autod.gis.data;

import com.autod.gis.model.LayerInfo;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.autod.gis.map.LayerManager;

import java.io.IOException;
import java.util.ArrayList;

public class Config
{
    private static Config instance;

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

    public ArrayList<LayerInfo> layers = new ArrayList<>();

    public int animationDuration = 500;
    public boolean canRotate = true;
    public ArrayList<LayerInfo> baseLayers = new ArrayList<>();
    public int esriBaseLayer =2;

    public double defaultScale = 10000;

    public int gpsMinTime = 1000;

    public int gpsMinDistance = 5;

    public boolean location = true;

    public boolean autoCenterWhenRecording = true;

    public boolean keepLocationBackground = true;

    public boolean featureLayerQueryExtentEveryTime = true;

    public boolean showMapCompass = false;

    public boolean useBarometer = false;

    public boolean useRelativeAltitude = false;
    public String lastExtent="";

    public void save()
    {
        save(false);
    }

    public void save(boolean includingLayers)
    {
        if (includingLayers)
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
        layers.clear();
        for (Layer layer : LayerManager.getInstance().getLayers())
        {
            String path = ((ShapefileFeatureTable) ((FeatureLayer) layer).getFeatureTable()).getPath();
            try
            {
                path = FileHelper.getRelativePath(path, FileHelper.getShapefileDirPath());
            }
            catch (IOException e)
            {
                assert false;
                e.printStackTrace();
            }
            LayerInfo layerInfo = new LayerInfo(path, layer.isVisible(), layer.getOpacity());
            layers.add(layerInfo);
        }
    }

    public void trySave()
    {
        Config.getInstance().save(true);
    }
}
