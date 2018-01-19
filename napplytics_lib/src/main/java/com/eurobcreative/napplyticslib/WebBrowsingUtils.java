package com.eurobcreative.napplyticslib;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

import static android.content.Context.TELEPHONY_SERVICE;

public class WebBrowsingUtils {
    private static final String LOG_TAG = "NAPPLYTICS";

    public static final String WEB_BROWSING_ACTION = "web_browsing_action";

    private static final String HTTP1_1 = "HTTP1_1";
    private static final String HTTP2 = "HTTP2";
    private static final String HTTP1_1TLS = "HTTP1_1TLS";

    protected static final String URL_WEB_BROWSING = "http://www.google.es";

    private static Context mContext;

    // The maximum number of bytes we will read from requested URL. Set to 1Mb.
    private static final long MAX_HTTP_RESPONSE_SIZE = 1024 * 1024;
    // The size of the response body we will report to the service.
    // If the response is larger than MAX_BODY_SIZE_TO_UPLOAD bytes, we will
    // only report the first MAX_BODY_SIZE_TO_UPLOAD bytes of the body.
    private static final int MAX_BODY_SIZE_TO_UPLOAD = 1024;
    // The buffer size we use to read from the WEB_BROWSING_OPTION response stream
    private static final int READ_BUFFER_SIZE = 1024;

    protected static TelephonyManager telephonyManager;
    protected static CustomPhoneStateListener customPhoneStateListener;

    public static void calculateWebBrowsingService(Context _context) {
        mContext = _context;

        startPhoneStateListener();
    }

