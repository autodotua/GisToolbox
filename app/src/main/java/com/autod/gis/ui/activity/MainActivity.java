package com.autod.gis.ui.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.autod.gis.BuildConfig;
import com.autod.gis.data.Config;
import com.autod.gis.layer.BaseLayerHelper;
import com.autod.gis.layer.LayerManager;
import com.autod.gis.map.LocationDisplayHelper;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.ui.fragment.EditFragment;
import com.autod.gis.ui.fragment.FeatureAttributionTableFragment;
import com.autod.gis.ui.part.MenuHelper;
import com.autod.gis.R;
import com.autod.gis.map.TrackHelper;

import java.text.DecimalFormat;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener
{


    private static MainActivity instance;

    public static MainActivity getInstance()
    {
        return instance;
    }


    //private ImageView imgMapCompass;

    private ImageView imgSatellite;
    private TextView tvwScale;
    private LinearLayout lltSideButtons;
    private boolean initialized = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        instance = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        // CrashHandler.getInstance().init(this);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
     MenuHelper.getInstance().initialize(getMenuInflater(), menu);
        return true; // true：允许创建的菜单显示出来，false：创建的菜单将无法显示。

    }

    /**
     * 菜单的点击事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        MenuHelper.getInstance().menuClick(item);

        return true;
    }


    /**
     * 初始化
     */
    private void Initialize()
    {
        setTitle("GIS工具箱");
        MapViewHelper.getInstance().Initialize();
        initializeControls();

        //设置地图指南针
        try
        {
            BaseLayerHelper.loadBaseLayer();
        }
        catch (Exception ex)
        {
            Toast.makeText(this, "加载底图失败", Toast.LENGTH_SHORT).show();
        }
        initialized = true;
    }


    /**
     * 检查权限
     */
    private void checkPermission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 1024);
        }
        else
        {
            Initialize();
        }
    }

    /**
     * 权限申请回复
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1024)
        {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
            {

                Initialize();
                Toast.makeText(this, "重新获取权限后可能要重新启动才能获取全部功能", Toast.LENGTH_SHORT).show();

            }
            else
            {
                //如果没有申请到权限则重复申请
                Toast.makeText(this, "必须授予所有权限才能使用本软件", Toast.LENGTH_SHORT).show();
                checkPermission();
            }
        }
    }

    @Override
    protected void onResume()
    {
        if (initialized)
        {
            try
            {
                if (Config.getInstance().location)
                {
                    LocationDisplayHelper.instance.start();
                }

            }
            catch (Exception ex)
            {
            }
        }


        //  mapView.resume();
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }


    @Override
    protected void onStop()
    {
        if (initialized)
        {

            //MapViewHelper.getInstance().unlinkMapAndMapView();
            try
            {
                if (Config.getInstance().location && !Config.getInstance().keepLocationBackground)
                {
                    LocationDisplayHelper.instance.stop();
                }
            }
            catch (Exception ex)
            {
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        if (initialized)
        {
            MapViewHelper.getInstance().unlinkMapAndMapView();
        }
        super.onDestroy();
    }

    /**
     * 初始化字段
     */
    private void initializeControls()
    {
        ImageButton btnPan = findViewById(R.id.main_btn_pan);
        btnPan.setOnClickListener(this);
        btnPan.setOnLongClickListener(this);

        ImageButton btnImport = findViewById(R.id.main_btn_import);
        btnImport.setOnClickListener(this);
        btnImport.setOnLongClickListener(this);


        ImageButton btnLayer = findViewById(R.id.main_btn_layer);
        btnLayer.setOnClickListener(this);

        ImageButton btnZoomIn = findViewById(R.id.main_btn_zoom_in);
        btnZoomIn.setOnClickListener(this);
        btnZoomIn.setOnLongClickListener(this);

        ImageButton btnZoomOut = findViewById(R.id.main_btn_zoom_out);
        btnZoomOut.setOnClickListener(this);
        btnZoomOut.setOnLongClickListener(this);

        ImageButton btnEdit = findViewById(R.id.main_btn_edit);
        btnEdit.setOnClickListener(this);

        MapViewHelper.getInstance().imgMapCompass = findViewById(R.id.main_img_map_compass);
        MapViewHelper.getInstance().imgMapCompass.setOnClickListener(this);
        MapViewHelper.getInstance().imgMapCompass.setVisibility(Config.getInstance().showMapCompass ? View.VISIBLE : View.INVISIBLE);


        EditFragment.getInstance().Initialize();

        FeatureAttributionTableFragment.getInstance().Initialize();


        ImageButton btnZoomToLayer = findViewById(R.id.main_btn_zoom_to_layer);
        btnZoomToLayer.setOnClickListener(this);

        ImageButton btnTable = findViewById(R.id.main_btn_table);
        btnTable.setOnClickListener(this);
        ImageButton btnBooleanBooleanStatistics = findViewById(R.id.main_btn_statistics);
        btnBooleanBooleanStatistics.setOnClickListener(this);

        tvwScale = instance.findViewById(R.id.main_tvw_scale);

        ImageButton btnResetMap = findViewById(R.id.main_btn_reset_map);
        btnResetMap.setOnClickListener(this);
        //btnResetMap.setOnLongClickListener(this);

        ImageButton btnZoomToDefault = findViewById((R.id.main_btn_zoom_to_default));
        btnZoomToDefault.setOnClickListener(this);
        //btnZoomToDefault.setOnLongClickListener(this);

        ImageButton btnTrack = findViewById(R.id.main_btn_track);
        btnTrack.setOnClickListener(this);

        imgSatellite = findViewById(R.id.main_img_satellite);


        lltSideButtons = findViewById(R.id.main_llt_side_buttons);
        if (Config.getInstance().sideButtonsRight)
        {
            setSideButtonPosition();
        }


        //refresh();
    }

    public void setSideButtonPosition()
    {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (Config.getInstance().sideButtonsRight)
        {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        }
        else
        {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        }
        params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        lltSideButtons.setLayoutParams(params);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == 1)
            {
                LayerManager.getInstance().addLayer(data.getData());
            }
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        double scale;
        switch (v.getId())
        {
            case R.id.main_btn_import:
                Intent intent = new Intent(this, FileListActivity.class);
                instance.startActivity(intent);
                break;
            case R.id.main_btn_pan:
                if (!Config.getInstance().location)
                {
                    Toast.makeText(MainActivity.getInstance(), "没有开启定位功能", Toast.LENGTH_SHORT).show();
                    break;
                }
                LocationDisplayHelper.instance.showPanModeDialog();
                break;
            case R.id.main_btn_zoom_to_default:
                MapViewHelper.getInstance().mapView.setViewpointRotationAsync(0);
                break;
            case R.id.main_btn_zoom_in:
                try
                {
                    scale = MapViewHelper.getInstance().mapView.getMapScale();
                    updateScale(0.1);
                    MapViewHelper.getInstance().mapView.setViewpointScaleAsync(scale * 0.1);
                }
                catch (Exception ex)
                {

                }
                break;
            case R.id.main_btn_zoom_out:
                try
                {
                    scale = MapViewHelper.getInstance().mapView.getMapScale();
                    updateScale(10);
                    MapViewHelper.getInstance().mapView.setViewpointScaleAsync(scale * 10);
                }
                catch (Exception ex)
                {

                }
                break;
        }

        return true;
    }

    @Override
    public void onClick(View view)
    {
        Intent intent;
        double scale;
        switch (view.getId())
        {
            case R.id.main_btn_import:
                intent = new Intent(this, ImportFilesActivity.class);
                instance.startActivity(intent);
                break;
            case R.id.main_btn_pan:
                if (!Config.getInstance().location)
                {
                    Toast.makeText(MainActivity.getInstance(), "没有开启定位功能", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!LocationDisplayHelper.instance.setPan())
                {
                    Toast.makeText(MainActivity.getInstance(), "还没有定位", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.main_btn_layer:
                intent = new Intent(this, LayerListActivity.class);
                startActivity(intent);
                break;
            case R.id.main_btn_zoom_in:
                try
                {
                    scale = MapViewHelper.getInstance().mapView.getMapScale();
                    updateScale(0.5);
                    MapViewHelper.getInstance().mapView.setViewpointScaleAsync(scale * 0.5);
                }
                catch (Exception ignored)
                {

                }
                break;
            case R.id.main_btn_zoom_out:
                try
                {
                    scale = MapViewHelper.getInstance().mapView.getMapScale();
                    updateScale(2);
                    MapViewHelper.getInstance().mapView.setViewpointScaleAsync(scale * 2);
                }
                catch (Exception ex)
                {

                }
                break;
            case R.id.main_img_map_compass:
                MapViewHelper.getInstance().mapView.setViewpointRotationAsync(0);
                break;
            case R.id.main_btn_zoom_to_layer:
                MapViewHelper.getInstance().zoomToLayer(false);

                break;
            case R.id.main_btn_table:
                FeatureAttributionTableFragment.getInstance().foldOrUnfold();
                break;
            case R.id.main_btn_edit:
                EditFragment.getInstance().foldOrUnfold();
                break;
            case R.id.main_btn_reset_map:
                LayerManager.getInstance().resetLayers();
                break;
            case R.id.main_btn_zoom_to_default:
                if (eggClickTimes++ == 20)
                {
                    Toast.makeText(MainActivity.getInstance(), new String(Base64.decode("YXV0b2RvdHVh", Base64.DEFAULT)), Toast.LENGTH_SHORT).show();
                }
                MapViewHelper.getInstance().mapView.setViewpointScaleAsync(Config.getInstance().defaultScale).addDoneListener(() -> instance.setScaleText(Config.getInstance().defaultScale));
                break;
            case R.id.main_btn_track:
                changeTrackStatus();
                break;

        }
    }

    private void changeTrackStatus()
    {
        Animation ani = new AlphaAnimation(0.1f, 1.0f);
        ani.setDuration(2000);
        ani.setRepeatMode(Animation.REVERSE);
        ani.setRepeatCount(Animation.INFINITE);

        if (TrackHelper.getStatus() == TrackHelper.Status.NotRunning)
        {
            if (TrackHelper.start())
            {
                imgSatellite.setVisibility(View.VISIBLE);
                imgSatellite.setAnimation(ani);
            }
        }
        else if (TrackHelper.getStatus() == TrackHelper.Status.Running)
        {
            new AlertDialog.Builder(this)
                    .setTitle("正在记录轨迹")
                    .setPositiveButton("暂停", (dialog, which) -> {
                        TrackHelper.pause();
                        imgSatellite.clearAnimation();
                        imgSatellite.setVisibility(View.GONE);
                    })
                    .setNegativeButton("停止", (dialog, which) -> {
                        TrackHelper.stop();
                        imgSatellite.clearAnimation();
                        imgSatellite.setVisibility(View.GONE);
                    })
                    .setNeutralButton("取消", (dialog, which) -> {

                    }).create().show();
        }
        else if (TrackHelper.getStatus() == TrackHelper.Status.Pausing)
        {
            new AlertDialog.Builder(this)
                    .setTitle("记录轨迹暂停中")
                    .setPositiveButton("继续", (dialog, which) -> {
                        TrackHelper.resume();
                        imgSatellite.setVisibility(View.VISIBLE);
                        imgSatellite.setAnimation(ani);
                    })
                    .setNegativeButton("停止", (dialog, which) -> {
                        TrackHelper.stop();
                    })
                    .setNeutralButton("取消", (dialog, which) -> {

                    }).create().show();
        }
    }


    public void setSideButtonsVisible(boolean visiable)
    {
        if (Config.getInstance().sideButtonsRight)
        {
            if (visiable)
            {
                ObjectAnimator.ofFloat(lltSideButtons, "translationX", lltSideButtons.getWidth(), 0).setDuration(Config.getInstance().animationDuration).start();
            }
            else
            {
                ObjectAnimator.ofFloat(lltSideButtons, "translationX", 0, lltSideButtons.getWidth()).setDuration(Config.getInstance().animationDuration).start();
            }
        }
    }

    int eggClickTimes = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    public void setScaleText(double value)
    {
        DecimalFormat formatter = new DecimalFormat("0.0");
        if (value > 10000)
        {
            tvwScale.setText("1:" + formatter.format(value / 10000) + "万");
        }
        else
        {
            tvwScale.setText("1:" + formatter.format(value));

        }
    }

    public void updateScale(double multi)
    {
        try
        {

            DecimalFormat formatter = new DecimalFormat("0.0");
            double value = MapViewHelper.getInstance().mapView.getMapScale() * multi;
            if (value > 10000)
            {
                tvwScale.setText("1:" + formatter.format(value / 10000) + "万");
            }
            else
            {
                tvwScale.setText("1:" + formatter.format(value));

            }
        }
        catch (Exception ex)
        {

        }
    }

    /*
    不知道这个方法是干什么的，甚至不知道是不是我写的，暂时在调用的地方注释掉了
     */
    public void refresh()
    {
//        new Handler().postDelayed(() -> {
//            if (BaseLayerHelper.baseLayerCount != LayerManager.getInstance().getLayers().size())
//            {
//                LayerManager.getInstance().resetLayers();
//            }
//            if (BaseLayerHelper.baseLayerCount != LayerManager.getInstance().getLayers().size()
//                    || BaseLayerHelper.baseLayerCount == 0
//                    || LayerManager.getInstance().getLayers().size() == 0)
//                refresh();
//        }, 573);
    }


}
