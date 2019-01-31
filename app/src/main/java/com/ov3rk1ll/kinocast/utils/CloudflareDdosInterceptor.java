package com.ov3rk1ll.kinocast.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.ui.MainActivity;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@SuppressWarnings("deprecation")
public class CloudflareDdosInterceptor implements Interceptor {
    private static final String TAG = CloudflareDdosInterceptor.class.getSimpleName();

    private Context context;

    public CloudflareDdosInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();

        final Response response = chain.proceed(request);
        if(response.code() == 503) {
            Log.d(TAG, "intercept: Status " + response.code() + " for " + request.url());
            Log.d(TAG, "intercept: Cookie: " + response.header("Set-Cookie"));
            Log.v(TAG, "intercept: try to handle request to " + request.url().toString());
            String body = response.body().string();

            if (body.contains("DDoS protection by Cloudflare") && !request.url().toString().contains("/cdn-cgi/l/chk_jschl")) {
                final CountDownLatch latch = new CountDownLatch(1);
                Cloudflare cf = new Cloudflare(request.url().toString());
                cf.setUser_agent(Utils.USER_AGENT);
                cf.getCookies(new Cloudflare.cfCallback() {
                    @Override
                    public void onSuccess(List<HttpCookie> cookieList) {
                        Log.v(TAG, "Success: Cloudflare.getCookies : " + Cloudflare.listToString(cookieList));
                        InjectedCookieJar jar = (InjectedCookieJar) Parser.getInstance().getClient().cookieJar();

                        for (HttpCookie cookie : cookieList) {
                            jar.addCookie(cookie);
                        }
                        jar.save();
                        latch.countDown();
                    }

                    @Override
                    public void onFail() {
                        Log.e(TAG, "Fail: Cloudflare.getCookies for " + request.url());
                        latch.countDown();
                    }
                });

                try {
                    latch.await(10L, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Request.Builder build = request.newBuilder();
                // run the action request again
                Log.v(TAG, "intercept: will retry request to " + request.url());
                return chain.proceed(build.build());
            }
        }
        return response;
    }
}
