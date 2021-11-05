package com.example.simplemusic.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.example.simplemusic.activity.MyAPP;
import com.example.simplemusic.bean.Music;
import com.example.simplemusic.util.Utils;
import com.example.simplemusic.db.PlayingMusic;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private MediaPlayer player;
    private List<Music> playingMusicList;
    private List<OnStateChangeListenr> listenrList;
    private MusicServiceBinder binder;
    private AudioManager audioManager;
    private Music currentMusic;
    private boolean autoPlayAfterFocus;
    private boolean isNeedReload;
    private int playMode;
    private SharedPreferences spf;

    @Override
    public void onCreate() {
        super.onCreate();
        initPlayList();
        listenrList = new ArrayList<>();
        player = new MediaPlayer();
        player.setOnCompletionListener(onCompletionListener);
        binder = new MusicServiceBinder();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()) {
            player.stop();
        }
        player.release();

        playingMusicList.clear();
        listenrList.clear();
        handler.removeMessages(66);
        audioManager.abandonAudioFocus(audioFocusListener);
    }

    public interface OnStateChangeListenr {
        void onPlayProgressChange(long played, long duration);

        void onPlay(Music item);

        void onPause();
    }

    public class MusicServiceBinder extends Binder {

        public void addPlayList(Music item) {
            addPlayListInner(item);
        }

        public void addPlayList(List<Music> items) {
            addPlayListInner(items);
        }

        public void removeMusic(int i) {
            removeMusicInner(i);
        }

        public void playOrPause() {
            if (player.isPlaying()) {
                pauseInner();
            } else {
                playInner();
            }
        }

        public void playNext() {
            playNextInner();
        }

        public void playPre() {
            playPreInner();
        }

        public int getPlayMode() {
            return getPlayModeInner();
        }

        public void setPlayMode(int mode) {
            setPlayModeInner(mode);
        }

        public void seekTo(int pos) {
            seekToInner(pos);
        }

        public Music getCurrentMusic() {
            return getCurrentMusicInner();
        }

        public boolean isPlaying() {
            return isPlayingInner();
        }

        public List<Music> getPlayingList() {
            return getPlayingListInner();
        }

        public void registerOnStateChangeListener(OnStateChangeListenr l) {
            listenrList.add(l);
        }

        public void unregisterOnStateChangeListener(OnStateChangeListenr l) {
            listenrList.remove(l);
        }
    }

    private void addPlayListInner(Music music) {
        if (!playingMusicList.contains(music)) {
            playingMusicList.add(0, music);
            PlayingMusic playingMusic = new PlayingMusic(music.songUrl, music.title, music.artist, music.imgUrl, music.isOnlineMusic);
            MyAPP.getInstance().getDatabase().PlayingMusic().insertPlayingMusic(playingMusic);
        }
        currentMusic = music;
        isNeedReload = true;
        playInner();
    }

    private void addPlayListInner(List<Music> musicList) {
        playingMusicList.clear();
        MyAPP.getInstance().getDatabase().PlayingMusic().deleteAll();

        playingMusicList.addAll(musicList);
        for (Music i : musicList) {
            PlayingMusic playingMusic = new PlayingMusic(i.songUrl, i.title, i.artist, i.imgUrl, i.isOnlineMusic);
            MyAPP.getInstance().getDatabase().PlayingMusic().insertPlayingMusic(playingMusic);
        }
        currentMusic = playingMusicList.get(0);
        playInner();
    }

    private void removeMusicInner(int i) {
        MyAPP.getInstance().getDatabase().PlayingMusic().deleteByTitle(playingMusicList.get(i).title);

        playingMusicList.remove(i);
    }

    private void playInner() {

        audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (currentMusic == null && playingMusicList.size() > 0) {
            currentMusic = playingMusicList.get(0);
            isNeedReload = true;
        }

        playMusicItem(currentMusic, isNeedReload);


    }

    private void pauseInner() {
        player.pause();

        for (OnStateChangeListenr l : listenrList) {
            l.onPause();
        }
        isNeedReload = false;
    }

    private void playPreInner() {
        int currentIndex = playingMusicList.indexOf(currentMusic);
        if (currentIndex - 1 >= 0) {
            currentMusic = playingMusicList.get(currentIndex - 1);
            isNeedReload = true;
            playInner();
        }
    }

    private void playNextInner() {

        if (playMode == Utils.TYPE_RANDOM) {
            int i = (int) (0 + Math.random() * (playingMusicList.size() + 1));
            currentMusic = playingMusicList.get(i);
        } else {
            int currentIndex = playingMusicList.indexOf(currentMusic);
            if (currentIndex < playingMusicList.size() - 1) {
                currentMusic = playingMusicList.get(currentIndex + 1);
            } else {
                currentMusic = playingMusicList.get(0);
            }
        }
        isNeedReload = true;
        playInner();
    }

    private void seekToInner(int pos) {
        player.seekTo(pos);
    }

    private Music getCurrentMusicInner() {
        return currentMusic;
    }

    private boolean isPlayingInner() {
        return player.isPlaying();
    }

    public List<Music> getPlayingListInner() {
        return playingMusicList;
    }

    private int getPlayModeInner() {
        return playMode;
    }

    private void setPlayModeInner(int mode) {
        playMode = mode;
    }

    private void prepareToPlay(Music item) {
        try {
            player.reset();
            //设置播放音乐的地址
            player.setDataSource(MusicService.this, Uri.parse(item.songUrl));
            //准备播放音乐
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playMusicItem(Music item, boolean reload) {

        if (item == null) {
            return;
        }

        if (reload) {
            prepareToPlay(item);
        }
        player.start();
        for (OnStateChangeListenr l : listenrList) {
            l.onPlay(item);
        }
        isNeedReload = true;

        handler.removeMessages(66);
        handler.sendEmptyMessage(66);
    }

    private void initPlayList() {
        playingMusicList = new ArrayList<>();
        List<PlayingMusic> list = MyAPP.getInstance().getDatabase().PlayingMusic().queryPlayingMusic();

        for (PlayingMusic i : list) {
            Music m = new Music(i.songUrl, i.title, i.artist, i.imgUrl, i.isOnlineMusic);
            playingMusicList.add(m);
        }
        if (playingMusicList.size() > 0) {
            currentMusic = playingMusicList.get(0);
            isNeedReload = true;
        }
    }

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

            Utils.count++;

            if (playMode == Utils.TYPE_SINGLE) {
                //单曲循环
                isNeedReload = true;
                playInner();
            } else {
                playNextInner();
            }
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 66:
                    long played = player.getCurrentPosition();
                    long duration = player.getDuration();
                    for (OnStateChangeListenr l : listenrList) {
                        l.onPlayProgressChange(played, duration);
                    }
                    sendEmptyMessageDelayed(66, 1000);
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {

        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (player.isPlaying()) {
                        autoPlayAfterFocus = false;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (player.isPlaying()) {
                        autoPlayAfterFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (player.isPlaying()) {
                        autoPlayAfterFocus = true;
                        pauseInner();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (!player.isPlaying() && autoPlayAfterFocus) {
                        autoPlayAfterFocus = false;
                        playInner();
                    }
                    break;
            }
        }
    };
}
