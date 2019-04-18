package com.ov3rk1ll.kinocast.data;

import android.content.Context;

import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.api.mirror.Host;

import java.io.Serializable;

public class ViewModel implements Serializable {

    private int seriesID;
    private int min_age;
    private String slug;
    private String title;
    private String image;
    private String summary;
    private float rating;
    private int languageResId;
    private String genre;
    private String imdbId;
    @SuppressWarnings("unused")
    private String year;

    private Type type;
    private int parserId;

    private transient Season[] seasons;
    private transient Host[] mirrors;
    private transient Parser parser;

    public ViewModel() {
        min_age = -1;
    }

    public int getSeriesID() {
        return seriesID;
    }

    public void setSeriesID(int seriesID) {
        this.seriesID = seriesID;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImage() {
        return this.image;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getLanguageResId() {
        return languageResId;
    }

    public void setLanguageResId(int languageResId) {
        this.languageResId = languageResId;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Season[] getSeasons() {
        return seasons;
    }

    public void setSeasons(Season[] seasons) {
        this.seasons = seasons;
    }

    public Host[] getMirrors() {
        return mirrors;
    }

    public void setMirrors(Host[] mirrors) {
        this.mirrors = mirrors;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public int getMinAge() { return min_age; }

    public void setMinAge(int min_age) { this.min_age = min_age; }

    public int getParserId() {
        return parserId;
    }

    public void setParserId(int parserId) {
        this.parserId = parserId;
    }

    public Parser getParser(Context context){
        if(parser == null || parser.getParserId()!= parserId) {
            parser = Parser.getParser(context, parserId);
        }
        return parser;
    }

    public enum Type{
        MOVIE,
        SERIES,
        DOCUMENTATION
    }
}
