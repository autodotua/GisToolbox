package com.autod.gis.ui.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.autod.gis.R;
import com.autod.gis.layer.LayerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * 属性列表
 */
public class BooleanStatisticsActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener
{

    public static BooleanStatisticsActivity layerListActivity;
    private ArrayList<String> layerNames = new ArrayList<>();
    private ArrayList<FeatureLayer> layers = new ArrayList<>();
    private ProgressBar pgb;
    private TextView tvw;

    //   private HashMap<String,Layer> layerNameToLayer=new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        layerListActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boolean_statistics);
        setTitle("图层管理");


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

        pgb = findViewById(R.id.boolean_pgb);

        tvw = findViewById(R.id.boolean_tvw_result);
        addItem();
        addItem();
        Button btnCalc = findViewById(R.id.boolean_btn_calc);
        btnCalc.setOnClickListener(this);
    }

    //类似HashMap<Integer,BooleanStatisticItem>
    private SparseArray<BooleanStatisticItem> items = new SparseArray<>();

    private void addItem()
    {
        //新增一项
        LinearLayout itemsLayout = findViewById(R.id.regional_llt_items);
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout itemLayout = (LinearLayout) inflater.inflate(R.layout.item_boolean_statistics, null, false);

        //如果是横屏，修改为单行布局
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT)
        {
            LinearLayout leftLayout = itemLayout.findViewById(R.id.boolean_llt_layer);
            LinearLayout rightLayout = itemLayout.findViewById(R.id.boolean_llt_attribute);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

            leftLayout.setLayoutParams(p);
            p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f);
            rightLayout.setLayoutParams(p);

            ((LinearLayout) itemLayout.getChildAt(0)).setOrientation(LinearLayout.HORIZONTAL);
        }
        itemsLayout.addView(itemLayout);


        //设置图层下拉菜单的值
        Spinner spnLayer = itemLayout.findViewById(R.id.boolean_spn_layer);

        spnLayer.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, layerNames));
        spnLayer.setOnItemSelectedListener(this);

        Spinner spnAttributeName = itemLayout.findViewById(R.id.boolean_spn_attribute_name);
        spnLayer.setTag(spnAttributeName);
//        spnLayer.setAdapter(new ArrayAdapter<>(this, R.layout.item_text, layerNames));
        spnAttributeName.setOnItemSelectedListener(this);
//
//        EditText edt=itemLayout.findViewById(R.id.boolean_edt_attribute_value);
//        edt.addTextChangedListener(new onte);

//布尔统计信息
        BooleanStatisticItem item = new BooleanStatisticItem();
        item.viewId = View.generateViewId();
        item.layer = layers.get(0);
        items.append(item.viewId, item);
        itemLayout.setId(item.viewId);
//        btnLayer.setOnClickListener(this);
    }


    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    public void onClick(View view)
    {
//        Toast.makeText(this, String.valueOf(((LinearLayout) view.getParent().getParent()).getId()), Toast.LENGTH_SHORT).show();
        switch (view.getId())
        {
            case R.id.boolean_btn_calc:

                pgb.setVisibility(View.VISIBLE);
                for (int i = 0; i < items.size(); i++)
                {
                    BooleanStatisticItem item = items.valueAt(i);

                    EditText edt = findViewById(item.viewId).findViewById(R.id.boolean_edt_attribute_value);
                    item.attributeValue = edt.getText().toString();

                    FeatureTable table = item.layer.getFeatureTable();
                    QueryParameters para = new QueryParameters();
                    if (item.attributeValue != null && !item.attributeValue.trim().equals(""))
                    {
                        para.setWhereClause(String.format("%s = '%s'", item.fieldName, item.attributeValue));
                    }
                    ListenableFuture<FeatureQueryResult> result = table.queryFeaturesAsync(para);
                    result.addDoneListener(() -> {
                        try
                        {
                            List<Geometry> tempGeometries = new ArrayList<>();
                            for (Feature feature : result.get())
                            {
                                tempGeometries.add(feature.getGeometry());
                            }
                            preparingGeometries.add(GeometryEngine.union(tempGeometries));
                            if ((++completedCount) == items.size())
                            {
                                calc();
                            }
                        }
                        catch (Exception ex)
                        {

                        }
                    });

                }


                break;
        }
    }

    private List<Geometry> preparingGeometries = new ArrayList<>();
    private int completedCount = 0;

    private void calc()
    {
        pgb.setVisibility(View.GONE);
        Spinner spnType = findViewById(R.id.boolean_spn_type);
        Geometry tempGeometry = null;
        if (spnType.getSelectedItemPosition() == 0)
        {
            for (Geometry geometry : preparingGeometries)
            {
                if (tempGeometry == null)
                {
                    tempGeometry = geometry;
                    continue;
                }

                tempGeometry = GeometryEngine.intersection(tempGeometry, geometry);
            }

            double area = GeometryEngine.areaGeodetic(tempGeometry, null, GeodeticCurveType.GEODESIC);
            tvw.setText(String.format(Locale.getDefault(),"面积：%.2f㎡",area));
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        switch (parent.getId())
        {
            case R.id.boolean_spn_layer:
            {
                FeatureLayer layer = layers.get(position);
                Spinner spnAttributeName = (Spinner) parent.getTag();
                ArrayList<String> fields = new ArrayList<>();
                for (Field field : layer.getFeatureTable().getFields())
                {
                    fields.add(field.getName());
                }
                spnAttributeName.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item, fields));

//三个getParent()，分别获取图层llt、内部产生阴影的llt和外部llt
                BooleanStatisticItem item = items.get(((View) (parent.getParent().getParent().getParent())).getId());
                item.layer = layer;
//                item.fieldName = fields.get(0);
            }
            break;
            case R.id.boolean_spn_attribute_name:
            {
                BooleanStatisticItem item = items.get(((View) (parent.getParent().getParent().getParent())).getId());

                item.fieldName = ((ArrayAdapter<String>) parent.getAdapter()).getItem(position);
            }
            break;
        }


    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {

    }

    class BooleanStatisticItem
    {
        public FeatureLayer layer;
        public String fieldName;
        public String attributeValue;
        public int viewId;
    }

}
