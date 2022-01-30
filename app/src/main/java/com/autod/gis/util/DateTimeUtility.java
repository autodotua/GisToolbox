package com.autod.gis.util;

import java.util.Date;
import java.util.Locale;

public class DateTimeUtility
{
    public static String formatTimeSpan(Date from,Date to)
    {
        if(from.after(to))
        {
            Date temp=from;
            from=to;
            to=temp;
        }
        int totalS = (int) ((to.getTime()- from.getTime()) / 1000);
        int h = totalS / 3600;
        int m = (totalS - h * 3600) / 60;
        int s = totalS % 60;
        return String.format(Locale.CHINA,"%d:%02d:%02d",h,m,s);
    }

}
