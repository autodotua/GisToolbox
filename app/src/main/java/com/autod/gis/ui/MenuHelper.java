package com.autod.gis.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.text.InputType;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.autod.gis.map.LayerManager;
import com.autod.gis.map.LocationDisplayHelper;
import com.autod.gis.map.MeasureHelper;
import com.autod.gis.programming.GetString;
import com.autod.gis.service.TrackService;
import com.autod.gis.ui.activity.BaseLayerListActivity;
import com.autod.gis.ui.activity.MainActivity;

import static com.autod.gis.ui.activity.MainActivity.BaseLayerListActivityID;

public class MenuHelper
{
    private MenuItem menuRotate;
    private MenuItem menuLocation;
    private MenuItem menuCenterWhenRecording;
    private MenuItem menuKeepLocationBackground;
    private MenuItem menuFeatureLayerQueryExtentEveryTime;
    private MenuItem menuMapCompass;
    private MenuItem menuUseBarometer;
    private MenuItem menuUseRelativeAltitude;
    private MenuItem menuUseGpsLocationDataSource;
    private MenuItem menuScreenOn;

    public static void showSetValueDialog(Context context, String title, String message, String value, int type, GetString r)
    {
        final EditText editText = new EditText(context);
        editText.setInputType(type);
        editText.setText(value);
        editText.setSingleLine(false);
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> r.get(editText.getText().toString()))
                .create().show();
    }

    public void initialize(MenuInflater menuInflater, Menu menu)
    {
        menuInflater.inflate(R.menu.menu, menu);
        menuRotate = menu.findItem(R.id.menu_rotate);

        menuLocation = menu.findItem(R.id.menu_show_location_display);

        menuCenterWhenRecording = menu.findItem(R.id.menu_center_when_recording);

        menuKeepLocationBackground = menu.findItem(R.id.menu_keep_location_display_background);

        menuFeatureLayerQueryExtentEveryTime = menu.findItem(R.id.menu_feature_layer_query_extent_every_time);

        menuMapCompass = menu.findItem(R.id.menu_compass);

        menuUseBarometer = menu.findItem(R.id.menu_use_barometer);

        menuUseRelativeAltitude = menu.findItem(R.id.menu_use_relative_altitude);

        menuUseGpsLocationDataSource = menu.findItem(R.id.menu_use_gps_data_source);

        menuScreenOn = menu.findItem(R.id.menu_screen_on);

        resetValues();
    }

    public void resetValues()
    {
        menuRotate.setChecked(Config.getInstance().canRotate);
        menuLocation.setChecked(Config.getInstance().location);
        menuCenterWhenRecording.setChecked(Config.getInstance().autoCenterWhenRecording);
        menuKeepLocationBackground.setChecked(Config.getInstance().keepLocationBackground);
        menuFeatureLayerQueryExtentEveryTime.setChecked(Config.getInstance().featureLayerQueryExtentEveryTime);
        menuMapCompass.setChecked(Config.getInstance().showMapCompass);
        menuUseBarometer.setChecked(Config.getInstance().useBarometer);
        menuUseRelativeAltitude.setChecked(Config.getInstance().useRelativeAltitude);
        menuUseGpsLocationDataSource.setChecked(Config.getInstance().useGpsLocationDataSource);
        menuScreenOn.setChecked(Config.getInstance().screenAlwaysOn);
    }

    public void menuClick(MainActivity activity, MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_rotate:
                Config.getInstance().canRotate = !Config.getInstance().canRotate;
                menuRotate.setChecked(Config.getInstance().canRotate);
                break;
            case R.id.menu_center_when_recording:
                Config.getInstance().autoCenterWhenRecording = !Config.getInstance().autoCenterWhenRecording;
                menuCenterWhenRecording.setChecked(Config.getInstance().autoCenterWhenRecording);
                break;
            case R.id.menu_keep_location_display_background:
                Config.getInstance().keepLocationBackground = !Config.getInstance().keepLocationBackground;
                menuKeepLocationBackground.setChecked(Config.getInstance().keepLocationBackground);
                break;
            case R.id.menu_use_barometer:
                Config.getInstance().useBarometer = !Config.getInstance().useBarometer;
                menuUseBarometer.setChecked(Config.getInstance().useBarometer);
                break;
            case R.id.menu_use_relative_altitude:
                Config.getInstance().useRelativeAltitude = !Config.getInstance().useRelativeAltitude;
                menuUseRelativeAltitude.setChecked(Config.getInstance().useRelativeAltitude);
                break;
            case R.id.menu_feature_layer_query_extent_every_time:
                Config.getInstance().featureLayerQueryExtentEveryTime = !Config.getInstance().featureLayerQueryExtentEveryTime;
                menuFeatureLayerQueryExtentEveryTime.setChecked(Config.getInstance().featureLayerQueryExtentEveryTime);
                break;
            case R.id.menu_compass:
                Config.getInstance().showMapCompass = !Config.getInstance().showMapCompass;
                menuMapCompass.setChecked(Config.getInstance().showMapCompass);
                Toast.makeText(activity, "设置成功，将在下次启动时应用", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_show_location_display:
                Config.getInstance().location = !Config.getInstance().location;
                menuLocation.setChecked(Config.getInstance().location);
                if (Config.getInstance().location)
                {
                    LocationDisplayHelper.getInstance().start(activity);
                }
                else
                {
                    LocationDisplayHelper.getInstance().stop(activity);
                }
                break;
            case R.id.menu_tile_url:
                activity.startActivityForResult(new Intent(activity, BaseLayerListActivity.class), BaseLayerListActivityID);
                break;
            case R.id.menu_create_feature_layer:
                LayerManager.getInstance().createFeatureLayer(activity);
                break;
            case R.id.menu_about:
                new AlertDialog.Builder(activity)
                        .setTitle("关于")
                        .setPositiveButton("确定", (dialog, which) -> {
                        })
                        .setMessage(new String(Base64.decode("YXV0b2RvdHVh", Base64.DEFAULT)))
                        .create().show();

                break;
            case R.id.menu_default_scale:
                showSetValueDialog(activity, "默认比例尺",
                        "地图默认的比例尺",
                        String.valueOf(Config.getInstance().gpsMinTime), InputType.TYPE_CLASS_NUMBER, p ->
                        {
                            try
                            {
                                double scale = Double.parseDouble(p);
                                if (scale <= 0)
                                {
                                    Toast.makeText(activity, "比例尺不可小于0", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Config.getInstance().defaultScale = scale;
                                Config.getInstance().save();
                                Toast.makeText(activity, "设置成功", Toast.LENGTH_SHORT).show();

                            }
                            catch (Exception ex)
                            {
                                Toast.makeText(activity, "设置失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case R.id.menu_gps_min_time:
                showSetValueDialog(activity, "GPS最小更新时间",
                        "设置GPS最小更新的时间间隔，当位置更新时，获取两个位置之间的最小时间。单位为毫秒",
                        String.valueOf(Config.getInstance().gpsMinTime), InputType.TYPE_CLASS_NUMBER, p ->
                        {
                            try
                            {
                                int time = Integer.parseInt(p);
                                if (time <= 0)
                                {
                                    Toast.makeText(activity, "最小时间不可小于1", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Config.getInstance().gpsMinTime = time;
                                Config.getInstance().save();
                                Toast.makeText(activity, "设置成功", Toast.LENGTH_SHORT).show();
                            }
                            catch (Exception ex)
                            {
                                Toast.makeText(activity, "设置失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case R.id.menu_gps_min_distance:
                showSetValueDialog(activity, "GPS最小更新距离",
                        "设置GPS最小更新的距离间隔，当位置更新时，仅当于上一个点距离超过该值时才会更新",
                        String.valueOf(Config.getInstance().gpsMinDistance), InputType.TYPE_CLASS_NUMBER, p ->
                        {
                            try
                            {
                                int time = Integer.parseInt(p);
                                if (time <= 0)
                                {
                                    Toast.makeText(activity, "最小距离不可小于1米", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Config.getInstance().gpsMinDistance = time;
                                Config.getInstance().save();
                                Toast.makeText(activity, "设置成功", Toast.LENGTH_SHORT).show();
                            }
                            catch (Exception ex)
                            {
                                Toast.makeText(activity, "设置失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
                case R.id.menu_navigation_point_height_factor:
                showSetValueDialog(activity, "导航模式图标位置",
                        "设置定位点导航模式下，距离屏幕下边缘与屏幕高度的比值",
                        String.valueOf(Config.getInstance().navigationPointHeightFactor), InputType.TYPE_CLASS_NUMBER, p ->
                        {
                            try
                            {
                                float value = Float.parseFloat(p);
                                if (value <= 0||value>=1)
                                {
                                    Toast.makeText(activity, "值需要介于0和1之间", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Config.getInstance().navigationPointHeightFactor = value;
                                Config.getInstance().save();
                                Toast.makeText(activity, "设置成功", Toast.LENGTH_SHORT).show();
                            }
                            catch (Exception ex)
                            {
                                Toast.makeText(activity, "设置失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case R.id.menu_use_gps_data_source:
                Config.getInstance().useGpsLocationDataSource = !Config.getInstance().useGpsLocationDataSource;
                menuUseGpsLocationDataSource.setChecked(Config.getInstance().useGpsLocationDataSource);
                if (Config.getInstance().location)
                {
                    LocationDisplayHelper.getInstance().resetLocationDataSource(activity);
                }
                break;
            case R.id.menu_gps_data_source_min_fixed_satellite_count:
                showSetValueDialog(activity, activity.getString(R.string.menu_gps_data_source_min_fixed_satellite_count),
                        "少于该数量的卫星被确定位置时，将不认可当前定位",
                        String.valueOf(Config.getInstance().gpsLocationDataSourceMinFixedSatelliteCount), InputType.TYPE_CLASS_NUMBER, p ->
                        {
                            try
                            {
                                int value = Integer.parseInt(p);
                                if (value <= 0)
                                {
                                    Toast.makeText(activity, "卫星数量不可小于0", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (value > 20)
                                {
                                    Toast.makeText(activity, "卫星数量不可大于20", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Config.getInstance().gpsLocationDataSourceMinFixedSatelliteCount = value;
                                Config.getInstance().save();
                                Toast.makeText(activity, "设置成功", Toast.LENGTH_SHORT).show();

                            }
                            catch (Exception ex)
                            {
                                Toast.makeText(activity, "设置失败，请输入一个整数", Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case R.id.menu_exit:
                if (activity.getTrackService() == null)
                {
                    activity.finish();
                }
                else
                {
                    new AlertDialog.Builder(activity)
                            .setTitle("退出")
                            .setMessage("正在记录轨迹，是否停止？")
                            .setPositiveButton("否", (d, w) -> {
                                activity.finish();
                            })
                            .setNegativeButton("是", (d, w) -> {
                                activity.stopTrack(true);
                                activity.finish();
                            }).create().show();
                }
                break;
            case R.id.menu_reset:
                LayerManager.getInstance().resetLayers(activity);
                break;
            case R.id.menu_measure_length:
                MeasureHelper.MeasureLength(activity.findViewById(android.R.id.content));
                break;
            case R.id.menu_measure_area:
                MeasureHelper.MeasureArea(activity.findViewById(android.R.id.content));
                break;
            case R.id.menu_screen_on:
                Config.getInstance().screenAlwaysOn = !Config.getInstance().screenAlwaysOn;
                menuScreenOn.setChecked(Config.getInstance().screenAlwaysOn);
                activity.findViewById(R.id.main_map).setKeepScreenOn(Config.getInstance().screenAlwaysOn);
                break;
            default:
                break;
        }
        Config.getInstance().save();
    }
}
