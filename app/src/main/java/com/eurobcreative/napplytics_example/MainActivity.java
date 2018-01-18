package com.eurobcreative.napplytics_example;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.eurobcreative.napplyticslib.HttpUtils;
import com.eurobcreative.napplyticslib.VideoUtils;

import static com.eurobcreative.napplyticslib.CustomPhoneStateListener.RESULT;
import static com.eurobcreative.napplyticslib.HttpUtils.HTTP_ACTION;
import static com.eurobcreative.napplyticslib.HttpUtils.mContext;
import static com.eurobcreative.napplyticslib.VideoUtils.VIDEO_ACTION;
import static com.eurobcreative.napplyticslib.VideoUtils.runCalculateVideoMode;

public class MainActivity extends AppCompatActivity {

    public static BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result;

            String action = intent.getAction();
            switch (action) {
                case HTTP_ACTION:
                    result = intent.getStringExtra(RESULT);
                    Log.d("Result", "Http: " + result);

                    HttpUtils.stopPhoneStateListener();

                    try {
                        mContext.unregisterReceiver(broadcastReceiver);

                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    }

                    break;

                case VIDEO_ACTION:
                    result = intent.getStringExtra(RESULT);
                    Log.d("Result", "Video: " + result);

                    VideoUtils.stopPhoneStateListener();

                    try {
                        mContext.unregisterReceiver(broadcastReceiver);

                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    }

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE}, 1);

            } else {
                //runCalculateHttpMode(this);
                runCalculateVideoMode(this);
            }
        } else {
            //runCalculateHttpMode(this);
            runCalculateVideoMode(this);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HTTP_ACTION);
        intentFilter.addAction(VIDEO_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("PERMISSION", "ACCEPTED");
                    //runCalculateHttpMode(this);
                    runCalculateVideoMode(this);

                } else {
                    Log.i("PERMISSION", "DENIED");
                }

                return;
            }
        }
    }
}
