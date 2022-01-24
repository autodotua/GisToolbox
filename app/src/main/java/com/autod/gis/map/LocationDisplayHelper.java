package com.autod.gis.map;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;

import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.autod.gis.ui.activity.MainActivity;

public class LocationDisplayHelper
{
    public static LocationDisplayHelper instance = new LocationDisplayHelper();

    public LocationDisplay locationDisplay;


    private boolean confirmLocationOn()
    {
        if (locationDisplay == null)
        {
            if (MapViewHelper.getInstance().mapView == null)
            {
                return false;
            }
            locationDisplay = MapViewHelper.getInstance().mapView.getLocationDisplay();

        }
        return true;
    }

    public void start()
    {
        if (confirmLocationOn() && !locationDisplay.isStarted())
        {
            locationDisplay.startAsync();
        }
    }

    public void stop()
    {
        if (confirmLocationOn() && locationDisplay.isStarted())
        {
            locationDisplay.stop();
        }
    }

    public boolean setPan()
    {

//        if (locationDisplay.getAutoPanMode() == LocationDisplay.AutoPanMode.OFF || locationDisplay.getAutoPanMode() == LocationDisplay.AutoPanMode.COMPASS_NAVIGATION)
//        {

        if (confirmLocationOn())
        {
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            //MapViewHelper.getInstance().mapView.setViewpointScaleAsync(Config.getInstance().defaultScale).addDoneListener(() -> MainActivity.getInstance().setScaleText(Config.getInstance().defaultScale));
            return true;
        }
        return false;
//        }
//        else
//        {
//            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
//        }
    }

    public void showPanModeDialog()
    {
        final String[] items = {"普通", "置中", "导航", "指南针"};
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(MainActivity.getInstance());
        listDialog.setTitle("罗盘模式");
       //Toast.makeText(MainActivity.getInstance(), String.valueOf(locationDisplay.getAutoPanMode()), Toast.LENGTH_SHORT).show();

        listDialog.setItems(items, (DialogInterface dialog, int which) ->
        {
            // which 下标从0开始
            // ...To-do
            switch (which)
            {
                case 0:
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.OFF);
                    break;
                case 1:
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
                    break;
                case 2:
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
                    break;
                case 3:
                    locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
                    break;
            }

        });
        listDialog.show();

    }

    private boolean isGpsAble(LocationManager lm)
    {
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    //打开设置页面让用户自己设置
    private void openGPS()
    {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        MainActivity.getInstance().startActivityForResult(intent, 0);
    }



}
