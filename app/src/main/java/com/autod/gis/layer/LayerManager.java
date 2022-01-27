package com.autod.gis.layer;

import android.app.AlertDialog;
import android.content.Context;
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

    public static String[] names()
    {
        return Arrays.toString(Basemap.Type.values()).replaceAll("^.|.$", "").split(", ");
    }

    /**
     * 加载图层，已隐藏入口
     */
    public void loadEsriLayer(Context context)
    {
        AlertDialog.Builder listDialog = new AlertDialog.Builder(context);
        listDialog.setTitle("加载地图");
        listDialog.setItems(names(), (DialogInterface dialog, int which) ->
        {
            Intent intent;
            map = new ArcGISMap(Basemap.Type.values()[which], 30, 120, 8);
            MapViewHelper.getInstance().mapView.setMap(map);
        });
        listDialog.show();
    }

    /**
     * 重置图层
     */
    public void resetLayers(Context context)
    {
        try
        {
            BaseLayerHelper.loadBaseLayer(context);
        }
        catch (Exception ex)
        {
            Toast.makeText(context, "重置失败", Toast.LENGTH_SHORT).show();
        }
        if (MapViewHelper.getInstance().mapView.getCallout() != null && MapViewHelper.getInstance().mapView.getCallout().isShowing())
        {
            MapViewHelper.getInstance().mapView.getCallout().dismiss();
        }
    }

    /**
     * 加载栅格GeoPackage图层
     *
     * @param path
     */
    private void loadRasterGeoPackageLayer(Context context, String path)
    {
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
                        loadComplete(context, geoPackageRasterLayer, path);
                    }
                }
                else
                {

                    String emptyMessage = "图层为空!";
                    Toast.makeText(context, emptyMessage, Toast.LENGTH_LONG).show();

                }
            }
            else
            {
                String error = "打开失败：\n" + geoPackage.getLoadError().toString();
                Toast.makeText(context, error, Toast.LENGTH_LONG).show();

            }
        });
    }


//

    private void loadTileCacheLayer(Context context, String path)
    {
        TileCache tile = new TileCache(path);
        tile.loadAsync();
        tile.addDoneLoadingListener(() -> {
            if (tile.getLoadStatus() == LoadStatus.LOADED)
            {
                if (tile.getTileInfo() != null)
                {
                    ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(tile);
                    getLayers().add(tiledLayer);
                    loadComplete(context, tiledLayer, path);
                }
                else
                {

                    Toast.makeText(context, "图层为空", Toast.LENGTH_LONG).show();

                }
            }
            else
            {
                String error = "打开失败：\n" + tile.getLoadError().toString();
                Toast.makeText(context, error, Toast.LENGTH_LONG).show();

            }
        });
    }

    /**
     * 加载shapeFile图层
     *
     * @param path
     */
    private void loadFeatureLayer(Context context, String path)
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
                if (path.contains("Track"))
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
                loadComplete(context, layer, path);
            }
            else
            {
                String error = "打开失败\n: " + table.getLoadError().toString();
                Toast.makeText(context, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 加载.Geodatabase图层
     *
     * @param path
     */
    private void loadGeodatabaseLayer(Context context, String path)
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
                            loadComplete(context, featureLayer, fPath);
                        }
                        else
                        {
                            Toast.makeText(context, "加载失败\n" + geodatabase.getLoadError().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                    // add feature layer to the map
                    MapViewHelper.getInstance().getMap().getOperationalLayers().add(featureLayer);
                }
            }
            else
            {
                Toast.makeText(context, "加载失败\n" + geodatabase.getLoadError().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 加载图片格式栅格图层
     *
     * @param path
     */
    private void loadRasterLayer(Context context, String path)
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
                        loadComplete(context, rasterLayer, path);
                    }
                    else
                    {
                        Toast.makeText(context, "打开失败：" + raster.getLoadError().toString(), Toast.LENGTH_SHORT).show();
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
    private void loadComplete(Context context, Layer layer, String path)
    {
        if (!(layer == null && path == null))
        {
            layerFilePath.put(layer, path);
            if (layer instanceof FeatureLayer)
            {
                LayerStyleHelper.setLayerStyle((FeatureLayer) layer, path);
            }
            currentLayer = layer;
            if (BaseLayerHelper.isLoadingBaseLayers)
            {
                if (Config.getInstance().layerVisible.containsKey(path))
                {
                    layer.setVisible(Config.getInstance().layerVisible.get(path));
                }
                if ((++BaseLayerHelper.baseLayerIndex) < Config.getInstance().layerPath.size())
                {
                    addLayer(context, Config.getInstance().layerPath.get(BaseLayerHelper.baseLayerIndex));

                }
                else
                {
                    MapViewHelper.getInstance().zoomToLayer(context,false);
                    BaseLayerHelper.isLoadingBaseLayers = false;
                }
            }
            else
            {
                    MapViewHelper.getInstance().zoomToLayer(context,false);
            }
            Config.getInstance().trySave();
        }
    }

    /**
     * 加入图层
     *
     * @param path
     */
    public void addLayer(Context context, String path)
    {
        try
        {
            if (path.endsWith(".gpkg"))
            {
                loadRasterGeoPackageLayer(context, path);
            }
            else if (path.endsWith(".tpk"))
            {
                loadTileCacheLayer(context, path);
            }
            else if (path.endsWith(".shp"))
            {
                loadFeatureLayer(context, path);
            }
            else if (path.endsWith(".gdbf"))
            {
                loadGeodatabaseLayer(context, path);
            }
            else if (path.endsWith(".tif") || path.endsWith(".jpg") || path.endsWith(".png"))
            {
                loadRasterLayer(context, path);
            }
            else
            {
                Toast.makeText(context, "不支持的格式", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception ex)
        {
            Toast.makeText(context, "加载图层" + path + "失败，加载终止", Toast.LENGTH_SHORT).show();
        }
        //Config.getInstance().lastPath = new File(path).getParent();

    }

    public void addLayer(Context context, Uri uri)
    {

        String path = Environment.getExternalStorageDirectory() + "/" + uri.getPath().split(":")[1];
        addLayer(context, path);
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


    public void createFeatureLayer(Context context)
    {
        String[] valueArray = {"点", "线", "面"};
        new AlertDialog.Builder(context)
                .setTitle("选择矢量图层的类型")
                .setItems(valueArray, (dialog, which) ->
                {
                    final EditText editText = new EditText(context);
                    editText.setSingleLine(true);
                    editText.setText(FileHelper.getTimeBasedFileName(null));
                    new AlertDialog.Builder(context)
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
                                String path = null;
                                try
                                {
                                    path = FileHelper.createShapefile(type, FileHelper.getProgramPath() + editText.getText().toString());
                                }
                                catch (Exception ex)
                                {
                                    Toast.makeText(context, "创建Shapefile失败：" + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                if (path != null)
                                {
                                    addLayer(context, path);
                                }
                            })
                            .create().show();

                }).create().show();


    }

}

