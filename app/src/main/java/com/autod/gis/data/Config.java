package com.autod.gis.data;

import android.content.Context;
import android.widget.Toast;

import com.esri.arcgisruntime.layers.Layer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.autod.gis.BuildConfig;
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

            if (isNormal())
            {
//                instance.normal = true;
                instance.useTiledBaseLayer = true;
                instance.featureLayerQueryExtentEveryTime = true;
                instance.useBarometer = true;
            }
            instance.tileUrls.add("https://mt1.google.cn/vt/lyrs=s&x={x}&y={y}&z={z}");

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
                Toast.makeText(MainActivity.getInstance(), "加载配置文件失败", Toast.LENGTH_SHORT).show();
                instance = new Config();
            }
        }
    }

    public static boolean isNormal()
    {
        return BuildConfig.BUILD_TYPE.equals("normal");
    }

    public String gpkPath = FileHelper.getProgramPath() + "/BaseMap/raster.gpkg";
    public ArrayList<String> layerPath = new ArrayList<String>()
    {{
        //add(FileHelper.getProgramPath() + "/BaseShapefiles/等高线.shp");
//        add(FileHelper.getProgramPath() + "/BaseShapefiles/县.shp");
//        add(FileHelper.getProgramPath() + "/BaseShapefiles/镇.shp");
//        add(FileHelper.getProgramPath() + "/BaseShapefiles/村.shp");
//        add(FileHelper.getProgramPath() + "/BaseShapefiles/小班.shp");
//        add(FileHelper.getProgramPath() + "/BaseShapefiles/公益林.shp");
    }};
    public HashMap<String, Boolean> layerVisible = new HashMap<String, Boolean>()
    {{
//        put(FileHelper.getProgramPath() + "/BaseShapefiles/等高线.shp", false);
//        put(FileHelper.getProgramPath() + "/BaseShapefiles/县.shp", false);
//        put(FileHelper.getProgramPath() + "/BaseShapefiles/镇.shp", true);
//        put(FileHelper.getProgramPath() + "/BaseShapefiles/村.shp", true);
//        put(FileHelper.getProgramPath() + "/BaseShapefiles/小班.shp", false);
//        put(FileHelper.getProgramPath() + "/BaseShapefiles/公益林.shp", true);
    }};
    public String styleFolder = FileHelper.getProgramPath() + "/Style";

    public String getUniqueValueStyleFile(String shapeFileName)
    {
        String path = styleFolder + "/" + shapeFileName.substring(0, shapeFileName.length() - 4) + ".uniqueValue.style";
        return path;
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

    //    public boolean normal = false;
    public boolean useTiledBaseLayer = false;
    public ArrayList<String> tileUrls = new ArrayList<>();

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

    public  void  trySave(Context context)
    {
        try
        {

            Config.getInstance().save(true);
//            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show();
        }
        catch (Exception ex)
        {
            Toast.makeText(context, "配置文件保存失败", Toast.LENGTH_SHORT).show();
        }
    }
}
