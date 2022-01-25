package com.autod.gis.ui.fragment;


import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Polyline;
import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.autod.gis.ui.activity.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * 单要素属性表Fragment
 * A simple {@link Fragment} subclass.
 */
public class FeatureAttributionTableFragment extends Fragment implements View.OnClickListener
{


    public FeatureAttributionTableFragment()
    {
    }

    private Button btnEdit;

    private Button btnCloseOrReset;

    private static FeatureAttributionTableFragment instance;

    public static FeatureAttributionTableFragment getInstance()
    {
        return instance;
    }

    private View control;

    public View getControl()
    {
        return control;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        instance = this;

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_attri_table, container, false);
    }

    /**
     * 初始化面板
     */
    public void Initialize()
    {
        control = MainActivity.getInstance().findViewById(R.id.main_fgm_attri);
        btnCloseOrReset = MainActivity.getInstance().findViewById(R.id.attri_table_btn_close);
        btnCloseOrReset.setOnClickListener(this);
        btnEdit = MainActivity.getInstance().findViewById(R.id.attri_table_btn_edit);
        btnEdit.setOnClickListener(this);
        control.post(() -> control.setTranslationY(-control.getHeight()));

    }

    private FeatureTable featureTable;
    private Feature feature;

    /**
     * 加载表格
     *
     * @param featureTable
     * @param feature
     */
    public void loadTable(FeatureTable featureTable, Feature feature)
    {
        this.featureTable = featureTable;
        TextView tvwFeatureArea = MainActivity.getInstance().findViewById(R.id.attri_table_tvw_area);
        // tvwFeatureArea = instance.findViewById(R.id.tvwArea);
        Geometry geometry = feature.getGeometry();
        if (geometry.getGeometryType() == GeometryType.POLYGON)
        {
            tvwFeatureArea.setText(getString(R.string.attri_table_area, GeometryEngine.areaGeodetic(geometry, null, GeodeticCurveType.NORMAL_SECTION), GeometryEngine.lengthGeodetic(geometry, null, GeodeticCurveType.NORMAL_SECTION)));
//            tvwFeatureArea.setText("面积：" + NumberFormat.getInstance().format(GeometryEngine.areaGeodetic((Polygon) geometry, null, GeodeticCurveType.NORMAL_SECTION)) + "㎡\n周长："
//                    + NumberFormat.getInstance().format((GeometryEngine.lengthGeodetic(geometry, null, GeodeticCurveType.NORMAL_SECTION))) + "m");

        }
        else if (featureTable.getGeometryType() == GeometryType.POLYLINE)
        {
            Polyline line = (Polyline) geometry;
            tvwFeatureArea.setText(getString(R.string.attri_table_length, GeometryEngine.lengthGeodetic(line, null, GeodeticCurveType.NORMAL_SECTION)));
//            tvwFeatureArea.setText(NumberFormat.getInstance().format(GeometryEngine.lengthGeodetic(line, null, GeodeticCurveType.NORMAL_SECTION)) + "m");
        }
        btnEttMap.clear();
        fieldEttMap.clear();
        // get the point that was clicked and convert it to a point in map coordinates

        // add done loading listener to fire when the selection returns
        this.feature = feature;
        showTable();
    }

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 读取属性并显示
     */
    public void showTable()
    {

        if (feature == null)
        {
            return;
        }
        try
        {
//            FeatureQueryResult result = future.get();
//            Iterator<Feature> iterator = result.iterator();
            TableLayout tbl = MainActivity.getInstance().findViewById(R.id.attri_table_tbl_attributions);
            tbl.removeAllViews();
            //Feature feature==null;
            //boolean first = true;

            Map<String, Object> attr = feature.getAttributes();
            Set<String> keys = attr.keySet();
            for (String key : keys)
            {

                final Object value = attr.get(key);

                //属性表表格布局
                TableRow row = new TableRow(MainActivity.getInstance());
                row.setGravity(Gravity.CENTER_VERTICAL);
                TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                row.setLayoutParams(lp);
                final float scale = getContext().getResources().getDisplayMetrics().density;
                //字段名称按钮
                Button btnName = new Button(MainActivity.getInstance());
                btnName.setWidth((int) (96 * scale));
                btnName.setText(key);

                //属性值编辑框
                EditText ettValue = new EditText(MainActivity.getInstance());
                btnEttMap.put(btnName, ettValue);
                //在还未开始编辑时，不允许编辑
                setEditTextEditable(ettValue, false);
                if (value instanceof Calendar)
                {
                    ettValue.setText(dateFormat.format(((Calendar) value).getTime()));
                }
                else
                {
                    ettValue.setText(String.valueOf(value));
                }
                ettValue.setWidth((int) (220 * scale));

                ///字段类型文本框
                TextView tvwType = new TextView(MainActivity.getInstance());
                try
                {
                    //List<Field> fs=  featureTable.getFields();

                    Field field = featureTable.getField(key);
                    Field.Type type = field.getFieldType();

                    //根据类型来限制编辑框的输入
                    switch (type)
                    {
                        case DOUBLE:
                            ettValue.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            break;
                        case INTEGER:
                            ettValue.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
                            break;
                    }

                    fieldEttMap.put(field, ettValue);
                    btnName.setOnClickListener(v -> showDomain(field, value));
                    //tvwType.setText(type.toString());

                    ettValue.setTag(type);
                }
                catch (Exception ex)
                {
                    tvwType.setText("出错");
                }
                //用于
                //btnName.setTag(index);

                row.addView(btnName);
                row.addView(ettValue);
                //row.addView(tvwType);
                tbl.addView(row);
            }
            // index++;
            // center the mapview on selected feature
            //Envelope envelope = feature.getGeometry().getExtent();
            //mapView.setViewpointGeometryAsync(envelope, 200);
            // show CallOut

            //}
        }
        catch (Exception e)
        {
            Toast.makeText(MainActivity.getInstance(), "加载要素属性错误", Toast.LENGTH_SHORT).show();
        }
        finally
        {
        }
    }

    /**
     * 根据按钮获得编辑框
     */
    private Map<Button, EditText> btnEttMap = new HashMap<>();
    /**
     * 根据字段获得编辑框
     */
    private Map<Field, EditText> fieldEttMap = new HashMap<>();

    /**
     * 保存属性
     */
    private void saveAttributes()
    {
        TableLayout tbl = MainActivity.getInstance().findViewById(R.id.attri_table_tbl_attributions);
        Set<Button> existButtons = btnEttMap.keySet();

        Map<String, Object> attr = feature.getAttributes();
        Set<String> keys = attr.keySet();
        for (String key : keys)
        {
            Button btnName = null;
            for (Button btnKey : existButtons)
            {
                if (btnKey.getText().toString().equals(key))
                {
                    //寻找按钮
                    btnName = btnKey;
                    break;
                }
            }
            if (btnName == null)
            {
                return;
            }
            EditText ettValue = btnEttMap.get(btnName);
            String value = ettValue.getText().toString();
            Field.Type type = (Field.Type) ettValue.getTag();
            //保存数据
            if (type == Field.Type.TEXT)
            {
                if (!value.equals(attr.get(key)))
                {
                    attr.put(key, value);
                }
            }
            else if (type == Field.Type.DOUBLE)
            {
                double dblValue = Double.valueOf(value);
                if (!attr.get(key).equals(dblValue))
                {
                    attr.put(key, dblValue);
                }
            }
            else if (type == Field.Type.INTEGER)
            {
                int intValue = Integer.valueOf(value);
                if (!attr.get(key).equals(intValue))
                {
                    attr.put(key, intValue);
                }
            }
        }
        //更新数据
        featureTable.updateFeatureAsync(feature);
        // index++;
        // }
    }

    //收缩和展开属性表
    public void foldOrUnfold()
    {
        if (control.getTranslationY() != 0)//打开
        {
            ObjectAnimator.ofFloat(control, "translationY", -control.getHeight(), 0).setDuration(Config.getInstance().animationDuration).start();
        }
        else//关闭
        {
            ObjectAnimator.ofFloat(control, "translationY", 0, -control.getHeight()).setDuration(Config.getInstance().animationDuration).start();
        }
    }

    /**
     * 是否正在编辑属性
     */
    private boolean isEditing = false;

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.attri_table_btn_close:
                /**
                 * 如果按钮时“关闭”，说明没在编辑，则收缩窗口；否则说明正在编辑，要重置属性
                 */
                if (btnCloseOrReset.getText().equals("关闭"))
                {
                    foldOrUnfold();
                }
                else
                {
                    showTable();
                }
                break;
            //单击编辑按钮
            case R.id.attri_table_btn_edit:
