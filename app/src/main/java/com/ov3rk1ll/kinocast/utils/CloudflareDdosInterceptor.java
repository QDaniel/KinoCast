package com.ov3rk1ll.kinocast.utils;

import android.content.Context;
import android.util.Log;

import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.ui.DetailActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
            final String[] solvedUrl = {null};

            if (body.contains("DDoS protection by Cloudflare") && !request.url().toString().contains("/cdn-cgi/l/chk_jschl")) {
                if(body.contains("\"class=\\\"g-recaptcha\"")) {

                    Document doc = Jsoup.parse(body);
                    String token = "";
                    String sec = doc.select("form input[name=s]").val();
                    String html = doc.html();
                    int i = html.indexOf("'sitekey': '");
                    if(i<0) return null;
                    String skey = html.substring(i + 12);
                    i = skey.indexOf("'");
                    if(i<0) return null;
                    skey = skey.substring(0, i);

                    Recaptcha rc = new Recaptcha(request.url().toString(), skey, token, true);
                    final AtomicBoolean done = new AtomicBoolean(false);
                    try{
                        rc.handle(DetailActivity.activity, new Recaptcha.RecaptchaListener() {
                            @Override
                            public void onHashFound(String hash) {
                                solvedUrl[0] = hash;
                                done.set(true);
                            }

                            @Override
                            public void onError(Exception ex)
                            {
                                done.set(true);
                            }

                            @Override
                            public void onCancel()
                            {
                                done.set(true);
                            }
                        });

                        synchronized (done) {
                            while (done.get() == false) {
                                done.wait(1000); // wait here until the listener fires
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    throw new IOException("Cloudflare need recapcha please wait. H:" + String.valueOf(solvedUrl[0]));
                }
                final CountDownLatch latch = new CountDownLatch(1);
                Cloudflare cf = new Cloudflare(request.url().toString());
                cf.setUser_agent(Utils.USER_AGENT);
                cf.getCookies(new Cloudflare.cfCallback() {
                    @Override
                    public void onSuccess(List<HttpCookie> cookieList) {
                        if(cookieList != null) {
                            Log.v(TAG, "Success: Cloudflare.getCookies : " + Cloudflare.listToString(cookieList));
                            InjectedCookieJar jar = (InjectedCookieJar) Parser.getInstance().getClient().cookieJar();

                            for (HttpCookie cookie : cookieList) {
                                jar.addCookie(cookie);
                            }
                            jar.save();
                        }
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
