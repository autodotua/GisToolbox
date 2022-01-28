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

import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.autod.gis.map.LayerManager;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.model.LayerInfo;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class BaseLayerListAdapter extends BaseAdapter
{
    private LayoutInflater mInflater;
    private Context context;
    private ArrayList<LayerInfo> layers = Config.getInstance().baseLayers;

    public void addLayer(String url)
    {
        layers.add(new LayerInfo(url));
    }

    public void save()
    {
        Config.getInstance().baseLayers = layers;
    }

    public BaseLayerListAdapter(Context context)
    {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public int getCount()
    {
        return layers.size();    //隐藏除矢量图之外的4个基础图层
    }

    @Override
    public Object getItem(int arg0)
    {
        return null;
    }

    @Override
    public long getItemId(int arg0)
    {
        return 0;
    }

    private List<ViewHolder> holders = new ArrayList<>();


    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int index, View convertView, ViewGroup parent)
    {
        ViewHolder holder;
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

        int realIndex = layers.size() - 1 - index;
        holders.add(holder);
        holder.rbtnCurrentLayer.setVisibility(View.GONE);
        holder.chkVisible.setChecked(layers.get(realIndex).isVisible());
        int opacity = (int) (layers.get(realIndex).getOpacity() * 100);
        holder.skbOpacity.setProgress(opacity);
        holder.tvwOpacity.setText(opacity + "%");
        holder.tvwFilePath.setText(layers.get(realIndex).getPath());
        holder.chkVisible.setOnClickListener(v ->
                layers.get(realIndex).setVisible(holders.get(index).chkVisible.isChecked()));

        holder.btnUp.setOnClickListener(v ->
        {
            if (realIndex < layers.size() - 1)
            {
                LayerInfo l = layers.get(realIndex);
                layers.remove(realIndex);
                layers.add(realIndex + 1, l);
            }

            notifyDataSetChanged();
        });
        holder.btnDown.setOnClickListener(v ->
        {
            if (realIndex >= 0)
            {
                LayerInfo l = layers.get(realIndex);
                layers.remove(realIndex);
                layers.add(realIndex - 1, l);
            }
            //notifyDataSetChanged();
        });
        holder.btnMenu.setOnClickListener(v ->
        {
            LayerInfo l = layers.get(realIndex);

            new AlertDialog.Builder(context)
                    .setTitle("移除图层")
                    .setMessage("确认删除？")
                    .setPositiveButton("确定", (dialogInterface, i)
                            -> {
                        layers.remove(realIndex);
                        notifyDataSetChanged();
                    })
                    .setNegativeButton("取消", null)
                    .show();

        });

        //滑动Slider时实时改变图层透明度。但是因为UI线程堵塞的关系，要等结束滑动才能看见效果。
        holder.skbOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                holders.get(index).tvwOpacity.setText(i + "%");
                layers.get(realIndex).setOpacity((float) seekBar.getProgress() / 100);
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

    public static final class ViewHolder
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



