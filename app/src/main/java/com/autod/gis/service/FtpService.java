package com.autod.gis.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

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

public class FtpService extends Service
{
    /**
     * FTP服务器实例
     */
    private org.apache.ftpserver.FtpServer server;

    /**
     * 初始化FTP服务
     *
     * @return
     */
    public boolean initializeFtpServer()
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
            authorities.add(new ConcurrentLoginPermission(0, Integer.MAX_VALUE));
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

    /**
     * 获取当前在WiFi网段下的IP地址
     *
     * @return
     */
    public static String getWIFILocalIpAddress(Context context)
    {

        //获取wifi服务
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (wifiManager == null)
        {
            Toast.makeText(context, "获取WIFI状态失败", Toast.LENGTH_SHORT).show();
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
     * @param ip
     * @return
     */
    private static String formatIpAddress(int ip)
    {

        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        try
        {
            initializeFtpServer();
            server.start();
        }
        catch (Exception ex)
        {
            Toast.makeText(getApplicationContext(), "启动FTP失败：" + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        try
        {
            if (server != null && !server.isStopped() && !server.isSuspended())
            {
                server.stop();
            }
        }
        catch (Exception ex)
        {
            Toast.makeText(getApplicationContext(), "停止FTP失败：" + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
        super.onDestroy();
    }
}
