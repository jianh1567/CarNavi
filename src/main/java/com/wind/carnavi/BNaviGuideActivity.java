/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.wind.carnavi;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.wind.carnavi.control.InitConfig;
import com.wind.carnavi.listener.UiMessageListener;
import com.wind.carnavi.util.AutoCheck;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBRouteGuidanceListener;
import com.baidu.mapapi.bikenavi.adapter.IBTTSPlayer;
import com.baidu.mapapi.bikenavi.model.BikeRouteDetailInfo;
import com.baidu.mapapi.bikenavi.model.RouteGuideKind;
import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.chainofresponsibility.logger.LoggerProxy;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BNaviGuideActivity extends Activity {
    private static final String TEXT = "欢迎使用百度语音合成，请在代码中修改合成文本";

    // ================== 初始化参数设置开始 ==========================
    /**
     * 发布时请替换成自己申请的appId appKey 和 secretKey。注意如果需要离线合成功能,请在您申请的应用中填写包名。
     * 本demo的包名是com.baidu.tts.sample，定义在build.gradle中。
     */
    protected String appId = "11005757";

    protected String appKey = "Ovcz19MGzIKoDDb3IsFFncG1";

    protected String secretKey = "e72ebb6d43387fc7f85205ca7e6706e2";

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
    private TtsMode ttsMode = TtsMode.ONLINE;

    // ================选择TtsMode.ONLINE  不需要设置以下参数; 选择TtsMode.MIX 需要设置下面2个离线资源文件的路径
    private static final String TEMP_DIR = "/sdcard/baiduTTS"; // 重要！请手动将assets目录下的3个dat 文件复制到该目录

    // 请确保该PATH下有这个文件
    private static final String TEXT_FILENAME = TEMP_DIR + "/" + "bd_etts_text.dat";

    // 请确保该PATH下有这个文件 ，m15是离线男声
    private static final String MODEL_FILENAME =
            TEMP_DIR + "/" + "bd_etts_common_speech_m15_mand_eng_high_am-mix_v3.0.0_20170505.dat";

    protected SpeechSynthesizer mSpeechSynthesizer;

    private static final String TAG = "BNaviGuideActivity";

    private BikeNavigateHelper mNaviHelper;

    protected Handler mainHandler;

    private ScreenOffBroadcastReciver mScreenOffBroadcastReciver;

    private AudioManager mAudioManager;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNaviHelper.quit();
        if (mSpeechSynthesizer != null) {
            mSpeechSynthesizer.stop();
            mSpeechSynthesizer.release();
            mSpeechSynthesizer = null;
            print("释放资源成功");
        }
        unregisterReceiver(mScreenOffBroadcastReciver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNaviHelper.resume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        initHandler();
        initPermission();
        initTTs();

        mNaviHelper = BikeNavigateHelper.getInstance();

        View view = mNaviHelper.onCreate(BNaviGuideActivity.this);
        if (view != null) {
            setContentView(view);
        }

        addHomeView();

        mNaviHelper.startBikeNavi(BNaviGuideActivity.this);

        mNaviHelper.setTTsPlayer(new IBTTSPlayer() {
            @Override
            public int playTTSText(String s, boolean b) {
                Log.d(TAG, "playTTSText " + s);
                speak(s);
                return 0;
            }
        });

        mNaviHelper.setRouteGuidanceListener(this, new IBRouteGuidanceListener() {
            @Override
            public void onRouteGuideIconUpdate(Drawable icon) {

            }

            @Override
            public void onRouteGuideKind(RouteGuideKind routeGuideKind) {

            }

            @Override
            public void onRoadGuideTextUpdate(CharSequence charSequence, CharSequence charSequence1) {
                String guide = "";
                try {
                    guide = new String(charSequence1.toString().getBytes("GBK"), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if(guide.contains("骑") && guide.contains("导航")){
                    speak("骑行导航开始");
                 }else {
                    speak(charSequence.toString() + "，，，" + charSequence1.toString());
                 }
            }

            @Override
            public void onRemainDistanceUpdate(CharSequence charSequence) {
                speak("距离终点距离还有" + charSequence.toString());
            }

            @Override
            public void onRemainTimeUpdate(CharSequence charSequence) {
                speak("距离到达终点时间还有" + charSequence.toString());
            }

            @Override
            public void onGpsStatusChange(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onGpsStatusChange " + charSequence.toString());
                speak(charSequence.toString());
            }

            @Override
            public void onRouteFarAway(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onRouteFarAway " + charSequence.toString());
                speak(charSequence.toString());
            }

            @Override
            public void onRoutePlanYawing(CharSequence charSequence, Drawable drawable) {
                Log.d(TAG, "onRoutePlanYawing " + charSequence.toString());
                speak(charSequence.toString());
            }

            @Override
            public void onReRouteComplete() {

            }

            @Override
            public void onArriveDest() {

            }

            @Override
            public void onVibrate() {

            }

            @Override
            public void onGetRouteDetailInfo(BikeRouteDetailInfo bikeRouteDetailInfo) {

            }
        });

        registerBroadcastReciver();
    }

    private void addHomeView(){
        FrameLayout contentParent =
                (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);

        ImageView imageView = new ImageView(this);
        imageView.setBackgroundResource(R.mipmap.ic_home);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(150,150);
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        params.setMargins(0,0,50,50);

        imageView.setLayoutParams(params);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            }
        });
        contentParent.addView(imageView);
    }

    public static int px2dip(Context context, float pxValue) {
         final float scale = context.getResources().getDisplayMetrics().density;
         return (int) (pxValue / scale + 0.5f);
    }

    private void registerBroadcastReciver(){
        mScreenOffBroadcastReciver=new ScreenOffBroadcastReciver();
        IntentFilter recevierFilter=new IntentFilter();
        recevierFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenOffBroadcastReciver, recevierFilter);
    }

    private class  ScreenOffBroadcastReciver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                finish();
            }
        }
    }

    private void initHandler() {
        mainHandler = new Handler() {
            /*
             * @param msg
             */
            @Override
            public void handleMessage(final Message msg) {
                super.handleMessage(msg);
                if (msg.obj != null) {
                    print(msg.obj.toString());
                }

                switch (msg.what){
                    case MainHandlerConstant.START_SPEECH:
                         if(msg.arg1 == 0 && msg.arg2 == 0){
                             int request = mAudioManager.requestAudioFocus(afChangeListener,
                                     AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                             if(request == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                                 Log.i(TAG, "get audiofocus====");
                             }
                         }
                         break;
                    case MainHandlerConstant.FINISHI_SPEECH:
                         if(msg.arg1 == 1 && msg.arg2 == 1){
                             mAudioManager.abandonAudioFocus(afChangeListener);
                             Log.i(TAG, "abandon audiofocus====");
                         }
                         break;
                }
            }

        };
    }

    AudioManager.OnAudioFocusChangeListener  afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.i(TAG, "focusChange = " + focusChange);
        }
    };

    /**
     * 注意此处为了说明流程，故意在UI线程中调用。
     * 实际集成中，该方法一定在新线程中调用，并且该线程不能结束。具体可以参考NonBlockSyntherizer的写法
     */
    private void initTTs() {
        LoggerProxy.printable(true); // 日志打印在logcat中
        print("initTTs start");
        boolean isMix = ttsMode.equals(TtsMode.MIX);
        boolean isSuccess;
        if (isMix) {
            // 检查2个离线资源是否可读
            isSuccess = checkOfflineResources();
            if (!isSuccess) {
                return;
            } else {
                print("离线资源存在并且可读, 目录：" + TEMP_DIR);
            }
        }
        // 日志更新在UI中，可以换成MessageListener，在logcat中查看日志
        SpeechSynthesizerListener listener = new UiMessageListener(mainHandler);

        // 1. 获取实例
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(this);

        // 2. 设置listener
        mSpeechSynthesizer.setSpeechSynthesizerListener(listener);

        // 3. 设置appId，appKey.secretKey
        int result = mSpeechSynthesizer.setAppId(appId);
        checkResult(result, "setAppId");
        result = mSpeechSynthesizer.setApiKey(appKey, secretKey);
        checkResult(result, "setApiKey");

        // 4. 支持离线的话，需要设置离线模型
        if (isMix) {
            // 检查离线授权文件是否下载成功，离线授权文件联网时SDK自动下载管理，有效期3年，3年后的最后一个月自动更新。
            isSuccess = checkAuth();
            if (!isSuccess) {
                return;
            }
            // 文本模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, TEXT_FILENAME);
            // 声学模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, MODEL_FILENAME);
        }

        // 5. 以下setParam 参数选填。不填写则默认值生效
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置合成的音量，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9");
        // 设置合成的语速，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");

        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线

        mSpeechSynthesizer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        // x. 额外 ： 自动so文件是否复制正确及上面设置的参数
        Map<String, String> params = new HashMap<>();
        // 复制下上面的 mSpeechSynthesizer.setParam参数
        // 上线时请删除AutoCheck的调用
        if (isMix) {
            params.put(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, TEXT_FILENAME);
            params.put(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, MODEL_FILENAME);
        }
        InitConfig initConfig =  new InitConfig(appId, appKey, secretKey, ttsMode, params, listener);
        AutoCheck.getInstance(getApplicationContext()).check(initConfig, new Handler() {
            @Override
            /**
             * 开新线程检查，成功后回调
             */
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainDebugMessage();
                        print(message); // 可以用下面一行替代，在logcat中查看代码
                        // Log.w("AutoCheckMessage", message);
                    }
                }
            }

        });

        // 6. 初始化
        result = mSpeechSynthesizer.initTts(ttsMode);
        checkResult(result, "initTts");
        print("initTTs end");
    }

    /**
     * 检查appId ak sk 是否填写正确，另外检查官网应用内设置的包名是否与运行时的包名一致。本demo的包名定义在build.gradle文件中
     *
     * @return
     */
    private boolean checkAuth() {
        AuthInfo authInfo = mSpeechSynthesizer.auth(ttsMode);
        if (!authInfo.isSuccess()) {
            // 离线授权需要网站上的应用填写包名。本demo的包名是com.baidu.tts.sample，定义在build.gradle中
            String errorMsg = authInfo.getTtsError().getDetailMessage();
            print("【error】鉴权失败 errorMsg=" + errorMsg);
            return false;
        } else {
            print("验证通过，离线正式授权文件存在。");
            return true;
        }
    }

    /**
     * 检查 TEXT_FILENAME, MODEL_FILENAME 这2个文件是否存在，不存在请自行从assets目录里手动复制
     *
     * @return
     */
    private boolean checkOfflineResources() {
        String[] filenames = {TEXT_FILENAME, MODEL_FILENAME};
        for (String path : filenames) {
            File f = new File(path);
            if (!f.canRead()) {
                print("[ERROR] 文件不存在或者不可读取，请从assets目录复制同名文件到：" + path);
                print("[ERROR] 初始化失败！！！");
                return false;
            }
        }
        return true;
    }

    private void speak(String text) {
        /* 以下参数每次合成时都可以修改
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
         *  设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "5"); 设置合成的音量，0-9 ，默认 5
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5"); 设置合成的语速，0-9 ，默认 5
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5"); 设置合成的语调，0-9 ，默认 5
         *
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
         *  MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
         *  MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
         *  MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
         *  MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
         */

        if (mSpeechSynthesizer == null) {
            print("[ERROR], 初始化失败");
            return;
        }

        int result = mSpeechSynthesizer.speak(text);
        checkResult(result, "speak");
    }

    private void stop() {
        print("停止合成引擎 按钮已经点击");
        int result = mSpeechSynthesizer.stop();
        checkResult(result, "stop");
    }

    private void print(String message) {
        Log.i(TAG, message);
    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            print("error code :" + result + " method:" + method + ", 错误码文档:http://yuyin.baidu.com/docs/tts/122 ");
        }
    }

    //  下面是android 6.0以上的动态授权

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }
}
