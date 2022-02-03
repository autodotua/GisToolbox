package com.autod.gis.model;

import android.location.Location;

import java.util.Date;

public class TrackInfo
{
    private Date startTime;
    private Location location;
    private int count;
    private double length;
    private int satelliteCount;
    private int fixedSatelliteCount;

    public TrackInfo(Date startTime, Location location, int count, double length, int satelliteCount, int fixedSatelliteCount)
    {
        this.startTime = startTime;
        this.location = location;
        this.count = count;
        this.length = length;
        this.satelliteCount = satelliteCount;
        this.fixedSatelliteCount = fixedSatelliteCount;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public Location getLocation()
    {
        return location;
    }

    public int getCount()
    {
        return count;
    }

    public double getLength()
    {
        return length;
    }

    public int getSatelliteCount()
    {
        return satelliteCount;
    }

    public int getFixedSatelliteCount()
    {
        return fixedSatelliteCount;
    }
}
