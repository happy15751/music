package com.example.simplemusic.activity;

import android.app.Application;

import androidx.room.Room;

import com.example.simplemusic.db.DatabaseUtils;


public class MyAPP extends Application {
    private static MyAPP instance;
    private DatabaseUtils wordDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public synchronized DatabaseUtils getDatabase() {
        if (wordDatabase == null) {
            wordDatabase = Room.databaseBuilder(this, DatabaseUtils.class, "music_catabase")
                    .fallbackToDestructiveMigration()//数据库更新时删除数据重新创建
                    .allowMainThreadQueries().build();

        }
        return wordDatabase;
    }

    public static MyAPP getInstance() {
        return instance;
    }
}
