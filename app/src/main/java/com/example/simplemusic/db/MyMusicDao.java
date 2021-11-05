package com.example.simplemusic.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;


@Dao
public interface MyMusicDao {
    @Insert
    void insertMyMusic(MyMusic myMusic);

    @Query("select * from mymusic")
    List<MyMusic> queryMyMusic();

    @Query("delete from mymusic")
    void deleteAll();

    @Query("delete from mymusic where title=:title")
    void deleteByTitle(String title);

}

