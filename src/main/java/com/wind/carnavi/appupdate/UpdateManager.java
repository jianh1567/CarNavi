package com.wind.carnavi.appupdate;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private static UpdateManager instance = null;
    private final static int BUFFER_SIZE = 4096;

    public UpdateManager(){

    }

    public synchronized static UpdateManager getInstance() {
        if (instance == null){
            instance = new UpdateManager();
        }
        return instance;
    }

    public void AppUpdate(Context context, String updateUrl, Handler handler){
        InputStream in = null;
        OutputStream out = null;
        String updateJson = null;
        VersionData versionData = null;
        try {
            URL url = new URL(updateUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(6 * 1000);
            connection.setReadTimeout(6 * 1000);
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            out = connection.getOutputStream();
            String postBody = "apkName=appName"+ "&apkPackage="+context.getPackageName()
                    + "&apkVersionCode=" + getVersionCode(context);
            Log.i(TAG, "TAG_postBody: " + postBody);
            out.write(postBody.getBytes(Charset.forName("UTF-8")));
            out.flush();

            if (connection.getResponseCode() == 200){
                in = connection.getInputStream();
            }
            byte[] data = null;
            if (in != null){
                data = InputStreamToByte(in);
            }
            if (data != null && data.length > 0){
                updateJson = new String(data, Charset.forName("UTF-8"));
            }
            Log.i(TAG, "updateJson: " + updateJson);
            Gson gson = new Gson();
            versionData = gson.fromJson(updateJson,VersionData.class);
            Message message = new Message();
            message.what = Constants.UPDATE_APP_VERSION_MSG;
            message.obj = versionData;
            handler.sendMessage(message);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] InputStreamToByte(InputStream in) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1) {
            outStream.write(data, 0, count);
        }
        data = null;
        return outStream.toByteArray();

    }

    /**
     * get App versionName
     * @param context
     * @return
     */
    public String getVersionName(Context context){
        PackageManager packageManager=context.getPackageManager();
        PackageInfo packageInfo;
        String versionName="";
        try {
            packageInfo=packageManager.getPackageInfo(context.getPackageName(),0);
            versionName=packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    public int getVersionCode(Context context){
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo;
        int versionCode = 0;
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(),0);
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
}
