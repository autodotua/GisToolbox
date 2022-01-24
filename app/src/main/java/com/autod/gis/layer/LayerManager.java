package com.autod.gis.layer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.GeoPackage;
import com.esri.arcgisruntime.data.Geodatabase;
import com.esri.arcgisruntime.data.GeodatabaseFeatureTable;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.RasterLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.raster.Raster;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.symbology.Symbol;
import com.autod.gis.data.Config;
import com.autod.gis.data.FileHelper;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.ui.activity.MainActivity;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LayerManager
{
    private static LayerManager instance = new LayerManager();

    public static LayerManager getInstance()
    {
        return instance;
    }

    public Layer currentLayer = null;

    public ArcGISMap map = new ArcGISMap();

    public Map<Layer, String> layerFilePath = new HashMap<>();

//    public List<Layer> mmpkLayer = new ArrayList<>();

    public Map<Layer, LayerInfo> layerLayerProperties = new HashMap<>();

    public static String[] names()
    {
        return Arrays.toString(Basemap.Type.values()).replaceAll("^.|.$", "").split(", ");
    }

    /**
     * 加载图层，已隐藏入口
     */
    public void loadEsriLayer()
    {
        AlertDialog.Builder listDialog = new AlertDialog.Builder(MainActivity.getInstance());
        listDialog.setTitle("加载地图");
        listDialog.setItems(names(), (DialogInterface dialog, int which) ->
        {
            Intent intent;
            map = new ArcGISMap(Basemap.Type.values()[which], 30, 120, 8);
            MapViewHelper.getInstance().mapView.setMap(map);
      /*
            switch (which)
            {
                case 0:
                    map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 30, 120, 8);
                    mapView.setMap(map);
                    break;
                case 1:
                    //ArcGISMap map = new ArcGISMap(Basemap.createImagery());
                    map = new ArcGISMap(Basemap.Type.IMAGERY, 30, 120, 8);
                    mapView.setMap(map);
                    break;
                case 2:
                    intent = new Intent(instance, FileListActivity.class);
                    instance.startActivity(intent);
                    break;
             //   case 3:
                //    ArcGISVectorTiledLayer layer=new ArcGISVectorTiledLayer("http://webst01.is.autonavi.com/appmaptile?style=6&x=13417&y=6499&z=14");
                  //  map=new ArcGISMap(new Basemap(layer));
                  //  mapView.setMap(map);
               //     getLayers().add(layer);
               //     break;
            }
            */
        });
        listDialog.show();
    }

    /**
     * 重置图层
     */
    public void resetLayers()
    {
        try
        {
            BaseLayerHelper.loadBaseLayer();

        }
        catch (Exception ex)
        {
            Toast.makeText(MainActivity.getInstance(), "重置失败", Toast.LENGTH_SHORT).show();
        }
        if (MapViewHelper.getInstance().mapView.getCallout() != null && MapViewHelper.getInstance().mapView.getCallout().isShowing())
        {
            MapViewHelper.getInstance().mapView.getCallout().dismiss();
        }
    }


//    private void openGeoPackage()
//    {
//
//        // Get the full path to the local GeoPackage
//        String geoPackagePath =
//                Environment.getExternalStorageDirectory() + "/tile.gpkg";
//
//
//        // Open the GeoPackage
//        GeoPackage geoPackage = new GeoPackage(geoPackagePath);
//        geoPackage.loadAsync();
//        geoPackage.addDoneLoadingListener(() -> {
//            if (geoPackage.getLoadStatus() == LoadStatus.LOADED)
//            {
//                // Read the feature tables and get the first one
//                FeatureTable geoPackageTable = geoPackage.getGeoPackageFeatureTables().get(0);
//
//                // Make sure a feature table was found in the package
//                if (geoPackageTable == null)
//                {
//                    Toast.makeText(MainActivity.getInstance(), "No feature table found in the package!", Toast.LENGTH_LONG).show();
//
//                    return;
//                }
//
//                // Create a layer to show the feature table
//                FeatureLayer featureLayer = new FeatureLayer(geoPackageTable);
//
//                // Add the feature table as a layer to the map (with default symbology)
//                map.getOperationalLayers().add(featureLayer);
//            }
//            else
//            {
//                Toast.makeText(MainActivity.getInstance(), "GeoPackage failed to load! " + geoPackage.getLoadError(), Toast.LENGTH_LONG).show();
//
//            }
//        });
//    }

    /**
     * 加载栅格GeoPackage图层
     *
     * @param path
     */
    private void loadRasterGeoPackageLayer(String path)
    {

        // open the GeoPackage
        GeoPackage geoPackage = new GeoPackage(path);
        geoPackage.loadAsync();
        geoPackage.addDoneLoadingListener(() -> {
            if (geoPackage.getLoadStatus() == LoadStatus.LOADED)
            {
                if (!geoPackage.getGeoPackageRasters().isEmpty())
                {
                    for (Raster geoPackageRaster : geoPackage.getGeoPackageRasters())
                    {
                        RasterLayer geoPackageRasterLayer = new RasterLayer(geoPackageRaster);
                        MapViewHelper.getInstance().mapView.getMap().getOperationalLayers().add(geoPackageRasterLayer);
                        loadComplete(geoPackageRasterLayer, path);
                    }
                }
                else
                {

                    String emptyMessage = "图层为空!";
                    Toast.makeText(MainActivity.getInstance(), emptyMessage, Toast.LENGTH_LONG).show();

                }
            }
            else
            {
                String error = "打开失败：\n" + geoPackage.getLoadError().toString();
                Toast.makeText(MainActivity.getInstance(), error, Toast.LENGTH_LONG).show();

            }
        });
    }


//

    private void loadTileCacheLayer(String path)
    {

        // open the GeoPackage
        TileCache tile = new TileCache(path);
        tile.loadAsync();
        tile.addDoneLoadingListener(() -> {
            if (tile.getLoadStatus() == LoadStatus.LOADED)
            {
                if (tile.getTileInfo() != null)
                {
                    ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(tile);
                    getLayers().add(tiledLayer);
                    loadComplete(tiledLayer, path);
                }
                else
                {

                    Toast.makeText(MainActivity.getInstance(), "图层为空", Toast.LENGTH_LONG).show();

                }
            }
            else
            {
                String error = "打开失败：\n" + tile.getLoadError().toString();
                Toast.makeText(MainActivity.getInstance(), error, Toast.LENGTH_LONG).show();

            }
        });
    }

    /**
     * 加载shapeFile图层
     *
     * @param path
     */
    private void loadFeatureLayer(String path)
    {
        ShapefileFeatureTable table = new ShapefileFeatureTable(path);

        table.loadAsync();
        table.addDoneLoadingListener(() -> {
            if (table.getLoadStatus() == LoadStatus.LOADED)
            {
                FeatureLayer layer = new FeatureLayer(table);
                layer.setSelectionColor(Color.YELLOW);
                layer.setSelectionWidth(10);
                LayerStyleHelper.setLayerStyle(layer, new File(path).getName());
                if (path.contains("轨迹"))
                {
                    Symbol symbol = null;
                    if (table.getGeometryType() == GeometryType.POLYGON)
                    {
                        SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.parseColor("#64B5F6"), 2f);
                        symbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.LTGRAY, lineSymbol);
                    }
                    else
                    {
                        symbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.parseColor("#64B5F6"), 2f);
                    }
                    layer.setRenderer(new SimpleRenderer(symbol));
                }

                //defaultFeatureLayerProperty(featureLayer);
                getLayers().add(layer);
                loadComplete(layer, path);
            }
            else
            {
                String error = "打开失败\n: " + table.getLoadError().toString();
                Toast.makeText(MainActivity.getInstance(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 加载.Geodatabase图层
     *
     * @param path
     */
    private void loadGeodatabaseLayer(String path)
    {

        if (path.endsWith("gdbf"))
        {
            path = path.substring(0, path.length() - 1);
        }
        // create a new geodatabase from local path
        final Geodatabase geodatabase = new Geodatabase(path);
        final String fPath = path;
        // load the geodatabase
        geodatabase.loadAsync();

        // create feature layer from geodatabase and add to the map
        geodatabase.addDoneLoadingListener(() -> {
            if (geodatabase.getLoadStatus() == LoadStatus.LOADED)
            {
                for (FeatureTable geodatabaseFeatureTable : geodatabase.getGeodatabaseFeatureTables())
                {

                    geodatabaseFeatureTable.loadAsync();
                    // create a layer from the geodatabase feature table and add to map
                    final FeatureLayer featureLayer = new FeatureLayer(geodatabaseFeatureTable);
                    featureLayer.addDoneLoadingListener(() ->
                    {
                        if (featureLayer.getLoadStatus() == LoadStatus.LOADED)
                        {
                            loadComplete(featureLayer, fPath);
                        }
                        else
                        {
                            Toast.makeText(MainActivity.getInstance(), "加载失败\n" + geodatabase.getLoadError().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                    // add feature layer to the map
                    MapViewHelper.getInstance().getMap().getOperationalLayers().add(featureLayer);
                }
            }
            else
            {
                Toast.makeText(MainActivity.getInstance(), "加载失败\n" + geodatabase.getLoadError().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 加载图片格式栅格图层
     *
     * @param path
     */
    private void loadRasterLayer(String path)
    {
        // create a raster from a local raster file
        Raster raster = new Raster(path);
        // create a raster layer
        final RasterLayer rasterLayer = new RasterLayer(raster);
        rasterLayer.loadAsync();
        // set viewpoint on the raster
        rasterLayer.addDoneLoadingListener(() ->
                {
                    if (rasterLayer.getLoadStatus() == LoadStatus.LOADED)
                    {
                        getLayers().add(rasterLayer);
                        loadComplete(rasterLayer, path);
                    }
                    else
                    {
                        Toast.makeText(MainActivity.getInstance(), "打开失败：" + raster.getLoadError().toString(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }


    /**
     * 用于保存打开工程文件待加入的图层
     */
    //public List<String> needAddedLayers;

    /**
     * 图层添加成功
     *
     * @param layer
     * @param path
     */
    private void loadComplete(Layer layer, String path)
    {
        if (!(layer == null && path == null))
        {
            layerFilePath.put(layer, path);
            if (layer instanceof FeatureLayer)
            {
                LayerStyleHelper.setLayerStyle((FeatureLayer) layer, path);
            }
            currentLayer = layer;
            //Envelope geometry = layer.getFullExtent();
//        if (geometry != null)
//        {
            //applyLayerProperty(layer, DataAnalysis.getLayerProperty(layer));
        }
//        if (needAddedLayers.size() != 0)
//        {
//            //因为要保存图层顺序，所以不能一起加入。
//            String needAddedPath = needAddedLayers.get(0);
//            needAddedLayers.remove(0);
//            addLayer(needAddedPath);
//            if (needAddedLayers.size() == 0)
//            {
//                currentLayer = getLayer(0);
//            }
//        }

//        if (MainActivity.getInstance().frgAttri.getTranslationY() == 0)
//        {
//            MainActivity.getInstance().frgAttri.setTranslationY(-MainActivity.getInstance().frgAttri.getHeight());
//        }
        // }

        //MainActivity.getInstance().updateScale(1);

        if (BaseLayerHelper.isLoadingBaseLayers)
        {
            if (Config.getInstance().layerVisible.containsKey(path))
            {
                layer.setVisible(Config.getInstance().layerVisible.get(path));
            }
            if ((++BaseLayerHelper.baseLayerIndex) < Config.getInstance().layerPath.size())
            {
                addLayer(Config.getInstance().layerPath.get(BaseLayerHelper.baseLayerIndex));

            }
            else
            {
                MapViewHelper.getInstance().zoomToLayer(false);
                BaseLayerHelper.isLoadingBaseLayers = false;
            }
        }
        else
        {
            MapViewHelper.getInstance().zoomToLayer(false);
        }
        Config.getInstance().trySave(MainActivity.getInstance());
    }

//    private void defaultFeatureLayerProperty(FeatureLayer layer)
//    {
//        // create a new simple renderer for the line feature layer
//        SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.rgb(0, 128, 0), 1.5f);
//
//        FillSymbol symbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.argb(32, 0, 255, 0), lineSymbol);
//        SimpleRenderer simpleRenderer = new SimpleRenderer(symbol);
//        layer.setRenderer(simpleRenderer);
//    }

    /**
     * 应用图层样式
     *
     * @param layer
     * @param property
     */
//    public void applyLayerProperty(Layer layer, LayerInfo property)
//    {
//        layer.setOpacity(property.opacity);
//
//        if (layer instanceof FeatureLayer )
////        if (layer instanceof FeatureLayer && !mmpkLayer.contains(layer))
//        {
//            FeatureLayer featureLayer = (FeatureLayer) layer;
//            FeatureLayerInfo featureLayerInfo = property.featureLayerInfo;
//            if (featureLayerInfo == null)
//            {
//                featureLayerInfo = new FeatureLayerInfo();
//            }
//
//            SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, featureLayerInfo.borderColor, featureLayerInfo.borderThickness);
//            FillSymbol symbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, featureLayerInfo.fillColor, lineSymbol);
//            SimpleRenderer simpleRenderer = new SimpleRenderer(symbol);
//            featureLayer.setRenderer(simpleRenderer);
//
//        }
//        layerLayerProperties.put(layer, property);
//    }


    /**
     * 加入图层
     *
     * @param path
     */
    public void addLayer(String path)
    {
        try
        {
            if (path.endsWith(".gpkg"))
            {
                loadRasterGeoPackageLayer(path);
            }
            else if (path.endsWith(".tpk"))
            {
                loadTileCacheLayer(path);
            }
            else if (path.endsWith(".shp"))
            {
                loadFeatureLayer(path);
            }
            else if (path.endsWith(".gdbf"))
            {
                loadGeodatabaseLayer(path);
            }
            else if (path.endsWith(".tif") || path.endsWith(".jpg") || path.endsWith(".png"))
            {
                loadRasterLayer(path);
            }
//        else if (path.endsWith(".mmxd"))
//        {
//            getLayers().clear();
//            needAddedLayers = DataAnalysis.getSavedLayers(path);
//            BaseLayerHelper.loadBaseLayer();
//        }
//        else if (path.endsWith(".mmpk"))
//        {
//            loadMobilePackageMap(path);
//        }
            else
            {
                Toast.makeText(MainActivity.getInstance(), "不支持的格式", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        catch (Exception ex)
        {
            Toast.makeText(MainActivity.getInstance(), "加载图层" + path + "失败，加载终止", Toast.LENGTH_SHORT).show();
        }
        //Config.getInstance().lastPath = new File(path).getParent();

    }

    public void addLayer(Uri uri)
    {

        String path = Environment.getExternalStorageDirectory() + "/" + uri.getPath().split(":")[1];
        addLayer(path);
    }

    /**
     * 获取所有图层
     *
     * @return
     */
    public LayerList getLayers()
    {
        return map.getOperationalLayers();
    }

    /**
     * 获取指定索引的图层
     *
     * @param index
     * @return
     */
    public Layer getLayer(int index)
    {
        return getLayers().get(index);
    }


    public void createFeatureLayer()
    {
        String[] valueArray = {"点", "线", "面"};
        new AlertDialog.Builder(MainActivity.getInstance())
                .setTitle("选择矢量图层的类型")
                .setItems(valueArray, (dialog, which) ->
                {
                    final EditText editText = new EditText(MainActivity.getInstance());
                    editText.setSingleLine(true);
                    editText.setText(FileHelper.getTimeBasedFileName(null));
                    new AlertDialog.Builder(MainActivity.getInstance())
                            .setTitle("输入文件名")
                            .setView(editText)
                            .setPositiveButton("确定", (dialog2, which2) ->
                            {
                                GeometryType type = GeometryType.POINT;
                                switch (which)
                                {
                                    case 1:
                                        type = GeometryType.POLYLINE;
                                        break;
                                    case 2:
                                        type = GeometryType.POLYGON;
                                        break;
                                }
                                String path = FileHelper.createShapefile(type, FileHelper.getProgramPath() + editText.getText().toString());

                                if (path != null)
                                {
                                    addLayer(path);
                                }
                            })
                            .create().show();

                }).create().show();


    }

}

