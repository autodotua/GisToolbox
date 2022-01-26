package com.autod.gis.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.autod.gis.layer.LayerManager;
import com.autod.gis.ui.adapter.FileListAdapter;
import com.autod.gis.R;

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
import java.util.List;

import static com.autod.gis.data.FileHelper.isFileOk;


/**
 * FTP远程管理和自动列举合适的文件类
 */
public class ImportFilesActivity extends AppCompatActivity implements View.OnClickListener
{
    /**
     * 文件列表适配器
     */
    FileListAdapter adapter;
    Button btnFtp;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_files);

        if (!InitializeFtpServer())
        {
            Toast.makeText(this, "初始化FTP服务器失败", Toast.LENGTH_SHORT).show();
        }

        ActionBar actionBar = this.getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        btnFtp = findViewById(R.id.import_btn_ftp);
        btnFtp.setOnClickListener(this);
        tvwFtp = findViewById(R.id.import_tvw_ftp);


        Bundle extra = getIntent().getExtras();
        if (extra != null && extra.containsKey("EnableFiles") && !extra.getBoolean("EnableFiles"))
        {
            return;
        }


        ListView lvwFile = findViewById(R.id.import_lvw_files);
        adapter = new FileListAdapter(this);
        lvwFile.setAdapter(adapter);

        //单击文件列表的列表项则加载图层
        lvwFile.setOnItemClickListener((adapterView, view, i, l) ->
        {
            File clickedFile = adapter.files.get(i);

            LayerManager.getInstance().addLayer(this,clickedFile.getAbsolutePath());
            Intent intent = new Intent(ImportFilesActivity.this, MainActivity.class);
            startActivity(intent);
        });

        lvwFile.setOnItemLongClickListener((parent, view, i, id) -> {

            File clickedFile = adapter.files.get(i);
            Toast.makeText(this, clickedFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return true;
        });
        searchFiles();
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

    TextView tvwFtp;

    @Override
    protected void onStop()
    {
        super.onStop();
        if (!server.isSuspended() && !server.isStopped())
        {
            server.stop();
        }
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
                            //如果FTP首次打开，则start；否则恢复
                            if (server.isSuspended())
                            {
                                server.resume();
                            }
                            else
                            {
                                server.start();
                            }
                            btnFtp.setText("关闭FTP");
                            String ip = getWIFILocalIpAddress();
                            if (ip != null)
                            {

                                tvwFtp.setText("ftp://" + ip + ":2222");
                            }
                        }
                        catch (Exception ex)
                        {
                            Toast.makeText(this, "打开FTP失败：\n" + ex.toString(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "关闭FTP":
                        server.suspend();
                        btnFtp.setText("打开FTP");
                        tvwFtp.setText("");
                        searchFiles();
                        break;
                }
                break;

        }
    }

    /**
     * 匹配的文件列表
     */
    List<File> matchedFiles = new ArrayList<>();

    /**
     * 搜索匹配格式的文件
     */
    private void searchFiles()
    {
        File gisDirectory = new File(Environment.getExternalStorageDirectory().toString() + "/Gis");
        if (!gisDirectory.exists())
        {
            if (gisDirectory.mkdir())
            {
                Toast.makeText(this, "文件夹为空", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "文件夹不存在且创建失败", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        matchedFiles.clear();
        listMatchedFiles(gisDirectory);

        adapter.loadFiles(matchedFiles);
        adapter.notifyDataSetChanged();
    }

    /**
     * 获取当前在WiFi网段下的IP地址
     *
     * @return
     */
    private String getWIFILocalIpAddress()
    {

        //获取wifi服务
        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (wifiManager == null)
        {
            Toast.makeText(this, "获取WIFI状态失败", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (!wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return formatIpAddress(ipAddress);
    }

    /**
     * 格式化IP地址
     *
     * @param ipAdress
     * @return
     */
    private String formatIpAddress(int ipAdress)
    {

        return (ipAdress & 0xFF) + "." +
                ((ipAdress >> 8) & 0xFF) + "." +
                ((ipAdress >> 16) & 0xFF) + "." +
                (ipAdress >> 24 & 0xFF);
    }

    /**
     * 递归遍历所有符合格式的文件
     *
     * @param parent
     */
    private void listMatchedFiles(File parent)
    {
        for (File child : parent.listFiles())
        {
            if (child.isDirectory())
            {
                listMatchedFiles(child);
            }
            else
            {
                if (isFileOk(child.getAbsolutePath()))
                {
                    matchedFiles.add(child);
                }
            }
        }
    }

    /**
     * FTP服务器实例
     */
    static FtpServer server;

    /**
     * 初始化FTP服务
     *
     * @return
     */
    boolean InitializeFtpServer()
    {
        //System.setProperty("java.net.preferIPv6Addresses", "false");
        FtpServerFactory serverFactory = new FtpServerFactory();

        ListenerFactory factory = new ListenerFactory();

        // set the port of the listener
        int port = 2222;
        factory.setPort(port);

        // replace the default listener
        serverFactory.addListener("default", factory.createListener());
        final PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        try
        {
            //设置账户
            final UserManager userManager = userManagerFactory.createUserManager();
            BaseUser user = new BaseUser();
            //设置匿名登陆
            user.setName("anonymous");
            user.setPassword("");
            //设置根目录为Gis文件夹
            File gisDirectory = new File(Environment.getExternalStorageDirectory().toString() + "/Gis");
            if (gisDirectory.exists())
            {
                user.setHomeDirectory(gisDirectory.getAbsolutePath());
            }
            else
            {
                user.setHomeDirectory(Environment.getExternalStorageDirectory().toString());
            }

            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            authorities.add(new ConcurrentLoginPermission(2, Integer.MAX_VALUE));
            user.setAuthorities(authorities);
            userManager.save(user);
            serverFactory.setUserManager(userManager);
            // start the server
            server = serverFactory.createServer();

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}