//                if(featureTable==null)
//                {
//                    Toast.makeText(MainActivity.getInstance(), "当前图层不可编辑", Toast.LENGTH_SHORT).show();
//                    return;
//                }
                if (btnEdit.getText().equals("编辑"))
                {
                    for (Map.Entry<Button, EditText> edit : btnEttMap.entrySet())
                    {
                        setEditTextEditable(edit.getValue(), true);
                    }
                    btnEdit.setText("正在编辑");
                    btnEdit.getPaint().setFakeBoldText(true);
                    btnCloseOrReset.setText("重置");
                    isEditing = true;
                }
                else
                {
                    try
                    {

                        saveAttributes();
                        Toast.makeText(MainActivity.getInstance(), "保存成功", Toast.LENGTH_SHORT).show();
                    }
                    catch (Exception ex)
                    {
                        Toast.makeText(MainActivity.getInstance(), "保存失败", Toast.LENGTH_SHORT).show();
                    }
                    finally
                    {

                        btnEdit.setText("编辑");
                        btnEdit.getPaint().setFakeBoldText(false);
                        btnCloseOrReset.setText("关闭");
                        isEditing = false;
                    }
                }
        }

    }

    /**
     * 单击字段按钮显示属性域（简单）
     *
     * @param field
     * @param value
     */
    public void showDomain(Field field, Object value)
    {

        //Toast.makeText(MainActivity.getInstance(), String.valueOf(field.getDomain()==null), Toast.LENGTH_SHORT).show();
        //判断是否正在编辑
        if (!isEditing)
        {
            Toast.makeText(MainActivity.getInstance(), "只可在编辑时使用", Toast.LENGTH_SHORT).show();
            return;
        }
        //attributeDominFragment.Show(type)

        Object[] objValues = getAllValues(field);
        if (objValues.length == 0)
        {
            Toast.makeText(MainActivity.getInstance(), "没有集合", Toast.LENGTH_SHORT).show();
            return;
        }
        //如果是文本，则使用选取的方式，把所有值放入列表对话框
        if (field.getFieldType() == Field.Type.TEXT)
        {
            String[] valueArray = Arrays.copyOf(objValues, objValues.length, String[].class);
            AlertDialog.Builder listDialog = new AlertDialog.Builder(MainActivity.getInstance());
            listDialog.setTitle("选择数据（共" + valueArray.length + "个）");
            listDialog.setItems(valueArray, (DialogInterface dialog, int which) ->
                    FeatureAttributionTableFragment.this.fieldEttMap.get(field).setText(valueArray[which]));
            listDialog.show();
        }
        //如果是整数，则显示NumberPicker进行选取，并且限制最大最小值
        else if (field.getFieldType() == Field.Type.INTEGER)
        {
            Integer[] valueArray = Arrays.copyOf(objValues, objValues.length, Integer[].class);
            Integer max = Integer.MIN_VALUE;
            Integer min = Integer.MAX_VALUE;
            for (Integer i : valueArray)
            {
                if (i > max)
                {
                    max = i;
                }
                if (i < min)
                {
                    min = i;
                }

            }
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.getInstance());
            dialog.setTitle("设置数据");
            NumberPicker picker = new NumberPicker(MainActivity.getInstance());
            picker.setMinValue(min);
            picker.setMaxValue(max);
            picker.setValue((int) value);
            dialog.setView(picker);
            dialog.setPositiveButton("确定", (dialogInterface, i) ->
                    FeatureAttributionTableFragment.this.fieldEttMap.get(field).setText(String.valueOf(picker.getValue())));

            dialog.show();
        }
        //如果是浮点型，只能提示一下了
        else if (field.getFieldType() == Field.Type.DOUBLE)
        {
            Double[] valueArray = Arrays.copyOf(objValues, objValues.length, Double[].class);
            Double max = -Double.MAX_VALUE;
            Double min = Double.MAX_VALUE;
            for (Double i : valueArray)
            {
                if (i > max)
                {
                    max = i;
                }
                if (i < min)
                {
                    min = i;
                }

            }
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.getInstance());
            dialog.setTitle("设置数据");
            LinearLayout layout = new LinearLayout(MainActivity.getInstance());
            layout.setOrientation(LinearLayout.VERTICAL);
            TextView tvw = new TextView(MainActivity.getInstance());
            tvw.setText(min + "~" + max);
            tvw.setTextSize(24);
            tvw.setGravity(Gravity.CENTER_HORIZONTAL);
            layout.addView(tvw);
            EditText ett = new EditText(MainActivity.getInstance());
            ett.setText(String.valueOf(value));
            ett.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
            ett.selectAll();
            layout.addView(ett);
            dialog.setView(layout);
            dialog.setPositiveButton("确定", (dialogInterface, i) ->
                    FeatureAttributionTableFragment.this.fieldEttMap.get(field).setText(ett.getText().toString()));

            dialog.show();
        }
        else
        {
            Toast.makeText(MainActivity.getInstance(), "没有适合的属性域", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取某个字段在该属性表上的所有不重复的值
     *
     * @param field
     * @return
     */
    private Object[] getAllValues(Field field)
    {
        try
        {
            Set<Object> valueSet = new HashSet<>();
            QueryParameters query = new QueryParameters();
            query.setGeometry(featureTable.getExtent());
            ListenableFuture<FeatureQueryResult> result = featureTable.queryFeaturesAsync(query);
            for (Feature currentFeature : result.get())
            {
                valueSet.add(currentFeature.getAttributes().get(field.getName()));
            }

            Object[] valueArray = new Object[valueSet.size()];
            valueSet.toArray(valueArray);
            return valueArray;
        }

        catch (Exception ex)
        {
            return new Object[0];
        }
    }

    private static void setEditTextEditable(EditText e, boolean b)
    {
        e.setCursorVisible(b);
        e.setFocusable(b);
        e.setFocusableInTouchMode(b);
    }
}
