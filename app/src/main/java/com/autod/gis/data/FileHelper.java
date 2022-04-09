package com.autod.gis.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import com.esri.arcgisruntime.geometry.GeometryType;

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
import java.nio.file.FileAlreadyExistsException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FileHelper
{
    public static final List<String> SupportedBaseLayerExtensions = new ArrayList<String>()
    {{
        add("tpk");
        add("jpg");
        add("jpeg");
        add("tif");
        add("tiff");
        add("png");
    }};

    public static void writeTextToFile(String path, String value)
    {
        writeTextToFile(new File(path), value);
    }

    /**
     * 将文本文件写入到磁盘
     *
     * @param file
     * @param value
     */
    public static void writeTextToFile(File file, String value)
    {
        //首先把文件写入到temp文件中，然后将原文件备份，之后将temp文件重命名为原文件，来防止写入时出错导致文件锁坏
        try
        {
            File tempFile = new File(file.getAbsolutePath() + ".temp"); //用于写入的临时文件
            File bakFile = new File(file.getAbsolutePath() + ".bak"); //用于备份的文件
            if (!Objects.requireNonNull(file.getParentFile()).exists())
            {
                file.getParentFile().mkdirs();
            }
            OutputStream out = new FileOutputStream(tempFile);
            Writer writer = new OutputStreamWriter(out);
            writer.write(value);
            writer.close();
            copy(file, bakFile);
            tempFile.renameTo(file);

        }
        catch (Exception ex)
        {

        }
    }

    public static void copy(File src, File dst) throws IOException
    {
        try (InputStream in = new FileInputStream(src))
        {
            try (OutputStream out = new FileOutputStream(dst))
            {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }
            }
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

    public static boolean createFolderIfNotExists(File folder)
    {
        if (folder.exists())
            return true;
        else
            return folder.mkdirs();
    }

    public static String getFilePath(String subPath, boolean isFile)
    {
        if (subPath.startsWith("/"))
        {
            subPath = subPath.substring(1);
        }
        String path = Environment.getExternalStorageDirectory().toString() + "/GIS/" + subPath;
        File file = new File(path);
        if (isFile)
        {
            createFolderIfNotExists(Objects.requireNonNull(file.getParentFile()));
        }
        else
        {
            createFolderIfNotExists(file);
        }
        return path;
    }

    public static String getConfigDir()
    {
        return getFilePath("Configs/", false);
    }

    public static String getDefaultConfigPath()
    {
        return getConfigPath("config");
    }

    public static String getConfigJson()
    {
        return getConfigJson("config");
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

    public static void saveConfigJson(String json)
    {
        writeTextToFile(getDefaultConfigPath(), json);
    }

    public static void saveConfigJson(String name, String json)
    {
        String path = getConfigPath(name);
        writeTextToFile(path, json);
    }

    public static String getCrashLogPath(String name)
    {
        return getFilePath("Logs/" + name, true);
    }

    public static String getConfigPath(String name)
    {
        return getConfigDir() + name + ".json";
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
        return getFilePath("Shapefile/" + name, true);
    }

    public static String getBaseLayerPath(String name)
    {
        return getFilePath("Base/" + name, true);
    }

    public static String getBaseLayerDirPath()
    {
        return getBaseLayerPath("");
    }


    public static String getPolylineTrackFilePath(String name)
    {
        return getFilePath("Track/Shapefile/" + name, true);
    }

    public static String getGpxTrackFilePath(String name)
    {
        return getFilePath("Track/" + name, true);
    }

    public static String getStyleFile(String shapeFileName)
    {
        return getShapefilePath(shapeFileName.substring(0, shapeFileName.length() - 4) + ".style", false);
    }

    public static String getRelativePath(String path, String root) throws IOException
    {
        if (!path.startsWith(root))
        {
            throw new IOException(root + "非" + path + "的父目录");
        }
        path = path.substring(root.length());
        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        return path;
    }

    public static String createShapefile(Context context, GeometryType type, String targetPath)
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
                "EmptyShapefiles/" + strType + ".shp",
                "EmptyShapefiles/" + strType + ".prj",
                "EmptyShapefiles/" + strType + ".dbf",
                "EmptyShapefiles/" + strType + ".cpg",
                "EmptyShapefiles/" + strType + ".shx",

        };

        for (String filePath : files)
        {
            copyAssets(context, filePath, targetPath + "." + filePath.split("\\.")[1]);
        }
        return targetPath.split("\\.")[0] + ".shp";
    }

    /**
     * 将assets下的文件放到sd指定目录下
     *
     * @param context    上下文
     * @param assetsPath assets下的路径
     * @param target     sd卡的路径
     */
    public static void copyAssets(Context context, String assetsPath, String target)
    {
        AssetManager assetManager = context.getAssets();
        try
        {
            InputStream is = assetManager.open(assetsPath);
            byte[] buffer = new byte[1024];
            int length = 0;
            File file = new File(target);
            if (!file.exists())
            {
                file.getParentFile().mkdirs();
                // 创建文件
                file.createNewFile();
            }
            else
            {
                throw new IOException("文件已存在：" + file.getAbsolutePath());
            }

            FileOutputStream fs = new FileOutputStream(file);
            while ((length = is.read(buffer)) != -1)
            {
                fs.write(buffer, 0, length);
            }

            fs.flush();
            is.close();
            fs.close();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static String getTimeBasedFileName(Date time)
    {
        if (time == null)
        {
            time = new Date();
        }
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA);
        return dateTimeFormat.format(time);
    }
}
