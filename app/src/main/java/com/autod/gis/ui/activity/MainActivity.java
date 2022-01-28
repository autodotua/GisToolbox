package com.autod.gis.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.autod.gis.ui.activity.ImportFilesActivity.ImportFilesActivityID;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener
{
    //private ImageView imgMapCompass;

    private ImageView imgSatellite;
    private TextView tvwScale;
    private LinearLayout lltSideButtons;
    private boolean initialized = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        setTitle("GIS工具箱");
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
        MapViewHelper.getInstance().mapView.addMapScaleChangedListener(mapScaleChangedEvent -> updateScale());
        LayerManager.getInstance().initialize(this);
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


        ((EditFragment) getSupportFragmentManager().findFragmentById(R.id.main_fgm_edit)).initialize(this);

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

        imgSatellite = findViewById(R.id.main_img_satellite);


        lltSideButtons = findViewById(R.id.main_llt_side_buttons);
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
                LayerManager.getInstance().addLayer(this, path);
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
                LocationDisplayHelper.instance.showPanModeDialog(this);
                break;
            case R.id.main_btn_zoom_in:
                try
                {
                    scale = MapViewHelper.getInstance().mapView.getMapScale();
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
                startActivityForResult(intent, ImportFilesActivityID);
                break;
            case R.id.main_btn_pan:
                if (!Config.getInstance().location)
                {
                    Toast.makeText(this, "没有开启定位功能", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!LocationDisplayHelper.instance.setPan())
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
                    scale = MapViewHelper.getInstance().mapView.getMapScale();
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
                if (eggClickTimes++ == 20)
                {
                    Toast.makeText(this, new String(Base64.decode("YXV0b2RvdHVh", Base64.DEFAULT)), Toast.LENGTH_SHORT).show();
                }
                MapViewHelper.getInstance().zoomToLayer(this, false);
                break;
            case R.id.main_btn_table:
                ((FeatureAttributionTableFragment) getSupportFragmentManager().findFragmentById(R.id.main_fgm_attri)).foldOrUnfold();
                break;
            case R.id.main_btn_edit:
                ((EditFragment) getSupportFragmentManager().findFragmentById(R.id.main_fgm_edit)).foldOrUnfold(this);
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
    }

    private void changeTrackStatus()
    {
        if (TrackHelper.getInstance().getStatus() == TrackHelper.Status.NotRunning)
        {
            if (TrackHelper.getInstance().start(this))
            {
                setTrackIcon(true);
            }
        }
        else if (TrackHelper.getInstance().getStatus() == TrackHelper.Status.Running)
        {

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("正在记录轨迹")
                    .setMessage(getLocationMessage())
                    .setPositiveButton("暂停", (d, which) -> {
                        TrackHelper.getInstance().pause(this);
                        setTrackIcon(false);
                    })
                    .setNegativeButton("停止", (d, which) -> {
                        TrackHelper.getInstance().stop(this);
                        setTrackIcon(false);
                    })
                    .setNeutralButton("取消", (d, which) -> {

                    }).create();
            showTrackdialog(dialog);
        }
        else if (TrackHelper.getInstance().getStatus() == TrackHelper.Status.Pausing)
        {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("暂停记录轨迹中")
                    .setMessage(getLocationMessage())
                    .setPositiveButton("继续", (d, which) -> {
                        TrackHelper.getInstance().resume(this);
                        setTrackIcon(true);
                    })
                    .setNegativeButton("停止", (d, which) -> {
                        TrackHelper.getInstance().stop(this);
                    })
                    .setNeutralButton("取消", (d, which) -> {

                    }).create();
            showTrackdialog(dialog);
        }
    }

    private void showTrackdialog(AlertDialog dialog)
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
                                dialog.setMessage(getLocationMessage());
                            }
                        }
                );
            }
        });
        dialog.setOnDismissListener(dialog1 -> showing.set(false));
        t.start();
        dialog.show();
    }

    private String getLocationMessage()
    {
        Location loc = TrackHelper.getInstance().getLastLocation();
        if (loc != null)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
            String time = sdf.format(new Date(loc.getTime()));
            int count = TrackHelper.getInstance().getCount();
            double length = TrackHelper.getInstance().getLength();
            double lng = loc.getLongitude();
            double lat = loc.getLatitude();
            double alt = loc.getAltitude();
            double pAlt = SensorHelper.getInstance() == null ? Double.NaN : SensorHelper.getInstance().getCurrentAltitude();
            double speed = loc.getSpeed();
            double bearing = loc.getBearing();
            double hAcc = loc.getAccuracy();
            double vAcc = Double.NaN;
            double sAcc = Double.NaN;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            {
                vAcc = loc.getVerticalAccuracyMeters();
                sAcc = loc.getSpeedAccuracyMetersPerSecond();
            }
            return getResources().getString(R.string.msg_gps_detail, time, count, length, lng, lat, alt, pAlt, speed, bearing, hAcc, vAcc, sAcc);
        }
        return "暂无位置信息";
    }

    private void setTrackIcon(boolean on)
    {
        if (on)
        {
            Animation ani = new AlphaAnimation(0.1f, 1.0f);
            ani.setDuration(2000);
            ani.setRepeatMode(Animation.REVERSE);
            ani.setRepeatCount(Animation.INFINITE);
            imgSatellite.setVisibility(View.VISIBLE);
            imgSatellite.setAnimation(ani);
        }
        else
        {
            imgSatellite.clearAnimation();
            imgSatellite.setVisibility(View.GONE);
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


    private void updateScale()
    {
        try
        {
            double value = MapViewHelper.getInstance().mapView.getMapScale();
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
