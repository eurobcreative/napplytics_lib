# napplytics_lib
<h3><b>NApplytics Android Library</b></h3>

NApplytics Library performs measurements of multiple actions to recommend the best configuration options, and so developers can apply this configuration in their Android applications. This library uses Mobilyzer library for web browsing service and ExoPlayer library for video streaming service to calculate network throughput.

For the web browsing service, we have three configuration options: HTTP 1.1, HTTP 1.1 TLS and HTTP 2. In this case, we need the throughput value, the RSSI (Received Signal Strength Indicator) value and the RSRP (Reference Signals Received Power) value, and then the App calculates three Satisfaction Indices (SI) and it displays the option with the best SI.

For the video streaming service, we have two configuration options: HTTP and RTSP. In this case, we need the throughput value, the RSSI value and the RSRP value, and then the App calculates two Satisfaction Indices and it displays the option with the best SI.

<h3><b>Instructions to create an Android App using NApplytics Android Library</b></h3>

<b>Usage: Gradle dependency</b>

Add the following lines to your project level build.gradle:

```
allprojects {
	repositories {
		maven { url "https://jitpack.io" }
	}
}
```

Add the following lines to your app build.gradle:

```
dependencies {
  compile 'com.github.eurobcreative:napplytics_lib:0.0.3'
}
```


<b>Permissions</b>

Your app has to required the following permissions:

  - ```android.permission.INTERNET```
  - ```android.permission.ACCESS_COARSE_LOCATION```
  - ```android.permission.READ_PHONE_STATE```
  - ```android.permission.ACCESS_NETWORK_STATE```


<b>API</b>

The API of this library provides the following methods to calculate the most appropriate protocol for Web Browsing and Video Streaming services, respectively:

```
public static void calculateWebBrowsingService(Context context);
```

```
public static void calculateVideoStreamingService(Context context);
```

<b>Results</b>

To receive the results you must register a BroadcastReceiver, adding the actions ```WEB_BROWSING_ACTION``` and ```VIDEO_STREAMING_ACTION```, as in:

```
IntentFilter intentFilter = new IntentFilter();
intentFilter.addAction(WEB_BROWSING_ACTION);
intentFilter.addAction(VIDEO_STREAMING_ACTION);
registerReceiver(broadcastReceiver, intentFilter);
LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
```

Then, the results will be provided in the following tags:

  - Throughput: ```THROUGHPUT_RESULT```
  - Satisfaction Index Throughput: ```SI_THROUGHPUT_RESULT```
  - RSSI: ```RSSI_RESULT```
  - Satisfaction Index RSSI: ```SI_RSSI_RESULT```
  - RSRP: ```RSRP_RESULT```
  - Satisfaction Index RSRP: ```SI_RSRP_RESULT```
  
  In the case of Web Browsing:
  
  - HTTP1_1: ```HTTP1_1_RESULT```
  - HTTP1_1_TLS: ```HTTP1_1_TLS_RESULT```
  - HTTP2: ```HTTP2_RESULT```
  
  In the case of Video Streaming:
  
  - HTTP: ```HTTP_RESULT```
  - RTSP: ```RTSP_RESULT```
  
  The protocol chosen by this library will be provided in ```RESULT```.
  
