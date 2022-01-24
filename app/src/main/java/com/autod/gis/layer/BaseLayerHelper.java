package com.autod.gis.layer;

import android.widget.Toast;

import com.esri.arcgisruntime.data.GeoPackage;
import com.esri.arcgisruntime.io.RequestConfiguration;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.RasterLayer;
import com.esri.arcgisruntime.layers.WebTiledLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.autod.gis.data.Config;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.ui.activity.MainActivity;

import java.io.File;
import java.util.ArrayList;


public class BaseLayerHelper
{


    public static void loadBaseLayer()
    {
        if (Config.getInstance().useTiledBaseLayer)
        {
            loadTiledBaseLayer();
        }
        else
        {
            loadGpkBaseLayer();
        }
    }

    private static void loadGpkBaseLayer()
    {
       // baseLayerCount = 0;
        //IndexHelper.checkIndexs();
        if (new File(Config.getInstance().gpkPath).exists())
        {
            GeoPackage geoPackage = new GeoPackage(Config.getInstance().gpkPath);
            geoPackage.loadAsync();
            geoPackage.addDoneLoadingListener(() -> {
                if (geoPackage.getLoadStatus() == LoadStatus.LOADED)
                {
                    RasterLayer geoPackageRasterLayer = new RasterLayer(geoPackage.getGeoPackageRasters().get(0));
                    //geoPackageRasterLayer.setVisible(false);
                    Basemap base = new Basemap();
                    base.getBaseLayers().add(geoPackageRasterLayer);
                    LayerManager.getInstance().map = new ArcGISMap(base);
                    MapViewHelper.getInstance().linkMapAndMapView();
                    //baseLayerCount += 1;
//                    if (Config.getInstance().layerPath.size() > 0)
//                    {
//                        loadFeatureLayer(0);
//                    }
                    loadOtherLayers();
                }
            });
        }
        else
        {
            Toast.makeText(MainActivity.getInstance(), "底图不存在", Toast.LENGTH_SHORT).show();
            LayerManager.getInstance().map = new ArcGISMap();
            MapViewHelper.getInstance().linkMapAndMapView();
            loadOtherLayers();

//                loadFeatureLayer(0);

        }
//        Basemap base=new Basemap();
//        (LayerManager.getInstance().map = new ArcGISMap(base)).addLoadStatusChangedListener(loadStatusChangedEvent -> {
//            if (loadStatusChangedEvent.getNewLoadStatus() == LoadStatus.LOADED)
//            {
//               // loadBaseLayer2();
//            }
//        });
//       LayerManager.getInstance().map.loadAsync();
//        loadBaseLayer2();
        //LayerManager.getInstance().map = new ArcGISMap();

        MapViewHelper.getInstance().linkMapAndMapView();
        //loadBaseLayer2();
        //loadBaseLayer1();
    }

    private static void loadTiledBaseLayer()
    {
        //WebTiledLayer baseLayer = new WebTiledLayer("http://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&scl=1&style=8&x={col}&y={row}&z={level}");
        ArrayList<Layer> layers = new ArrayList<>();
        for (String url : Config.getInstance().tileUrls)
        {
            url = url.replace("{x}", "{col}").replace("{y}", "{row}").replace("{z}", "{level}");
            WebTiledLayer layer = new WebTiledLayer(url);
            RequestConfiguration requestConfiguration =new RequestConfiguration();
            requestConfiguration.getHeaders().put("referer", "http://www.arcgis.com");
            layer.setRequestConfiguration( requestConfiguration);
            layers.add(layer);
        }
        //Basemap basemap = new Basemap(baseLayer);
        Basemap basemap = new Basemap(layers, null);
        basemap.loadAsync();
        //Basemap basemap = new Basemap(new WebTiledLayer("http://webrd0{subDomain}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x={col}&y={row}&z={level}", new string[] { "1", "2", "3", "4" }));
        basemap.addDoneLoadingListener(() -> {

            LayerManager.getInstance().map = new ArcGISMap(basemap);
            MapViewHelper.getInstance().linkMapAndMapView();

            //loadFeatureLayer(0);
            loadOtherLayers();

        });


    }

    private static void loadOtherLayers()
    {
        if (Config.getInstance().layerPath.size() > 0)
        {
            isLoadingBaseLayers =true;
            baseLayerIndex = 0;
            LayerManager.getInstance().addLayer(Config.getInstance().layerPath.get(0));
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
