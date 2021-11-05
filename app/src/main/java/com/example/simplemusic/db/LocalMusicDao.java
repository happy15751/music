package com.example.simplemusic.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;


@Dao
public interface LocalMusicDao {
    @Insert
    void inserLocalMusic(LocalMusic user);

    @Query("select * from localmusic")
    List<LocalMusic> queryLocalMusic();

    @Query("delete from localmusic")
    void deleteAll();
    @Query("delete from localmusic where title=:title")
    void deleteByTitle(String title);
}

