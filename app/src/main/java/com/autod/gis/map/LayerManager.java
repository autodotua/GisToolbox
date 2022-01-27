package com.autod.gis.map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.autod.gis.model.LayerInfo;
import com.esri.arcgisruntime.arcgisservices.LabelDefinition;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.io.RequestConfiguration;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.layers.RasterLayer;
import com.esri.arcgisruntime.layers.WebTiledLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.raster.Raster;
import com.esri.arcgisruntime.symbology.Renderer;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.symbology.Symbol;
import com.autod.gis.data.Config;
import com.autod.gis.data.FileHelper;
import com.esri.arcgisruntime.symbology.UniqueValueRenderer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class LayerManager
{
    private static final String TAG = "Layer";
    private static LayerManager instance = new LayerManager();

    public static LayerManager getInstance()
    {
        return instance;
    }

    public Layer currentLayer = null;

    public ArcGISMap map = new ArcGISMap();

    public static String[] names()
    {
        return Arrays.toString(Basemap.Type.values()).replaceAll("^.|.$", "").split(", ");
    }

    public void initialize(Context context)
    {
        try
        {
            Basemap basemap = getBaseMap();
            basemap.addDoneLoadingListener(() -> {
                LayerManager.getInstance().map = new ArcGISMap(basemap);
                MapViewHelper.getInstance().linkMapAndMapView();
                loadLayers(context);
            });
        }
        catch (Exception ex)
        {
            Toast.makeText(context, "底图加载失败\n" + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLayers(Context context)
    {
        for (LayerInfo layerInfo : Config.getInstance().layers)
        {
            try
            {
               Layer layer= addLayer(layerInfo);
                layer.addDoneLoadingListener(()->{
                    if(layer.getLoadStatus()== LoadStatus.FAILED_TO_LOAD)
                    {
                        Toast.makeText(context, "图层" + new File(layerInfo.getPath()).getName() + "加载失败\n" + layer.getLoadError(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            catch (Exception ex)
            {
                Toast.makeText(context, "图层" + new File(layerInfo.getPath()).getName() + "加载失败\n" + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Basemap getBaseMap()
    {
        ArrayList<Layer> layers = new ArrayList<>();
        for (String url : Config.getInstance().baseUrls)
        {
            Layer layer = null;
            if (url.startsWith("http://") || url.startsWith("https://"))
            {
                layer = getWebTiledLayer(url);
            }
            else if (url.endsWith(".tpk"))
            {
                layer = getTileCacheLayer(FileHelper.getBaseLayerPath(url));
            }
            else if (url.endsWith(".jpg")
                    || url.endsWith("jpeg")
                    || url.endsWith("tif")
                    || url.endsWith("tiff")
                    || url.endsWith("png")
            )
            {

                layer = getRasterLayer(FileHelper.getBaseLayerPath(url));
            }
            layers.add(layer);
        }
        return new Basemap(layers, null);

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
            initialize(context);
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

    private WebTiledLayer getWebTiledLayer(String url)
    {
        url = url.replace("{x}", "{col}").replace("{y}", "{row}").replace("{z}", "{level}");
        WebTiledLayer layer = new WebTiledLayer(url);
        RequestConfiguration requestConfiguration = new RequestConfiguration();
        requestConfiguration.getHeaders().put("referer", "http://www.arcgis.com");
        layer.setRequestConfiguration(requestConfiguration);
        return layer;
    }

    private ArcGISTiledLayer getTileCacheLayer(String path)
    {
        TileCache tile = new TileCache(path);
        tile.loadAsync();
        return new ArcGISTiledLayer(tile);

    }

    /**
     * 加载图片格式栅格图层
     *
     * @param path
     */
    private RasterLayer getRasterLayer(String path)
    {
        Raster raster = new Raster(path);
        final RasterLayer rasterLayer = new RasterLayer(raster);
        rasterLayer.loadAsync();
        return rasterLayer;

    }


    /**
     * 加入图层
     *
     * @param path
     */
    public Layer addLayer(String path)
    {
        ShapefileFeatureTable table = new ShapefileFeatureTable(path);

        table.loadAsync();
        FeatureLayer layer = new FeatureLayer(table);
        layer.setSelectionColor(Color.YELLOW);
        layer.setSelectionWidth(10);
        setFeatureLayerStyle(layer, new File(path).getName());
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
        else
        {
            setFeatureLayerStyle(layer, path);
        }

        getLayers().add(layer);
        return layer;
    }

    public Layer addLayer(Context context, Uri uri)
    {
        String path = Environment.getExternalStorageDirectory() + "/" + uri.getPath().split(":")[1];
        return addLayer(path);
    }

    public Layer addLayer(LayerInfo layerInfo)
    {
        Layer layer = addLayer(layerInfo.getPath());
        layer.setOpacity(layerInfo.getOpacity());
        layer.setVisible(layerInfo.isVisible());
        return layer;
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
                                    path = FileHelper.createShapefile(type, FileHelper.getShapefilePath(editText.getText().toString(), false));
                                }
                                catch (Exception ex)
                                {
                                    Toast.makeText(context, "创建Shapefile失败：" + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                if (path != null)
                                {
                                    addLayer(path);
                                }
                            })
                            .create().show();

                }).create().show();


    }

    static void setFeatureLayerStyle(FeatureLayer layer, String name)
    {
        if (new File(FileHelper.getStyleFile(name)).exists())
        {
            try
            {
                String jsonStr = FileHelper.readTextFile(FileHelper.getStyleFile(name));

                JSONObject json = new JSONObject(jsonStr);


                JSONObject jBasic = json.getJSONObject("Basic");
                if (jBasic.has("MinScale"))
                {
                    double value = jBasic.getDouble("MinScale");
                    layer.setMinScale(value);
                }

                JSONObject jRenderer = json.getJSONObject("Renderer");
                UniqueValueRenderer renderer = (UniqueValueRenderer) Renderer.fromJson(jRenderer.toString());
                layer.setRenderer(renderer);

                JSONArray jLabels = json.getJSONArray("Labels");
                for (int i = 0; i < jLabels.length(); i++)
                {
                    LabelDefinition label = LabelDefinition.fromJson(jLabels.getJSONObject(i).toString());
                    layer.getLabelDefinitions().add(label);
                }
                layer.setLabelsEnabled(true);
            }
            catch (Exception ex)
            {
                Log.e(TAG, ex.getMessage());
            }
        }
    }

}

