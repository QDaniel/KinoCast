package com.ov3rk1ll.kinocast.api;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.api.mirror.Openload;
import com.ov3rk1ll.kinocast.api.mirror.Streamango;
import com.ov3rk1ll.kinocast.api.mirror.Vivo;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.ui.MainActivity;
import com.ov3rk1ll.kinocast.utils.Recaptcha;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CineToParser extends Parser {
    public static final int PARSER_ID = 2;
    public static final String URL_DEFAULT = "https://cine.to/";
    public static final String TAG = "CineToParser";

    private static final SparseIntArray languageResMap = new SparseIntArray();
    private static final SparseArray<String> languageKeyMap = new SparseArray<>();

    static {
        languageResMap.put(1, R.drawable.lang_en);
        languageKeyMap.put(1, "en");
        languageResMap.put(2, R.drawable.lang_de);
        languageKeyMap.put(2, "de");
        languageResMap.put(4, R.drawable.lang_zh);
        languageKeyMap.put(4, "zh");
        languageResMap.put(5, R.drawable.lang_es);
        languageKeyMap.put(5, "es");
        languageResMap.put(6, R.drawable.lang_fr);
        languageKeyMap.put(6, "fr");
        languageResMap.put(7, R.drawable.lang_tr);
        languageKeyMap.put(7, "tr");
        languageResMap.put(8, R.drawable.lang_jp);
        languageKeyMap.put(8, "jp");
        languageResMap.put(9, R.drawable.lang_ar);
        languageKeyMap.put(9, "ar");
        languageResMap.put(11, R.drawable.lang_it);
        languageKeyMap.put(11, "it");
        languageResMap.put(12, R.drawable.lang_hr);
        languageKeyMap.put(12, "hr");
        languageResMap.put(13, R.drawable.lang_sr);
        languageKeyMap.put(13, "sr");
        languageResMap.put(14, R.drawable.lang_bs);
        languageKeyMap.put(14, "bs");
        languageResMap.put(15, R.drawable.lang_de_en);
        languageKeyMap.put(15, "de");
        languageResMap.put(16, R.drawable.lang_nl);
        languageKeyMap.put(16, "nl");
        languageResMap.put(17, R.drawable.lang_ko);
        languageKeyMap.put(17, "ko");
        languageResMap.put(24, R.drawable.lang_el);
        languageKeyMap.put(24, "el");
        languageResMap.put(25, R.drawable.lang_ru);
        languageKeyMap.put(25, "ru");
        languageResMap.put(26, R.drawable.lang_hi);
        languageKeyMap.put(26, "hi");
    }

    @Override
    public String getDefaultUrl() {
        return URL_DEFAULT;
    }

    @Override
    public String getParserName() {
        return "Cine.to";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    private List<ViewModel> parseList(JSONObject doc) {
        List<ViewModel> list = new ArrayList<>();
        try {
            JSONArray entries = doc.getJSONArray("entries");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject item = entries.getJSONObject(i);

                try {
                    String lang = item.getString("language");
                    String imdb = item.getString("imdb");
                    for (String ln : lang.split(",")) {
                        ViewModel model = new ViewModel();
                        model.setParserId(PARSER_ID);
                        model.setSlug(imdb);
                        model.setTitle(item.getString("title"));
                        model.setImdbId("tt" + imdb);
                        model.setType(ViewModel.Type.MOVIE);

                        int lnId = Integer.valueOf(ln);
                        model.setLanguageResId(languageResMap.get(lnId));
                        list.add(model);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing " + doc.toString(), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing " + doc.toString(), e);
        }
        return list;
    }

    @Override
    public List<ViewModel> parseList(String url) throws IOException {
        Log.i("Parser", "parseList: " + url);

        HashMap<String, String> data = new HashMap<>();
        data.put("kind", "all");
        data.put("genre", "0");
        data.put("rating", rating);
        data.put("year[0]", "1902");
        data.put("year[1]", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
        data.put("term", term);
        data.put("language", lang);
        data.put("page", "1");
        data.put("count", "25");

        JSONObject doc = Utils.readJson(URL_BASE + "request/search", data.entrySet());
        return parseList(doc);
    }


    private ViewModel parseDetail(JSONObject doc_e, JSONObject doc_l, ViewModel item) {
        try {
            JSONObject entry = doc_e.getJSONObject("entry");
            JSONObject links = doc_l.getJSONObject("links");
            item.setTitle(entry.getString("title"));
            if (item.getLanguageResId() == R.drawable.lang_de) {
                item.setSummary(entry.getString("plot_de"));
            } else {
                item.setSummary(entry.getString("plot_en"));
            }
            item.setType(ViewModel.Type.MOVIE);
            item.setRating(Float.parseFloat(entry.getString("rating")));

            List<Host> hostlist = new ArrayList<>();
            if (links.has("streamango")) {
                JSONArray entries = links.getJSONArray("streamango");
                for (int i = 1; i < entries.length(); i++) {
                    Host h = new Streamango();
                    h.setMirror(i);
                    h.setSlug("https://cine.to/out/" + entries.getInt(i));
                    if (h.isEnabled()) {
                        hostlist.add(h);
                    }
                }
            }
            if (links.has("openload")) {
                JSONArray entries = links.getJSONArray("openload");
                for (int i = 1; i < entries.length(); i++) {
                    Host h = new Openload();
                    h.setMirror(i);
                    h.setSlug("https://cine.to/out/" + entries.getInt(i));
                    if (h.isEnabled()) {
                        hostlist.add(h);
                    }
                }
            }
            if (links.has("vivo")) {
                JSONArray entries = links.getJSONArray("vivo");
                for (int i = 1; i < entries.length(); i++) {
                    Host h = new Vivo();
                    h.setMirror(i);
                    h.setSlug("https://cine.to/out/" + entries.getInt(i));
                    if (h.isEnabled()) {
                        hostlist.add(h);
                    }
                }
            }
            item.setMirrors(hostlist.toArray(new Host[hostlist.size()]));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return item;
    }

    @Override
    public ViewModel loadDetail(ViewModel item) {
        HashMap<String, String> data = new HashMap<>();
        data.put("ID", item.getSlug());
        JSONObject doc_e = Utils.readJson(URL_BASE + "request/entry", data.entrySet());
        data.put("lang", Integer.toString(languageResMap.keyAt(languageResMap.indexOfValue(item.getLanguageResId()))));
        JSONObject doc_l = Utils.readJson(URL_BASE + "request/links", data.entrySet());
        return parseDetail(doc_e, doc_l, item);
    }

    @Override
    public ViewModel loadDetail(String url) {
        if(url.contains("#")) url = url.substring(0, url.indexOf("#"));

        String[] parts = url.split("/");
        ViewModel model = new ViewModel();
        model.setParserId(PARSER_ID);
        model.setSlug(parts[0]);
        model.setImdbId("tt" + parts[0]);
        model.setLanguageResId(Integer.parseInt(parts[1]));
        return loadDetail(model);
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode) {
        String url = "aGET/MirrorByEpisode/?Addr=" + item.getSlug() + "&SeriesID=" + item.getSeriesID() + "&Season=" + season + "&Episode=" + episode;
        try {
            Document doc = getDocument(URL_BASE + url);

            List<Host> hostlist = new ArrayList<>();
            Elements hosts = doc.select("li");
            for (Element host : hosts) {
                int hosterId = Integer.valueOf(host.id().replace("Hoster_", ""));
                String count = host.select("div.Data").text();
                count = count.substring(count.indexOf("/") + 1, count.indexOf(" ", count.indexOf("/")));
                int c = Integer.valueOf(count);
                for (int i = 0; i < c; i++) {
                    Host h = Host.selectById(hosterId);
                    if (h == null) continue;
                    h.setMirror(i + 1);
                    if (h.isEnabled()) {
                        hostlist.add(h);
                    }
                }
            }

            return hostlist;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String getMirrorLink(DetailActivity.QueryPlayTask queryTask, Host host, final String url) {

        String href = "";
        Method getLink = null;
        final boolean[] requestDone = {false};
        final String[] solvedUrl = {null};

        try {
            MainActivity.activity.runOnUiThread(new Runnable() {
                @SuppressLint("SetJavaScriptEnabled")
                @Override
                public void run() {
                    // Virtual WebView
                    final WebView webView = MainActivity.webView;
                    CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(MainActivity.activity);
                    cookieSyncMngr.startSync();
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.setAcceptCookie(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        cookieManager.acceptThirdPartyCookies(webView);
                    }
                    cookieSyncMngr.sync();

                    webView.setVisibility(View.GONE);
                    webView.getSettings().setUserAgentString(Utils.USER_AGENT);
                    webView.getSettings().setJavaScriptEnabled(true);
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            Log.v(TAG, "shouldOverrideUrlLoading: wants to load " + url);
                            solvedUrl[0] = url;
                            requestDone[0] = true;
                            return true;
                        }
                    });
                    webView.loadUrl(url);
                    Log.v(TAG, "load " + url + " in webview");

                }
            });

            int timeout = 50;
            // Wait for the webView to load the correct url
            while (!requestDone[0]) {
                SystemClock.sleep(100);
                timeout--;
                if (timeout <= 0)
                    break;
            }
            if (solvedUrl[0] != null) {

                return solvedUrl[0];
            }


            MainActivity.activity.runOnUiThread(new Runnable() {
                @SuppressLint("SetJavaScriptEnabled")
                @Override
                public void run() {
                    MainActivity.webView.evaluateJavascript(
                            "(function() { return document.documentElement.innerHTML; })();",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String html) {
                                    // code here
                                    Log.d("HTML", html);
                                    solvedUrl[0] = html;
                                    requestDone[0] = true;
                                }
                            });
                }
            });

            timeout = 30;
            // Wait for the webView to load the correct url
            while (!requestDone[0]) {
                SystemClock.sleep(100);
                timeout--;
                if (timeout <= 0)
                    break;
            }

            Pattern pattern = Pattern.compile("gcaptchaSetup \\('([a-zA-Z0-9\\-]*)',", Pattern.CASE_INSENSITIVE);
            // in case you would like to ignore case sensitivity,
            // you could use this statement:
            // Pattern pattern = Pattern.compile("\\s+", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(solvedUrl[0]);
            matcher.find();
            Recaptcha rc = new Recaptcha(url, matcher.group(1), "", false);
            queryTask.getDialog().dismiss();

            final AtomicBoolean done = new AtomicBoolean(false);
            try{
                rc.handle(DetailActivity.activity, new Recaptcha.RecaptchaListener() {
                    @Override
                    public void onHashFound(String hash) {
                        solvedUrl[0] = hash;
                        done.set(true);
                    }

                    @Override
                    public void onError(Exception ex) {
                        done.set(true);
                    }

                    @Override
                    public void onCancel() {
                        done.set(true);
                    }
                });
            }
            catch (Exception e ) {
                e.printStackTrace();
            }
            synchronized (done) {
                while (done.get() == false) {
                    done.wait(1000); // wait here until the listener fires
                }
            }
            return Utils.getRedirectTarget(url + "?token=" + solvedUrl[0]);

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host) {
        return getMirrorLink(queryTask, host, host.getSlug());
    }

    @Override
    public String getMirrorLink(DetailActivity.QueryPlayTask queryTask, ViewModel item, Host host, int season, String episode) {
        return getMirrorLink(queryTask, host, host.getSlug());
    }

    @SuppressWarnings("deprecation")
    @Override
    public String[] getSearchSuggestions(String query) {
        String url = KinoxParser.URL_DEFAULT + "aGET/Suggestions/?q=" + URLEncoder.encode(query) + "&limit=10&timestamp=" + SystemClock.elapsedRealtime();
        String data = getBody(url);
        /*try {
            byte ptext[] = data.getBytes("ISO-8859-1");
            data = new String(ptext, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/
        String suggestions[] = data != null ? data.split("\n") : new String[0];
        if (suggestions[0].trim().equals("")) return null;
        // Remove duplicates
        return new HashSet<>(Arrays.asList(suggestions)).toArray(new String[new HashSet<>(Arrays.asList(suggestions)).size()]);
    }

    @Override
    public String getPageLink(ViewModel item) {
        return item.getSlug() + "/" + Integer.toString(item.getLanguageResId());
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getSearchPage(String query) {
        term = query;
        rating = "1";
        lang = "0";
        return URL_BASE + "search";
    }

    private String term = "";
    private String lang = "0";
    private String rating = "1";

    @Override
    public String getCineMovies() {
        term="";
        rating = "1";
        lang = "2";
        return URL_BASE + "search";
    }

    @Override
    public String PreSaveParserUrl(String newUrl){
        return newUrl.endsWith("/") ? newUrl : newUrl + "/";
    }
}
