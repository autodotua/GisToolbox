package com.autod.gis.ui.activity;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.autod.gis.layer.LayerManager;
import com.autod.gis.ui.UIHelper;
import com.autod.gis.ui.part.SimpleTableView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * 属性列表
 */
public class RegionalStatisticActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener
{

    private ArrayList<String> layerNames = new ArrayList<>();
    private ArrayList<FeatureLayer> layers = new ArrayList<>();
    private Spinner spnAttribute;
    private ProgressBar pgb;
    private TextView tvw;
    private Spinner spnLayer1;
    private Spinner spnLayer2;
    private AlertDialog dialog;
    private ListenableFuture<?> currentTask = null;

    //   private HashMap<String,Layer> layerNameToLayer=new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regional_statistic);
        setTitle("区域统计");

        for (Layer layer : LayerManager.getInstance().getLayers())
        {
            if (layer instanceof FeatureLayer)
            {
                String path = LayerManager.getInstance().layerFilePath.get(layer);
                String name = path.substring(path.lastIndexOf("/") + 1);
                layerNames.add(name);
                layers.add((FeatureLayer) layer);
            }
        }

        pgb = findViewById(R.id.regional_pgb);
        spnAttribute = findViewById(R.id.regional_spn_attribute);
        tvw = findViewById(R.id.regional_tvw_result);

        spnLayer1 = findViewById(R.id.regional_spn_layer_1);
        spnLayer2 = findViewById(R.id.regional_spn_layer_2);

