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
import java.text.DecimalFormat;
import java.util.List;

public class CustomPhoneStateListener extends PhoneStateListener implements AdaptiveMediaSourceEventListener,
        BandwidthMeter.EventListener, Player.EventListener{
    private static String LOG_TAG = "NAPPLYTICS";

    /**
     * Keep track of the task to ensure we can cancel it if requested.
     */
    private static CallServer callServer = null;

    // General Results
    public static final String RESULT = "result";
    public static final String THROUGHPUT_RESULT = "throughput_result";
    public static final String SI_THROUGHPUT_RESULT = "si_throughput_result";
    public static final String RSSI_RESULT = "rssi_result";
    public static final String SI_RSSI_RESULT = "si_rssi_result";
    public static final String RSRP_RESULT = "rsrp_result";
    public static final String SI_RSRP_RESULT = "si_rsrp_result";
    // Web Browsing Results
    public static final String HTTP1_1_RESULT = "http1_1_result";
    public static final String HTTP1_1_TLS_RESULT = "http1_1_tls_result";
    public static final String HTTP2_RESULT = "http2_result";
    // Video Streaming Results
    public static final String HTTP_RESULT = "http_result";
    public static final String RTSP_RESULT = "rtsp_result";

    protected static final int WEB_BROWSING_OPTION = 0;
    protected static final int VIDEO_STREAMING_OPTION = 1;
    private static int option = -1;

    private int network_type = VideoStreamingUtils.HSPA_NETWORK;

    private long total_size = 0;
    private long total_ms = 0;

    private static long throughput = -1;
    private static int rssi = 1;
    private static int rsrp = 1;


    private static Context mContext;
    private static boolean access = false;


    private static DataSource.Factory mediaDataSourceFactory;
    private static SimpleExoPlayer player;
    private static DefaultTrackSelector trackSelector;
    private static boolean shouldAutoPlay;
    private static BandwidthMeter bandwidthMeter;


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

            if (option == WEB_BROWSING_OPTION) {
                Log.d("Result", "Web Browsing: Call Server");
                callServer = new CallServer();
                callServer.execute((Void[]) null);

            } else if (option == VIDEO_STREAMING_OPTION) {
                Log.d("Result", "Video Streaming: Init Player");
                callVideoStreaming();

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

    // WEB BROWSING
    private class CallServer extends AsyncTask<Void, Void, Long> {
        CallServer() {}

        @Override
        protected Long doInBackground(Void... params) {
            Long success = null;

            boolean isConnected = WebBrowsingUtils.isNetworkAvailable(mContext);
            if (isConnected) {
                try {
                    throughput = WebBrowsingUtils.calculateWebBrowsingThroughput();
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
                float si_throughput = WebBrowsingUtils.SIThroughputWebBrowsing(throughput);
                float si_rsrp = WebBrowsingUtils.SIRsrpWebBrowsing(rsrp);
                float si_rssi = WebBrowsingUtils.SIRssiWebBrowsing(rssi);

                double http1_1 = WebBrowsingUtils.calculateWebBrowsingHttp1_1(si_throughput, si_rsrp, si_rssi);
                double http1_1tls = WebBrowsingUtils.calculateWebBrowsingHttp1_1TLS(si_throughput, si_rsrp, si_rssi);
                double http2 = WebBrowsingUtils.calculateWebBrowsingHttp2(si_throughput, si_rsrp, si_rssi);

                String result = WebBrowsingUtils.getBetterWebBrowsingService(http1_1, http1_1tls, http2);

                DecimalFormat decimalFormatSI = new DecimalFormat("0.0000");
                DecimalFormat decimalFormat = new DecimalFormat("0.000000");
                String result_text = mContext.getResources().getString(R.string.web_browsing_result, throughput, rssi, rsrp,
                        decimalFormatSI.format(si_throughput), decimalFormatSI.format(si_rsrp), decimalFormatSI.format(si_rssi),
                        decimalFormat.format(http1_1), decimalFormat.format(http1_1tls), decimalFormat.format(http2), result);
                Log.d("Result", result_text);

                Intent intent = new Intent(WebBrowsingUtils.WEB_BROWSING_ACTION);
                intent.putExtra(RESULT, result);
                intent.putExtra(THROUGHPUT_RESULT, throughput);
                intent.putExtra(SI_THROUGHPUT_RESULT, si_throughput);
                intent.putExtra(RSRP_RESULT, rsrp);
                intent.putExtra(SI_RSRP_RESULT, si_rssi);
                intent.putExtra(RSSI_RESULT, rssi);
                intent.putExtra(SI_RSSI_RESULT, si_rsrp);
                intent.putExtra(HTTP1_1_RESULT, http1_1);
                intent.putExtra(HTTP1_1_TLS_RESULT, http1_1tls);
                intent.putExtra(HTTP2_RESULT, http2);
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

    // VIDEO STREAMING
    private void callVideoStreaming() {
        shouldAutoPlay = true;
        Handler eventHandler = new Handler();
        bandwidthMeter = new DefaultBandwidthMeter(eventHandler, this);
        mediaDataSourceFactory = new DefaultDataSourceFactory(mContext, Util.getUserAgent(mContext,
                "mediaPlayerSample"), (TransferListener<? super DataSource>) bandwidthMeter);

        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance(mContext, trackSelector);

        player.setPlayWhenReady(shouldAutoPlay);

        MediaSource mediaSource = new DashMediaSource(Uri.parse(VideoStreamingUtils.URL_VIDEO_STREAMING), mediaDataSourceFactory,
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
        Log.d("VIDEO_STREAMING", "onBandwidthSample = " + elapsedMs + " ms, " + bytes + " B, " + bitrate + " bitrate (bps)");

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
                Log.d("VIDEO_STREAMING", "Total Size = " + total_size + " B");

                throughput = ((total_size * 8) / 1000000) / (total_ms / 1000); // video_throughput -> Mbps (1000000 = 1000 x 1000)

                float si_throughput, si_rssi, si_rsrp;
                double http, rtsp;

                if (network_type == VideoStreamingUtils.LTE_NETWORK){
                    si_throughput = VideoStreamingUtils.SIThroughputVideoStreamingLTE(throughput);
                    si_rssi = VideoStreamingUtils.SIRssiVideoStreamingLTE(rssi);
                    si_rsrp = VideoStreamingUtils.SIRsrpVideoStreamingLTE(rsrp);

                    http = VideoStreamingUtils.calculateVideoStreamingHttpLTE(si_throughput, si_rsrp, si_rssi);
                    rtsp = VideoStreamingUtils.calculateVideoStreamingRTSPLTE(si_throughput, si_rsrp, si_rssi);

                } else {
                    si_throughput = VideoStreamingUtils.SIThroughputVideoStreamingHSPA(throughput);
                    si_rssi = VideoStreamingUtils.SIRssiVideoStreamingHSPA(rssi);
                    si_rsrp = VideoStreamingUtils.SIRsrpVideoStreamingHSPA(rsrp);

                    http = VideoStreamingUtils.calculateVideoStreamingHttpHSPA(si_throughput, si_rsrp, si_rssi);
                    rtsp = VideoStreamingUtils.calculateVideoStreamingRTSPHSPA(si_throughput, si_rsrp, si_rssi);
                }

                String result = VideoStreamingUtils.getBetterVideoStreamingService(http, rtsp);

                DecimalFormat decimalFormatSI = new DecimalFormat("0.0000");
                DecimalFormat decimalFormat = new DecimalFormat("0.000000");
                String result_text = mContext.getResources().getString(R.string.video_streaming_result,
                        VideoStreamingUtils.networkTypeArray[network_type], throughput, rssi, rsrp,
                        decimalFormatSI.format(si_throughput), decimalFormatSI.format(si_rsrp), decimalFormatSI.format(si_rssi),
                        decimalFormat.format(http), decimalFormat.format(rtsp), result);
                Log.d("Result", result_text);

                Intent intent = new Intent(VideoStreamingUtils.VIDEO_STREAMING_ACTION);
                intent.putExtra(RESULT, result);
                intent.putExtra(THROUGHPUT_RESULT, throughput);
                intent.putExtra(SI_THROUGHPUT_RESULT, si_throughput);
                intent.putExtra(RSRP_RESULT, rsrp);
                intent.putExtra(SI_RSRP_RESULT, si_rssi);
                intent.putExtra(RSSI_RESULT, rssi);
                intent.putExtra(SI_RSSI_RESULT, si_rsrp);
                intent.putExtra(HTTP_RESULT, http);
                intent.putExtra(RTSP_RESULT, rtsp);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                break;
        }

        Log.d("VIDEO_STREAMING", "onPlayerStateChanged:  PlayWhenReady = " + playWhenReady + ", State = " + state);
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