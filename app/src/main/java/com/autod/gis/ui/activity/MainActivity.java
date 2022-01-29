package com.autod.gis.ui.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.autod.gis.ui.fragment.EditFragment;
import com.autod.gis.ui.fragment.FeatureAttributionTableFragment;
import com.autod.gis.ui.MenuHelper;
import com.autod.gis.R;
import com.autod.gis.map.TrackHelper;
import com.autod.gis.map.SensorHelper;
import com.esri.arcgisruntime.mapping.Viewpoint;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener
{
    private TextView tvwScale;
    private boolean initialized = false;
    private Timer trackInfoTimer = new Timer();
    private TextView tvwTrackInfo;
    private View topBar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        setTitle("GIS工具箱");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//设置透明状态栏
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //getWindow().setStatusBarColor(Color.TRANSPARENT);
        checkPermission();
    }

    /**
     * 初始化
     */
    private void Initialize()
    {

        MapViewHelper.getInstance().Initialize(this);
        initializeControls();
        MapViewHelper.getInstance().getMapView().addMapScaleChangedListener(mapScaleChangedEvent -> updateScale());
        LayerManager.getInstance().initialize(this);
        initializeTrack();
        initialized = true;
    }

    private void initializeTrack()
    {
        trackInfoTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                runOnUiThread(() -> {
                    tvwTrackInfo.setText(getLocationMessage(2));
                });
            }
        }, 0, 1000);
        if (TrackHelper.getInstance().getStatus() == TrackHelper.Status.Running)
        {
            topBarAnimation(1);
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
                    LocationDisplayHelper.getInstance().start();
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
        if(initialized)
        {
            Config.getInstance().lastExtent = MapViewHelper.getInstance().getMapView().getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY).getTargetGeometry().toJson();
            Config.getInstance().trySave();
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
                    LocationDisplayHelper.getInstance().stop();
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
            LocationDisplayHelper.getInstance().stop();
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

        tvwTrackInfo = findViewById(R.id.main_tvw_track_info);
        topBar = findViewById(R.id.main_llt_top);
    }
    public final static int BaseLayerListActivityID =1;
    public final static int ImportFilesActivityID =2;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            if (requestCode == ImportFilesActivityID)
            {
                String path = data.getStringExtra("path");
                LayerManager.getInstance().addLayer(this, path);
                Config.getInstance().trySave();
            }
            else  if(requestCode==BaseLayerListActivityID)
            {
                LayerManager.getInstance().resetLayers(this);
                Config.getInstance().trySave();
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
                if (!LocationDisplayHelper.getInstance().setPan())
                {
                    Toast.makeText(this, "还没有定位", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.main_btn_layer:
                intent = new Intent(this, LayerListActivity.class);
                startActivity(intent);
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
                MapViewHelper.getInstance().zoomToLayer(this, false);
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
                MenuHelper.getInstance().initialize(inflater, menu);
                popup.setOnMenuItemClickListener(item -> {
                    MenuHelper.getInstance().menuClick(MainActivity.this, item);
                    return true;
                });
                popup.show();
                break;
            case R.id.main_btn_track:
                changeTrackStatus();
                break;

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

    private void changeTrackStatus()
    {
        if (TrackHelper.getInstance().getStatus() == TrackHelper.Status.Stop)
        {
            if (TrackHelper.getInstance().start(this))
            {
                topBarAnimation(1);
            }
        }
        else if (TrackHelper.getInstance().getStatus() == TrackHelper.Status.Running)
        {

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("正在记录轨迹")
                    .setMessage(getLocationMessage(1))
                    .setPositiveButton("暂停", (d, which) -> {
                        TrackHelper.getInstance().pause(this);
                        topBarAnimation(0);
                    })
                    .setNegativeButton("停止", (d, which) -> {
                        TrackHelper.getInstance().stop(this);
                        topBarAnimation(0);
                    })
                    .setNeutralButton("取消", (d, which) -> {

                    }).create();
            showTrackDialog(dialog);
        }
        else if (TrackHelper.getInstance().getStatus() == TrackHelper.Status.Pausing)
        {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("暂停记录轨迹中")
                    .setMessage(getLocationMessage(1))
                    .setPositiveButton("继续", (d, which) -> {
                        TrackHelper.getInstance().resume(this);
                        topBarAnimation(1);
                    })
                    .setNegativeButton("停止", (d, which) -> {
                        TrackHelper.getInstance().stop(this);
                    })
                    .setNeutralButton("取消", (d, which) -> {

                    }).create();
            showTrackDialog(dialog);
        }
    }

    private void showTrackDialog(AlertDialog dialog)
    {
        AtomicBoolean showing = new AtomicBoolean(true);
        Thread t = new Thread(() -> {
            while (showing.get())
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (Exception ex)
                {
                }
                runOnUiThread(() -> {
                            if (dialog.isShowing())
                            {
                                dialog.setMessage(getLocationMessage(1));
                            }
                        }
                );
            }
        });
        dialog.setOnDismissListener(dialog1 -> showing.set(false));
        t.start();
        dialog.show();
    }

    private CharSequence getLocationMessage(int type)
    {
        Location loc = TrackHelper.getInstance().getLastLocation();
        if (loc != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss", Locale.CHINA);
            String time = sdf.format(new Date(loc.getTime()));
Date diff=new Date(System.currentTimeMillis()-TrackHelper.getInstance().getStartTime().getTime());
            String duration=sdf.format(diff);
            int count = TrackHelper.getInstance().getCount();
            double length = TrackHelper.getInstance().getLength();
            double lng = loc.getLongitude();
            double lat = loc.getLatitude();
            double gpsAlt = loc.getAltitude();
            double pAlt = SensorHelper.getInstance() == null ? Double.NaN : SensorHelper.getInstance().getCurrentAltitude();
            double alt = Config.getInstance().useBarometer && !Double.isNaN(pAlt) ? pAlt : gpsAlt;
            double speed = loc.getSpeed();
            double speedKm = speed * 3.6;
            double bearing = loc.getBearing();
            String bearingDesc = angle2Direction(bearing);
            double hAcc = loc.getAccuracy();
            double vAcc = Double.NaN;
            double sAcc = Double.NaN;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            {
                vAcc = loc.getVerticalAccuracyMeters();
                sAcc = loc.getSpeedAccuracyMetersPerSecond();
            }
            if (type == 1)
            {
                return Html.fromHtml(getResources().getString(R.string.msg_gps_detail, time, count, length, lng, lat, gpsAlt, pAlt, speed, speedKm, bearing, bearingDesc, hAcc, vAcc, sAcc), Html.FROM_HTML_MODE_LEGACY);
            }
            else if (type == 2)
            {
                return Html.fromHtml(getResources().getString(R.string.msg_gps_detail_bar, duration, length, lng, lat, alt, speed, speedKm, bearing, bearingDesc), Html.FROM_HTML_MODE_LEGACY);

            }
            else
            {
                return "未知";
            }
        }
        return Html.fromHtml("暂无位置信息", Html.FROM_HTML_MODE_LEGACY);
    }

    private String angle2Direction(double angle)
    {
        angle += 22.5;
        String[] types = new String[]{"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        return types[(int) (angle / 45)];
    }

    private void topBarAnimation(int direction)
    {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 92f, getResources().getDisplayMetrics())
                + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, getResources().getDisplayMetrics());
        ValueAnimator anim = ValueAnimator.ofInt(topBar.getMeasuredHeight(), direction * (int) px);
        anim.addUpdateListener(valueAnimator -> {
            int val = (Integer) valueAnimator.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = topBar.getLayoutParams();
            layoutParams.height = val;
            topBar.setLayoutParams(layoutParams);
        });
        anim.setDuration(Config.getInstance().animationDuration);
        anim.start();
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


    public static void showDrawBar(@NonNull View rootView, String message, @NonNull Runnable onOkClickListener, @Nullable Runnable onCancelClickListener)
    {
        TextView tvw = rootView.findViewById(R.id.main_tvw_draw);
        TextView btnBarOk = rootView.findViewById(R.id.main_tvw_draw_ok);
        TextView btnBarCancel = rootView.findViewById(R.id.main_tvw_draw_cancel);
        View bar = rootView.findViewById(R.id.main_llt_draw_bar);
        btnBarOk.setEnabled(true);
        btnBarCancel.setEnabled(true);
        tvw.setText(message);
        btnBarOk.setOnClickListener(v -> {
            btnBarOk.setEnabled(false);
            btnBarCancel.setEnabled(false);
            ObjectAnimator.ofFloat(bar, "translationY", 0).setDuration(Config.getInstance().animationDuration).start();
            onOkClickListener.run();
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
}
