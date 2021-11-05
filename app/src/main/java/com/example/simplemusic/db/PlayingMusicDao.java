package com.example.simplemusic.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;


@Dao
public interface PlayingMusicDao {
    @Insert
    void insertPlayingMusic(PlayingMusic myMusic);

    @Query("select * from playingmusic")
    List<PlayingMusic> queryPlayingMusic();

    @Query("delete from playingmusic")
    void deleteAll();

    @Query("delete from playingmusic where title=:title")
    void deleteByTitle(String title);
}

