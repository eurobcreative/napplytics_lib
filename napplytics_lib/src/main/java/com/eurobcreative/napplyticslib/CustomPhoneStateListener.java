package com.eurobcreative.napplyticslib;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class CustomPhoneStateListener extends PhoneStateListener implements AdaptiveMediaSourceEventListener, BandwidthMeter.EventListener, Player.EventListener{
    public static String LOG_TAG = "NAPPLYTICS";

    /**
     * Keep track of the task to ensure we can cancel it if requested.
     */
    public static CallServer callServer = null;

    public static final String RESULT = "result";

    public static int HTTP = 0;
    public static int VIDEO = 1;
    private static int option = -1;

    public int network_type = VideoUtils.HSPA_NETWORK;

    public static long total_size = 0;
    private long total_ms = 0;

    public static long throughput = -1;
    private static int rssi = 1;
    private static int rsrp = 1;


    private static Context mContext;
    public static boolean access = false;


    public static DataSource.Factory mediaDataSourceFactory;
    public static SimpleExoPlayer player;
    public static DefaultTrackSelector trackSelector;
    public static boolean shouldAutoPlay;
    public static BandwidthMeter bandwidthMeter;


    public CustomPhoneStateListener(Context context, int option, int network_type) {
        this.mContext = context;
        this.option = option;
        this.network_type = network_type;
        this.access = true;
    }

    public CustomPhoneStateListener(Context context, int option) {
        this.mContext = context;
        this.option = option;
        this.access = true;
    }

    /**
     * In this method Java Reflection API is being used please see link before
     * using.
     *
     * @see <a href="http://docs.oracle.com/javase/tutorial/reflect/">http://docs.oracle.com/javase/tutorial/reflect/</a>
     */
    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);

        if (access) {
            access = false;

            Log.i(LOG_TAG, "onSignalStrengthsChanged: " + signalStrength);
            Log.i(LOG_TAG, "onSignalStrengthsChanged: getEvdoDbm " + signalStrength.getEvdoDbm());

            rssi = signalStrength.getEvdoDbm();

            // Calculate rsrp
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            for (CellInfo cellInfo : cellInfoList) {
                if (cellInfo instanceof CellInfoLte) {
                    if (cellInfo.isRegistered()) {
                        rsrp = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                    }
                }
            }

            if (option == HTTP) {
                Log.d("Result", "Http: Call Server");
                callServer = new CallServer();
                callServer.execute((Void[]) null);

            } else if (option == VIDEO) {
                Log.d("Result", "Video: Init Player");
                callVideo();

            } else {
                // Do nothing
            }

            try {
                Method[] methods = android.telephony.SignalStrength.class.getMethods();
                for (Method mthd : methods) {
                    if (mthd.getName().equals("getLteRsrp")) {
                        Log.i(LOG_TAG, "onSignalStrengthsChanged: " + mthd.getName() + " " + mthd.invoke(signalStrength));
                    }
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        } else {
            // Do nothing
        }
    }

    // WEB
    private class CallServer extends AsyncTask<Void, Void, Long> {
        CallServer() {}

        @Override
        protected Long doInBackground(Void... params) {
            Long success = null;

            boolean isConnected = HttpUtils.isNetworkAvailable(mContext);
            if (isConnected) {
                try {
                    throughput = HttpUtils.calculateThroughput();
                    success = throughput;
                } catch (MeasurementError measurementError) {
                    measurementError.printStackTrace();
                }
            }

            return success;
        }

        @Override
        protected void onPostExecute(final Long success) {
            if (success != null) {
                Log.d("Result", "Http: throughput = " + throughput + ", rsrp = " + rsrp + ", rssi = " + rssi);

                String result = HttpUtils.calculateBetterHttpMode(throughput, rsrp, rssi);
                Intent intent = new Intent(HttpUtils.HTTP_ACTION);
                intent.putExtra(RESULT, result);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

            } else {
                //Error
            }

            callServer = null;
        }

        @Override
        protected void onCancelled() {
            callServer = null;
        }
    }

    // VIDEO
    public void callVideo() {
        shouldAutoPlay = true;
        Handler eventHandler = new Handler();
        bandwidthMeter = new DefaultBandwidthMeter(eventHandler, this);
        mediaDataSourceFactory = new DefaultDataSourceFactory(mContext, Util.getUserAgent(mContext,
                "mediaPlayerSample"), (TransferListener<? super DataSource>) bandwidthMeter);

        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector);

        player.setPlayWhenReady(shouldAutoPlay);

        MediaSource mediaSource = new DashMediaSource(Uri.parse(VideoUtils.URL_VIDEO), mediaDataSourceFactory,
                new DefaultDashChunkSource.Factory(mediaDataSourceFactory), eventHandler, this);

        player.addListener(this);
        player.prepare(mediaSource);
    }

    @Override
    public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                              int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs,
                              long mediaEndTimeMs, long elapsedRealtimeMs) {
    }

    @Override
    public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                                int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs,
                                long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
    }

    @Override
    public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                               int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs,
                               long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
    }

    @Override
    public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat,
                            int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs,
                            long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded,
                            IOException error, boolean wasCanceled) {
        Log.d("VIDEO_ERROR", error.toString());
    }

    @Override
    public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {
    }

    @Override
    public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason,
                                          Object trackSelectionData, long mediaTimeMs) {
    }

    @Override
    public void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
        Log.d("VIDEO_BANDWIDTH", elapsedMs + " ms, " + bytes + " B, " + bitrate + " bitrate (bps)");

        total_size += bytes;
        total_ms += elapsedMs;
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        String state = "";
        switch (playbackState) {
            case Player.STATE_IDLE:
                state = "Idle";
                break;

            case Player.STATE_BUFFERING:
                state = "Buffering";
                break;

            case Player.STATE_READY:
                state = "Ready";
                break;

            case Player.STATE_ENDED:
                state = "Ended";
                Log.d("VIDEO_TOTAL_SIZE", Long.toString(total_size));

                throughput = ((total_size * 8) / 1000000) / (total_ms / 1000); // video_throughput -> Mbps (1000000 = 1000 x 1000)

                Log.d("Result", "Video: throughput = " + throughput + ", rsrp = " + rsrp + ", rssi = " + rssi);

                float si_throughput, si_rssi, si_rsrp;
                double http, rtsp;

                if (network_type == VideoUtils.LTE_NETWORK){
                    si_throughput = VideoUtils.SIThroughputVideoLTE(throughput);
                    si_rssi = VideoUtils.SIRssiVideoLTE(rssi);
                    si_rsrp = VideoUtils.SIRsrpVideoLTE(rsrp);

                    http = VideoUtils.calculateVideoHttpLTE(si_throughput, si_rsrp, si_rssi);
                    rtsp = VideoUtils.calculateVideoRTSPLTE(si_throughput, si_rsrp, si_rssi);

                } else {
                    si_throughput = VideoUtils.SIThroughputVideoHSPA(throughput);
                    si_rssi = VideoUtils.SIRssiVideoHSPA(rssi);
                    si_rsrp = VideoUtils.SIRsrpVideoHSPA(rsrp);

                    http = VideoUtils.calculateVideoHttpHSPA(si_throughput, si_rsrp, si_rssi);
                    rtsp = VideoUtils.calculateVideoRTSPHSPA(si_throughput, si_rsrp, si_rssi);
                }

                String result = VideoUtils.getBetterVideoMode(http, rtsp);

                Intent intent = new Intent(VideoUtils.VIDEO_ACTION);
                intent.putExtra(RESULT, result);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                break;
        }

        Log.d("VIDEO_STATE", "PlayWhenReady: " + playWhenReady + ", State: " + state);
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.d("VIDEO_PLAYER_ERROR", error.toString());
    }

    @Override
    public void onPositionDiscontinuity() {
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    }
}