package com.autod.gis.ui.dialog;

import android.app.Dialog;
import android.view.View;

public class FeatureLayerSettingDialog extends Dialog implements View.OnClickListener
{
    //已弃用
    @Override
    public void onClick(View v)
    {

    }
    public FeatureLayerSettingDialog()
    {
        super(null);
    }
//
//    public FeatureLayerSettingDialog(@NonNull Context context)
//    {
//        super(context);
//    }
//
//    public FeatureLayerSettingDialog(@NonNull Context context, FeatureLayer layer, float opacity)
//    {
//        this(context);
//        currentLayer = layer;
//        this.opacity = opacity;
//    }
//
//    /**
//     * 边框颜色选择按钮
//     */
//    private ColorPanelView colorBorder;
//    /**
//     * 填充颜色选择按钮
//     */
//    private ColorPanelView colorFill;
//    /**
//     * 边框粗细编辑框
//     */
//    private EditText ettBorderThickness;
//    /**
//     * 透明度，不可编辑，仅传入，用于写入style文件
//     */
//    private float opacity;
//    /**
//     * 当前的要素类图层
//     */
//    private FeatureLayer currentLayer;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.dialog_feature_layer_setting);
//        colorBorder = findViewById(R.id.colorFeatureLayerSettingBorder);
//        colorBorder.setOnClickListener(this);
//        colorFill = findViewById(R.id.colorFeatureLayerSettingFill);
//        colorFill.setOnClickListener(this);
//
//        Button btnOk = findViewById(R.id.btnFeatureLayerSettingOk);
//        btnOk.setOnClickListener(this);
//        Button btnDelete = findViewById(R.id.btnFeatureLayerSettingDelete);
//        btnDelete.setOnClickListener(this);
//        ettBorderThickness = findViewById(R.id.ettFeatureLayerSettingBorderThickness);
//
//        //获取当前图层的样式，并显示
//        getLayerRenderer();
//    }
//
//    /**
//     * 获取当前图层的样式
//     */
//    private void getLayerRenderer()
//    {
//        try
//        {
//
//            SimpleRenderer renderer = (SimpleRenderer) currentLayer.getRenderer();
//            SimpleFillSymbol symbol = (SimpleFillSymbol) renderer.getSymbol();
//            SimpleLineSymbol lineSymbol = (SimpleLineSymbol) symbol.getOutline();
//
//            // Toast.makeText(layerListActivity, String.valueOf(symbol.getColor()), Toast.LENGTH_SHORT).show();
//            colorBorder.setColor(lineSymbol.getColor());
//            colorFill.setColor(symbol.getColor());
//            ettBorderThickness.setText(String.valueOf(lineSymbol.getWidth()));
//        }
//        catch (Exception ex)
//        {
//            Toast.makeText(layerListActivity, "读取要素外观失败：\n" + ex.toString(), Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    /**
//     * 显示颜色选择对话框
//     *
//     * @param view
//     */
//    @Override
//    public void onClick(View view)
//    {
//        if (view instanceof ColorPanelView)
//        {
//            int currentColor = ((ColorPanelView) view).getColor();
//            ColorPickerDialog dialog = ColorPickerDialog.newBuilder().setShowAlphaSlider(true).setColor(currentColor).create();
//
//            dialog.setColorPickerDialogListener(new ColorPickerDialogListener()
//            {
//                @Override
//                public void onColorSelected(int dialogId, int color)
//                {
//
//                    ((ColorPanelView) view).setColor(color);
//                }
//
//                @Override
//                public void onDialogDismissed(int dialogId)
//                {
//                }
//            });
//            dialog.show(layerListActivity.getFragmentManager(), "color-picker-dialog");
//        }
//        else if (view instanceof Button)
//        {
//            switch (view.getId())
//            {
//                case R.id.btnFeatureLayerSettingOk:
//                    setLayerProperties();
//                    dismiss();
//                    break;
//
//                case R.id.btnFeatureLayerSettingDelete:
//                   LayerManager.getInstance().getLayers().remove(currentLayer);
//                    FeatureLayerSettingDialog dialog = this;
//                    new Handler().postDelayed(new Runnable()
//                    {
//                        public void run()
//                        {
//                            layerListActivity.notifyDataSetChanged();
//                            dialog.dismiss();
//                        }
//                    }, 573);
//                    break;
//            }
//        }
//    }
//
//    /**
//     * 根据选取的颜色和粗细来设置当前图层的样式
//     */
//    private void setLayerProperties()
//    {
//        float thickness = 1;
//        //输入的边框值不一定可用，故加入try块
//        try
//        {
//            thickness = Float.valueOf(ettBorderThickness.getText().toString());
//        }
//        catch (Exception ignored)
//        {
//        }
//        FeatureLayerInfo featureLayerInfo = new FeatureLayerInfo(colorBorder.getColor(), colorFill.getColor(), thickness);
//        LayerInfo property = new LayerInfo(opacity, featureLayerInfo);
//       LayerManager.getInstance().applyLayerProperty(currentLayer, property);
////        float thickness = 1;
////        try
////        {
////            thickness = Float.valueOf(ettBorderThickness.getText().toString());
////        }
////        catch (Exception ex)
////        {
////
////        }
////        SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, colorBorder.getColor(), thickness);
////        int color = colorFill.getColor();
////        FillSymbol symbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID,
////                Color.argb(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color)), lineSymbol);
////        SimpleRenderer simpleRenderer = new SimpleRenderer(symbol);
////        featureLayer.setRenderer(simpleRenderer);
//    }
}
