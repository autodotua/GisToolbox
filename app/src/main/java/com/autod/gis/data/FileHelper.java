package com.autod.gis.data;

import android.os.Environment;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.GeometryType;
import com.autod.gis.ui.activity.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileHelper
{
    public static void writeTextToFile(String path, String value)
    {
        writeTextToFile(new File(path), value);
    }

    /**
     * 将文本文件写入到磁盘
     *
     * @param file
     * @param value
     * @throws IOException
     */
    public static void writeTextToFile(File file, String value)
    {
        try
        {
            if (!file.getParentFile().exists())
            {
                file.getParentFile().mkdirs();
            }
            OutputStream out = new FileOutputStream(file);

            Writer writer = new OutputStreamWriter(out);
            writer.write(value);

            writer.close();
        }
        catch (Exception ex)
        {

        }
    }

    /**
     * 读取文本文件
     *
     * @param file
     * @return
     */
    public static String readTextFile(String file)
    {
        return readTextFile(new File(file));
    }

    /**
     * 读取文本文件
     *
     * @param file
     * @return
     */
    public static String readTextFile(File file)
    {
        try
        {
            InputStream instream = new FileInputStream(file);
            InputStreamReader inputreader = new InputStreamReader(instream);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            StringBuilder result = new StringBuilder();
            //分行读取
            while ((line = buffreader.readLine()) != null)
            {
                result.append(line).append("\n");
            }
            instream.close();
            return result.toString();
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public static String getFilePath(String subPath)
    {
        if (subPath.startsWith("/"))
        {
            subPath = subPath.substring(1);
        }
        return Environment.getExternalStorageDirectory().toString() + "/GIS/" + subPath;
    }

    public static String getConfigPath(String name)
    {
        return getFilePath(name + ".json");
    }

    public static String getShapefileDirPath()
    {
        return getShapefilePath("", false);
    }

    public static String getShapefilePath(String name, boolean addDotShp)
    {
        if (addDotShp && !name.endsWith(".shp"))
        {
            name = name + ".shp";
        }
        return getFilePath("Shapefile/" + name);
    }

    public static String getBaseLayerPath(String name)
    {
        return getFilePath("Base/" + name);
    }

    public static String getConfigPath()
    {
        return getConfigPath("config");
    }

    public static String getConfigJson()
    {
        File file = new File(getConfigPath());
        if (file.exists())
        {
            return readTextFile(file);
        }
        else
        {
            return null;
        }
    }

    public static String getConfigJson(String name)
    {
        File file = new File(getConfigPath(name));
        if (file.exists())
        {
            return readTextFile(file);
        }
        else
        {
            return null;
        }
    }

    public static void setConfigJson(String json)
    {
        writeTextToFile(getConfigPath(), json);
    }

    public static void setConfigJson(String name, String json)
    {
        String path = getConfigPath(name);
        writeTextToFile(path, json);
    }

    public static String getEmptyShapefilesPath()
    {
        return getFilePath("EmptyShapefiles");
    }


    public static String getPolylineTrackFilePath(String name)
    {
        return getFilePath("Track/Shapefile/" + name);
    }

    public static String getGpxTrackFilePath(String name)
    {
        return getFilePath("Track/" + name);
    }

    public static String getStyleFile(String shapeFileName)
    {
        return getShapefilePath(shapeFileName.substring(0, shapeFileName.length() - 4) + ".style", false);
    }

    //文件拷贝
    //要复制的目录下的所有非子目录(文件夹)文件拷贝
    public static boolean copyFile(String fromFile, String toFile)
    {

        try
        {
            File file = new File(toFile);
            if (file.exists())
            {
                file.delete();
            }
            File directory = file.getParentFile();
            if (!directory.exists())
            {
                directory.mkdirs();
            }
            InputStream fosfrom = new FileInputStream(fromFile);
            OutputStream fosto = new FileOutputStream(toFile);
            byte bt[] = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0)
            {
                fosto.write(bt, 0, c);
            }
            fosfrom.close();
            fosto.close();
            return true;

        }
        catch (Exception ex)
        {
            return false;
        }
    }

    public static String createShapefile(GeometryType type, String targetPath) throws IOException
    {
        if (targetPath == null)
        {
            return null;
        }
        String strType = "";
        switch (type)
        {
            case POINT:
                strType = "Point";
                break;
            case POLYGON:
                strType = "Polygon";
                break;
            case POLYLINE:
                strType = "Polyline";
                break;
        }
        String[] files = new String[]{
                FileHelper.getEmptyShapefilesPath() + "/" + strType + ".shp",
                FileHelper.getEmptyShapefilesPath() + "/" + strType + ".prj",
                FileHelper.getEmptyShapefilesPath() + "/" + strType + ".dbf",
                FileHelper.getEmptyShapefilesPath() + "/" + strType + ".cpg",
                FileHelper.getEmptyShapefilesPath() + "/" + strType + ".shx",

        };

        for (String filePath : files)
        {
            File file = new File(filePath);
            if (!file.exists())
            {
                throw new IOException("空文件" + file.getName() + "不存在");
            }
        }
        for (String filePath : files)
        {
            if (!FileHelper.copyFile(filePath, targetPath + "." + filePath.split("\\.")[1]))
            {
                return null;
            }
        }
        return targetPath.split("\\.")[0] + ".shp";
    }

    public static String getTimeBasedFileName(Date time)
    {
        if (time == null)
        {
            time = new Date();
        }
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
        return dateTimeFormat.format(time);
    }
}
