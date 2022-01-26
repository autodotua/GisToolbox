package com.autod.gis.layer;

import android.content.Context;

import com.esri.arcgisruntime.io.RequestConfiguration;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.WebTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.autod.gis.data.Config;
import com.autod.gis.map.MapViewHelper;

import java.util.ArrayList;


public class BaseLayerHelper
{


    public static void loadBaseLayer(Context context)
    {
        loadTiledBaseLayer(context);
    }


    private static void loadTiledBaseLayer(Context context)
    {
        ArrayList<Layer> layers = new ArrayList<>();
        for (String url : Config.getInstance().baseUrls)
        {
            url = url.replace("{x}", "{col}").replace("{y}", "{row}").replace("{z}", "{level}");
            WebTiledLayer layer = new WebTiledLayer(url);
            RequestConfiguration requestConfiguration = new RequestConfiguration();
            requestConfiguration.getHeaders().put("referer", "http://www.arcgis.com");
            layer.setRequestConfiguration(requestConfiguration);
            layers.add(layer);
        }
        Basemap basemap = new Basemap(layers, null);
        basemap.loadAsync();
        basemap.addDoneLoadingListener(() -> {

            LayerManager.getInstance().map = new ArcGISMap(basemap);
            MapViewHelper.getInstance().linkMapAndMapView();

            loadOtherLayers(context);

        });
    }

    private static void loadOtherLayers(Context context)
    {
        if (Config.getInstance().layerPath.size() > 0)
        {
            isLoadingBaseLayers = true;
            baseLayerIndex = 0;
            LayerManager.getInstance().addLayer( context, Config.getInstance().layerPath.get(0));
        }
    }

//    private static void loadBaseLayer1()
//    {
//        String mmpkPath = Environment.getExternalStorageDirectory() + "/Gis/BaseMap/云和县重点公益林.mmpk";
//
//        MobileMapPackage mpk = new MobileMapPackage(mmpkPath);
//        mpk.loadAsync();
//        mpk.addDoneLoadingListener(() -> {
//            if (mpk.getLoadStatus() == LoadStatus.LOADED)
//            {
//               LayerManager.getInstance().map = new ArcGISMap();// mpk.getMaps().get(0);
//               LayerManager.getInstance().getLayer(0).setVisible(false);
//                baseLayerCount +=LayerManager.getInstance().getLayers().size();
//                MapViewHelper.getInstance().linkMapAndMapView();
//                loadBaseLayer2();
//            }
//
//        });
////       LayerManager.getInstance().map=new ArcGISMap();
////        MapViewHelper.getInstance().linkMapAndMapView();
////        loadBaseLayer2();
//    }
//
//    private static void loadBaseLayer2()
//    {
//        String gpkgPath = Environment.getExternalStorageDirectory() + "/Gis/BaseMap/云和县影像.gpkg";
//        GeoPackage geoPackage = new GeoPackage(gpkgPath);
//        geoPackage.loadAsync();
//        geoPackage.addDoneLoadingListener(() -> {
//            if (geoPackage.getLoadStatus() == LoadStatus.LOADED)
//            {
//                RasterLayer geoPackageRasterLayer = new RasterLayer(geoPackage.getGeoPackageRasters().get(0));
//                //geoPackageRasterLayer.setVisible(false);
//               LayerManager.getInstance().addLayer(geoPackageRasterLayer);
//                baseLayerCount += 1;
//                loadFeatureLayer();
//            }
//        });
//
//    }

//    private static void loadFeatureLayer(int index)
//    {
//        String shpPath = Config.getInstance().layerPath.get(index);
//        if (!new File(shpPath).exists())
//        {
//            Toast.makeText(MainActivity.getInstance(), "图层" + shpPath + "不存在", Toast.LENGTH_SHORT).show();
//            if (index < Config.getInstance().layerPath.size() - 1)
//            {
//                loadFeatureLayer(index + 1);
//            }
//            return;
//        }
//        String fileName = new File(shpPath).getName();
//
//        ShapefileFeatureTable table = new ShapefileFeatureTable(shpPath);
//        table.loadAsync();
//
//
//        table.addDoneLoadingListener(() -> {
//            if (table.getLoadStatus() == LoadStatus.LOADED)
//            {
//                FeatureLayer featureLayer = new FeatureLayer(table);
//                setLayerStyle(featureLayer, fileName);
//
//                featureLayer.setSelectionColor(Color.YELLOW);
//                featureLayer.setSelectionWidth(10);
//
//                if (!Config.getInstance().layerVisible.get(shpPath))
//                {
//                    featureLayer.setVisible(false);
//                }
//                LayerManager.getInstance().getLayers().add(featureLayer);
//                LayerManager.getInstance().layerFilePath.put(featureLayer, shpPath);
//                baseLayerCount += 1;
//                if (index == Config.getInstance().layerPath.size() - 1)
//                {
//                    LayerManager.getInstance().currentLayer = featureLayer;
//                    //MapViewHelper.getInstance().mapView.setViewpointScaleAsync(9000).addDoneListener(() -> instance.setScaleText(9000));
//                    //loadComplete(featureLayer, shpPath);
//                    MapViewHelper.getInstance().zoomToLayer(false);
//                }
//                else
//                {
//                    loadFeatureLayer(index + 1);
//                }
//            }
//
//        });
//    }
//
//
//    public static int baseLayerCount;

    static int baseLayerIndex = 0;

    static boolean isLoadingBaseLayers = false;


}
