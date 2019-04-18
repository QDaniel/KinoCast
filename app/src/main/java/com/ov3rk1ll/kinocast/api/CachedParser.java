package com.ov3rk1ll.kinocast.api;

import com.ov3rk1ll.kinocast.api.mirror.Host;
import com.ov3rk1ll.kinocast.data.ViewModel;
import com.ov3rk1ll.kinocast.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class CachedParser extends Parser {
    protected final List<ViewModel> lastModels = new ArrayList<>();

    protected List<ViewModel> UpdateModels(List<ViewModel> models){
        List<ViewModel> list = new ArrayList<>();
        for ( ViewModel m: models) {
            list.add(UpdateModel(m));
        }
        return list;
    }

    protected ViewModel UpdateModel(ViewModel model){
        if(model == null) return null;
        for ( ViewModel m: lastModels) {
            if(m.getSlug().equalsIgnoreCase(model.getSlug())) {
                if(!Utils.isStringEmpty(model.getImage())) m.setImage(model.getImage());
                if(!Utils.isStringEmpty(model.getGenre())) m.setGenre(model.getGenre());
                if(!Utils.isStringEmpty(model.getImdbId())) m.setImdbId(model.getImdbId());
                if(!Utils.isStringEmpty(model.getTitle())) m.setTitle(model.getTitle());
                if(!Utils.isStringEmpty(model.getSummary())) m.setSummary(model.getSummary());
                if(m.getType() == ViewModel.Type.SERIES){
                    if(model.getSeasons() != null && model.getSeasons().length > 0) m.setSeasons(model.getSeasons());
                    if(model.getSeriesID() > 0) m.setSeriesID(model.getSeriesID());
                } else {
                    if(model.getMirrors() != null && model.getMirrors().length > 0) m.setMirrors(model.getMirrors());
                }
                return m;
            }
        }
        lastModels.add(model);
        return model;
    }

    protected ViewModel FindModel(String slug){
        for ( ViewModel m: lastModels) {
            if(m.getSlug().equalsIgnoreCase(slug)) {
                return m;
            }
        }
        return null;
    }

    @Override
    public ViewModel loadDetail(ViewModel item, boolean showui) {
        return UpdateModel(item);
    }

    @Override
    public List<Host> getHosterList(ViewModel item, int season, String episode) {
        item = loadDetail(item, true);
        List<Host> hostlist = new ArrayList<>();
        if(item.getMirrors() != null) {
            for (Host host : item.getMirrors()) {
                hostlist.add(host);
            }
        }
        return hostlist;
    }

    @Override
    public List<ViewModel> parseList(String url) throws IOException {
        if(!Utils.isStringEmpty(url) && url.startsWith("search:")){
            return searchModels(url.substring(7));
        }
        return null;
    }

    @Override
    public String[] getSearchSuggestions(String query) {
        List<String> list = new ArrayList<>();
        String[] cols = query.split("\\s+");
        for (ViewModel m: lastModels ) {
            boolean found = false;
            for ( String c: cols ) {
                found = strContains(m.getTitle(), c) || strContains(m.getSlug(), c) || strContains(m.getSummary(), c);
                if(!found) break;
            }
            if(found) list.add(m.getTitle());
        }
        return list.toArray(new String[list.size()]);
    }

    public List<ViewModel> searchModels(String query) {
        List<ViewModel> list = new ArrayList<>();
        String[] cols = query.toLowerCase().split("\\s+");
        for (ViewModel m: lastModels ) {
            boolean found = false;
            for ( String c: cols ) {
                found = strContains(m.getTitle(), c) || strContains(m.getSlug(), c) || strContains(m.getSummary(), c);
                if(!found) break;
            }
            if(found) list.add(m);
        }
        return list;
    }

    private boolean strContains(String content, String search){
        if(content==null || search == null) return false;
        return  content.toLowerCase().contains(search);
    }
    @Override
    public String getSearchPage(String query) {
        return "search:" + query;
    }
}
