package com.ov3rk1ll.kinocast.api.mirror;

import android.net.Uri;

import com.ov3rk1ll.kinocast.ui.DetailActivity;
import com.ov3rk1ll.kinocast.utils.Utils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

public abstract class Host implements Serializable {
    protected int mirror;
    protected String url;
    protected String urlVideo;
    protected String slug;
    protected String comment;

    public static Class<?>[] HOSTER_LIST = {
            DivxStage.class,
            NowVideo.class,
            SharedSx.class,
            Sockshare.class,
            StreamCloud.class,
            Vodlocker.class,
            StreamCherry.class,
            Streamango.class,
            Vidoza.class,
            VShare.class,
            Vidlox.class,
            VidCloud.class,
            Vivo.class,
            Openload.class,
            RapidVideo.class,
            VeryStream.class,
            VevIo.class,
            Direct.class
    };

    public static Host selectById(int id) {
        for (Class<?> h : HOSTER_LIST) {
            try {
                Host host = (Host) h.getConstructor().newInstance();
                if (host.getId() == id) {
                    return host;
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Host selectByUri(Uri uri ) {
        for (Class<?> h : HOSTER_LIST) {
            try {
                Host host = (Host) h.getConstructor().newInstance();
                if (host.canHandleUri(uri)) {
                    host.handleUri(uri);
                    return host;
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Host() {

    }

    public Host(int mirror) {
        this.mirror = mirror;
    }

    public boolean isEnabled() {
        return false;
    }

    public abstract int getId();

    public abstract String getName();

    public int getMirror() {
        return mirror;
    }

    public void setMirror(int mirror) {
        this.mirror = mirror;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVideoUrl() {
        return urlVideo;
    }

    public void setVideoUrl(String url) {
        this.urlVideo = url;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getVideoPath(DetailActivity.QueryPlayTask queryTask) {
        return null;
    }

    @Override
    public String toString() {
        String text = getName() + " #" + mirror;
        if(!Utils.isStringEmpty(comment)) text = text + " (" + comment + ")";
        return text;
    }

    public Boolean canHandleUri(Uri uri) {
        return false;
    }
    public void handleUri(Uri uri) {

    }

}
