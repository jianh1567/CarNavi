package com.wind.carnavi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.baidu.mapapi.bikenavi.adapter.IBRoutePlanListener;
import com.baidu.mapapi.bikenavi.model.BikeRoutePlanError;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.wind.carnavi.appupdate.ConfirmDialog;
import com.wind.carnavi.appupdate.Constants;
import com.wind.carnavi.appupdate.DownloadFileManager;
import com.wind.carnavi.appupdate.NetWorkHelper;
import com.wind.carnavi.appupdate.UpdateManager;
import com.wind.carnavi.appupdate.VersionData;

import java.io.File;
import java.util.ArrayList;

import javax.xml.datatype.Duration;

public class BNavigationActivity extends Activity implements OnGetGeoCoderResultListener{
    private final String TAG = "BNavigationActivity";
    public LocationClient mLocationClient = null;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private boolean isFirstIn = true;
    private GeoCoder mSearch;

    private MapView mMapView;
    private BaiduMap mBaiduMap;

    private Marker mMarkerA;
    private Marker mMarkerB;

    private LatLng startPt,endPt;

    private BikeNavigateHelper mNaviHelper;
    BikeNaviLaunchParam param;
    private static boolean isPermissionRequested = false;

    BitmapDescriptor bdA = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_marka);
    BitmapDescriptor bdB = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_markb);

    private String mCity = "上海市";
    private String mAddr = "上海师范大学";
    private Handler mHandler = new Handler();
    private ConfirmDialog confirmDialog;
    private static final String path = Environment.getExternalStorageDirectory() + "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_main);

        requestPermission();
        mMapView = (MapView) findViewById(R.id.mapview);
        mBaiduMap = mMapView.getMap();

        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);

        mLocationClient = new LocationClient(getApplicationContext());

        //声明LocationClient类
        mLocationClient.registerLocationListener(new MyLocationListener());
        initLocationOption();

        Button bikeBtn = (Button) findViewById(R.id.button);
        bikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetWorkHelper.isNetworkAvailable(BNavigationActivity.this)){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            UpdateManager.getInstance().AppUpdate(BNavigationActivity.this,"http://111.231.110.234:8080/crm/Apk/update.action",handler);
                        }
                    }).start();
                }else{
                    Toast.makeText(BNavigationActivity.this,getString(R.string.network_not_available),Toast.LENGTH_SHORT).show();
                }
            }
        });

        try {
            mNaviHelper = BikeNavigateHelper.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = getIntent();
        if(intent.hasExtra("addr")){
            mAddr = intent.getStringExtra("addr");
            Log.i(TAG, "mAddr = " + mAddr);
        }

        searchGeocode();
    }

    private void searchGeocode(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    mSearch.geocode(new GeoCodeOption()
                            .city(mCity)
                            .address(mAddr));
                }catch (Exception e){
                    Toast.makeText(BNavigationActivity.this, "网络错误", Toast.LENGTH_LONG);
                }
            }
        }, 1000);
    }

    private void initLocationOption(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        option.setOpenGps(true);
        option.setLocationNotify(true);
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);
        option.setWifiCacheTimeOut(5*60*1000);
        option.setEnableSimulateGps(false);
        option.setIsNeedAddress(true);

        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(BNavigationActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        String strInfo = String.format("纬度：%f 经度：%f",
                result.getLocation().latitude, result.getLocation().longitude);
        Log.i(TAG, "endPt latitude = " + result.getLocation().latitude + " longitude " + result.getLocation().longitude);
        endPt = new LatLng(result.getLocation().latitude,  result.getLocation().longitude);
        mBaiduMap.clear();
        initOverlay();
        startBikeNavi();
//        Toast.makeText(BNavigationActivity.this, strInfo, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(BNavigationActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        mBaiduMap.clear();
        mBaiduMap.addOverlay(new MarkerOptions().position(result.getLocation())
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.icon_marka)));
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result
                .getLocation()));
        Toast.makeText(BNavigationActivity.this, result.getAddress()+" adcode: "+result.getAdcode(),
                Toast.LENGTH_LONG).show();

    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明

            if (location == null || mMapView == null) {
                return;
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius()).direction(100)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);// 更新定位点位置.

            mCurrentLat = location.getLatitude();    //定位的当前位置维度 31.168754  经度 121.41196
            mCurrentLon = location.getLongitude();
            Log.i(TAG, "mCurrentLat = " + mCurrentLat + " mCurrentLon = " + mCurrentLon);
            startPt = new LatLng(mCurrentLat, mCurrentLon);
            if(isFirstIn){
                mCity = location.getCity();

                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(new LatLng(mCurrentLat, mCurrentLon)).zoom(19);

                mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                isFirstIn = false;
            }
        }
    }

    public void initOverlay() {
        // add marker overlay
        LatLng llA = startPt;
        LatLng llB = endPt;

        if(llA == null){
            Toast.makeText(BNavigationActivity.this, "GPS信号弱，不能定位当前位置",
                    Toast.LENGTH_LONG).show();
            return;
        }
        MarkerOptions ooA = new MarkerOptions().position(llA).icon(bdA)
                .zIndex(9).draggable(true);

        mMarkerA = (Marker) (mBaiduMap.addOverlay(ooA));
        mMarkerA.setDraggable(true);
        MarkerOptions ooB = new MarkerOptions().position(llB).icon(bdB)
                .zIndex(5);
        mMarkerB = (Marker) (mBaiduMap.addOverlay(ooB));
        mMarkerB.setDraggable(true);


        mBaiduMap.setOnMarkerDragListener(new BaiduMap.OnMarkerDragListener() {
            public void onMarkerDrag(Marker marker) {
            }

            public void onMarkerDragEnd(Marker marker) {
                if(marker == mMarkerA){
                    startPt = marker.getPosition();
                }else if(marker == mMarkerB){
                    endPt = marker.getPosition();
                }
                param.stPt(startPt).endPt(endPt);
            }

            public void onMarkerDragStart(Marker marker) {
            }
        });
    }

    private void startBikeNavi() {
        Log.d("View", "startBikeNavi");
        try {
            mNaviHelper.initNaviEngine(this, new IBEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d("View", "engineInitSuccess");
                    routePlanWithParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d("View", "engineInitFail");
                }
            });
        } catch (Exception e) {
            Log.d("Exception", "startBikeNavi");
            e.printStackTrace();
        }
    }

    private void routePlanWithParam() {
        param = new BikeNaviLaunchParam().stPt(startPt).endPt(endPt).vehicle(1);
        mNaviHelper.routePlanWithParams(param, new IBRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d("View", "onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d("View", "onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(BNavigationActivity.this, BNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
                Log.d("View", "onRoutePlanFail");
            }

        });
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {

            isPermissionRequested = true;

            ArrayList<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (permissions.size() == 0) {
                return;
            } else {
                requestPermissions(permissions.toArray(new String[permissions.size()]), 0);
            }
        }
    }

    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if(intent.hasExtra("addr")){
            mAddr = intent.getStringExtra("addr");
            Log.i(TAG, "onNewIntent mAddr = " + mAddr);
        }

        searchGeocode();
    }

    public Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.UPDATE_APP_DOWNLOAD_FAILED:
                    Toast.makeText(BNavigationActivity.this,getString(R.string.app_update_download_failed),Toast.LENGTH_SHORT).show();
                    break;
                case Constants.UPDATE_APP_DOWNLOAD_LOADING:
                    int point = (int)msg.obj;
                    confirmDialog.UpdateProgress(getString(R.string.app_download_loading),point + "%","","",point);
                    break;
                case Constants.UPDATE_APP_VERSION_MSG:
                    VersionData versionData = (VersionData) msg.obj;
                    UpdateVersion(versionData);
                    break;
                default:break;
            }
            super.handleMessage(msg);
        }
    };

    private void UpdateVersion(VersionData versionData){
        if (versionData != null){
            if (!versionData.status){
                Toast.makeText(BNavigationActivity.this,getString(R.string.app_recent_version),Toast.LENGTH_SHORT).show();
            }else{
                if (versionData.updateinfo != null
                        && versionData.updateinfo.apkPackage != null
                        && versionData.updateinfo.apkPackage.equals(getPackageName())){
                    if (versionData.updateinfo.apkVersionCode > UpdateManager.getInstance().getVersionCode(BNavigationActivity.this)){
                        createUpdateDialog(versionData.updateinfo.strategy,versionData.updateinfo.apkVersionName
                                ,versionData.updateinfo.downAddress,versionData.updateinfo.apkContent);
                    }else{
                        Toast.makeText(BNavigationActivity.this,getString(R.string.app_recent_version),Toast.LENGTH_SHORT).show();
                    }

                }
            }
        }
    }

    private void createUpdateDialog(final int type,String versionName, final String download_url, String log){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(getString(R.string.app_found));
        stringBuffer.append("APP");
        stringBuffer.append(getString(R.string.app_new_version));
        stringBuffer.append("V");
        stringBuffer.append(versionName);

        confirmDialog = new ConfirmDialog(this,stringBuffer.toString(),log
                ,getString(R.string.app_start_update),getString(R.string.app_reject_update),type);
        confirmDialog.setClickListener(new ConfirmDialog.BtnClickListener() {
            @Override
            public void doConfirm() {
                if (NetWorkHelper.isNetworkAvailable(BNavigationActivity.this)){
                    startDownload(type,download_url);
                }else{
                    confirmDialog.dismiss();
                    Toast.makeText(BNavigationActivity.this,getString(R.string.network_not_available),Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void doCancel() {
                confirmDialog.dismiss();
            }
        });

        if (NetWorkHelper.isNetworkAvailable(BNavigationActivity.this)){
            if (type == 1){
                confirmDialog.show();
            }else if (type == 2){
                startDownload(type,download_url);
            }else{
                confirmDialog.show();
                startDownload(type,download_url);
            }
        }else {
            Toast.makeText(BNavigationActivity.this,getString(R.string.network_not_available),Toast.LENGTH_SHORT).show();
        }

        confirmDialog.setCanceledOnTouchOutside(false);
    }

    private void startDownload(final int type,final String download_url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DownloadFileManager.getInstance().downLoadFile(type,download_url, new DownloadFileManager.DownloadListener() {
                    @Override
                    public void loading(int point) {
                        Log.i("download","point: " + point);
                    }

                    @Override
                    public void success(File file) {
                        Log.i("download","download success");
                        confirmDialog.dismiss();
                        File file1 = new File(path + "/windUtil.apk" );
                        DownloadFileManager.getInstance().openFile(BNavigationActivity.this,file1);
                    }

                    @Override
                    public void failed() {
                        Log.i("download","download failed");
                        confirmDialog.dismiss();
                        Message message = new Message();
                        message.what = Constants.UPDATE_APP_DOWNLOAD_FAILED;
                        handler.sendMessage(message);
                    }
                },path,"windUtil.apk",handler);
            }
        }).start();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mSearch.destroy();
        bdA.recycle();
        bdB.recycle();
    }
}
