package com.example.simplemusic.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.simplemusic.bean.Music;
import com.example.simplemusic.adapter.MusicAdapter;
import com.example.simplemusic.adapter.PlayingMusicAdapter;
import com.example.simplemusic.R;
import com.example.simplemusic.util.Utils;
import com.example.simplemusic.service.MusicService;
import com.example.simplemusic.db.LocalMusic;


import java.util.ArrayList;
import java.util.List;

public class LocalMusicActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView musicCountView;
    private ListView musicListView;
    private TextView playingTitleView;
    private TextView playingArtistView;
    private ImageView playingImgView;
    private ImageView btnPlayOrPause;

    private List<Music> localMusicList;
    private MusicAdapter adapter;
    private MusicService.MusicServiceBinder serviceBinder;
    private MusicUpdateTask updateTask;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localmusic);

        initActivity();

        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Music music = localMusicList.get(position);
                serviceBinder.addPlayList(music);
            }
        });

        adapter.setOnMoreButtonListener(new MusicAdapter.onMoreButtonListener() {
            @Override
            public void onClick(final int i) {
                final Music music = localMusicList.get(i);
                final String[] items = new String[]{"Collect", "Add Play List", "Delete"};
                AlertDialog.Builder builder = new AlertDialog.Builder(LocalMusicActivity.this);
                builder.setTitle(music.title + "-" + music.artist);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                MainActivity.addMymusic(music);
                                break;
                            case 1:
                                serviceBinder.addPlayList(music);
                                break;
                            case 2:
                                localMusicList.remove(i);

                                MyAPP.getInstance().getDatabase().localMusicDao().deleteByTitle(music.title);
                                adapter.notifyDataSetChanged();
                                musicCountView.setText("Play All(" + localMusicList.size() + "song)");
                                break;
                        }
                    }
                });
                builder.create().show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_all:
                serviceBinder.addPlayList(localMusicList);
                break;
            case R.id.refresh:
                localMusicList.clear();
                MyAPP.getInstance().getDatabase().localMusicDao().deleteAll();

                updateTask = new MusicUpdateTask();
                updateTask.execute();
                break;
            case R.id.player:
                Intent intent = new Intent(LocalMusicActivity.this, PlayerActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.bottom_in, R.anim.bottom_silent);
                break;
            case R.id.play_or_pause:
                serviceBinder.playOrPause();
                break;
            case R.id.playing_list:
                showPlayList();
                break;

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateTask != null && updateTask.getStatus() == AsyncTask.Status.RUNNING) {
            updateTask.cancel(true);
        }
        updateTask = null;
        localMusicList.clear();
        unbindService(mServiceConnection);
    }

    private void initActivity() {
        ImageView btn_playAll = this.findViewById(R.id.play_all);
        musicCountView = this.findViewById(R.id.play_all_title);
        ImageView btn_refresh = this.findViewById(R.id.refresh);
        musicListView = this.findViewById(R.id.music_list);
        RelativeLayout playerToolView = this.findViewById(R.id.player);
        playingImgView = this.findViewById(R.id.playing_img);
        playingTitleView = this.findViewById(R.id.playing_title);
        playingArtistView = this.findViewById(R.id.playing_artist);
        btnPlayOrPause = this.findViewById(R.id.play_or_pause);
        ImageView btn_playingList = this.findViewById(R.id.playing_list);

        btn_playAll.setOnClickListener(this);
        btn_refresh.setOnClickListener(this);
        playerToolView.setOnClickListener(this);
        btnPlayOrPause.setOnClickListener(this);
        btn_playingList.setOnClickListener(this);

        localMusicList = new ArrayList<>();

        Intent i = new Intent(this, MusicService.class);
        bindService(i, mServiceConnection, BIND_AUTO_CREATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("LocalMusic");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSupportActionBar(toolbar);
        List<LocalMusic> list = MyAPP.getInstance().getDatabase().localMusicDao().queryLocalMusic();
        for (LocalMusic s : list) {
            Music m = new Music(s.songUrl, s.title, s.artist, s.imgUrl, s.isOnlineMusic);
            localMusicList.add(m);
        }

        adapter = new MusicAdapter(this, R.layout.music_item, localMusicList);
        musicListView.setAdapter(adapter);

        musicCountView.setText("Play All(" + localMusicList.size() + "song)");
    }

    private void showPlayList() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("PlayList");

        final List<Music> playingList = serviceBinder.getPlayingList();

        if (playingList.size() > 0) {
            final PlayingMusicAdapter playingAdapter = new PlayingMusicAdapter(this, R.layout.playinglist_item, playingList);
            builder.setAdapter(playingAdapter, new DialogInterface.OnClickListener() {
                //监听列表项点击事件
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    serviceBinder.addPlayList(playingList.get(which));
                }
            });

            playingAdapter.setOnDeleteButtonListener(new PlayingMusicAdapter.onDeleteButtonListener() {
                @Override
                public void onClick(int i) {
                    serviceBinder.removeMusic(i);
                    playingAdapter.notifyDataSetChanged();
                }
            });
        } else {
            builder.setMessage("no play music");
        }

        builder.setCancelable(true);

        builder.create().show();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            serviceBinder = (MusicService.MusicServiceBinder) service;

            serviceBinder.registerOnStateChangeListener(listenr);

            Music item = serviceBinder.getCurrentMusic();

            if (serviceBinder.isPlaying()) {
                btnPlayOrPause.setImageResource(R.drawable.zanting);
                playingTitleView.setText(item.title);
                playingArtistView.setText(item.artist);
                if (item.isOnlineMusic) {
                    Bitmap bm = BitmapFactory.decodeFile(item.imgUrl);

                    playingImgView.setImageBitmap(bm);
                } else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(resolver, item.imgUrl);
                    Bitmap bm = BitmapFactory.decodeFile(item.imgUrl);

                    playingImgView.setImageBitmap(img);
                }
            } else if (item != null) {
                btnPlayOrPause.setImageResource(R.drawable.bofang);
                playingTitleView.setText(item.title);
                playingArtistView.setText(item.artist);
                if (item.isOnlineMusic) {
                    Bitmap bm = BitmapFactory.decodeFile(item.imgUrl);

                    playingImgView.setImageBitmap(bm);
                } else {
                    ContentResolver resolver = getContentResolver();
                    Bitmap img = Utils.getLocalMusicBmp(resolver, item.imgUrl);
                    Bitmap bm = BitmapFactory.decodeFile(item.imgUrl);

                    playingImgView.setImageBitmap(img);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBinder.unregisterOnStateChangeListener(listenr);
        }
    };

    private MusicService.OnStateChangeListenr listenr = new MusicService.OnStateChangeListenr() {

        @Override
        public void onPlayProgressChange(long played, long duration) {
        }

        @Override
        public void onPlay(Music item) {
            btnPlayOrPause.setImageResource(R.drawable.zanting);
            playingTitleView.setText(item.title);
            playingArtistView.setText(item.artist);
            btnPlayOrPause.setEnabled(true);
            if (item.isOnlineMusic) {
                Bitmap bm = BitmapFactory.decodeFile(item.imgUrl);

                playingImgView.setImageBitmap(bm);
            } else {
                ContentResolver resolver = getContentResolver();
                Bitmap img = Utils.getLocalMusicBmp(resolver, item.imgUrl);
                Bitmap bm = BitmapFactory.decodeFile(item.imgUrl);

                playingImgView.setImageBitmap(img);
            }
        }

        @Override
        public void onPause() {
            btnPlayOrPause.setImageResource(R.drawable.bofang);
            btnPlayOrPause.setEnabled(true);
        }
    };

    @SuppressLint("StaticFieldLeak")
    private class MusicUpdateTask extends AsyncTask<Object, Music, Void> {

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(LocalMusicActivity.this);
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        // 子线程中获取音乐
        @Override
        protected Void doInBackground(Object... params) {

            String[] searchKey = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Albums.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.IS_MUSIC
            };

            ContentResolver resolver = getContentResolver();
            Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, searchKey, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext() && !isCancelled()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    Uri musicUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID));
                    int isMusic = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
                    if (isMusic != 0 && duration / (500 * 60) >= 2) {
                        Uri musicPic = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
                        Music data = new Music(musicUri.toString(), title, artist, musicPic.toString(), false);
                        publishProgress(data);
                    }
                }
                cursor.close();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Music... values) {
            Music data = values[0];
            if (!localMusicList.contains(data)) {
                localMusicList.add(data);
                LocalMusic music = new LocalMusic(data.songUrl, data.title, data.artist, data.imgUrl, data.isOnlineMusic);
                MyAPP.getInstance().getDatabase().localMusicDao().inserLocalMusic(music);
            }
            MusicAdapter adapter = (MusicAdapter) musicListView.getAdapter();
            adapter.notifyDataSetChanged();
            musicCountView.setText("Play ALl(" + localMusicList.size() + "song)");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
