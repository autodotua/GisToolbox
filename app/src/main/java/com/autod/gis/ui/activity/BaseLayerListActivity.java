package com.autod.gis.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.autod.gis.R;
import com.autod.gis.data.Config;
import com.autod.gis.data.FileHelper;
import com.autod.gis.map.LayerManager;
import com.autod.gis.ui.adapter.BaseLayerListAdapter;
import com.autod.gis.ui.adapter.FileListAdapter;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.autod.gis.ui.activity.MainActivity.ImportFilesActivityID;

/**
 * FTP远程管理和自动列举合适的文件类
 */
public class BaseLayerListActivity extends AppCompatActivity
{
    private BaseLayerListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setResult(RESULT_OK);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_layer_list);

        ActionBar actionBar = this.getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        ListView list = findViewById(R.id.base_lvw_layers);
        adapter = new BaseLayerListAdapter(this);
        list.setAdapter(adapter);

        Button btnAdd = findViewById(R.id.base_btn_add);
        btnAdd.setOnClickListener(v -> {
            final EditText editText = new EditText(BaseLayerListActivity.this);
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setSingleLine(true);
            new android.app.AlertDialog.Builder(BaseLayerListActivity.this)
                    .setTitle("请输入网址或文件名")
                    .setMessage("支持瓦片地址、栅格文件")
                    .setView(editText)
                    .setPositiveButton("确定", (dialog, which) -> {
                        adapter.addLayer(editText.getText().toString());
                    })
                    .create().show();
        });

        Button btnBrowse = findViewById(R.id.base_btn_browse);
        btnBrowse.setOnClickListener(v -> {
            Intent intent = new Intent(this, ImportFilesActivity.class);
            intent.putExtra("base", true);
            startActivityForResult(intent, ImportFilesActivityID);
        });
        Button btnEsri = findViewById(R.id.base_btn_esri);
        btnEsri.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Esri底图（强制位于最底层）")
                    .setSingleChoiceItems(LayerManager.EsriBaseLayers, Config.getInstance().esriBaseLayer, (dialog, which) -> {
                        Config.getInstance().esriBaseLayer = which;
                        dialog.dismiss();
                    })
                    .create().show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            if (requestCode == ImportFilesActivityID)
            {
                String path = data.getStringExtra("path");
                adapter.addLayer(path);
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        adapter.save();
        super.onDestroy();
    }
}
