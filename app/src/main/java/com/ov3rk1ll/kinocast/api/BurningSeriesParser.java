package com.ov3rk1ll.kinocast.api;

import android.os.Bundle;
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
import com.ov3rk1ll.kinocast.api.mirror.Vivo;
import com.ov3rk1ll.kinocast.data.Season;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Recaptcha;
import com.ov3rk1ll.kinocast.utils.Utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class BurningSeriesParser extends CachedParser {
    public static final int PARSER_ID =11;
    public static final String URL_DEFAULT = "https://bs.to/";
    public static final String TAG = "BurningSeriesParser";

    private static final SparseIntArray languageResMap = new SparseIntArray();
    private static final SparseArray<String> languageKeyMap = new SparseArray<>();

    static {
        languageResMap.put(2, R.drawable.lang_en);
        languageKeyMap.put(2, "en");
        languageResMap.put(1, R.drawable.lang_de);
        languageKeyMap.put(1, "de");
        languageResMap.put(4, R.drawable.lang_zh);
        languageKeyMap.put(4, "zh");
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
        return "bs.to";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }


    private String getSlug(String url){
        int i = url.indexOf("serie/");
        if(i >= 0) url = url.substring(i + 6);
        i = url.indexOf("/");
        if(i > 0) url = url.substring(0, i);
        return url;
    }

    @Override
    public List<ViewModel> parseList(String url) throws IOException {
        List<ViewModel> list = super.parseList(url);
        if(list != null) return list;

        Log.i(TAG, "parseList: " + url);
        Map<String, String> cookies = new HashMap<>();
        Document doc = getDocument(url, cookies);
        return parseList(doc);
    }
    private List<ViewModel> parseList(Document doc) {
        List<ViewModel> list = new ArrayList<>();

        Elements links = doc.select("li > a[href*=serie/]");

        for (Element link : links) {
            try {
                String url = link.attr("href");
                String slug = getSlug(url);
                if(Utils.isStringEmpty(slug)) continue;
                String title = link.attr("title");
                if(Utils.isStringEmpty(title)) continue;

                ViewModel model = new ViewModel();
                model.setType(ViewModel.Type.SERIES);
                model.setParserId(PARSER_ID);
                model.setTitle(title);
                model.setSlug(slug);
                model.setLanguageResId(R.drawable.lang_de_en);

                list.add(model);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing " + link.html(), e);
            }
        }

        return UpdateModels(list);
    }


    private ViewModel parseDetail(Document doc, ViewModel model, boolean full){
        model.setType(ViewModel.Type.SERIES);
        model.setTitle(doc.select("section.serie #sp_left > h2").first().textNodes().get(0).text().trim());
        model.setSummary(doc.select("section.serie .top p.justify").text());
        model.setLanguageResId(R.drawable.lang_de_en);
        model.setImage(buildUrl(doc.select("section.serie #sp_right > img").attr("src").substring(1)));

        if(full) {
            List<Season> list = new ArrayList<>();

            Elements seasons = doc.select("div#seasons li > a[href*=serie/" + model.getSlug() + "/]");
            Elements langs = doc.select("select.series-language > option");

            for (Element season : seasons) {
                Season s = new Season();
                Set<String> cls = season.parent().classNames();
                for (String cl: cls) {
                    if(cl.startsWith("s") && cl.length() < 4){
                        s.id = Integer.parseInt(cl.substring(1));
                    }
                }

                s.name = season.text();
                List<String> rels = new ArrayList<>();
                for (Element lang : langs) {
                    try {
                        Document docs = getDocument(buildUrl("serie/" + model.getSlug() + "/" + s.id + "/" + lang.val()));
                        String urlses = "serie/" + model.getSlug() + "/" + s.id + "/";
                        Elements links = docs.select("table.episodes td > a[href*=" + urlses + "]");

                        for (Element link : links) {
                            try {
                                String url = link.attr("href");
                                if (!url.endsWith("/de") && !url.endsWith("/en") && !url.endsWith("/des"))
                                    continue;
                                String title = link.attr("title");
                                if (Utils.isStringEmpty(title)) continue;
                                String epslug = url.replace(urlses, "");
                                if (!rels.contains(epslug)) rels.add(epslug);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing " + link.html(), e);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                java.util.Collections.sort(rels, new EpisodeComparator());

                s.episodes = rels.toArray(new String[rels.size()]);
                list.add(s);
            }
            model.setSeasons(list.toArray(new Season[list.size()]));
        }

        return UpdateModel(model);
    }

    @Override
    public ViewModel loadDetail(ViewModel item, boolean showui){
        try {
            item = UpdateModel(item);
            if(item.getSeasons() == null){
                Document doc = super.getDocument(getPageLink(item));
                return parseDetail(doc, item, showui);
            }
        } catch (IOException e) {
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
            model.setSlug(getSlug(url));
            model = parseDetail(doc, UpdateModel(model), false);

            return model;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode) {
        List<Host> hostlist = new ArrayList<>();

        try {
            Document doc = super.getDocument(URL_BASE + "serie/" + item.getSlug() + "/" + season + "/" + episode);

            Elements links = doc.select("ul.hoster-tabs li > a");

            for (Element link : links) {
                String name = link.text().trim();
                Host host = getHostByName(name);
                if (host == null) continue;
                int count = getHostCount(hostlist, host);
                host.setMirror(count + 1);
                host.setSlug(buildUrl(link.attr("href")));
                if (host.isEnabled()) {
                    hostlist.add(host);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hostlist;
    }

    private Host getHostByName(String name) {
        if("streamango".equalsIgnoreCase(name)) return new Streamango();
        if("streamcherry".equalsIgnoreCase(name)) return new StreamCherry();
        if("vidcloud".equalsIgnoreCase(name)) return new VidCloud();
        if("vidoza".equalsIgnoreCase(name)) return new Vidoza();
        if("rapidvideo".equalsIgnoreCase(name)) return new RapidVideo();
        if("openload".equalsIgnoreCase(name)) return new Openload();
        if("vivo".equalsIgnoreCase(name)) return new Vivo();
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
        String uri = url;
        if(!uri.contains("/out/")) {
            try {
                Document doc = getDocument(url);
                uri = buildUrl(doc.select("a.hoster-player[href*=/out/]").attr("href"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        final String[] solvedUrl = {null};

        try {
            Document doc = getDocument(uri);
            String token = "";
            String sec = doc.select("form input[name=s]").val();
            String html = doc.html();
            int i = html.indexOf("'sitekey': '");
            if(i<0) return null;
            String skey = html.substring(i + 12);
            i = skey.indexOf("'");
            if(i<0) return null;
            skey = skey.substring(0, i);
            Recaptcha rc = new Recaptcha(uri, skey, token, true);
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

            String ret = Utils.getRedirectTarget(uri + "?t=" + solvedUrl[0] + "&s=" + sec);
            if (ret != null && !ret.contains("/bs.to/"))
                return ret;

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

    @Override
    public String getPageLink(ViewModel item) {
        return URL_BASE + "serie/" + item.getSlug() + "/de";
    }

    @Override
    public String getCineMovies() {

        return URL_BASE + "andere-serien";
    }

    @Override
    public String PreSaveParserUrl(String newUrl){
        return newUrl.endsWith("/") ? newUrl : newUrl + "/";
    }

    @Override
    public List<Bundle> getMenuItems(){
        List<Bundle> list = new ArrayList<>();
        Bundle b;
        b = buildBundle(getCineMovies(), 0, "Serien");
        if(b != null) list.add(b);
        return list;
    }

    public class EpisodeComparator implements Comparator<String> {

        public EpisodeComparator(){
        }
        private int getEpisodeId(String episode){
            try {
                int i = episode.indexOf("-");
                if(i > 0) return Integer.parseInt(episode.substring(0, i));
            } catch (Exception e) { }
            return 0;
        }

        @Override
        public int compare(String o1, String o2) {
            int w1 = getEpisodeId(o1);
            int w2 = getEpisodeId(o2);
            return compate(w1, w2);
        }

        private int compate(int x, int y){
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }

}
