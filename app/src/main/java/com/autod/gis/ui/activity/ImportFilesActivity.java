package com.autod.gis.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.autod.gis.data.FileHelper;
import com.autod.gis.map.LayerManager;
import com.autod.gis.service.FtpService;
import com.autod.gis.ui.adapter.FileListAdapter;
import com.autod.gis.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * FTP远程管理和自动列举合适的文件类
 */
public class ImportFilesActivity extends AppCompatActivity implements View.OnClickListener
{
    TextView tvwFtp;
    /**
     * 匹配的文件列表
     */
    List<File> matchedFiles = new ArrayList<>();
    private FileListAdapter adapter;
    private Button btnFtp;
    private boolean forBaseLayer;
    private  boolean hasOpenedFTP=false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_files);
        Bundle extra = getIntent().getExtras();
        forBaseLayer = extra != null && extra.containsKey("base");

        ActionBar actionBar = this.getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        btnFtp = findViewById(R.id.import_btn_ftp);
        btnFtp.setOnClickListener(this);
        tvwFtp = findViewById(R.id.import_tvw_ftp);


        ListView lvwFile = findViewById(R.id.import_lvw_files);
        adapter = new FileListAdapter(this);
        lvwFile.setAdapter(adapter);

        //单击文件列表的列表项则加载图层
        lvwFile.setOnItemClickListener((adapterView, view, i, l) ->
        {
            File clickedFile = adapter.files.get(i);
            String path = clickedFile.getAbsolutePath();
            try
            {
                if (forBaseLayer)
                {
                    path = FileHelper.getRelativePath(path, FileHelper.getBaseLayerDirPath());
                }
                else
                {
                    path = FileHelper.getRelativePath(path, FileHelper.getShapefileDirPath());
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            Intent data = new Intent();
            data.putExtra("path", path);
            data.putExtra("reset",hasOpenedFTP);
            setResult(RESULT_OK, data);
            finish();
        });

        lvwFile.setOnItemLongClickListener((parent, view, i, id) -> {
            File clickedFile = adapter.files.get(i);
            Toast.makeText(this, clickedFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return true;
        });


        searchFiles();
    }


    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        stopService(new Intent(this,FtpService.class));
        super.onDestroy();
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.import_btn_ftp:
                switch (btnFtp.getText().toString())
                {
                    case "打开FTP":
                        try
                        {
                            hasOpenedFTP=true;
                            Intent data = new Intent();
                            data.putExtra("reset",true);
                            setResult(RESULT_OK, data);
                            startService(new Intent(this, FtpService.class));
                            btnFtp.setText(R.string.btn_close_ftp);
                            String ip = FtpService.getWIFILocalIpAddress(this);
                            if (ip != null)
                            {
                                tvwFtp.setText(String.format("ftp://%s:2222", ip));
                            }
                        }
                        catch (Exception ex)
                        {
                            Toast.makeText(this, "打开FTP失败：\n" + ex.toString(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "关闭FTP":
                        stopService(new Intent(this,FtpService.class));
                        btnFtp.setText(R.string.btn_open_ftp);
                        tvwFtp.setText("");
                        searchFiles();
                        break;
                }
                break;

        }
    }

    /**
     * 搜索匹配格式的文件
     */
    private void searchFiles()
    {
        File gisDirectory = new File(forBaseLayer ? FileHelper.getBaseLayerPath("") : FileHelper.getShapefileDirPath());
        if (!gisDirectory.exists())
        {
            Toast.makeText(this, "文件夹为空", Toast.LENGTH_SHORT).show();
            return;
        }

        matchedFiles.clear();
        List<String> extensions;
        extensions = forBaseLayer ? FileHelper.SupportedBaseLayerExtensions : new ArrayList<>(Collections.singletonList("shp"));

        listMatchedFiles(gisDirectory, extensions);

        adapter.loadFiles(matchedFiles);
        adapter.notifyDataSetChanged();
    }


    /**
     * 递归遍历所有符合格式的文件
     *
     * @param parent
     */
    private void listMatchedFiles(File parent, List<String> extensions)
    {
        for (File child : parent.listFiles())
        {
            if (child.isDirectory())
            {
                listMatchedFiles(child, extensions);
            }
            else
            {
                for (String extension : extensions)
                {
                    if (child.getName().endsWith("." + extension))
                    {
                        matchedFiles.add(child);
                        break;
                    }
                }

            }
        }
    }

}
