package com.wind.carnavi.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wind.carnavi.BNavigationActivity;

/**
 * Created by houjian on 2018/6/5.
 */

public class NaviBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "NaviBroadcastReceiver";
    private static final String ACTION_START_NAVI = "com.wind.startnavi";
    private static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION_START_NAVI)) {
            String dstName = intent.getStringExtra("addr");

            Intent mIntent = new Intent(context, BNavigationActivity.class);
            mIntent.putExtra("addr", dstName);
            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mIntent);
        }
    }
}
