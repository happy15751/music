package com.example.simplemusic.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PlayingMusic  {
    @PrimaryKey(autoGenerate = true)
    private int id;
    public String artist;
    public String title;
    public String songUrl;
    public String imgUrl;
    public boolean isOnlineMusic;

    public PlayingMusic(String songUrl, String title, String artist, String imgUrl, boolean isOnlineMusic) {
        this.title = title;
        this.artist = artist;
        this.songUrl = songUrl;
        this.imgUrl = imgUrl;
        this.isOnlineMusic = isOnlineMusic;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSongUrl() {
        return songUrl;
    }

    public void setSongUrl(String songUrl) {
        this.songUrl = songUrl;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public boolean isOnlineMusic() {
        return isOnlineMusic;
    }

    public void setOnlineMusic(boolean onlineMusic) {
        isOnlineMusic = onlineMusic;
    }
}
