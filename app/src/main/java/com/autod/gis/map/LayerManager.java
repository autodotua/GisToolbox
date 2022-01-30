package com.autod.gis.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.autod.gis.R;
import com.autod.gis.model.LayerInfo;
import com.esri.arcgisruntime.arcgisservices.LabelDefinition;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.Geometry;
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
import com.esri.arcgisruntime.mapping.BasemapStyle;
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
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class LayerManager
{
    public final static String[] EsriBaseLayers = new String[]{"无", "OpenStreetMap", "卫星影像", "带标签卫星影像", "街道", "亮色", "暗色", "街道", "地貌", "地形", "海洋"};
    private static final String TAG = "Layer";
    private static LayerManager instance = new LayerManager();
    private FeatureLayer currentLayer = null;
    private ArcGISMap map = new ArcGISMap();
    private ArrayList<Callable<Basemap>> EsriBaseLayersMap = new ArrayList<Callable<Basemap>>()
    {{
        add(null);
        add(Basemap::createOpenStreetMap);
        add(Basemap::createImagery);
        add(Basemap::createImageryWithLabelsVector);
        add(Basemap::createStreetsVector);
        add(Basemap::createLightGrayCanvasVector);
        add(Basemap::createDarkGrayCanvasVector);
        add(Basemap::createTopographicVector);
        add(Basemap::createTerrainWithLabelsVector);
        add(Basemap::createOceans);
    }};

    public static LayerManager getInstance()
    {
        return instance;
    }

    static void setFeatureLayerStyle(FeatureLayer layer, String name)
    {
        if (new File(FileHelper.getStyleFile(name)).exists())
        {
            try
            {
                String jsonStr = FileHelper.readTextFile(FileHelper.getStyleFile(name));
                JSONObject json = new JSONObject(jsonStr);

                JSONObject jRenderer = json.getJSONObject("Renderer");
                UniqueValueRenderer renderer = (UniqueValueRenderer) Renderer.fromJson(jRenderer.toString());
                layer.setRenderer(renderer);
                JSONArray jLabels =json.has("Labels")? json.getJSONArray("Labels"): json.getJSONArray("LabelDefinitions");
                for (int i = 0; i < jLabels.length(); i++)
                {
                    LabelDefinition label = LabelDefinition.fromJson(jLabels.getJSONObject(i).toString());
                    layer.getLabelDefinitions().add(label);
                }
                layer.setLabelsEnabled(true);


                JSONObject jDisplay = json.getJSONObject("Display");
                if (jDisplay.has("MinScale"))
                {
                    double value = jDisplay.getDouble("MinScale");
                    layer.setMinScale(value);
                }
                if (jDisplay.has("MaxScale"))
                {
                    double value = jDisplay.getDouble("MaxScale");
                    layer.setMinScale(value);
                }
                if (jDisplay.has("Opacity"))
                {
                    double value = jDisplay.getDouble("Opacity");
                    layer.setOpacity((float) value);
                }
            }
            catch (Exception ex)
            {
                Log.e(TAG, ex.getMessage());
            }
        }
    }

    public FeatureLayer getCurrentLayer()
    {
        return currentLayer;
    }

    public void setCurrentLayer(FeatureLayer currentLayer)
    {
        this.currentLayer = currentLayer;
    }

    public ArcGISMap getMap()
    {
        return map;
    }

    public void initialize(Activity context)
    {
        try
        {
            Toast toast = Toast.makeText(context, "正在加载地图...", Toast.LENGTH_SHORT);
            toast.show();
            Basemap basemap = getBaseMap(context);
            basemap.addDoneLoadingListener(() -> {
                LayerManager.getInstance().map = new ArcGISMap(basemap);
                MapViewHelper.getInstance().linkMapAndMapView();
                if (!Config.getInstance().lastExtent.equals(""))
                {
                    try
                    {
                        MapViewHelper.getInstance().getMapView().setViewpointGeometryAsync(Geometry.fromJson(Config.getInstance().lastExtent));
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
                loadLayers(context);
                map.addLoadStatusChangedListener(s -> {
                    if (s.getNewLoadStatus() == LoadStatus.FAILED_TO_LOAD)
                    {
                        toast.setText("加载失败，请检查网络和设置");
                        toast.show();
                    }
                });
                if (TrackHelper.getInstance().getStatus() == TrackHelper.Status.Running)
                {
                    TrackHelper.getInstance().resumeOverlay();
                    Toast.makeText(context, "轨迹记录继续运行", Toast.LENGTH_SHORT).show();
                }
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
            addLayer(context, layerInfo);
        }
    }

    private Basemap getBaseMap(Context context)
    {
        Basemap basemap;
        if (Config.getInstance().esriBaseLayer > 0 && Config.getInstance().esriBaseLayer < EsriBaseLayersMap.size())
        {
            try
            {
                basemap = EsriBaseLayersMap.get(Config.getInstance().esriBaseLayer).call();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                basemap = new Basemap();
            }
        }
        else
        {
            basemap = new Basemap();
        }
        for (LayerInfo layerInfo : Config.getInstance().baseLayers)
        {
            String url = layerInfo.getPath();
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
            Layer finalLayer = layer;
            layer.addDoneLoadingListener(() -> {
                if (finalLayer.getLoadStatus() == LoadStatus.FAILED_TO_LOAD)
                {
                    Toast.makeText(context, "加载底图" + url + "失败\n" + finalLayer.getLoadError(), Toast.LENGTH_SHORT).show();
                }
            });
            layer.setOpacity(layerInfo.getOpacity());
            layerInfo.setVisible(layer.isVisible());
            basemap.getBaseLayers().add(layer);
        }
        return basemap;

    }

    /**
     * 重置图层
     */
    public void resetLayers(Activity context)
    {
        try
        {
            initialize(context);
        }
        catch (Exception ex)
        {
            Toast.makeText(context, "重置失败", Toast.LENGTH_SHORT).show();
        }
        if (MapViewHelper.getInstance().getMapView().getCallout() != null
                && MapViewHelper.getInstance().getMapView().getCallout().isShowing())
        {
            MapViewHelper.getInstance().getMapView().getCallout().dismiss();
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
    public Layer addLayer(Context context, String path)
    {
        try
        {
            if (!path.startsWith("/"))
            {
                path = FileHelper.getShapefilePath(path, false);
            }
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

            String finalPath = path;
            layer.addDoneLoadingListener(() -> {
                if (layer.getLoadStatus() == LoadStatus.FAILED_TO_LOAD)
                {
                    Toast.makeText(context, "图层" + finalPath + "加载失败\n" + layer.getLoadError(), Toast.LENGTH_SHORT).show();
                }
            });
            setCurrentLayer(layer);
            return layer;
        }
        catch (Exception ex)
        {
            Toast.makeText(context, "图层" + new File(path).getName() + "加载失败\n" + ex.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public Layer addLayer(Context context, Uri uri)
    {
        String path = Environment.getExternalStorageDirectory() + "/" + uri.getPath().split(":")[1];
        return addLayer(context, path);
    }

    public Layer addLayer(Context context, LayerInfo layerInfo)
    {
        Layer layer = addLayer(context, layerInfo.getPath());
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
                                    path = FileHelper.createShapefile(context, type, FileHelper.getShapefilePath(editText.getText().toString(), false));
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

