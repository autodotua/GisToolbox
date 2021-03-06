package com.autod.gis.ui.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.autod.gis.data.Config;
import com.autod.gis.data.FileHelper;
import com.autod.gis.map.LayerManager;
import com.autod.gis.ui.UIHelper;
import com.autod.gis.ui.adapter.LayerListAdapter;
import com.autod.gis.R;
import com.autod.gis.ui.MenuHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


/**
 * 属性列表
 */
public class LayerListActivity extends AppCompatActivity implements View.OnClickListener
{


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layer_list);
        setTitle("图层管理");

        ActionBar actionBar = this.getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        ListView lvwLayer = findViewById(R.id.layer_lvw_list);
        adapter = new LayerListAdapter(this);
        lvwLayer.setAdapter(adapter);

        Button btnSave = findViewById(R.id.layer_btn_save);
        Button btnSaveAs = findViewById(R.id.layer_btn_save_as);
        btnSave.setOnClickListener(this);
        btnSaveAs.setOnClickListener(this);
        Button btnOpen = findViewById(R.id.layer_btn_open);
        btnOpen.setOnClickListener(this);
    }

    LayerListAdapter adapter;

    public void notifyDataSetChanged()
    {
        adapter.notifyDataSetChanged();
    }


    @Override
    protected void onPause()
    {
        super.onPause();
        Config.getInstance().save();
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.layer_btn_save:
                save();
                break;
            case R.id.layer_btn_save_as:
                saveAs();
                break;

            case R.id.layer_btn_open:
                open();
                break;

        }
    }

    private void open()
    {
        ArrayList<String> configNames = new ArrayList<>();
        HashMap<String, String> configNamePaths = new HashMap<>();
        File gisFolder = new File(FileHelper.getConfigDir());
        for (File file : Objects.requireNonNull(gisFolder.listFiles()))
        {
            if (file.getPath().endsWith(".json"))
            {
                String name = file.getName().replace(".json", "");
                configNames.add(name);
                configNamePaths.put(name, file.getAbsolutePath());
            }
        }
        String[] configNameArray = configNames.toArray(new String[0]);
        for (int i = 0; i < configNameArray.length; i++)
        {
            if (configNameArray[i].equals("config"))
            {
                configNameArray[i] = "默认";
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择配置文件");

        //列表对话框；
        builder.setItems(configNameArray, (dialog, which) -> {
            try
            {
                Config.setInstance(configNames.get(which));
                setResult(RESULT_OK);
                finish();
            }
            catch (Exception ex)
            {
                UIHelper.showSimpleErrorDialog(this, "打开失败：\n" + ex.getMessage());
            }
            finish();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void save()
    {
        try
        {

            Config.getInstance().save(true);
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        }
        catch (Exception ex)
        {
            UIHelper.showSimpleErrorDialog(this, "保存失败：\n" + ex.getMessage());
        }
    }

    private void saveAs()
    {
        final EditText editText = new EditText(this);
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setTitle("请输入文件名").setView(editText);
        inputDialog.setPositiveButton("确定", (dialog, which) -> {
            String name = editText.getText().toString();
            if (!isValidFileName(name))
            {
                Toast.makeText(LayerListActivity.this, "文件名不合法，未保存", Toast.LENGTH_SHORT).show();
            }
            else
            {
                try
                {
                    Config.getInstance().save(name, true);
                    Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                }
                catch (Exception ex)
                {
                    UIHelper.showSimpleErrorDialog(this, "保存失败：\n" + ex.getMessage());
                }
            }
        }).show();
    }

    /**
     * 判断文件名是否合法
     *
     * @param fileName
     * @return
     */
    public static boolean isValidFileName(String fileName)
    {
        if (fileName == null || fileName.length() > 255)
        {
            return false;
        }
        else
        {
            return fileName.matches("[^\\s\\\\/:\\*\\?\\\"<>\\|](\\x20|[^\\s\\\\/:\\*\\?\\\"<>\\|])*[^\\s\\\\/:\\*\\?\\\"<>\\|\\.]$");
        }
    }
}