    private static void startPhoneStateListener() {
        telephonyManager = (TelephonyManager) mContext.getSystemService(TELEPHONY_SERVICE);
        customPhoneStateListener = new CustomPhoneStateListener(mContext, CustomPhoneStateListener.WEB_BROWSING_OPTION);
        telephonyManager.listen(customPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    protected static void stopPhoneStateListener() {
        telephonyManager.listen(customPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }


    protected static float SIThroughputWebBrowsing(long throughput) {
        if (throughput <= 0) {
            return 1.0f;

        } else if (throughput > 0 && throughput <= 500) {
            return ((float) throughput / 500) + 1;

        } else if (throughput > 500 && throughput <= 800) {
            return (((float) (throughput - 500)) / 300) + 2;

        } else if (throughput > 800 && throughput <= 900) {
            return (((float) (throughput - 800)) / 100) + 3;

        } else if (throughput > 900 && throughput <= 1300) {
            return (((float) (throughput - 900)) / 400) + 4;

        } else {
            return 5.0f;
        }
    }

    protected static float SIRsrpWebBrowsing(int rsrp) {
        if (rsrp <= -109) {
            return 1.0f;

        } else if (rsrp > -109 && rsrp <= -103) {
            return ((float) (rsrp + 109) / 6) + 1;

        } else if (rsrp > -103 && rsrp <= -97) {
            return (((float) (rsrp + 103)) / 6) + 2;

        } else if (rsrp > -97 && rsrp <= -92) {
            return (((float) (rsrp + 97)) / 5) + 3;

        } else if (rsrp > -92 && rsrp <= -88) {
            return (((float) (rsrp + 92)) / 4) + 4;

        } else {
            return 5.0f;
        }
    }

    protected static float SIRssiWebBrowsing(int rssi) {
        if (rssi <= -81) {
            return 1.0f;

        } else if (rssi > -81 && rssi <= -76) {
            return ((float) (rssi + 81) / 5) + 1;

        } else if (rssi > -76 && rssi <= -71) {
            return (((float) (rssi + 76)) / 5) + 2;

        } else if (rssi > -71 && rssi <= -66) {
            return (((float) (rssi + 71)) / 5) + 3;

        } else if (rssi > -66 && rssi <= -60) {
            return (((float) (rssi + 66)) / 6) + 4;

        } else {
            return 5.0f;
        }
    }


    protected static double calculateWebBrowsingHttp1_1(float si_throughput, float si_rsrp, float si_rssi) {
        return (0.275906516431794 * si_throughput) + (0.20608960296224 * si_rsrp) + (0.187881476178343 * si_rssi);
    }

    protected static double calculateWebBrowsingHttp1_1TLS(float si_throughput, float si_rsrp, float si_rssi) {
        return (0.273217929983119 * si_throughput) + (0.224515432839308 * si_rsrp) + (0.191683339409728 * si_rssi);
    }

    protected static double calculateWebBrowsingHttp2(float si_throughput, float si_rsrp, float si_rssi) {
        return (0.274590130897324 * si_throughput) + (0.224955809413482 * si_rsrp) + (0.200835527674662 * si_rssi);
    }


    protected static String getBetterWebBrowsingService(double http1_1, double http1_1tls, double http2) {
        if (http1_1 > http2 && http1_1 > http1_1tls) {
            return HTTP1_1;

        } else if (http2 > http1_1 && http2 > http1_1tls) {
            return HTTP2;

        } else {
            return HTTP1_1TLS;
        }
    }


    protected static long calculateWebBrowsingThroughput() throws MeasurementError {

        float throughput = 0;
        long duration = 0;
        long originalHeadersLen = 0;

        ByteBuffer body = ByteBuffer.allocate(MAX_BODY_SIZE_TO_UPLOAD);
        String errorMsg = "";
        InputStream inputStream = null;

        try {
            URL urlObj = new URL(URL_WEB_BROWSING);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlObj.openConnection();
            httpURLConnection.setRequestMethod("GET");

            byte[] readBuffer = new byte[READ_BUFFER_SIZE];
            int readLen;
            int totalBodyLen = 0;

            long startTime = System.currentTimeMillis();

            try {
                if (httpURLConnection != null) {
                    inputStream = httpURLConnection.getInputStream();
                    while ((readLen = inputStream.read(readBuffer)) > 0 && totalBodyLen <= MAX_HTTP_RESPONSE_SIZE) {
                        totalBodyLen += readLen;
                        // Fill in the body to report up to MAX_BODY_SIZE
                        if (body.remaining() > 0) {
                            int putLen = body.remaining() < readLen ? body.remaining() : readLen;
                            body.put(readBuffer, 0, putLen);
                        }
                    }
                    duration = System.currentTimeMillis() - startTime;
                }
            } finally {
                httpURLConnection.disconnect();
            }

            for (int i = 0; ; i++) {
                String headerName = httpURLConnection.getHeaderFieldKey(i);
                String headerValue = httpURLConnection.getHeaderField(i);

                if (headerName == null && headerValue == null) {
                    break; //No more headers

                } else {
                    /*
                    * TODO(Wenjie): There can be preceding and trailing white spaces in
                    * each header field. I cannot find internal methods that return the
                    * number of bytes in a header. The solution here assumes the encoding
                    * is one byte per character.
                    */
                    originalHeadersLen += headerValue.length();
                }
            }

            throughput = (((float)(originalHeadersLen + totalBodyLen) * 8.0f) / 1000.0f) / ((float) duration / 1000.0f); // throughput -> Kbps

        } catch (MalformedURLException e) {
            errorMsg += e.getMessage() + "\n";
            Log.e(LOG_TAG, e.getMessage());

        } catch (IOException e) {
            errorMsg += e.getMessage() + "\n";
            Log.e(LOG_TAG, e.getMessage());

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();

                    return (long) Math.round(throughput);

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Fails to close the input stream from the WEB_BROWSING_OPTION response");
                }
            }
        }
        throw new MeasurementError("Cannot get result from WEB_BROWSING_OPTION measurement because " + errorMsg);
    }


    //Check if exist connection to network
    protected static boolean isNetworkAvailable(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnectedOrConnecting();
        } else {
            return false;
        }
    }
}
