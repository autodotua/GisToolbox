package com.autod.gis.ui.fragment;


import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.autod.gis.ui.activity.MainActivity;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.SketchCreationMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;


import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.autod.gis.data.Config;
import com.autod.gis.map.LayerManager;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.R;

/**
 * 编辑功能的Fragment
 * 所有的编辑逻辑：先选取面/线（多选/单选），然后进行操作。
 */
public class EditFragment extends Fragment
{

    private Button btnClearSelection;
    private ToggleButton btnMutiSelect;
    private Button btnDraw;
    private Button btnSplit;
    private Button btnMerge;
    private Button btnDelete;
    private ImageButton btnUndo;
    private ImageButton btnRedo;
    private boolean isDrawing = false;
    public Feature editingFeature = null;
    /**
     * 所有编辑操作的按钮
     */
    private List<Button> editButtons = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_edit, container, false);
    }

    /**
     * 初始化Fragment
     */
    public void initialize()
    {

        int[] editBtnIds = {
                R.id.edit_btn_delete,
                R.id.edit_btn_draw,
                R.id.edit_btn_union,
                R.id.edit_btn_split,
        };
        btnDraw = getView().findViewById(R.id.edit_btn_draw);
        btnDelete = getView().findViewById(R.id.edit_btn_delete);
        btnSplit = getView().findViewById(R.id.edit_btn_split);
        btnMerge = getView().findViewById(R.id.edit_btn_union);
        GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
        MapViewHelper.getInstance().mapView.getGraphicsOverlays().add(graphicsOverlay);

        /**
         * 将所有的按钮注册单击事件
         */
        for (int btnId : editBtnIds)
        {
            Button btn = getView().findViewById(btnId);
            editButtons.add(btn);
            btn.setOnClickListener(v -> setButtonsClickEvent(v));
        }


        btnClearSelection = getView().findViewById(R.id.edit_btn_clear_selection);
        btnClearSelection.setOnClickListener(v -> MapViewHelper.getInstance().stopSelect());
        /**
         * 撤销按钮
         */
        btnUndo = getView().findViewById(R.id.edit_btn_undo);
        btnUndo.setOnClickListener(v ->
        {
            if (MapViewHelper.getInstance().getSketchEditor().canUndo())
            {
                MapViewHelper.getInstance().getSketchEditor().undo();
            }
            else
            {
                Toast.makeText(getContext(), "无法撤销", Toast.LENGTH_SHORT).show();
            }

        });

        btnRedo = getView().findViewById(R.id.edit_btn_redo);
        btnRedo.setOnClickListener(v ->
        {
            if (MapViewHelper.getInstance().getSketchEditor().canRedo())
            {
                MapViewHelper.getInstance().getSketchEditor().redo();
            }
            else
            {
                Toast.makeText(getContext(), "无法重做", Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * 多选单选切换按钮
         */
        btnMutiSelect = getView().findViewById(R.id.edit_tbtn_multi_select);
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
        updateButtonsEnable();
        MapViewHelper.getInstance().addOnSelectionStatusChangedEventListener(hasSelectedFeatures -> updateButtonsEnable());
    }

    private void updateButtonsEnable()
    {
        int count = MapViewHelper.getInstance().getSelectedFeatures().size();
        if (isDrawing)
        {
            editButtons.stream().forEach(b -> b.setEnabled(false));
            btnMutiSelect.setEnabled(false);
            btnClearSelection.setEnabled(false);
            btnUndo.setEnabled(true);
            btnRedo.setEnabled(true);
            return;
        }
        else
        {
            btnUndo.setEnabled(false);
            btnRedo.setEnabled(false);
            btnMutiSelect.setEnabled(true);
            btnClearSelection.setEnabled(true);
        }

        if (count > 0)
        {
            btnMutiSelect.setVisibility(View.GONE);
            btnClearSelection.setVisibility(View.VISIBLE);
        }
        else
        {
            btnMutiSelect.setVisibility(View.VISIBLE);
            btnClearSelection.setVisibility(View.GONE);
        }
        btnDraw.setEnabled(count <= 1);
        btnDraw.setText(count==0?R.string.edit_btn_draw:R.string.edit_btn_edit);
        btnMerge.setEnabled(count >= 2);
        btnDelete.setEnabled(count >= 1);
        btnSplit.setEnabled(count >= 1);
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
     * 设置按钮的单击事件
     *
     * @param v
     */
    private void setButtonsClickEvent(View v)
    {
        switch (v.getId())
        {
            case R.id.edit_btn_draw:
                startEditing();
                break;
            case R.id.edit_btn_split:
                if (MapViewHelper.getInstance().getSelectedFeatures().size() == 0)
                {
                    Toast.makeText(getContext(), "还未选择图形", Toast.LENGTH_SHORT).show();
                    break;
                }
                Toast.makeText(getContext(), "请绘制分割线", Toast.LENGTH_SHORT).show();
                startSplitting();
                break;
            case R.id.edit_btn_union:
                //合并操作
                if (MapViewHelper.getInstance().getSelectedFeatures().size() < 2)
                {
                    Toast.makeText(getContext(), "还未选择足够的图形", Toast.LENGTH_SHORT).show();
                    break;
                }
                startUnion();
                break;
            case R.id.edit_btn_delete:
                //删除操作
                if (MapViewHelper.getInstance().getSelectedFeatures().size() == 0)
                {
                    Toast.makeText(getContext(), "还未选择图形", Toast.LENGTH_SHORT).show();
                    break;
                }
                delete();
                break;
        }
    }

    /**
     * 合并操作
     */
    public void startUnion()
    {
        List<Geometry> geometries = new ArrayList<>();
        for (Feature feature : MapViewHelper.getInstance().getSelectedFeatures())
        {
            geometries.add(feature.getGeometry());
        }

        //合并
        Geometry resultGeometry = GeometryEngine.union(geometries);

        Feature resultFeature = getFeatureTable().createFeature();
        resultFeature.setGeometry(resultGeometry);

        // 删除原有要素后添加新的要素
        getFeatureTable().deleteFeaturesAsync(MapViewHelper.getInstance().getSelectedFeatures()).addDoneListener(() ->
        {
            addGeometryToFeatureTable(resultGeometry, MapViewHelper.getInstance().getSelectedFeatures().get(0).getAttributes());
        });
    }

    /**
     * 删除操作
     */
    public void delete()
    {
        getFeatureTable().deleteFeaturesAsync(MapViewHelper.getInstance().getSelectedFeatures());
        operationComplete();
    }


    /**
     * 开始绘制（点、线、面的草图）
     */
    private boolean startEditing()
    {
        FeatureTable featureTable = LayerManager.getInstance().getCurrentLayer().getFeatureTable();
        if (MapViewHelper.getInstance().getSelectedFeatures().size() > 1)
        {
            Toast.makeText(getContext(), "选择的要素超过一个，不可编辑", Toast.LENGTH_SHORT).show();
            return false;
        }

        else if (MapViewHelper.getInstance().getSelectedFeatures().size() == 1)
        {
            editingFeature = MapViewHelper.getInstance().getSelectedFeatures().get(0);
            Geometry geometry = editingFeature.getGeometry();
            switch (featureTable.getGeometryType())
            {
                case POINT:
                    startDraw(geometry, SketchCreationMode.POINT);
                    break;
                case MULTIPOINT:
                    startDraw(geometry, SketchCreationMode.MULTIPOINT);
                    break;
                case POLYLINE:
                    startDraw(geometry, SketchCreationMode.POLYLINE);
                    break;
                case POLYGON:
                    startDraw(geometry, SketchCreationMode.POLYGON);
                    break;
            }
            MainActivity.showDrawBar(getView().getRootView(), "正在编辑", this::endEditing, this::cancel);
        }
        else
        {
            //根据要素类的图形类型判断应该要画的图形类型
            switch (featureTable.getGeometryType())
            {
                case POINT:

                    startDraw(SketchCreationMode.POINT);
                    break;
                case MULTIPOINT:
                    startDraw(SketchCreationMode.MULTIPOINT);
                    break;
                case POLYLINE:
                    startDraw(SketchCreationMode.POLYLINE);
                    break;
                case POLYGON:
                    startDraw(SketchCreationMode.POLYGON);
                    break;
            }
            MainActivity.showDrawBar(getView().getRootView(), "正在绘制", this::endEditing, this::cancel);
        }

        return true;
    }


    private void endEditing()
    {
        if (!MapViewHelper.getInstance().getSketchEditor().isSketchValid())
        {
            stopDraw();
            return;
        }
        Geometry sketchGeometry = MapViewHelper.getInstance().getSketchEditor().getGeometry();
        stopDraw();
        //如果绘制了东西，那么把绘制的东西加入到图形中
        if (sketchGeometry != null)
        {
            if (editingFeature != null)
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
     * 准备分割，开始画线
     */
    public void startSplitting()
    {
        try
        {
            startDraw(SketchCreationMode.POLYLINE);
            MainActivity.showDrawBar(getView().getRootView(), "正在分割", this::endSplitting, this::cancel);
        }
        catch (Exception ignored)
        {
            Toast.makeText(getContext(), "开始分割失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void endSplitting()
    {
        if (!MapViewHelper.getInstance().getSketchEditor().isSketchValid()
                || !(MapViewHelper.getInstance().getSketchEditor().getGeometry() instanceof Polyline))
        {
            stopDraw();
            return;
        }
        Polyline line = (Polyline) MapViewHelper.getInstance().getSketchEditor().getGeometry();
        stopDraw();

        FeatureTable featureTable = getFeatureTable();
        //遍历每一个选取的面/线
        for (Feature feature : MapViewHelper.getInstance().getSelectedFeatures())
        {
            Geometry geometry = GeometryEngine.project(feature.getGeometry(), line.getSpatialReference());
            List<Geometry> parts = GeometryEngine.cut(geometry, line);
            if (parts.size() != 0)
            {
                //如果分割成的部分不为0个，说明线经过了该图形，该图形被分割，则删除原图形并加入分割后的部分
                for (Geometry geo : parts)
                {
                    addGeometryToFeatureTable(geo, feature.getAttributes());
                }
                featureTable.deleteFeatureAsync(feature);
            }
        }

        operationComplete();


    }

    private void cancel()
    {
        stopDraw();
        operationComplete();
    }

    private void startDraw(SketchCreationMode mode)
    {
        MapViewHelper.getInstance().getSketchEditor().start(mode);
        isDrawing = true;
        updateButtonsEnable();
    }

    private void startDraw(Geometry geometry, SketchCreationMode mode)
    {
        MapViewHelper.getInstance().getSketchEditor().start(geometry, mode);
        isDrawing = true;
        updateButtonsEnable();
    }

    private void stopDraw()
    {
        MapViewHelper.getInstance().getSketchEditor().stop();
        isDrawing = false;
        updateButtonsEnable();
    }


    /**
     * 操作完成后进行复位
     */
    private void operationComplete()
    {
        MapViewHelper.getInstance().stopSelect();
        updateButtonsEnable();
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
    private void addGeometryToFeatureTable(Geometry
                                                   geometry, @Nullable Map<String, Object> attributes)
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
    private FeatureTable getFeatureTable()
    {
        return LayerManager.getInstance().getCurrentLayer().getFeatureTable();
    }


}
