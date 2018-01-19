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

  - android.permission.INTERNET,
  - android.permission.ACCESS_COARSE_LOCATION,
  - android.permission.READ_PHONE_STATE,
  - android.permission.ACCESS_NETWORK_STATE


<b>Results</b>

To receive the results you must register a BroadcastReceiver,

  - Final result: "result",
  - Throughput: "throughput_result",
  - Satisfaction Index Throught = "si_throughput_result",
  - RSSI = "rssi_result",
  - Satisfaction Index RSSI = "si_rssi_result",
  - RSRP = "rsrp_result",
  - Satisfaction Index RSRP = "si_rsrp_result"
  
  In case of Web Browsing:
  
  - HTTP1_1 = "http1_1_result",
  - HTTP1_1_TLS = "http1_1_tls_result",
  - HTTP2 = "http2_result"
  
  In case of Video Streaming:
  
  - HTTP = "http_result",
  - RTSP = "rtsp_result"

  
