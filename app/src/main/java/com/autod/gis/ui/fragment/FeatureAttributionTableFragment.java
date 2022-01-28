package com.autod.gis.ui.fragment;


import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
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

import com.autod.gis.ui.activity.MainActivity;
import com.esri.arcgisruntime.UnitSystem;
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
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.Polyline;
import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.esri.arcgisruntime.geometry.Unit;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * 单要素属性表Fragment
 * A simple {@link Fragment} subclass.
 */
public class FeatureAttributionTableFragment extends Fragment
{


    public FeatureAttributionTableFragment()
    {
    }

    private Button btnEdit;

    private Button btnCloseOrReset;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_attri_table, container, false);
    }

    /**
     * 初始化面板
     */
    public void Initialize(Activity activity)
    {
        btnCloseOrReset = activity.findViewById(R.id.attri_table_btn_close);
        btnCloseOrReset.setOnClickListener(v -> onButtonClick(activity, v));
        btnEdit = activity.findViewById(R.id.attri_table_btn_edit);
        btnEdit.setOnClickListener(v -> onButtonClick(activity, v));

    }

    private FeatureTable featureTable;
    private Feature feature;

    /**
     * 加载表格
     *
     * @param featureTable
     * @param feature
     */
    public void loadTable(Activity activity, FeatureTable featureTable, Feature feature)
    {
        this.featureTable = featureTable;
        TextView tvwFeatureArea = activity.findViewById(R.id.attri_table_tvw_area);
        Geometry geometry = feature.getGeometry();
        if (geometry.getGeometryType() == GeometryType.POLYGON)
        {
            tvwFeatureArea.setText(Html.fromHtml(getString(R.string.attri_table_area,
                    GeometryEngine.areaGeodetic(geometry, null, GeodeticCurveType.NORMAL_SECTION),
                    GeometryEngine.lengthGeodetic(geometry, null, GeodeticCurveType.NORMAL_SECTION))));
        }
        else if (featureTable.getGeometryType() == GeometryType.POLYLINE)
        {
            Polyline line = (Polyline) geometry;
            tvwFeatureArea.setText(getString(R.string.attri_table_length, GeometryEngine.lengthGeodetic(line, null, GeodeticCurveType.NORMAL_SECTION)));
        }
        keyValueMap.clear();
        fieldEttMap.clear();
        this.feature = feature;
        showTable(activity);
    }

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 读取属性并显示
     */
    public void showTable(Activity activity)
    {

        if (feature == null)
        {
            return;
        }
        try
        {
            TableLayout tbl = activity.findViewById(R.id.attri_table_tbl_attributions);
            tbl.removeAllViews();

            Map<String, Object> attr = feature.getAttributes();
            Set<String> keys = attr.keySet();
            for (String key : keys)
            {

                final Object value = attr.get(key);

                //属性表表格布局
                TableRow row = new TableRow(activity);
                row.setGravity(Gravity.CENTER_VERTICAL);
                TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
                row.setLayoutParams(lp);
                final float scale = getContext().getResources().getDisplayMetrics().density;
                //字段名称按钮
                TextView tvwKey = new TextView(activity);
                tvwKey.setTypeface(null, Typeface.BOLD);
                tvwKey.setWidth((int) (96 * scale));
                tvwKey.setPadding(12,0,0,0);
                tvwKey.setText(key);

                //属性值编辑框
                EditText ettValue = new EditText(activity);
                keyValueMap.put(tvwKey, ettValue);
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
                TextView tvwType = new TextView(activity);
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

                    ettValue.setTag(type);
                }
                catch (Exception ex)
                {
                    tvwType.setText("出错");
                }
                row.addView(tvwKey);
                row.addView(ettValue);
                //row.addView(tvwType);
                tbl.addView(row);
            }
        }
        catch (Exception e)
        {
            Toast.makeText(activity, "加载要素属性错误", Toast.LENGTH_SHORT).show();
        }
        finally
        {
        }
    }

    /**
     * 根据按钮获得编辑框
     */
    private Map<TextView, EditText> keyValueMap = new HashMap<>();
    /**
     * 根据字段获得编辑框
     */
    private Map<Field, EditText> fieldEttMap = new HashMap<>();

    /**
     * 保存属性
     */
    private void saveAttributes(Activity activity)
    {
        TableLayout tbl = activity.findViewById(R.id.attri_table_tbl_attributions);
        Set<TextView> existButtons = keyValueMap.keySet();

        Map<String, Object> attr = feature.getAttributes();
        Set<String> keys = attr.keySet();
        for (String key : keys)
        {
            TextView tvw = null;
            for (TextView tvwKey : existButtons)
            {
                if (tvwKey.getText().toString().equals(key))
                {
                    //寻找按钮
                    tvw = tvwKey;
                    break;
                }
            }
            if (tvw == null)
            {
                return;
            }
            EditText ettValue = keyValueMap.get(tvw);
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



    /**
     * 是否正在编辑属性
     */
    private boolean isEditing = false;

    public void onButtonClick(Activity activity, View view)
    {
        switch (view.getId())
        {
            case R.id.attri_table_btn_close:
                /**
                 * 如果按钮时“关闭”，说明没在编辑，则收缩窗口；否则说明正在编辑，要重置属性
                 */
                if (btnCloseOrReset.getText().equals("关闭"))
                {
                    ObjectAnimator.ofFloat(getView(), "translationX", 0).setDuration(Config.getInstance().animationDuration).start();
                }
                else
                {
                    showTable(activity);
                }
                break;
            //单击编辑按钮
            case R.id.attri_table_btn_edit:
//                if(featureTable==null)
//                {
//                    Toast.makeText(context, "当前图层不可编辑", Toast.LENGTH_SHORT).show();
//                    return;
//                }
                if (btnEdit.getText().equals("编辑"))
                {
                    for (Map.Entry<TextView, EditText> edit : keyValueMap.entrySet())
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

                        saveAttributes(activity);
                        Toast.makeText(activity, "保存成功", Toast.LENGTH_SHORT).show();
                    }
                    catch (Exception ex)
                    {
                        Toast.makeText(activity, "保存失败", Toast.LENGTH_SHORT).show();
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

    private static void setEditTextEditable(EditText e, boolean b)
    {
        e.setCursorVisible(b);
        e.setFocusable(b);
        e.setFocusableInTouchMode(b);
    }
}