        spnLayer1.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, layerNames));
        spnLayer2.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, layerNames));

        for (int i = 0; i < layerNames.size(); i++)
        {
            if (Config.getInstance().lastRegionalStatisticLayerPath.equals(layerNames.get(i)))
            {
                spnLayer1.setSelection(i);
                break;
            }
        }
        spnLayer1.setOnItemSelectedListener(this);

        Button btnCalc = findViewById(R.id.regional_btn_calc);
        btnCalc.setOnClickListener(this);
    }


    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    public void onClick(View view)
    {
        Config.getInstance().lastRegionalStatisticLayerPath = (String) spnLayer1.getSelectedItem();
        Config.getInstance().lastRegionalStatisticAttributeName = (String) spnAttribute.getSelectedItem();
        Config.getInstance().trySave(this);

        dialog = showProgressDialog();
        new Thread(() -> {
            FeatureTable table1 = layers.get(spnLayer1.getSelectedItemPosition()).getFeatureTable();
            FeatureTable table2 = layers.get(spnLayer2.getSelectedItemPosition()).getFeatureTable();
            ListenableFuture<FeatureQueryResult> clipQuery = table2.queryFeaturesAsync(new QueryParameters());
            currentTask = clipQuery;
            clipQuery.addDoneListener(() -> {
                try
                {
                    List<Geometry> clipGeometries = new ArrayList<>();
                    for (Feature clipFeature : clipQuery.get())
                    {
                        clipGeometries.add(clipFeature.getGeometry());
                    }
//用于切割的图形，位于图层2
                    final Geometry clipGeometry = GeometryEngine.project(GeometryEngine.union(clipGeometries), table1.getSpatialReference());
//                clipGeometry = GeometryEngine.project(clipGeometry, table1.getSpatialReference());
                    QueryParameters clipParam = new QueryParameters();
                    clipParam.setGeometry(clipGeometry);
                    clipParam.setSpatialRelationship(QueryParameters.SpatialRelationship.INTERSECTS);

                    ListenableFuture<FeatureQueryResult> query = table1.queryFeaturesAsync(clipParam);
                    currentTask = query;
                    query.addDoneListener(() -> {
                        try
                        {
                            List<GeoElement> outElements = new ArrayList<>();
                            for (Feature feature : query.get())
                            {
                                if (GeometryEngine.within(feature.getGeometry(), clipGeometry))
                                {
                                    outElements.add(feature);
                                }
                                else
                                {
                                    Geometry clip = GeometryEngine.intersection(feature.getGeometry(), clipGeometry);
                                    Graphic geoElement = new Graphic(clip, feature.getAttributes());
                                    outElements.add(geoElement);
                                }
                            }
                            showTable(outElements);
                        }
                        catch (Exception ex)
                        {
                           UIHelper. showSimpleErrorDialog(this,ex.getMessage());
                        }
                        finally
                        {
                            currentTask = null;
                        }
                    });

                }
                catch (Exception ex)
                {

                    UIHelper. showSimpleErrorDialog(this,ex.getMessage());
                }
                finally
                {

                    currentTask = null;
//                    pgb.setVisibility(View.GONE);
                }
            });
        }).start();


    }

    public AlertDialog showProgressDialog()
    {

        int llPadding = 30;
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(llPadding, llPadding, llPadding, llPadding);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);

        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, llPadding, 0);
        progressBar.setLayoutParams(llParam);

        llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        TextView tvText = new TextView(this);
        tvText.setText("正在处理 ...");
        tvText.setTextColor(Color.parseColor("#000000"));
        tvText.setTextSize(20);
        tvText.setLayoutParams(llParam);

        ll.addView(progressBar);
        ll.addView(tvText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setPositiveButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    if (currentTask != null)
                    {
                        currentTask.cancel(true);
                    }
                })
                .setView(ll);


        AlertDialog dialog = builder.create();
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null)
        {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }
        return dialog;
    }

    private void showTable(List<GeoElement> geos)
    {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//    );
//                layout.setLayoutParams(params);
        layout.setGravity(Gravity.CENTER);

        ScrollView scrollView = new ScrollView(this);
//        LinearLayout.LayoutParams scrollViewParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,3f)
        layout.setPadding(16, 16, 16, 16);

        SimpleTableView table = new SimpleTableView(this, 3);
        table.AddRow(true, new Object[]{"属性值", "数量", "面积"});

//        TextView tvwTotalArea=new TextView(this);
//        tvwTotalArea.setPadding(0,8,0,0);
//        tvwTotalArea.setTextSize(20);
        scrollView.addView(table);
        layout.addView(scrollView);
//        layout.addView(tvwTotalArea);
//        table.setLayoutParams(params);

        HashMap<String, ResultInfo> elements = new HashMap<>();
        String attributeName = (String) spnAttribute.getSelectedItem();
        double totalArea=0;
        int count=0;
        for (GeoElement geo : geos)
        {
            String value = String.valueOf(geo.getAttributes().get(attributeName));
            double area = GeometryEngine.areaGeodetic(geo.getGeometry(), null, GeodeticCurveType.NORMAL_SECTION);
         totalArea+=area;
         count++;
            if (elements.containsKey(value))
            {
                elements.get(value).add(area);

            }
            else
            {
                ResultInfo result = new ResultInfo();
                result.add(area);
                elements.put(value, result);
            }
        }
        DecimalFormat df = new DecimalFormat("#,###.00");

//        tvwTotalArea.setText("共"+df.format(totalArea)+"㎡");
        for (String key : elements.keySet())
        {
            ResultInfo r = elements.get(key);
            table.AddRow(new String[]{key,
                    String.valueOf(r.count), df.format(r.area) + "㎡"});
        }
        table.AddRow(true,new String[]{"总计",
                String.valueOf(count), df.format(totalArea) + "㎡"});
        runOnUiThread(() -> {
            AlertDialog resultDialog = new AlertDialog.Builder(this)
                    .setPositiveButton("关闭", (dialog1, which) -> {

                    })
                    .setView(layout)
                    .setCancelable(false)
                    .create();
            resultDialog.show();
            dialog.dismiss();
        });

//        dialog.setView(layout);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        //图层被修改了
        FeatureLayer layer = layers.get(position);
        ArrayList<String> fields = new ArrayList<>();
        for (Field field : layer.getFeatureTable().getFields())
        {
            fields.add(field.getName());
        }
        spnAttribute.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fields));

        for (int i = 0; i < fields.size(); i++)
        {
            if (Config.getInstance().lastRegionalStatisticAttributeName.equals(fields.get(i)))
            {
                spnAttribute.setSelection(i);
                break;
            }
        }

    }


    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {

    }



    class ResultInfo
    {


        public void add(double area)
        {
            count++;
            this.area += area;
        }

        private int count = 0;
        private double area = 0;
    }
}
