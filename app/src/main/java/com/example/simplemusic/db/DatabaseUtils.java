package com.example.simplemusic.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;


@Database(entities = {MyMusic.class, LocalMusic.class,PlayingMusic.class}, version = 1, exportSchema = false)
public abstract class DatabaseUtils extends RoomDatabase {

    public abstract MyMusicDao myMusicDao();

    public abstract LocalMusicDao localMusicDao();
    public abstract PlayingMusicDao PlayingMusic();

}
