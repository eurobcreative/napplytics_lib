package com.eurobcreative.napplyticslib;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.Context.TELEPHONY_SERVICE;

public class VideoUtils {
    public static final String VIDEO_ACTION = "video_option";

    private static final String HTTP = "HTTP";
    private static final String RTSP = "RTSP";

    public static final String URL_VIDEO = "http://dash.edgesuite.net/dash264/TestCasesMCA/dolby/5/11/Living_Room_720p_51_51_192k_320k_25fps.mpd";
        //"http://www-itec.uni-klu.ac.at/ftp/datasets/DASHDataset2014/BigBuckBunny/15sec/BigBuckBunny_15s_simple_2014_05_09.mpd";

    public static final int HSPA_NETWORK = 0;
    public static final int LTE_NETWORK = 1;
    public static final String[] networkTypeArray = {"HSPA", "LTE"};

    public static TelephonyManager telephonyManager;
    public static CustomPhoneStateListener customPhoneStateListener;

    public static void runCalculateVideoMode(Context _context) {
        HttpUtils.mContext = _context;

        startPhoneStateListener();
    }

    public static void startPhoneStateListener() {
        telephonyManager = (TelephonyManager) HttpUtils.mContext.getSystemService(TELEPHONY_SERVICE);

        int currentNetworkType = telephonyManager.getNetworkType();
        int network_type;
        if (currentNetworkType == TelephonyManager.NETWORK_TYPE_LTE){
            network_type = LTE_NETWORK;

        } else {
            network_type = HSPA_NETWORK;
        }
        Log.d("Network_type", networkTypeArray[network_type]);

        customPhoneStateListener = new CustomPhoneStateListener(HttpUtils.mContext, CustomPhoneStateListener.VIDEO, network_type);
        telephonyManager.listen(customPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public static void stopPhoneStateListener() {
        telephonyManager.listen(customPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }


    // LTE
    public static float SIThroughputVideoLTE(long throughput /*Mbps*/) {
        if (throughput <= 5) {
            return 1.0f;

        } else if (throughput > 5 && throughput <= 8) {
            return ((float) (throughput - 5) / 3) + 1;

        } else if (throughput > 8 && throughput <= 11) {
            return (((float) (throughput - 8)) / 3) + 2;

        } else if (throughput > 11 && throughput <= 30) {
            return (((float) (throughput - 11)) / 19) + 3;

        } else if (throughput > 30 && throughput <= 55) {
            return (((float) (throughput - 30)) / 25) + 4;

        } else {
            return 5.0f;
        }
    }

    public static float SIRsrpVideoLTE(int rsrp) {
        if (rsrp <= -116) {
            return 1.0f;

        } else if (rsrp > -116 && rsrp <= -108) {
            return ((float) (rsrp + 116) / 8) + 1;

        } else if (rsrp > -108 && rsrp <= -101) {
            return (((float) (rsrp + 108)) / 7) + 2;

        } else if (rsrp > -101 && rsrp <= -95) {
            return (((float) (rsrp + 101)) / 6) + 3;

        } else if (rsrp > -95 && rsrp <= -89) {
            return (((float) (rsrp + 95)) / 6) + 4;

        } else {
            return 5.0f;
        }
    }

    public static float SIRssiVideoLTE(int rssi) {
        if (rssi <= -107) {
            return 1.0f;

        } else if (rssi > -107 && rssi <= -102) {
            return ((float) (rssi + 107) / 5) + 1;

        } else if (rssi > -102 && rssi <= -92) {
            return (((float) (rssi + 102)) / 10) + 2;

        } else if (rssi > -92 && rssi <= -84) {
            return (((float) (rssi + 92)) / 8) + 3;

        } else if (rssi > -84 && rssi <= -79) {
            return (((float) (rssi + 84)) / 5) + 4;

        } else {
            return 5.0f;
        }
    }


    public static double calculateVideoHttpLTE(float si_throughput, float si_rsrp, float si_rssi) {
        return (0.5215 * si_throughput) + (0.5495 * si_rsrp) + (0 * si_rssi);
    }

    public static double calculateVideoRTSPLTE(float si_throughput, float si_rsrp, float si_rssi) {
        return (0.8477 * si_throughput) + (0.2404 * si_rsrp) + (0 * si_rssi);
    }


    public static String calculateBetterVideoLTEMode(long throughput /*Mbps*/, int rsrp, int rssi) {
        float si_throughput = SIThroughputVideoLTE(throughput);
        float si_rssi = SIRssiVideoLTE(rssi);
        float si_rsrp = SIRsrpVideoLTE(rsrp);

        double http_lte = calculateVideoHttpLTE(si_throughput, si_rsrp, si_rssi);
        double rtsp_lte = calculateVideoRTSPLTE(si_throughput, si_rsrp, si_rssi);

        return getBetterVideoMode(http_lte, rtsp_lte);
    }


    // HSPA
    public static float SIThroughputVideoHSPA(long throughput /*Mbps*/) {
        if (throughput <= 1) {
            return 1.0f;

        } else if (throughput > 1 && throughput <= 2) {
            return (float) (throughput - 1) + 1;

        } else if (throughput > 2 && throughput <= 5) {
            return (((float) (throughput - 2)) / 3) + 2;

        } else if (throughput > 5 && throughput <= 7) {
            return (((float) (throughput - 5)) / 2) + 3;

        } else if (throughput > 7 && throughput <= 55) {
            return (((float) (throughput - 7)) / 2) + 4;

        } else {
            return 5.0f;
        }
    }

    public static float SIRsrpVideoHSPA(int rsrp) {
        if (rsrp <= -109) {
            return 1.0f;

        } else if (rsrp > -109 && rsrp <= -107) {
            return ((float) (rsrp + 109) / 2) + 1;

        } else if (rsrp > -107 && rsrp <= -100) {
            return (((float) (rsrp + 107)) / 7) + 2;

        } else if (rsrp > -100 && rsrp <= -96) {
            return (((float) (rsrp + 100)) / 4) + 3;

        } else if (rsrp > -96 && rsrp <= -86) {
            return (((float) (rsrp + 96)) / 10) + 4;

        } else {
            return 5.0f;
        }
    }

    public static float SIRssiVideoHSPA(int rssi) {
        if (rssi <= -108) {
            return 1.0f;

        } else if (rssi > -108 && rssi <= -107) {
            return (float) (rssi + 108) + 1;

        } else if (rssi > -107 && rssi <= -106) {
            return (float) (rssi + 107) + 2;

        } else if (rssi > -106 && rssi <= -104) {
            return (((float) (rssi + 106)) / 2) + 3;

        } else if (rssi > -104 && rssi <= -101) {
            return (((float) (rssi + 104)) / 3) + 4;

        } else {
            return 5.0f;
        }
    }


    public static double calculateVideoHttpHSPA(float si_throughput, float si_rsrp, float si_rssi) {
        return (0.6019 * si_throughput) + (0.5179 * si_rsrp) + (0 * si_rssi);
    }

    public static double calculateVideoRTSPHSPA(float si_throughput, float si_rsrp, float si_rssi) {
        return (0.3799 * si_throughput) + (0.3895 * si_rsrp) + (0 * si_rssi);
    }


    public static String calculateBetterVideoMode(long throughput /*Mbps*/, int rsrp, int rssi) {
        float si_throughput = SIThroughputVideoHSPA(throughput);
        float si_rssi = SIRsrpVideoHSPA(rssi);
        float si_rsrp = SIRssiVideoHSPA(rsrp);

        double http_lte = calculateVideoHttpHSPA(si_throughput, si_rsrp, si_rssi);
        double rtsp_lte = calculateVideoRTSPHSPA(si_throughput, si_rsrp, si_rssi);

        return getBetterVideoMode(http_lte, rtsp_lte);
    }


    public static String getBetterVideoMode(double http_lte, double rtsp_lte) {
        if (http_lte > rtsp_lte) {
            return HTTP;

        } else {
            return RTSP;
        }
    }
}
