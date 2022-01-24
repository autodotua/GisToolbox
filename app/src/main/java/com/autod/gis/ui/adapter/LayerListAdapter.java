package com.autod.gis.ui.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.esri.arcgisruntime.layers.Layer;
import com.autod.gis.layer.LayerManager;
import com.autod.gis.R;
import com.autod.gis.map.MapViewHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.autod.gis.ui.activity.LayerListActivity.layerListActivity;

public class LayerListAdapter extends BaseAdapter
{
    private LayoutInflater mInflater;


    public LayerListAdapter(Context context)
    {
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount()
    {
        // TODO Auto-generated method stub
        //int baseLayerCount = BaseLayerHelper.baseLayerCount;  //获取基础图层的数量
        return LayerManager.getInstance().getLayers().size();    //隐藏除矢量图之外的4个基础图层//没了
//        returnLayerManager.getInstance().getLayers().size();
    }

    @Override
    public Object getItem(int arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int arg0)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    private ViewHolder holder = null;
    private List<ViewHolder> holders = new ArrayList<>();


    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int index, View convertView, ViewGroup parent)
    {


        if (convertView == null)
        {

            holder = new ViewHolder();

            convertView = mInflater.inflate(R.layout.layout_layer_list_view, null);
            holder.chkVisible = convertView.findViewById(R.id.layer_chk_visible);
            holder.tvwFilePath = convertView.findViewById(R.id.layer_tvw_file_path);
            holder.skbOpacity = convertView.findViewById(R.id.layer_skb_opacity);
            holder.tvwOpacity = convertView.findViewById(R.id.layer_tvw_opacity);
            holder.rbtnCurrentLayer = convertView.findViewById(R.id.layer_rbtn_current_layer);
            holder.btnMenu = convertView.findViewById(R.id.layer_btn_menu);
            holder.btnUp = convertView.findViewById(R.id.layer_btn_to_up);
            holder.btnDown = convertView.findViewById(R.id.layer_btn_to_down);
            convertView.setTag(holder);

        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }

       // int baseLayerCount = BaseLayerHelper.baseLayerCount;  //获取基础图层的数量
//        int reverseIndex =LayerManager.getInstance().getLayers().size()-1 - index +1;  //zj mmpk中的三个基础图层

//        int reverseIndex =getCount()-1 +(baseLayerCount-1)-index;
        int realIndex = LayerManager.getInstance().getLayers().size() - 1 - index;
        holders.add(holder);
        holder.rbtnCurrentLayer.setChecked(LayerManager.getInstance().currentLayer == LayerManager.getInstance().getLayer(realIndex));
        if (holder.rbtnCurrentLayer.isChecked())
        {
            lastCheckedButton = holder.rbtnCurrentLayer;
        }
        holder.chkVisible.setChecked(LayerManager.getInstance().getLayers().get(realIndex).isVisible());
        holder.skbOpacity.setProgress((int) (LayerManager.getInstance().getLayers().get(realIndex).getOpacity() * 100));
        try
        {
            //String name=LayerManager.getInstance().getLayers().get(reverseIndex).getName();
            String path = new File(LayerManager.getInstance().layerFilePath.get(LayerManager.getInstance().getLayers().get(realIndex))).getName();
            //if(name!="")
            //{
            //    holder.tvwFilePath.setText(name);
            //}
            //else if(path!="")
            //{
            holder.tvwFilePath.setText(path);
            //}
            //else
            //{
            //    holder.tvwFilePath.setText("（无名称）");
            //}

        }
        catch (Exception ex)
        {
            holder.tvwFilePath.setText("（无名称）");
        }
        holder.chkVisible.setOnClickListener(v ->
        {
            LayerManager.getInstance().getLayers().get(realIndex).setVisible(holders.get(index).chkVisible.isChecked());
        });

        holder.rbtnCurrentLayer.setOnClickListener(v -> {
            MapViewHelper.getInstance().stopSelect();
            if (realIndex < LayerManager.getInstance().getLayers().size())
            {
                //Toast.makeText(MainActivity.getInstance(), String.valueOf(((RadioButton) v).isChecked()), Toast.LENGTH_SHORT).show();
                if (LayerManager.getInstance().currentLayer == LayerManager.getInstance().getLayer(realIndex))
                {
                    ((RadioButton) v).setChecked(false);
                    LayerManager.getInstance().currentLayer = null;
                    return;
                }
                LayerManager.getInstance().currentLayer = LayerManager.getInstance().getLayer(realIndex);

                if (lastCheckedButton != null)
                {
                    lastCheckedButton.setChecked(false);
                }
              
                lastCheckedButton = (RadioButton) v;
//                        for (int i = 0; i < holders.size(); i++)
//                        {
//                            if (i != index && holders.get(i).rbtnCurrentLayer.isChecked())
//                            {
//                                holders.get(i).rbtnCurrentLayer.setChecked(false);
//                            }
//                            else if (!holders.get(i).rbtnCurrentLayer.isChecked())
//                            {
//                                holders.get(i).rbtnCurrentLayer.setChecked(true);
//                            }
//                        }
            }
        });

        holder.btnUp.setOnClickListener(v ->
        {
            if (realIndex < LayerManager.getInstance().getLayers().size() - 1)
            {
                Layer l = LayerManager.getInstance().getLayer(realIndex);
                LayerManager.getInstance().getLayers().remove(realIndex);
                LayerManager.getInstance().getLayers().add(realIndex + 1, l);
            }

            notifyDataSetChanged();
        });
        holder.btnDown.setOnClickListener(v ->
        {
            //if (realIndex > baseLayerCount - 1)
            if (realIndex >=0)
            {
                Layer l = LayerManager.getInstance().getLayer(realIndex);
                LayerManager.getInstance().getLayers().remove(realIndex);
                LayerManager.getInstance().getLayers().add(realIndex - 1, l);
            }
            notifyDataSetChanged();
        });
        holder.btnMenu.setOnClickListener(v ->
        {
            Layer currentLayer = LayerManager.getInstance().getLayer(realIndex);

             new AlertDialog.Builder(layerListActivity)
                    .setTitle("移除图层")
                    .setMessage("确认删除？")
                    .setPositiveButton("确定", (dialogInterface, i)
                            -> LayerManager.getInstance().getLayers().remove(realIndex)
                            .addDoneLoadingListener(this::notifyDataSetChanged))
                    .setNegativeButton("取消",null)
                    .show();

        });

        /**
         * 滑动Slider时实时改变图层透明度。但是因为UI线程堵塞的关系，要等结束滑动才能看见效果。
         */
        holder.skbOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                holders.get(index).tvwOpacity.setText("透明度：" + String.valueOf(i) + "%");
                LayerManager.getInstance().getLayer(realIndex).setOpacity((float) seekBar.getProgress() / 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        return convertView;
    }

    RadioButton lastCheckedButton = null;

    public final class ViewHolder
    {
        /**
         * 当前图层单选框
         */
        RadioButton rbtnCurrentLayer;
        /**
         * 图层开关
         */
        Switch chkVisible;
        /**
         * 文件地址文本
         */
        TextView tvwFilePath;
        /**
         * 透明度滑块
         */
        SeekBar skbOpacity;
        /**
         * 透明度显示
         */
        TextView tvwOpacity;
        /**
         * 操作按钮
         */
        ImageButton btnMenu;
        Button btnUp;
        Button btnDown;
    }


}



