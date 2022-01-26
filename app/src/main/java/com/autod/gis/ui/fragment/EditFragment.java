package com.autod.gis.ui.fragment;


import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.GeometryType;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.SketchCreationMode;
import com.esri.arcgisruntime.mapping.view.SketchEditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;


import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.autod.gis.data.Config;
import com.autod.gis.layer.LayerManager;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.R;
import com.autod.gis.ui.activity.MainActivity;

/**
 * 编辑功能的Fragment
 * 所有的编辑逻辑：先选取面/线（多选/单选），然后进行操作。
 */
public class EditFragment extends Fragment
{

    private Button btnClearSelection;
    private ToggleButton btnMutiSelect;
    /**
     * 是否正在分割操作。由于分割操作分两步：选取、画线，因此需要一个变量来确定当前是否处于画线阶段
     */
    public boolean isSpliting = false;
    public Feature editingFeature = null;
    /**
     * 所有编辑操作的按钮
     */
    private List<Button> editButtons = new ArrayList<>();

    private View control;

    public View getControl()
    {
        return control;
    }

    /**
     * 点草图符号
     */
    private SimpleMarkerSymbol pointSymbol;

    /**
     * 线草图符号
     */
    private SimpleLineSymbol lineSymbol;

    /**
     * 面草图符号
     */
    private SimpleFillSymbol fillSymbol;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_edit, container, false);
    }

    /**
     * 初始化Fragment
     */
    public void initialize(Activity activity)
    {
        control = activity.findViewById(R.id.main_fgm_edit);

        int[] editBtnIds = {
                R.id.edit_btn_delete,
                R.id.edit_btn_draw,
                R.id.edit_btn_union,
                R.id.edit_btn_split,
        };

        graphicsOverlay = new GraphicsOverlay();
        MapViewHelper.getInstance().mapView.getGraphicsOverlays().add(graphicsOverlay);
        sketchEditor = new SketchEditor();
        MapViewHelper.getInstance().mapView.setSketchEditor(sketchEditor);
        pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, 0xFFFF0000, 20);
        lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFF8800, 4);
        fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.CROSS, 0x40FFA9A9, lineSymbol);

        MapViewHelper.getInstance().setOnSelectionStatusChangedEventListener(this::setSelectStatus);

        /**
         * 将所有的按钮注册单击事件
         */
        for (int btnId : editBtnIds)
        {
            Button btn = activity.findViewById(btnId);
            editButtons.add(btn);
            btn.setOnClickListener(v -> setButtonsClickEvent(activity,v));
        }


        btnClearSelection = activity.findViewById(R.id.edit_btn_clear_selection);
        btnClearSelection.setOnClickListener(v -> MapViewHelper.getInstance().stopSelect());
        /**
         * 撤销按钮
         */
        ImageButton btnUndo = activity.findViewById(R.id.edit_btn_undo);
        btnUndo.setOnClickListener(v ->
        {
            if (sketchEditor.canUndo())
            {
                sketchEditor.undo();
            }
            else
            {
                Toast.makeText(activity, "无法撤销", Toast.LENGTH_SHORT).show();
            }
//            }

        });

        ImageButton btnRedo =activity.findViewById(R.id.edit_btn_redo);
        btnRedo.setOnClickListener(v ->
        {
            if (sketchEditor.canRedo())
            {
                sketchEditor.redo();
            }
            else
            {
                Toast.makeText(activity, "无法重做", Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * 多选单选切换按钮
         */
        btnMutiSelect = activity.findViewById(R.id.edit_tbtn_multi_select);
        btnMutiSelect.setOnCheckedChangeListener((compoundButton, b) ->
        {
            if (b)
            {
                btnMutiSelect.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_draw_muiti_select), null, null);
            }
            else
            {
                btnMutiSelect.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_draw_single_select), null, null);

            }
        });
    }

    /**
     * 是否多选
     *
     * @return
     */
    public boolean isMultiSelect()
    {

        return btnMutiSelect.isChecked();
    }

    /**
     * 设置是否在选择状态
     *
     * @param isSelecting
     */
    public void setSelectStatus(boolean isSelecting)
    {
        if (isSelecting)
        {
            btnMutiSelect.setVisibility(View.GONE);
            btnClearSelection.setVisibility(View.VISIBLE);
        }
        else
        {
            btnMutiSelect.setVisibility(View.VISIBLE);
            btnClearSelection.setVisibility(View.GONE);
        }
    }

    /**
     * 设置按钮的单击事件
     *
     * @param v
     */
    private void setButtonsClickEvent(Activity activity, View v)
    {
        resetButtons();

        switch (v.getId())
        {
            case R.id.edit_btn_draw:
                Button btn = (Button) v;
                //如果在正常状态下，则开始画图；否则结束画图
                if (btn.getText().toString().equals("画图"))
                {
                    if (startEditing(activity))
                    {
                        btn.setText("完成");
                    }
                }
                else
                {
                    stopEditing();
                    btn.setText("画图");
                }
                break;
            case R.id.edit_btn_split:
                //如果在正常状态下，则结束选取面，开始画线
                if (!isSpliting)
                {
                    //检查是否选取了1+个面
                    if (MapViewHelper.getInstance().getSelectedFeatures().size() == 0)
                    {
                        Toast.makeText(activity, "还未选择面", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    else
                    {
                        Toast.makeText(activity, "请画线", Toast.LENGTH_SHORT).show();
                        prepareSplit(activity);
                    }
                }
                else
                {
                    stopEditing();
                    isSpliting = false;
                    Button btnSplit = activity.findViewById(R.id.edit_btn_split);
                    btnSplit.setText("分割");
                }
                break;
            case R.id.edit_btn_union:
                //合并操作
                if (MapViewHelper.getInstance().getSelectedFeatures().size() == 0)
                {
                    Toast.makeText(activity, "还未选择面", Toast.LENGTH_SHORT).show();
                    resetButtons();
                    break;
                }
                else
                {
                    startUnion();
                }
                break;
            case R.id.edit_btn_delete:
                //删除操作
                if (MapViewHelper.getInstance().getSelectedFeatures().size() == 0)
                {
                    Toast.makeText(activity, "还未选择面", Toast.LENGTH_SHORT).show();
                    resetButtons();
                    break;
                }
                else
                {
                    startDelete();
                }
                break;
        }
    }

    /**
     * 合并操作
     */
    public void startUnion()
    {
        FeatureTable featureTable = getFeatureTable();

        //将选取的图形加入列表
        List<Geometry> geometries = new ArrayList<>();
        for (Feature feature : MapViewHelper.getInstance().getSelectedFeatures())
        {
            geometries.add(feature.getGeometry());
        }

        //合并
        Geometry resultGeometry = GeometryEngine.union(geometries);

        Feature resultFeature = featureTable.createFeature();
        resultFeature.setGeometry(resultGeometry);

        // 删除原有要素后添加新的要素
        featureTable.deleteFeaturesAsync(MapViewHelper.getInstance().getSelectedFeatures()).addDoneListener(() ->
        {
            addGeometryToFeatureTable(resultGeometry, MapViewHelper.getInstance().getSelectedFeatures().get(0).getAttributes());
        });
    }

    /**
     * 删除操作
     */
    public void startDelete()
    {

        FeatureTable featureTable = ((FeatureLayer) LayerManager.getInstance().currentLayer).getFeatureTable();
        featureTable.deleteFeaturesAsync(MapViewHelper.getInstance().getSelectedFeatures());

        operationComplete();


    }

    /**
     * 准备分割，开始画线
     */
    public void prepareSplit(Activity activity)
    {
        Button btnSplit = activity.findViewById(R.id.edit_btn_split);
        isSpliting = true;
        try
        {
            btnSplit.setText("完成");
            // isDrawing = true;
            sketchEditor.start(SketchCreationMode.POLYLINE);
        }
        catch (Exception ignored)
        {
            Toast.makeText(activity, "开始画线失败", Toast.LENGTH_SHORT).show();
            resetButtons();
        }


    }

    /**
     * 重置按钮状态
     */
    private void resetButtons()
    {
        for (Button editBtn : editButtons)
        {
            // editBtn.setBackground(instance.getDrawable(R.drawable.btn_background_normal));
            if (editBtn.getId() == R.id.edit_btn_split)
            {
                editBtn.setText("分割");
            }
            if (editBtn.getId() == R.id.main_btn_edit)
            {
                if (editBtn.getText() == "完成")
                {
                    stopEditing();
                    editBtn.setText("画图");
                }
            }
        }
    }

    /**
     * 开始绘制（点、线、面的草图）
     */
    private boolean startEditing(Context context)
    {
        FeatureTable featureTable = ((FeatureLayer) LayerManager.getInstance().currentLayer).getFeatureTable();

        if (MapViewHelper.getInstance().getSelectedFeatures().size() > 1)
        {
            Toast.makeText(context, "选择的要素超过一个，不可编辑", Toast.LENGTH_SHORT).show();
            return false;
        }

        else if (MapViewHelper.getInstance().getSelectedFeatures().size() == 1)
        {
            editingFeature = MapViewHelper.getInstance().getSelectedFeatures().get(0);
            Geometry geometry = editingFeature.getGeometry();
            switch (featureTable.getGeometryType())
            {
                case POINT:

                    sketchEditor.start(geometry, SketchCreationMode.POINT);
                    break;
                case MULTIPOINT:
                    sketchEditor.start(geometry, SketchCreationMode.MULTIPOINT);
                    break;
                case POLYLINE:
                    sketchEditor.start(geometry, SketchCreationMode.POLYLINE);
                    break;
                case POLYGON:
                    sketchEditor.start(geometry, SketchCreationMode.POLYGON);
                    break;
            }
        }
        else
        {
            //根据要素类的图形类型判断应该要画的图形类型
            switch (featureTable.getGeometryType())
            {
                case POINT:

                    sketchEditor.start(SketchCreationMode.POINT);
                    break;
                case MULTIPOINT:
                    sketchEditor.start(SketchCreationMode.MULTIPOINT);
//                showSelectDialog(new String[]{"点", "多点"},
//                        new SketchCreationMode[]{SketchCreationMode.POINT, SketchCreationMode.MULTIPOINT});
                    break;
                case POLYLINE:
                    sketchEditor.start(SketchCreationMode.POLYLINE);
//                showSelectDialog(new String[]{"多段线", "自由线"},
////                        new SketchCreationMode[]{SketchCreationMode.POLYLINE, SketchCreationMode.FREEHAND_LINE});
                    break;
                case POLYGON:
                    sketchEditor.start(SketchCreationMode.POLYGON);
//                showSelectDialog(new String[]{"多边形", "自由面"},
//                        new SketchCreationMode[]{SketchCreationMode.POLYGON, SketchCreationMode.FREEHAND_POLYGON});
                    break;
            }
        }
        return true;
    }
//
//    private void showSelectDialog(final String[] items, final SketchCreationMode[] types)
//    {
//        AlertDialog.Builder listDialog = new AlertDialog.Builder(instance);
//        listDialog.setTitle("绘制类型");
//        listDialog.setItems(items, (DialogInterface dialog, int which) -> sketchEditor.start(types[which]));
//        listDialog.show();
//    }


    private void stopEditing()
    {
        // isDrawing = false;
        //结束草图绘制
        if (!sketchEditor.isSketchValid())
        {
            sketchEditor.stop();
            return;
        }
        //resetButtons();
        // get the geometry from sketch editor
        Geometry sketchGeometry = sketchEditor.getGeometry();
        sketchEditor.stop();
        //如果绘制了东西，那么把绘制的东西加入到图形中
        if (sketchGeometry != null)
        {

            // create a graphic from the sketch editor geometry
            Graphic graphic = new Graphic(sketchGeometry);

            // assign a symbol based on geometry type
            if (graphic.getGeometry().getGeometryType() == GeometryType.POLYGON)
            {
                graphic.setSymbol(fillSymbol);
            }
            else if (graphic.getGeometry().getGeometryType() == GeometryType.POLYLINE)
            {
                graphic.setSymbol(lineSymbol);
            }
            else if (graphic.getGeometry().getGeometryType() == GeometryType.POINT ||
                    graphic.getGeometry().getGeometryType() == GeometryType.MULTIPOINT)
            {
                graphic.setSymbol(pointSymbol);
            }

            //如果在分割，那么进行分割；否则直接生成新的要素
            if (isSpliting)
            {
                split((Polyline) sketchGeometry);
            }
            else if (editingFeature != null)
            {
                editingFeature.setGeometry(sketchGeometry);
                editingFeature.getFeatureTable().updateFeatureAsync(editingFeature);
                editingFeature = null;
            }
            else
            {
                addGeometryToFeatureTable(sketchGeometry);
            }
            operationComplete();

        }
    }

    /**
     * 操作完成后进行复位
     */
    private void operationComplete()
    {
        //isSpliting=false;
        resetButtons();
        MapViewHelper.getInstance().stopSelect();
    }

    /**
     * 进行分割操作
     *
     * @param line
     */
    private void split(Polyline line)
    {
        FeatureTable featureTable = getFeatureTable();
        int partsCount = 0;
        //遍历每一个选取的面/线
        for (Feature feature : MapViewHelper.getInstance().getSelectedFeatures())
        {
            //  GeometryType a=feature.getGeometry().getGeometryType();
            Geometry geometry = GeometryEngine.project(feature.getGeometry(), line.getSpatialReference());
//           int s0=featureTable.getSpatialReference().getWkid();
//           int s1=geometry.getSpatialReference().getWkid();
//           int s2=line.getSpatialReference().getWkid();
            List<Geometry> parts = GeometryEngine.cut(geometry, line);
            partsCount += parts.size();
            if (parts.size() != 0)
            {
                //如果分割成的部分不为0个，说明线经过了该图形，该图形被分割，则删除原图形并加入分割后的部分
                for (Geometry geo : parts)
                {
                    addGeometryToFeatureTable(geo, feature.getAttributes());
                }
                //Toast.makeText(context,String.valueOf(parts.size()),Toast.LENGTH_SHORT).show();

                featureTable.deleteFeatureAsync(feature);
            }
        }
//        if (partsCount <= mapviewHelper.selectedFeatures.size())
//        {
//            Toast.makeText(context, "切割失败", Toast.LENGTH_SHORT).show();
//        }
//        else
//        {
//            featureTable.deleteFeaturesAsync(mapviewHelper.selectedFeatures);
//        }
    }

    /**
     * 将图形变成要素并加入要素类，属性为空
     *
     * @param sketchGeometry
     */
    private void addGeometryToFeatureTable(Geometry sketchGeometry)
    {
        addGeometryToFeatureTable(sketchGeometry, null);
    }

    /**
     * 将图形变成要素并加入要素类
     *
     * @param geometry
     * @param attributes
     */
    private void addGeometryToFeatureTable(Geometry geometry, @Nullable Map<String, Object> attributes)
    {
        FeatureTable featureTable = getFeatureTable();
        Feature feature = featureTable.createFeature();
        if (attributes != null)
        {
            Map<String, Object> featureAttributes = feature.getAttributes();
            Set<String> keySet = featureAttributes.keySet();
            for (String key : keySet)
            {
                try
                {
                    //OID类型或OBJECTID字段一般为自动生成
                    if (featureTable.getField(key).getFieldType().toString().equals("OID") || featureTable.getField(key).getName().toLowerCase().equals("objectid"))
                    {
                        continue;
                    }
                    featureAttributes.put(key, attributes.get(key));
                }
                catch (Exception ex)
                {
                    //  Toast.makeText(context, key ,Toast.LENGTH_SHORT).show();
                }

            }
        }
        //将要素加入要素类
        feature.setGeometry(geometry);
        final ListenableFuture<Void> addFeatureOper = featureTable.addFeatureAsync(feature);

        // 在操作完成的监听事件中判断操作是否成功
        addFeatureOper.addDoneListener(() -> {
            try
            {
                addFeatureOper.get();
                if (addFeatureOper.isDone())
                {
                    //Log.i(LogTag, "Feature added!");
                }
                operationComplete();
            }
            catch (InterruptedException interruptedExceptionException)
            {
                // 处理异常
            }
            catch (ExecutionException executionException)
            {

            }
        });

        featureTable.updateFeatureAsync(feature);
    }

    /**
     * 获取当前要素类
     *
     * @return
     */
    FeatureTable getFeatureTable()
    {
        return ((FeatureLayer) LayerManager.getInstance().currentLayer).getFeatureTable();
    }

    public void foldOrUnfold(Activity activity)
    {
        if (MapViewHelper.getInstance().getMap() == null || LayerManager.getInstance().currentLayer == null)
        {
            Toast.makeText(activity, "没有选择当前图层", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!(LayerManager.getInstance().currentLayer instanceof FeatureLayer))
        {
            Toast.makeText(activity, "只有矢量图形可以编辑", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!((FeatureLayer) LayerManager.getInstance().currentLayer).getFeatureTable().isEditable())
        {
            Toast.makeText(activity, "不可编辑只读图层", Toast.LENGTH_SHORT).show();
            return;
        }
        if (control.getTranslationY() == 0)//打开
        {
            ObjectAnimator
                    .ofFloat(control, "translationY",
                            ((RelativeLayout.LayoutParams) control.getLayoutParams()).bottomMargin
                                    - activity.findViewById(R.id.main_llt_bottom_buttons).getHeight())
                    .setDuration(Config.getInstance().animationDuration).start();

        }
        else
        {
            ObjectAnimator.ofFloat(control, "translationY", 0).setDuration(Config.getInstance().animationDuration).start();

        }

    }

    private GraphicsOverlay graphicsOverlay;
    private SketchEditor sketchEditor;
}
