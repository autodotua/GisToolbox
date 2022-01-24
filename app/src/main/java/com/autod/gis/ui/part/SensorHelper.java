package com.autod.gis.ui.part;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import com.autod.gis.data.Config;
import com.autod.gis.ui.activity.MainActivity;

import static android.content.Context.SENSOR_SERVICE;

public class SensorHelper implements SensorEventListener
{
    private static SensorHelper instance;

    public static SensorHelper getInstance()
    {
        if (instance == null)
        {
            instance = new SensorHelper();
        }
        return instance;
    }

    private static SensorManager sensorManager;

    public SensorHelper()
    {
        sensorManager = (SensorManager) MainActivity.getInstance().getSystemService(SENSOR_SERVICE);
    }

    public void stop()
    {
        sensorManager.unregisterListener(this);
    }

    private Float initialPressure = null;

    public boolean start()
    {
        Sensor pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressure == null)
        {
            Toast.makeText(MainActivity.getInstance(), "该设备不支持气压计", Toast.LENGTH_SHORT).show();
            return false;
        }
        initialPressure = null;
        sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_UI);
        return true;


    }

    /**
     * 指南针
     *
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event)
    {
//        if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION)
//        {
//            mapviewHelper.setCompass(sensorEvent);
//        }

        if (event.sensor.getType() == Sensor.TYPE_PRESSURE)
        {
            /*压力传感器返回当前的压强，单位是百帕斯卡hectopascal（hPa）。*/
            float pressure = event.values[0];

            if (Config.getInstance().useRelativeAltitude)
            {
                if (initialPressure == null)
                {
                    initialPressure = pressure;
                }
                currentAltitude = SensorManager.getAltitude(initialPressure, pressure);
            }
            else
            {
                currentAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure);
            }
        }
    }

    private double currentAltitude = 0;

    public double getCurrentAltitude()
    {
        return currentAltitude;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }
}
