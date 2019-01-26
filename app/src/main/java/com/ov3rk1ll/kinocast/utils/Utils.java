package com.ov3rk1ll.kinocast.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseIntArray;

import com.google.android.gms.cast.framework.CastContext;
import com.ov3rk1ll.kinocast.api.Parser;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class Utils {
    //public static final String USER_AGENT = "KinoCast v" + BuildConfig.VERSION_NAME;
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";
    public static boolean DisableSSLCheck = false;

    public static boolean isStringEmpty(String val) {
        if (val == null) return true;
        if (val.isEmpty()) return true;
        if (val.equalsIgnoreCase("null")) return true;
        if (val.equalsIgnoreCase("http:null")) return true;
        if (val.equalsIgnoreCase("https:null")) return true;
        if (val.equalsIgnoreCase("https://null")) return true;
        if (val.equalsIgnoreCase("http://null")) return true;
        return false;
    }

    public static String getUrl(String url) {
        if (url != null && url.startsWith("//")) return "https:" + url;
        return url;
    }

    public static String getRedirectTarget(String url) {
        OkHttpClient client = enableTls12OnPreLollipop(new OkHttpClient.Builder()
                .followRedirects(false)
                .addNetworkInterceptor(new UserAgentInterceptor(USER_AGENT))
                .cookieJar(Parser.injectedCookieJar)
        ).build();
        Request request = new Request.Builder().url(url).build();
        try {
            Response response = client.newCall(request).execute();
            String ret = response.header("Location");
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getRedirectTarget(String url, Set<Map.Entry<String, String>> postData) {
        OkHttpClient client = enableTls12OnPreLollipop(new OkHttpClient.Builder()
                .followRedirects(false)
                .addNetworkInterceptor(new UserAgentInterceptor(USER_AGENT))
                .cookieJar(Parser.injectedCookieJar)
        ).build();

        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        for (Map.Entry<String, String> pair : postData) {
            bodyBuilder.addFormDataPart(pair.getKey(), pair.getValue());
        }
        RequestBody requestBody = bodyBuilder.build();
        Request request = new Request.Builder().url(url).post(requestBody).build();
        try {
            Response response = client.newCall(request).execute();
            String ret = response.header("Location");
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getMultiRedirectTarget(String url) {
        String lastRet = url;
        String lastUrl;
        do {
            lastUrl = lastRet;
            lastRet = getRedirectTarget(lastUrl);
        } while (lastRet != null);
        return lastUrl;
    }


    public static JSONObject readJson(String url) {
        OkHttpClient client = buildOkHttpClient();
        Request request = new Request.Builder().url(url).build();

        Log.i("Utils", "read json from " + url);
        try {
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            return new JSONObject(body);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject readJson(String url, Set<Map.Entry<String, String>> postData) {
        OkHttpClient client = buildOkHttpClient();

        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        for (Map.Entry<String, String> pair : postData) {
            bodyBuilder.addFormDataPart(pair.getKey(), pair.getValue());
        }
        RequestBody requestBody = bodyBuilder.build();

        Request request = new Request.Builder().url(url).post(requestBody).build();

        Log.i("Utils", "read json from " + url);
        try {
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            return new JSONObject(body);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Connection buildJsoup(String url) {
        return Jsoup.connect(url)
                .validateTLSCertificates(!DisableSSLCheck)
                .userAgent(Utils.USER_AGENT)
                .timeout(6000);
    }
    public static OkHttpClient buildOkHttpClient() {
        return enableTls12OnPreLollipop(new OkHttpClient.Builder()
                .addNetworkInterceptor(new UserAgentInterceptor(USER_AGENT))
                .cookieJar(Parser.injectedCookieJar)
        ).build();
    }

    public static OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
            try {

                ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
                        .cipherSuites(
                                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                                CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA)
                        .build();
                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(spec);
                specs.add(ConnectionSpec.CLEARTEXT);
                client.connectionSpecs(specs);

               /* SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);

                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init((KeyStore) null);
                    TrustManager[] trustManagers = tmf.getTrustManagers();
                X509TrustManager origTrustmanager = (X509TrustManager) trustManagers[0];

                client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), origTrustmanager);

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);

                client.connectionSpecs(specs);*/
            } catch (Exception exc) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
            }
        }

        return client;
    }
    @SuppressWarnings("deprecation")
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return ((netInfo != null) && netInfo.isConnected());
    }

    public static boolean isDownloadManagerAvailable(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return true;
        }
        return false;
    }
    public static SparseIntArray getWeightedHostList(Context context) {
        SparseIntArray sparseArray = new SparseIntArray();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int count = preferences.getInt("order_hostlist_count", -1);
        if (count == -1) return null;
        for (int i = 0; i < count; i++) {
            int key = preferences.getInt("order_hostlist_" + i, i);
            sparseArray.put(key, i);
        }
        return sparseArray;
    }

    public static SparseIntArray getWeightedParser(Context context) {
        SparseIntArray sparseArray = new SparseIntArray();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int count = preferences.getInt("order_parser_count", -1);
        if (count == -1) return null;
        for (int i = 0; i < count; i++) {
            int key = preferences.getInt("order_parser_" + i, i);
            sparseArray.put(key, i);
        }
        return sparseArray;
    }

    public static CastContext getCastContext(Context context) {
        try {
            return CastContext.getSharedInstance(context);
        } catch (Exception ex) {}
        return  null;
    }
}

