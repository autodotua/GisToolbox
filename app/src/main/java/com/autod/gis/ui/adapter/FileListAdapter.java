package com.autod.gis.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.autod.gis.R;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * 文件类表适配器
 */
public class FileListAdapter extends BaseAdapter
{
    private LayoutInflater mInflater;
    public List<File> files=new ArrayList<>();

    public FileListAdapter(Context context)
    {
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount()
    {
        // TODO Auto-generated method stub
        return files.size();
    }

    @Override
    public Object getItem(int index)
    {
        // TODO Auto-generated method stub
        return files.get(index);
    }

    @Override
    public long getItemId(int arg0)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    ViewHolder holder = null;

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {


        if (convertView == null)
        {

            holder = new ViewHolder();

            convertView = mInflater.inflate(R.layout.layout_file_list_view, null);
            holder.btnIsFolder = convertView.findViewById(R.id.btnIsFolder);
            holder.tvwFileName = convertView.findViewById(R.id.file_tvw_name);
            holder.tvwFileSize = convertView.findViewById(R.id.file_tvw_size);
            convertView.setTag(holder);

        }
        else
        {

            holder = (ViewHolder) convertView.getTag();
        }

        holder.btnIsFolder.setImageResource(((File) getItem(position)).isDirectory() ? R.drawable.ic_folder : R.drawable.ic_map);
        holder.tvwFileName.setText(((File) getItem(position)).getName());
        holder.tvwFileSize.setText(((File) getItem(position)).isFile() ? byteToFitString(((File) getItem(position)).length()) : "文件夹");


        return convertView;
    }

    public final class ViewHolder
    {
        ImageView btnIsFolder;
        TextView tvwFileName;
        TextView tvwFileSize;
    }

    /**
     * 枚举指定目录下的文件和文件夹并显示在列表上
     * @param path
     */
    public void OpenFolder(String path)
    {
        File folder = new File(path);
        if (!folder.isDirectory())
        {
            return;
        }
        files = new ArrayList<>();
        for (File file : folder.listFiles())
        {

            if (file.isDirectory())
            {
                files.add(file);
            }
            //只有符合拓展名的才会加入
            else if (file.getName().endsWith(".shp"))
            {
                files.add(file);
            }
        }
    }

    /**
     * 将指定的文件列表的文件加入列表
     * @param fileList
     */
    public void loadFiles(List<File> fileList)
    {
        files = new ArrayList<>();

        files.addAll(fileList);

    }

    /**
     * 字节转符合的大小显示
     * @param size
     * @return
     */
    private static String byteToFitString(long size)
    {
        if (size < 0)
        {
            return "";
        }
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        double dSize = size;
        if (dSize < 1024)
        {
            return decimalFormat.format(dSize) + "B";
        }
        dSize /= 1024;
        if (dSize < 1024)
        {
            return decimalFormat.format(dSize) + "KB";
        }
        dSize /= 1024;
        if (dSize < 1024)
        {
            return decimalFormat.format(dSize) + "MB";
        }
        dSize /= 1024;
        if (dSize < 1024)
        {
            return decimalFormat.format(dSize) + "GB";
        }
        dSize /= 1024;
        return decimalFormat.format(dSize) + "TB";
    }
}



