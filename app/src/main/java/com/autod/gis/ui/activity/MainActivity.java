package com.autod.gis.ui.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.text.Html;
import android.util.Base64;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.autod.gis.data.Config;
import com.autod.gis.map.LayerManager;
import com.autod.gis.map.LocationDisplayHelper;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.model.LayerInfo;
import com.autod.gis.model.TrackInfo;
import com.autod.gis.service.TrackService;
import com.autod.gis.ui.fragment.EditFragment;
import com.autod.gis.ui.fragment.FeatureAttributionTableFragment;
import com.autod.gis.ui.MenuHelper;
import com.autod.gis.R;
import com.autod.gis.map.SensorHelper;
import com.autod.gis.util.DateTimeUtility;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.Viewpoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener
{
    public final static int BaseLayerListActivityID = 1;
    public final static int ImportFilesActivityID = 2;
    public final static int LayerListActivityID = 3;
    int eggClickTimes = 0;
    private TextView tvwScale;
    private boolean initialized = false;
    private TextView topBarDetail;
    private View topBar;
    private TextView topBarTitle;
    private TrackService trackService;
    private boolean resumingTrack = false;
    private AlertDialog locationDetailDialog = null;
    private ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            TrackService.TrackBinder trackBinder = (TrackService.TrackBinder) service;
            trackService = trackBinder.getService();
            if (getTrackService() != null && resumingTrack)//Activity启动时，Service已经在运行
            {
                resumingTrack = false;
                getTrackService().resumeOverlay();
                topBarTitle.setText(R.string.main_track_recording_paused);
                topBar.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorOrange));
                topBarAnimation(1);
                Toast.makeText(MainActivity.this, "轨迹记录继续运行", Toast.LENGTH_SHORT).show();
            }
            trackService.addOnTrackTimerListener(trackInfo -> runOnUiThread(() -> {
                topBarDetail.setText(getLocationMessage(2, trackInfo));
                if (locationDetailDialog != null && locationDetailDialog.isShowing())
                {
                    CharSequence msg = getLocationMessage(1, trackInfo);
                    locationDetailDialog.setMessage(msg);
                }
            }));
            topBarDetail.setText(getLocationMessage(2, trackService.getLastTrackInfo()));
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
        }
    };

    public static void showDrawBar(@NonNull View rootView, String message, @Nullable Runnable onOkClickListener, @Nullable Runnable onCancelClickListener)
    {
        showDrawBar(rootView, message, onOkClickListener, onCancelClickListener, true);
    }

    public static void showDrawBar(@NonNull View rootView, String message, @Nullable Runnable onOkClickListener)
    {
        showDrawBar(rootView, message, onOkClickListener, null, false);
    }

    public static void showDrawBar(@NonNull View rootView, String message, @Nullable Runnable onOkClickListener, @Nullable Runnable onCancelClickListener, boolean showCancelButton)
    {
        TextView tvw = rootView.findViewById(R.id.main_tvw_draw);
        TextView btnBarOk = rootView.findViewById(R.id.main_tvw_draw_ok);
        TextView btnBarCancel = rootView.findViewById(R.id.main_tvw_draw_cancel);
        View bar = rootView.findViewById(R.id.main_llt_draw_bar);
        btnBarCancel.setVisibility(showCancelButton ? View.VISIBLE : View.GONE);
        btnBarOk.setEnabled(true);
        btnBarCancel.setEnabled(true);
        tvw.setText(message);
        btnBarOk.setOnClickListener(v -> {
            btnBarOk.setEnabled(false);
            btnBarCancel.setEnabled(false);
            ObjectAnimator.ofFloat(bar, "translationY", 0).setDuration(Config.getInstance().animationDuration).start();
            if (onOkClickListener != null)
            {
                onOkClickListener.run();
            }
        });
        btnBarCancel.setOnClickListener(v -> {
            btnBarOk.setEnabled(false);
            btnBarCancel.setEnabled(false);
            if (onCancelClickListener != null)
            {
                onCancelClickListener.run();
            }
            ObjectAnimator.ofFloat(bar, "translationY", 0).setDuration(Config.getInstance().animationDuration).start();
        });
        ObjectAnimator.ofFloat(bar, "translationY", -bar.getHeight()).setDuration(Config.getInstance().animationDuration).start();
    }

    public TrackService getTrackService()
    {
        return trackService;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        setTitle("GIS工具箱");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//设置透明状态栏
        checkPermission();
    }

    private boolean isServiceRunning(Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 初始化
     */
    private void Initialize()
    {
        MapViewHelper.getInstance().Initialize(this);
        initializeView();
        MapViewHelper.getInstance().getMapView().addMapScaleChangedListener(mapScaleChangedEvent -> updateScale());
        initializeTrack();
        LayerManager.getInstance().initialize(this);
        initialized = true;
    }

    private void initializeTrack()
    {
        if (isServiceRunning(TrackService.class))
        {
            //bindService后connection连接成功的调用晚于图层的初始化，导致轨迹覆盖层无法显示，因此需要在connection连接成功的事件里重设覆盖层
            resumingTrack = true;
            bindService(new Intent(this, TrackService.class), connection, 0);
        }
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
                    LocationDisplayHelper.getInstance().start(this);
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
        if (initialized)
        {
            Config.getInstance().lastExtent = MapViewHelper.getInstance().getMapView().getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY).getTargetGeometry().toJson();
            Config.getInstance().save();
        }
    }

    @Override
    protected void onStop()
    {
        if (initialized)
        {
            try
            {
                if (Config.getInstance().location && !Config.getInstance().keepLocationBackground)
                {
                    LocationDisplayHelper.getInstance().stop(this);
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
            LocationDisplayHelper.getInstance().stop(this);

            if (getTrackService() != null)
            {
                stopTrack(false);
            }
        }
        super.onDestroy();
    }

    /**
     * 初始化字段
     */
    private void initializeView()
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

        ((EditFragment) getSupportFragmentManager().findFragmentById(R.id.main_fgm_edit)).initialize();

        ((FeatureAttributionTableFragment) getSupportFragmentManager().findFragmentById(R.id.main_fgm_attri)).Initialize(this);

        ImageButton btnZoomToLayer = findViewById(R.id.main_btn_zoom_to_layer);
        btnZoomToLayer.setOnClickListener(this);

        ImageButton btnTable = findViewById(R.id.main_btn_table);
        btnTable.setOnClickListener(this);

        tvwScale = findViewById(R.id.main_tvw_scale);

        ImageButton btnResetMap = findViewById(R.id.main_btn_menu);
        btnResetMap.setOnClickListener(this);

        ImageButton btnTrack = findViewById(R.id.main_btn_track);
        btnTrack.setOnClickListener(this);

        topBarDetail = findViewById(R.id.main_tvw_track_info);
        topBar = findViewById(R.id.main_llt_top);
        topBar.setOnClickListener(this);

        topBarTitle=findViewById(R.id.main_tvw_track_title);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            if (requestCode == ImportFilesActivityID)
            {
                String path = data.getStringExtra("path");
                if (data.getBooleanExtra("reset", false))//开过FTP，需要重置
                {
                    try
                    {
                        if (path != null)//没有加图层
                        {
                            Config.getInstance().layers.add(new LayerInfo(path, true, 1));
                            Config.getInstance().save(false);
                        }
                        LayerManager.getInstance().resetLayers(this);
                    }
                    catch (Exception ex)
                    {
                        Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                        ex.printStackTrace();
                    }
                }
                else//不需要重置，只需要加图层
                {
                    Layer layer = LayerManager.getInstance().addLayer(this, path);
                    layer.addDoneLoadingListener(() -> {
                        if (layer.getLoadStatus() == LoadStatus.LOADED)
                        {
                            MapViewHelper.getInstance().zoomToLayer(this, layer);
                        }
                    });
                    Config.getInstance().save();
                }
            }
            else if (requestCode == BaseLayerListActivityID)
            {
                LayerManager.getInstance().resetLayers(this);
                Config.getInstance().save();
            }
            else if (requestCode == LayerListActivityID)
            {
                LayerManager.getInstance().resetLayers(this);
            }
        }

    }

    @Override
    public boolean onLongClick(View v)
    {
        double scale;
        switch (v.getId())
        {
            case R.id.main_btn_pan:
                if (!Config.getInstance().location)
                {
                    Toast.makeText(this, "没有开启定位功能", Toast.LENGTH_SHORT).show();
                    break;
                }
                LocationDisplayHelper.getInstance().showPanModeDialog(this);
                break;
            case R.id.main_btn_zoom_in:
                try
                {
                    scale = MapViewHelper.getInstance().getMapView().getMapScale();
                    MapViewHelper.getInstance().getMapView().setViewpointScaleAsync(scale * 0.1);
                }
                catch (Exception ex)
                {

                }
                break;
            case R.id.main_btn_zoom_out:
                try
                {
                    scale = MapViewHelper.getInstance().getMapView().getMapScale();
                    MapViewHelper.getInstance().getMapView().setViewpointScaleAsync(scale * 10);
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
                startActivityForResult(intent, ImportFilesActivityID);
                break;
            case R.id.main_btn_pan:
                if (!Config.getInstance().location)
                {
                    Toast.makeText(this, "没有开启定位功能", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!LocationDisplayHelper.getInstance().setPan(this))
                {
                    Toast.makeText(this, "还没有定位", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.main_btn_layer:
                intent = new Intent(this, LayerListActivity.class);
                startActivityForResult(intent, LayerListActivityID);
                break;
            case R.id.main_btn_zoom_in:
                try
                {
                    scale = MapViewHelper.getInstance().getMapView().getMapScale();
                    MapViewHelper.getInstance().getMapView().setViewpointScaleAsync(scale * 0.5);
                }
                catch (Exception ignored)
                {

                }
                break;
            case R.id.main_btn_zoom_out:
                try
                {
                    scale = MapViewHelper.getInstance().getMapView().getMapScale();
                    MapViewHelper.getInstance().getMapView().setViewpointScaleAsync(scale * 2);
                }
                catch (Exception ex)
                {

                }
                break;
            case R.id.main_btn_zoom_to_layer:
                if (eggClickTimes++ == 20)
                {
                    Toast.makeText(this, new String(Base64.decode("YXV0b2RvdHVh", Base64.DEFAULT)), Toast.LENGTH_SHORT).show();
                }

                if (LayerManager.getInstance().getCurrentLayer() == null)
                {
                    Toast.makeText(this, "请先选择当前图层", Toast.LENGTH_SHORT).show();
                    return;
                }
                MapViewHelper.getInstance().zoomToLayer(this, LayerManager.getInstance().getCurrentLayer());
                break;
            case R.id.main_btn_table:
                foldFeatureAttributionTable();
                break;
            case R.id.main_btn_edit:
                foldEditPanel();
                break;
            case R.id.main_btn_menu:
                PopupMenu popup = new PopupMenu(this, view);
                MenuInflater inflater = popup.getMenuInflater();
                Menu menu = popup.getMenu();
                MenuHelper helper = new MenuHelper();
                helper.initialize(inflater, menu);
                popup.setOnMenuItemClickListener(item -> {
                    helper.menuClick(MainActivity.this, item);
                    return true;
                });
                popup.show();
                break;
            case R.id.main_btn_track:
                changeTrackStatus(view);
                break;
            case R.id.main_llt_top:
                locationDetailDialog = new AlertDialog.Builder(this)
                        .setTitle("定位详细信息")
                        .setMessage(getLocationMessage(1, trackService.getLastTrackInfo()))
                        .setPositiveButton("关闭", null)
                        .create();
                locationDetailDialog.setOnDismissListener(dialog -> {
                    locationDetailDialog = null;
                });
                locationDetailDialog.show();

        }
    }  //收缩和展开属性表

    public void foldFeatureAttributionTable()
    {
        View control = findViewById(R.id.main_fgm_attri);
        if (control.getTranslationX() == 0)//打开
        {
            ObjectAnimator.ofFloat(control, "translationX", control.getWidth()).setDuration(Config.getInstance().animationDuration).start();
        }
        else//关闭
        {
            ObjectAnimator.ofFloat(control, "translationX", 0).setDuration(Config.getInstance().animationDuration).start();
        }
    }

    public void foldEditPanel()
    {
        if (MapViewHelper.getInstance().getMap() == null || LayerManager.getInstance().getCurrentLayer() == null)
        {
            Toast.makeText(this, "没有选择当前图层", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!LayerManager.getInstance().getCurrentLayer().getFeatureTable().isEditable())
        {
            Toast.makeText(this, "不可编辑只读图层", Toast.LENGTH_SHORT).show();
            return;
        }
        View control = findViewById(R.id.main_fgm_edit);
        if (control.getTranslationY() == 0)//打开
        {
            ObjectAnimator
                    .ofFloat(control, "translationY",
                            ((RelativeLayout.LayoutParams) control.getLayoutParams()).bottomMargin
                                    - findViewById(R.id.main_llt_bottom_buttons).getHeight())
                    .setDuration(Config.getInstance().animationDuration).start();

        }
        else
        {
            ObjectAnimator.ofFloat(control, "translationY", 0).setDuration(Config.getInstance().animationDuration).start();
        }

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState)
    {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @SuppressLint("RestrictedApi")
    private void changeTrackStatus(View view)
    {
        if (trackService == null)
        {
            startTrack();
        }
        else
        {
            boolean p = trackService.isPausing();

            PopupMenu popup = new PopupMenu(this, view);
            Menu menu=popup.getMenu();
            popup.getMenuInflater().inflate(R.menu.menu_track, menu);
            menu.findItem(R.id.menu_track_pause).setVisible(!p);
            menu.findItem(R.id.menu_track_resume).setVisible(p);
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId())
                {
                    case R.id.menu_track_stop:
                        stopTrack(true);
                        break;
                    case R.id.menu_track_pause:
                        topBarTitle.setText(R.string.main_track_recording_paused);
                        topBar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorOrange));
                        trackService.pause(this);
                        break;
                    case R.id.menu_track_resume:
                        topBarTitle.setText(R.string.main_track_recording);
                        topBar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
                        trackService.resume(this);
                        break;
                }
                return true;
            });
            popup.show();
        }
    }

    private void startTrack()
    {
        startService(new Intent(this, TrackService.class));
        bindService(new Intent(this, TrackService.class), connection, 0);
        topBarAnimation(1);
    }

    public void stopTrack(boolean stopService)
    {
        trackService = null;
        unbindService(connection);
        if (stopService)
        {
            stopService(new Intent(this, TrackService.class));
        }
        topBarAnimation(0);
    }

    private CharSequence getLocationMessage(int type, TrackInfo trackInfo)
    {
        if (trackService == null)
        {
            return "";
        }
        Location location = trackInfo.getLocation();
        if (location != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss", Locale.CHINA);
            String time = sdf.format(new Date(location.getTime()));
            String duration = DateTimeUtility.formatTimeSpan(trackInfo.getStartTime(), new Date());
            double lng = location.getLongitude();
            double lat = location.getLatitude();
            double gpsAlt = location.getAltitude();
            double pAlt = SensorHelper.getInstance() == null ? Double.NaN : SensorHelper.getInstance().getCurrentAltitude();
            double alt = Config.getInstance().useBarometer && !Double.isNaN(pAlt) ? pAlt : gpsAlt;
            double speed = location.getSpeed();
            double speedKm = speed * 3.6;
            double bearing = location.getBearing();
            String bearingDesc = angle2Direction(bearing);
            double hAcc = location.getAccuracy();
            double vAcc = Double.NaN;
            double sAcc = Double.NaN;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            {
                vAcc = location.getVerticalAccuracyMeters();
                sAcc = location.getSpeedAccuracyMetersPerSecond();
            }
            if (type == 1)
            {
                return Html.fromHtml(getResources().getString(R.string.msg_gps_detail, time, trackInfo.getCount(), trackInfo.getLength(), lng, lat, gpsAlt, pAlt, speed, speedKm, bearing, bearingDesc, hAcc, vAcc, sAcc, trackInfo.getFixedSatelliteCount(), trackInfo.getSatelliteCount()), Html.FROM_HTML_MODE_LEGACY);
            }
            else if (type == 2)
            {
                return Html.fromHtml(getResources().getString(R.string.msg_gps_detail_bar, duration, trackInfo.getLength(), lng, lat, alt, speed, speedKm, bearing, bearingDesc), Html.FROM_HTML_MODE_LEGACY);
            }
            else
            {
                return "未知类型";
            }
        }
        return "暂无位置信息";
    }

    private String angle2Direction(double angle)
    {
        angle += 11.25;
        String[] types = new String[]{"北", "北偏东", "东北", "东偏北", "东", "东偏南", "东南", "南偏东", "南", "南偏西", "西南", "西偏南", "西", "西偏北", "西北", "北偏西", "北"};
        return types[(int) (angle / 22.5)];
    }

    private void topBarAnimation(double direction)
    {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 92f, getResources().getDisplayMetrics())
                + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, getResources().getDisplayMetrics());
        ValueAnimator anim = ValueAnimator.ofInt(topBar.getMeasuredHeight(), (int) (direction * px));
        anim.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = topBar.getLayoutParams();
            layoutParams.height = val;
            topBar.setLayoutParams(layoutParams);
        });
        anim.setDuration(Config.getInstance().animationDuration);
        anim.start();
    }

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

    private void updateScale()
    {
        try
        {
            double value = MapViewHelper.getInstance().getMapView().getMapScale();
            if (value > 10000)
            {
                tvwScale.setText(getResources().getString(R.string.scale_small, value / 10000));
            }
            else
            {
                tvwScale.setText(getResources().getString(R.string.scale_large, value));

            }
        }
        catch (Exception ex)
        {

        }
    }
}
