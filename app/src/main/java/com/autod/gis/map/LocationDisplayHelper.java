package com.autod.gis.map;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.autod.gis.data.Config;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.location.AndroidLocationDataSource;
import com.esri.arcgisruntime.location.LocationDataSource;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;

public class LocationDisplayHelper
{
    private static LocationDisplayHelper instance = new LocationDisplayHelper();
    public LocationDisplay locationDisplay;
    private GpsLocationDataSource s;
    private AndroidLocationDataSource a;

    public static LocationDisplayHelper getInstance()
    {
        return instance;
    }

    private boolean confirmLocationOn(Context context)
    {
        if (MapViewHelper.getInstance().getMapView() == null)
        {
            return false;
        }
        locationDisplay = MapViewHelper.getInstance().getMapView().getLocationDisplay();
        if (locationDisplay.getLocationDataSource() instanceof AndroidLocationDataSource)
        {
            a = (AndroidLocationDataSource) locationDisplay.getLocationDataSource();
        }
        resetLocationDataSource(context);
        locationDisplay.setWanderExtentFactor(0f);//设置Pan为置中时，画面将实时跟随定位点移动
        return true;
    }

    public void resetLocationDataSource(Context context)
    {
        LocationDataSource d;
        if (Config.getInstance().useGpsLocationDataSource)
        {
            if (s == null)
            {
                s = new GpsLocationDataSource(context);
            }
            d = s;
        }
        else
        {
            d = a;
        }
        if (locationDisplay.getLocationDataSource() != d)
        {
            locationDisplay.setLocationDataSource(d);
        }
    }

    public void start(Context context)
    {
        if (confirmLocationOn(context) && !locationDisplay.isStarted())
        {
            locationDisplay.startAsync();
        }
    }

    public void stop(Context context)
    {
        if (confirmLocationOn(context) && locationDisplay.isStarted())
        {
            locationDisplay.stop();
        }
    }

    public boolean setPan(Context context)
    {

        if (confirmLocationOn(context))
        {
            locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
            return true;
        }
        return false;
    }

    public void showPanModeDialog(Context context)
    {
        final String[] items = {"不跟随", "置中", "导航", "指南针"};
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(context);
        listDialog.setTitle("罗盘模式");

        listDialog.setItems(items, (DialogInterface dialog, int which) ->
        {
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

    static class GpsLocationDataSource extends LocationDataSource
    {
        private LocationManager locationManager;
        private android.location.Location lastLocation;
        private Context context;
        private int fixedCount = 0;
        private LocationListener listener = new LocationListener()
        {
            @Override
            public void onLocationChanged(android.location.Location location)
            {
                lastLocation = location;
                update(fixedCount < Config.getInstance().gpsLocationDataSourceMinFixedSatelliteCount);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
                Toast.makeText(context, String.valueOf(status), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderEnabled(String provider)
            {

            }

            @Override
            public void onProviderDisabled(String provider)
            {

            }
        };
        private GnssStatus.Callback gnssCallback = new GnssStatus.Callback()
        {
            @Override
            public void onSatelliteStatusChanged(GnssStatus status)
            {
                int count = status.getSatelliteCount();
                int fixed = 0;
                for (int i = 0; i < count; i++)
                {
                    if (status.usedInFix(i))
                    {
                        fixed++;
                    }
                }
                fixedCount = fixed;
                update(fixed < Config.getInstance().gpsLocationDataSourceMinFixedSatelliteCount);
                super.onSatelliteStatusChanged(status);
            }
        };

        public GpsLocationDataSource(Context context)
        {
            this.context = context;
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        private void update(boolean lastKnown)
        {
            if (lastLocation == null)
            {
                return;
            }
            LocationDataSource.Location esriLocation = new com.esri.arcgisruntime.location.LocationDataSource.Location(
                    new Point(lastLocation.getLongitude(), lastLocation.getLatitude(), lastLocation.getAltitude(), SpatialReferences.getWgs84()),
                    lastLocation.getAccuracy(),
                    lastLocation.getSpeed(),
                    lastLocation.getBearing(),
                    lastKnown
            );
            updateLocation(esriLocation);
        }

        @Override
        protected void onStart()
        {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }

            Looper.prepare();
            locationManager.registerGnssStatusCallback(gnssCallback);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    Config.getInstance().gpsMinTime, Config.getInstance().gpsMinDistance, listener, Looper.getMainLooper());
            onStartCompleted(null);
            Looper.loop();
        }

        @Override
        protected void onStop()
        {
            locationManager.removeUpdates(listener);
            locationManager.unregisterGnssStatusCallback(gnssCallback);
        }

    }
}
