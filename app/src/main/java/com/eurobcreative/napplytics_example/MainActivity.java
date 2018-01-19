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

import static com.eurobcreative.napplyticslib.CustomPhoneStateListener.RESULT;
import static com.eurobcreative.napplyticslib.VideoStreamingUtils.VIDEO_STREAMING_ACTION;
import static com.eurobcreative.napplyticslib.WebBrowsingUtils.WEB_BROWSING_ACTION;
import static com.eurobcreative.napplyticslib.WebBrowsingUtils.calculateWebBrowsingService;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result;

            String action = intent.getAction();
            switch (action) {
                case WEB_BROWSING_ACTION:
                    result = intent.getStringExtra(RESULT);
                    Log.d("Result", "Web Browsing: " + result);

                    try {
                        unregisterReceiver(broadcastReceiver);

                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    }

                    break;

                case VIDEO_STREAMING_ACTION:
                    result = intent.getStringExtra(RESULT);
                    Log.d("Result", "Video Streaming: " + result);

                    try {
                        unregisterReceiver(broadcastReceiver);

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
                calculateWebBrowsingService(this);
                //calculateVideoStreamingService(this);
            }
        } else {
            calculateWebBrowsingService(this);
            //calculateVideoStreamingService(this);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WEB_BROWSING_ACTION);
        intentFilter.addAction(VIDEO_STREAMING_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("PERMISSION", "ACCEPTED");
                    calculateWebBrowsingService(this);
                    //calculateVideoStreamingService(this);

                } else {
                    Log.i("PERMISSION", "DENIED");
                }

                return;
            }
        }
    }
}
