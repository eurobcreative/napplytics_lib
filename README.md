# napplytics_lib
NApplytics Android Library


Usage: Gradle dependency

Add the following lines to your project level build.gradle:

allprojects {
	repositories {
		maven { url "https://jitpack.io" }
	}
}

Add the following lines to your app build.gradle:

dependencies {
  compile 'com.github.eurobcreative:napplytics_lib:0.0.3'
}


Permissions:

Your app has to required the following permissions:

  - android.permission.INTERNET,
  - android.permission.ACCESS_COARSE_LOCATION,
  - android.permission.READ_PHONE_STATE,
  - android.permission.ACCESS_NETWORK_STATE


Results:

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

  
