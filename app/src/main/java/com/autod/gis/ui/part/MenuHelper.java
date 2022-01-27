package com.autod.gis.ui.part;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.autod.gis.map.LayerManager;
import com.autod.gis.map.LocationDisplayHelper;
import com.autod.gis.map.MapViewHelper;
import com.autod.gis.map.TrackHelper;
import com.autod.gis.ui.activity.MainActivity;
import com.autod.gis.ui.dialog.DialogHelper;

import static com.autod.gis.ui.dialog.DialogHelper.showSetValueDialog;

public class MenuHelper
{


    private MenuItem menuRotate;
    private MenuItem menuLocation;
    private MenuItem menuCenterWhenRecording;
    private MenuItem menuKeepLocationBackground;
    private MenuItem menuFeatureLayerQueryExtentEveryTime;
    private MenuItem menuSideButtonsRight;
    private MenuItem menuMapCompass;
    private MenuItem menuUseBarometer;
    private MenuItem menuUseRelativeAltitude;


    private static MenuHelper instance = new MenuHelper();

    public static MenuHelper getInstance()
    {
        return instance;
    }

    public void initialize(MenuInflater menuInflater, Menu menu)
    {
        menuInflater.inflate(R.menu.menu, menu);
        menuRotate = menu.findItem(R.id.menu_rotate);

        menuLocation = menu.findItem(R.id.menu_location_display);

        menuCenterWhenRecording = menu.findItem(R.id.menu_center_when_recording);

        menuKeepLocationBackground = menu.findItem(R.id.menu_keep_location_display_background);

        menuFeatureLayerQueryExtentEveryTime = menu.findItem(R.id.menu_feature_layer_query_extent_every_time);

        menuSideButtonsRight = menu.findItem(R.id.menu_side_buttons_right);

        menuMapCompass = menu.findItem(R.id.menu_compass);

        menuUseBarometer = menu.findItem(R.id.menu_use_barometer);

        menuUseRelativeAltitude = menu.findItem(R.id.menu_use_relative_altitude);

        resetValues();
    }

    public void resetValues()
    {
        menuRotate.setChecked(Config.getInstance().canRotate);
        menuLocation.setChecked(Config.getInstance().location);
        menuCenterWhenRecording.setChecked(Config.getInstance().autoCenterWhenRecording);
        menuKeepLocationBackground.setChecked(Config.getInstance().keepLocationBackground);
        menuFeatureLayerQueryExtentEveryTime.setChecked(Config.getInstance().featureLayerQueryExtentEveryTime);
        menuSideButtonsRight.setChecked(Config.getInstance().sideButtonsRight);
        menuMapCompass.setChecked(Config.getInstance().showMapCompass);
        menuUseBarometer.setChecked(Config.getInstance().useBarometer);
        menuUseRelativeAltitude.setChecked(Config.getInstance().useRelativeAltitude);

    }

    public void menuClick(Activity context, MenuItem item)
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
                MapViewHelper.getInstance().imgMapCompass.setVisibility(Config.getInstance().showMapCompass ? View.VISIBLE : View.INVISIBLE);
                MapViewHelper.getInstance().setMapCompass();
                break;
            case R.id.menu_side_buttons_right:
                Config.getInstance().sideButtonsRight = !Config.getInstance().sideButtonsRight;
                menuSideButtonsRight.setChecked(Config.getInstance().sideButtonsRight);
                ((MainActivity)context).setSideButtonPosition();
                break;
            case R.id.menu_location_display:
                Config.getInstance().location = !Config.getInstance().location;
                menuLocation.setChecked(Config.getInstance().location);
                if (Config.getInstance().location)
                {
                    LocationDisplayHelper.instance.start();
                }
                else
                {
                    LocationDisplayHelper.instance.stop();
                }
                break;
            case R.id.menu_tile_url:
                DialogHelper.showSetValueDialog(context,
                        "设置底图地址",
                        "请输入一行一个地址，以行分隔，自动忽略空行",
                        TextUtils.join("\n\n", Config.getInstance().baseUrls),
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                        p ->
                        {
                            Config.getInstance().baseUrls.clear();
                            int count = 0;
                            for (String url : p.split("\n"))
                            {
                                if (url.length() > 0)
                                {
                                    Config.getInstance().baseUrls.add(url);
                                    count++;
                                }
                            }
                            Config.getInstance().save();
                            Toast.makeText(context, "成功获取" + count + "条瓦片地址", Toast.LENGTH_SHORT).show();
                            LayerManager.getInstance().resetLayers(context);
                        });
                break;
            case R.id.menu_load_esri_map:
                LayerManager.getInstance().loadEsriLayer(context);
                break;
            case R.id.menu_create_feature_layer:
                LayerManager.getInstance().createFeatureLayer(context);
                break;
            case R.id.menu_about:
                new AlertDialog.Builder(context)
                        .setTitle("关于")
                        .setPositiveButton("确定", (dialog, which) -> {
                        })
                        .setMessage(new String(Base64.decode("YXV0b2RvdHVh", Base64.DEFAULT)))
                        .create().show();

                break;
            case R.id.menu_default_scale:
                showSetValueDialog(context, "默认比例尺",
                        "地图默认的比例尺",
                        String.valueOf(Config.getInstance().gpsMinTime), InputType.TYPE_CLASS_NUMBER, p ->
                        {
                            try
                            {
                                double scale = Double.parseDouble(p);
                                if (scale <= 0)
                                {
                                    Toast.makeText(context, "比例尺不可小于0", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Config.getInstance().defaultScale = scale;
                                Config.getInstance().save();
                                Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show();

                            }
                            catch (Exception ex)
                            {
                                Toast.makeText(context, "设置失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case R.id.menu_gps_min_time:
                showSetValueDialog(context, "GPS最小更新时间",
                        "设置GPS最小更新的时间间隔，当位置更新时，获取两个位置之间的最小时间。单位为毫秒",
                        String.valueOf(Config.getInstance().gpsMinTime), InputType.TYPE_CLASS_NUMBER, p ->
                        {
                            try
                            {
                                int time = Integer.parseInt(p);
                                if (time <= 0)
                                {
                                    Toast.makeText(context, "最小时间不可小于1", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Config.getInstance().gpsMinTime = time;
                                Config.getInstance().save();
                                Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show();
                            }
                            catch (Exception ex)
                            {
                                Toast.makeText(context, "设置失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case R.id.menu_gps_min_distance:
                showSetValueDialog(context, "GPS最小更新距离",
                        "设置GPS最小更新的距离间隔，当位置更新时，仅当于上一个点距离超过该值时才会更新",
                        String.valueOf(Config.getInstance().gpsMinDistance), InputType.TYPE_CLASS_NUMBER, p ->
                        {
                            try
                            {
                                int time = Integer.parseInt(p);
                                if (time <= 0)
                                {
                                    Toast.makeText(context, "最小距离不可小于1米", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Config.getInstance().gpsMinDistance = time;
                                Config.getInstance().save();
                                Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show();
                            }
                            catch (Exception ex)
                            {
                                Toast.makeText(context, "设置失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            case R.id.menu_exit:
                if (TrackHelper.getInstance().getStatus() != TrackHelper.Status.NotRunning)
                {
                    TrackHelper.getInstance().stop(context);
                }
                context.finish();
                break;
            default:
                break;
        }
        Config.getInstance().save();
    }
}
