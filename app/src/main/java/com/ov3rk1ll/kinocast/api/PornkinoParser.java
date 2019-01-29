package com.ov3rk1ll.kinocast.api;

import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
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

import org.jsoup.Jsoup;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Response;

public class PornkinoParser extends Parser {
    public static final int PARSER_ID = 12;
    public static final String URL_DEFAULT = "https://pornkino.to/";
    public static final String TAG = "PornkinoParser";

    @Override
    public String getDefaultUrl() {
        return URL_DEFAULT;
    }

    @Override
    public String getParserName() {
        return "pornkino.to (Beta)";
    }

    @Override
    public int getParserId() {
        return PARSER_ID;
    }

    @Override
    public List<ViewModel> parseList(String url) throws IOException {
        Log.i(TAG, "parseList: " + url);
        Document doc = getDocument(url);
        return parseList(doc);
    }
    private List<ViewModel> parseList(Document doc) {
        List<ViewModel> list = new ArrayList<>();

        Elements files = doc.select("article.loop-entry");

        for (Element element : files) {
             try {
                Element link = element.select("a").first();
                if(Utils.isStringEmpty(link.attr("title"))) continue;

                ViewModel model = new ViewModel();
                model.setParserId(PARSER_ID);
                model.setTitle(link.attr("title"));
                String url = link.attr("href");
                model.setSlug(getSlug(url));
                Element img = element.select("a img").first();
                model.setImage(img.attr("file"));

                model.setLanguageResId(R.drawable.lang_de);

               /* String genre = element.select("div.Genre > div.floatleft").eq(1).text();
                if(!Utils.isStringEmpty(genre)) {
                    genre = genre.substring(genre.indexOf(":") + 1).trim();
                    if (genre.contains(",")) genre = genre.substring(0, genre.indexOf(","));
                    model.setGenre(genre);
                }*/

                list.add(model);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing " + element.html(), e);
            }
        }

        return list;
    }


    private ViewModel parseDetail(Document doc, ViewModel model, boolean showui){


        model.setTitle(doc.select("div#content > h1.post-header-title").text());
        model.setSummary(doc.select("div#content p.description").text());
        model.setLanguageResId(R.drawable.lang_de);
        model.setType(ViewModel.Type.MOVIE);

        if(showui) {
            Elements files = doc.select("div#content div.entry_left a[target=_blank]");
            List<Host> hostlist = new ArrayList<>();

            for (Element element : files) {
                String hurl = element.attr("href");

                Host h = Host.selectByUri(Uri.parse(hurl));
                if (h != null && h.isEnabled()) {
                    hostlist.add(h);
                    continue;
                }

                if(!element.html().contains("openload.co")
                    && !element.html().contains("streamcherry.com")
                    && !element.html().contains("vidoza.net")) continue;

                try {
                   if(hurl.contains("filecrypt.cc")) {
                        Document fdoc = getDocument(hurl);

                        Elements links = fdoc.select("tr.kwj3");
                        for(Element link: links) {
                            String name = link.select("td a.external_link").text();
                            Host host = getHostByName(name);
                            if (host == null) continue;
                            int count = getHostCount(hostlist, host);
                            host.setMirror(count + 1);

                            String lid = link.select("td button").attr("onclick");
                            if(!lid.startsWith("openLink('")) continue;
                            lid = lid.substring(10);
                            lid = lid.substring(0, lid.indexOf("'"));
                            host.setSlug("http://filecrypt.cc/Link/" + lid+ ".html");
                            if(links.size()>1) host.setComment(link.select("td[title]").first().textNodes().get(0).text().trim());

                            if (host.isEnabled()) {
                                hostlist.add(host);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing " + element.html(), e);
                }
            }
            model.setMirrors(hostlist.toArray(new Host[hostlist.size()]));
        }

   //     String imdburl = doc.select("div#content a[href*=imdb.com]").attr("href");

        return model;
    }

    @Override
    public ViewModel loadDetail(ViewModel item, boolean showui){
        try {
            Document doc = super.getDocument(buildUrl(item.getSlug()));

            return parseDetail(doc, item, showui);
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
            model = parseDetail(doc, model, false);

            return model;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String getSlug(String url){
        if(url.contains("#")) url = url.substring(0, url.indexOf("#"));
        return url.replace(URL_DEFAULT, "");
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode) {
        List<Host> hostlist = new ArrayList<>();
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

        try {

            Response resp = getDocumentResponse(url, null);
            String ret = resp.header("Location");
            if(!Utils.isStringEmpty(ret)){
                return ret;
            }
            String body = resp.body().string();
            if (resp.code() != 200) {
                Log.d(TAG, body);
                throw new IOException("Unexpected status code " + resp.code());
            }
            if (TextUtils.isEmpty(body)) {
                throw new IOException("Body for " + url + " is empty");
            }
            Document doc = Jsoup.parse(body);
            String u = doc.select("iframe").attr("src");
            if(!Utils.isStringEmpty(u)){
                return Utils.getRedirectTarget(u);
            }

            String html = doc.html();
            if(html.contains("location.href='")){
                u = html.substring(html.indexOf("location.href='")+15);
                u = u.substring(0,u.indexOf("'"));
                return Utils.getRedirectTarget(u);
            }

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
        return new String[0];
    }

    @Override
    public String getPageLink(ViewModel item) {
        return URL_BASE + item.getSlug();
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getSearchPage(String query) {
        return URL_BASE  +"?s=" + URLEncoder.encode(query);
    }

    @Override
    public String getCineMovies() {

        return URL_BASE;
    }

    @Override
    public String PreSaveParserUrl(String newUrl){
        return newUrl.endsWith("/") ? newUrl : newUrl + "/";
    }

    @Override
    public List<Bundle> getMenuItems(){
        List<Bundle> list = new ArrayList<>();
        Bundle b;
        b = buildBundle(getCineMovies(), 0, "XXX");
        if(b != null) list.add(b);
        return list;
    }
}
