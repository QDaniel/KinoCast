package com.ov3rk1ll.kinocast.api;

import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.api.mirror.Openload;
import com.ov3rk1ll.kinocast.api.mirror.RapidVideo;
import com.ov3rk1ll.kinocast.api.mirror.StreamCherry;
import com.ov3rk1ll.kinocast.api.mirror.Streamango;
import com.ov3rk1ll.kinocast.api.mirror.VidCloud;
import com.ov3rk1ll.kinocast.api.mirror.Vidoza;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Recaptcha;
import com.ov3rk1ll.kinocast.utils.TheMovieDb;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamworldParser extends Parser {
    public static final int PARSER_ID =10;
    public static final String URL_DEFAULT = "https://streamworld.co/";
    public static final String TAG = "StreamworldParser";

    private static final SparseIntArray languageResMap = new SparseIntArray();
    private static final SparseArray<String> languageKeyMap = new SparseArray<>();

    static {
        languageResMap.put(2, R.drawable.lang_en);
        languageKeyMap.put(2, "en");
        languageResMap.put(1, R.drawable.lang_de);
        languageKeyMap.put(1, "de");
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
        return "Streamworld.cc";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    @Override
    public List<ViewModel> parseList(String url) throws Exception {
        Log.i("Parser", "parseList: " + url);
        Document doc = getDocument(url);
        return parseList(doc);
    }
    private List<ViewModel> parseList(Document doc) {
        List<ViewModel> list = new ArrayList<>();

        Elements files = doc.select("td[colspan=2]");

        for (Element element : files) {
            element = element.parent();
            try {
                Element link = element.select("strong > a").first();
                if(Utils.isStringEmpty(link.text())) continue;

                ViewModel model = new ViewModel();
                model.setParserId(PARSER_ID);
                model.setTitle(link.text());
                String url = link.attr("href");
                model.setSlug(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));

                String ln = element.select("img[src*=languages]").attr("src");
                ln = ln.substring(ln.lastIndexOf("/") + 1);
                ln = ln.substring(0, ln.indexOf("."));
                int lnId = Integer.valueOf(ln);
                model.setLanguageResId(languageResMap.get(lnId));

               /* String genre = element.select("div.Genre > div.floatleft").eq(1).text();
                if(!Utils.isStringEmpty(genre)) {
                    genre = genre.substring(genre.indexOf(":") + 1).trim();
                    if (genre.contains(",")) genre = genre.substring(0, genre.indexOf(","));
                    model.setGenre(genre);
                }*/

                String rating = element.select("div.Genre > div.floatright").text();
                //rating = rating.substring(rating.indexOf(":") + 1, rating.indexOf("/") - 1);
                //model.setRating(Float.valueOf(rating.trim()));

                list.add(model);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing " + element.html(), e);
            }
        }

        if(list.isEmpty()) {
            files = doc.select("table tr");

            for (Element element : files) {
                if(element.select("td").size()<5) continue;

                try {
                    ViewModel model = new ViewModel();
                    model.setParserId(PARSER_ID);

                    String typ = element.selectFirst("td").text();
                    if("Serie".equalsIgnoreCase(typ)) model.setType(ViewModel.Type.SERIES);
                    Element a = element.selectFirst("td > .otherLittles a");
                    String url = a.attr("href");
                    model.setTitle(a.text());
                    model.setSlug(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));

                    String ln = element.select("img[src*=languages]").attr("src");
                    ln = ln.substring(ln.lastIndexOf("/") + 1);
                    ln = ln.substring(0, ln.indexOf("."));
                    int lnId = Integer.valueOf(ln);
                    model.setLanguageResId(languageResMap.get(lnId));

                    list.add(model);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing " + element.html(), e);
                }
            }
        }
        return list;
    }


    private ViewModel parseDetail(Document doc, ViewModel model){

        model.setTitle(doc.select("div#content > h1").text());
        model.setSummary(doc.select("div#descrArea").text());
        String ln = doc.select("img[src*=languages]").attr("src");
        ln = ln.substring(ln.lastIndexOf("/") + 1);
        ln = ln.substring(0, ln.indexOf("."));
        int lnId = Integer.valueOf(ln);
        model.setLanguageResId(languageResMap.get(lnId));

        model.setGenre(doc.select("span.genres").text());

        String imdburl = doc.select("div#content a[href*=imdb.com]").attr("href");
        model.setImdbId(TheMovieDb.getImdbId(imdburl));
        String rating = doc.select("span#imdbArea span.otherLittles").text();
        //rating = rating.substring(0, rating.indexOf("/"));
        rating = rating.trim();
        model.setRating(Float.valueOf(rating));

        return model;
    }

    @Override
    public ViewModel loadDetail(ViewModel item, boolean showui){
        try {
            Document doc = super.getDocument(URL_BASE + "film/" + item.getSlug() + ".html");

            return parseDetail(doc, item);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }

    @Override
    public ViewModel loadDetail(String url) {
        if(url.contains("#")) url = url.substring(0, url.indexOf("#"));
        ViewModel model = new ViewModel();
        model.setParserId(PARSER_ID);
        try {
            Document doc = getDocument(url);
            model.setSlug(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));
            model = parseDetail(doc, model);

            return model;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode) {
        List<Host> hostlist = new ArrayList<>();

        try {
            Document doc = super.getDocument(URL_BASE + "film/" + item.getSlug() + ".html");

            Elements formats = doc.select("table td a");
            String comment;

            for (Element format : formats) {
                String tf =format.text().trim();
                if(!format.attr("href").startsWith("/film/") ||  !tf.contains(" (") ||  !tf.endsWith(")") || tf.length() > 30) continue;

                comment = tf.substring(0, tf.indexOf(" ("));

                Document docf = super.getDocument(URL_BASE + format.attr("href").substring(1));

                Elements files = docf.select("table th.thlink a[href*=/streams-]");
                for (Element file : files) {

                    Document doch = super.getDocument(URL_BASE + file.attr("href").substring(1));

                    Elements mirrors = doch.select("table td a[href*=/stream/]");
                    for (Element mirror : mirrors) {

                        String name = mirror.attr("title");
                        Host host = getHostByName(name);
                        if (host == null) continue;
                        int count = getHostCount(hostlist, host);

                        host.setMirror(count);
                        host.setComment(comment);
                        host.setSlug(URL_BASE + mirror.attr("href").substring(1));
                        if (host.isEnabled()) {
                            hostlist.add(host);
                        }
                    }

                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return hostlist;
    }

    private Host getHostByName(String name) {
        if("streamango.com".equalsIgnoreCase(name)) return new Streamango();
        if("streamcherry.com".equalsIgnoreCase(name)) return new StreamCherry();
        if("vidcloud.co".equalsIgnoreCase(name)) return new VidCloud();
        if("vidoza.net".equalsIgnoreCase(name)) return new Vidoza();
        if("rapidvideo.com".equalsIgnoreCase(name)) return new RapidVideo();
        if("openload.co".equalsIgnoreCase(name)) return new Openload();
        return null;
    }

    private int getHostCount(List<Host> hosts, Host host) {
        int count = 0;
        for (Host h:hosts) {
            if(h.getClass().getName() == host.getClass().getName()) count++;
        }
        return count;
    }

    private String getMirrorLink(DetailActivity.QueryPlayTask queryTask, Host host, final String url) {

        final String[] solvedUrl = {null};

        try {
            Document doc = super.getDocument(url);

            Elements cap = doc.select("button.g-recaptcha");

            Recaptcha rc = new Recaptcha(url, cap.attr("data-sitekey"), "", true);
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
            }
            catch (Exception e ) {
                e.printStackTrace();
            }
            synchronized (done) {
                while (done.get() == false) {
                    done.wait(1000); // wait here until the listener fires
                }
            }

            HashMap<String, String> data = new HashMap<>();
            data.put("g-recaptcha-response", solvedUrl[0]);
            return Utils.getRedirectTarget(url, data.entrySet());

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
        return URL_BASE + "film/" + item.getSlug() + ".html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getSearchPage(String query) {
        return URL_BASE + "search";
    }

    @Override
    public String getCineMovies() {

        return URL_BASE;
    }

    @Override
    public String getPopularMovies() {
         return URL_BASE + "filme/sortieren/nach/imdb.html";
    }

    @Override
    public String getLatestMovies() {
        return URL_BASE + "filme/sortieren/nach/eingetragen.html";
    }

    @Override
    public String getPopularSeries() {
        return URL_BASE + "serien/sortieren/nach/imdb.html";
    }

    @Override
    public String getLatestSeries() {
        return URL_BASE + "serien/sortieren/nach/eingetragen.html";
    }

    @Override
    public String PreSaveParserUrl(String newUrl){
        return newUrl.endsWith("/") ? newUrl : newUrl + "/";
    }
}
