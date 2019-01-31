package com.ov3rk1ll.kinocast.utils;

import android.content.Context;
import android.os.Build;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.CookieCache;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.android.gms.common.util.Strings;

import org.jsoup.helper.StringUtil;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import okhttp3.Cookie;

public class InjectedCookieJar extends PersistentCookieJar {
    private  CookieCache ccache;

    private InjectedCookieJar(CookieCache cache, CookiePersistor persistor) {
        super(cache, persistor);
        ccache = cache;

    }

    public static InjectedCookieJar Build(Context context){
        return new InjectedCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));
    }

    public Cookie[] toArray(){
        List<Cookie> list = new ArrayList<>();
        Iterator<Cookie> ic = ccache.iterator();
        while(ic.hasNext()){
            list.add(ic.next());
        }
        return list.toArray(new Cookie[list.size()]);
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }

    public void addCookie(HttpCookie hc){
        Cookie.Builder cb = new Cookie.Builder();
        String domain = hc.getDomain();
        if(domain.startsWith("."))  domain = domain.substring(1);
        cb.domain(domain)
                .expiresAt(hc.getMaxAge() * 1000)
                .name(hc.getName())
                .value(hc.getValue())
                .path(hc.getPath());
        if(hc.getSecure()) cb.secure();
        if(hc.getMaxAge()<0) cb.expiresAt(0);
        else cb.expiresAt(System.currentTimeMillis() + (hc.getMaxAge() * 1000));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if(hc.isHttpOnly()) cb.httpOnly();
        }
        addCookie(cb.build());
    }

    public void addCookie(Cookie cookie){
        ccache.addAll(Arrays.asList(cookie));
    }

}
