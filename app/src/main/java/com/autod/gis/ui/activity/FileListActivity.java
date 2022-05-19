package com.autod.gis.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.autod.gis.map.LayerManager;
import com.autod.gis.ui.adapter.FileListAdapter;
import com.autod.gis.R;

import java.io.File;

import static com.autod.gis.data.FileHelper.getShapefileDirPath;

/**
 * 文件选取Activity。因导入更加方便，故已被隐藏，需要长按“导入”按钮进行打开。
 */
public class FileListActivity extends AppCompatActivity
{
    FileListAdapter adapter;

     String lastPath = "";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        ActionBar actionBar = this.getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        ListView lvwFile = findViewById(R.id.file_lvw_file);
        adapter = new FileListAdapter(this);


        File file;

        //如果是次级目录，那么应该有传过来的值，直接打开传过来的目录
        if (getIntent().getExtras() == null)
        {
            String path =lastPath;

            file = new File(path);
            if(!file.exists())
            {
                file=new File(getShapefileDirPath());
            }
        }
        else
        {
            String path = getIntent().getStringExtra("Path");
            file = new File(path);
        }


        //如果是根目录，隐藏“返回上一级”按钮
        Button btnFileListUp = findViewById(R.id.file_btn_up);

        if (file.getAbsolutePath().equals(Environment.getExternalStorageDirectory().toString()))
        {
            btnFileListUp.setVisibility(View.GONE);
        }

        //单击返回上一级按钮，打开新的本Activity并传值
        File finalFile = file;
        btnFileListUp.setOnClickListener(v ->
        {
            Intent intent = new Intent(FileListActivity.this, FileListActivity.class);
            String parent = finalFile.getParentFile().getAbsolutePath();
            intent.putExtra("Path", parent);
            startActivity(intent);
        });


        setTitle("打开文件 - " + file.getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), ""));
        if (!file.exists())
        {
            if (!file.mkdirs())
            {
                Toast.makeText(this, "目录创建失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        adapter.OpenFolder(file.getAbsolutePath());
        lvwFile.setAdapter(adapter);

        //单击列表项
        lvwFile.setOnItemClickListener((adapterView, view, i, l) -> {
            File clickedFile = adapter.files.get(i);
            //如果单击的是文件夹，则创建新的本Activity并传值
            if (clickedFile.isDirectory())
            {
                Intent intent = new Intent(FileListActivity.this, FileListActivity.class);
                intent.putExtra("Path", clickedFile.getAbsolutePath());
                startActivity(intent);
                //adapter.OpenFolder(clickedFile.getAbsolutePath());
            }
            else
            {
                // 若单击的是文件，那么加入图层，并且打开MainActivity。
                // 由于MainActivity设置了SingleTask，故不会新建实例，
                // 而是把MainActivity以上的返回栈的打开文件Activity全部出栈
               LayerManager.getInstance().addLayer(this, clickedFile.getAbsolutePath());
                Intent intent = new Intent(FileListActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

}
