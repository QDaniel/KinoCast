package com.ov3rk1ll.kinocast.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ov3rk1ll.kinocast.R;

import java.util.concurrent.atomic.AtomicBoolean;

public class Recaptcha {
    private static final String INTERCEPT = "_intercept?";
    private static final String FALLBACK_INTERCEPT = "_fallback";
    private static final String FALLBACK_FILTER = "g-recaptcha-response=";

    private final String baseUrl, publicKey, sToken;
    private final Boolean useButton;

    private static final String getRecaptchaHtml(String publicKey, String sToken, boolean useButton) {
        String elm = useButton ? "button": "div";
        String elm_txt = useButton ? "Weiter": "";
        return
                "<script type=\"text/javascript\">" +
                        "window.globalOnCaptchaEntered = function(res) { " +
                        "location.href = \"" + INTERCEPT + "\" + res; " +
                        "}" +
                        "</script>" +
                        "<script src=\"https://www.google.com/recaptcha/api.js\" async defer></script>" +
                        "<form action=\"" + FALLBACK_INTERCEPT + "\" method=\"GET\" id=\"_overchan_submitform\">" +
                        "<" + elm + " class=\"g-recaptcha\" data-sitekey=\"" + publicKey + "\" " +
                        (sToken != null && sToken.length() > 0 ? ("data-stoken=\"" + sToken + "\" ") : "") +
                        "data-callback=\"globalOnCaptchaEntered\">" + elm_txt + "</" + elm + ">" +
                        "</form>" +
                        "<script type=\"text/javascript\">" +
                        "function _overchan_add_fallback_submit() { " +
                        "var element = document.createElement(\"input\"); " +
                        "element.setAttribute(\"type\", \"submit\"); " +
                        "element.setAttribute(\"value\", \"Submit\");" +
                        "var foo = document.getElementById(\"_overchan_submitform\"); " +
                        "foo.appendChild(element); " +
                        "}" +
                        "</script>";
    }

    private volatile boolean done = false;
    private volatile String pushedHash = null;

    /**
     * @param baseUrl URL, с которого должна открываться капча
     * @param publicKey открытый ключ
     * @param sToken Secure Token
     */
    public Recaptcha(String baseUrl, String publicKey, String sToken, boolean useButton) {
        this.baseUrl = baseUrl;
        this.publicKey = publicKey;
        this.sToken = sToken;
        this.useButton = useButton;
    }

    public void handle(final Activity activity, final RecaptchaListener callback) {
        activity.runOnUiThread(new Runnable() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void run() {
                final Dialog dialog = new Dialog(activity);
                WebView webView = new WebView(activity);
                webView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                webView.setWebViewClient(new WebViewClient() {
                    AtomicBoolean fallbackButtonAdded = new AtomicBoolean(false);
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        if (url.contains(INTERCEPT) || url.contains(FALLBACK_INTERCEPT)) {
                            String hash = url.contains(INTERCEPT) ? url.substring(url.indexOf(INTERCEPT) + INTERCEPT.length()) :
                                    (url.contains(FALLBACK_FILTER) ? url.substring(url.indexOf(FALLBACK_FILTER) + FALLBACK_FILTER.length()) : null);
                            if (hash != null && hash.length() > 0 && !hash.equals(pushedHash)) {
                                pushedHash = hash;
                                callback.onHashFound(pushedHash);
                            }
                            if (!done) activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!done) {
                                        done = true;
                                        dialog.dismiss();
                                    }
                                }
                            });
                        }
                        super.onPageStarted(view, url, favicon);
                    }
                    @Override
                    public void onLoadResource(WebView view, String url) {
                        if (url.contains("/api/fallback?") && fallbackButtonAdded.compareAndSet(false, true)) {
                            view.loadUrl("javascript:_overchan_add_fallback_submit()");
                        }
                        super.onLoadResource(view, url);
                    }
                    @Override
                    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                        new AlertDialog.Builder(activity).
                                setTitle(R.string.error_ssl).
                                setMessage(R.string.ssl_connect_anyway).
                                setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        handler.proceed();
                                    }
                                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.cancel();
                            }
                        }).show();
                    }
                });
                //webView.getSettings().setUserAgentString(HttpConstants.USER_AGENT_STRING);
                webView.getSettings().setJavaScriptEnabled(true);
                dialog.setTitle("Recaptcha");
                dialog.setContentView(webView);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (!done) {
                            done = true;
                            callback.onCancel();
                        }
                    }
                });
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                dialog.show();
                String url = baseUrl != null ? baseUrl : "https://127.0.0.1/";
                webView.loadDataWithBaseURL(url, getRecaptchaHtml(publicKey, sToken, useButton), "text/html", "UTF-8", null);
            }
        });
    }

    public interface RecaptchaListener {
        void onHashFound(String hash);
        void onError(Exception ex);
        void onCancel();
    }
}
