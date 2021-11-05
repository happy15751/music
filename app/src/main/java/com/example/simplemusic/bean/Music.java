package com.example.simplemusic.bean;

import java.util.Objects;

public class Music {
    public String artist;
    public String title;
    public String songUrl;
    public String imgUrl;
    public boolean isOnlineMusic;

    public Music(String songUrl, String title, String artist, String imgUrl, boolean isOnlineMusic) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.imgUrl = imgUrl;
        this.isOnlineMusic = isOnlineMusic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Music music = (Music) o;
        return Objects.equals(title, music.title);
    }
}
