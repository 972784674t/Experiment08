package com.example.experiment08;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MusicPlayerService extends Service {

    public static final String TAG = "MusicPlayerService";

    //广播action
    private static final String PLAY_MUSIC = "com.ciom.playMusic";
    private static final String MUSIC_DURATION = "com.ciom.musicDuration";
    private static final String MUSIC_Current_Position = "com.ciom.musicCurrentPosition";
    private static final String NEXT_MUSIC = "com.ciom.nextMusic";

    //广播intent
    Intent playMusicIntent;
    Intent musicDurationIntent;
    Intent musicCurrentPositionIntent;
    Intent nextMusicIntent;

    private MediaPlayer mediaPlayer;

    //数据源
    List<Music> musicList;

    //播放状态管理: 0x11 未在播放状态  0x12 正在播放状态  0x13 暂停状态
    static int MUSIC_PLAYING_STATUS = 0X11;

    //暂停音乐时进度条的位置
    int currentPausePosition = 0;

    //当前播放第几首音乐
    int currentPlayPosition = 0;

    public MusicPlayerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()");
        mediaPlayer = new MediaPlayer();
        musicList = MusicUtils.getMusicList(this);

        //播放音乐广播
        playMusicIntent = new Intent();
        playMusicIntent.setAction(PLAY_MUSIC);

        //音乐长度广播
        musicDurationIntent = new Intent();
        musicDurationIntent.setAction(MUSIC_DURATION);

        //当前播放位置
        musicCurrentPositionIntent = new Intent();
        musicCurrentPositionIntent.setAction(MUSIC_Current_Position);

        //下一首音乐广播
        nextMusicIntent = new Intent();
        nextMusicIntent.setAction(NEXT_MUSIC);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //发送播放器AudioSessionId
        playMusicIntent.putExtra("autoSessionID", mediaPlayer.getAudioSessionId());
        sendBroadcast(playMusicIntent);
        //发送音乐长度广播
        musicDurationIntent.putExtra("musicDuration", mediaPlayer.getDuration());
        sendBroadcast(musicDurationIntent);


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {

        return new MusicBinder();
    }

    class MusicBinder extends Binder {

        void callPlayMusicInPosition(int position) {
            currentPlayPosition = position;
            Music music = musicList.get(position);
            playMusicInPosition(music);
        }

        void callPlayMusic() {
            playMusic();
        }

        void callPlayMusicByPausePosition(int progress) {
            playMusicByPausePosition(progress);
        }

        void callPauseMusic() {
            pauseMusic();
        }

        void callNextMusic() {
            playNextMusic();
        }

        void callLastMusic() {
            playLastMusic();
        }

    }

    /**
     * 通过music的position指定播放音乐
     *
     * @param music 需要播放的音乐
     */
    private void playMusicInPosition(Music music) {
        //如果是播放状态或者暂停状态则销毁重建mediaPlayer
        Log.d(TAG, "MUSIC_PLAYING_STATUS -> " + MUSIC_PLAYING_STATUS);
        if (MUSIC_PLAYING_STATUS == 0x12 || MUSIC_PLAYING_STATUS == 0x13) {
            stopMusic();
            mediaPlayer.reset();
        }
        try {
            mediaPlayer.setDataSource(music.getMusic_url());
            playMusic();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放或者暂停音乐
     * 1.从头播放
     * 2.从暂停播放
     */
    @SuppressLint("SetTextI18n")
    private void playMusic() {

        if ( mediaPlayer != null && !mediaPlayer.isPlaying() ) {
            //当前播放进度 == 0时，从头开始播放
            if ( currentPausePosition == 0 ) {
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                    //发送音乐长度广播
                    musicDurationIntent.putExtra("musicDuration", mediaPlayer.getDuration());
                    sendBroadcast(musicDurationIntent);
                    }
                });
                //切换为播放状态
                MUSIC_PLAYING_STATUS = 0X12;
                Log.d(TAG, "从头：playMusic()");
                //重置音乐开始时间
                MainActivity.nowTime.setText("00:00");
                MainActivity.songseek.setProgress(0);
                Log.d(TAG, " Thread -> " + android.os.Process.myTid() + " name: " + Thread.currentThread().getName());
                //每隔30毫秒发送音乐进度
                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                     Message obtain = Message.obtain();
                     obtain.arg1 = mediaPlayer.getCurrentPosition();
                     MainActivity.handler.sendMessage(obtain);
                    }
                }, 0, 50);

                //创建一个线程每隔500毫秒发送一次信息
//                Runnable runnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d(TAG, " musicThread -> " + Thread.currentThread().getId() + " name: " + Thread.currentThread().getName());
//                        while (true) {
//                            try {
//                                Thread.sleep(800);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            Message obtain = Message.obtain();
//                            obtain.arg1 = mediaPlayer.getCurrentPosition();
//                            MainActivity.handler.sendMessage(obtain);
//                        }
//                    }
//                };
//                Thread musicThread = new Thread(runnable);
//                musicThread.start();

            } else {
                //从暂停开始播放
                mediaPlayer.seekTo(currentPausePosition);
                mediaPlayer.start();
                MUSIC_PLAYING_STATUS = 0X12;
                Log.d(TAG, "从暂停：playMusic()");
            }

        }
    }

    /**
     * 拖动进度条播放音乐
     *
     * @param progress 音乐进度
     */
    private void playMusicByPausePosition(int progress) {
        mediaPlayer.seekTo(progress);
        mediaPlayer.start();
    }

    /**
     * 暂停音乐
     */
    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            currentPausePosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            //切换为暂停状态
            MUSIC_PLAYING_STATUS = 0x13;
            Log.d(TAG, "pause()");
        }
    }

    /**
     * 播放下一首音乐
     */
    private void playNextMusic() {
        //如果已经是最后一首，则播放第一首歌曲
        if (currentPlayPosition == musicList.size() - 1) {
            currentPlayPosition = 0;
            Music music = musicList.get(currentPlayPosition);
            playMusicInPosition(music);
        } else {
            currentPlayPosition += 1;
            playMusicInPosition(musicList.get(currentPlayPosition));
        }
    }

    /**
     * 播放上一首音乐
     */
    private void playLastMusic() {
        //如果是第一首歌曲，则播放最后一首歌曲
        if (currentPlayPosition == 0) {
            currentPlayPosition = musicList.size() - 1;
            Music music = musicList.get(currentPlayPosition);
            playMusicInPosition(music);
        } else {
            //如果播放时长不超过2秒，则重新播放
            if( mediaPlayer.getCurrentPosition() < 2500 ){
                playMusicInPosition(musicList.get(currentPlayPosition));
            }else {
                currentPlayPosition -= 1;
                playMusicInPosition(musicList.get(currentPlayPosition));
            }
        }
    }

    /**
     * 停止音乐
     */
    private void stopMusic() {
        if (mediaPlayer != null) {
            //音乐播放进度归0
            currentPausePosition = 0;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
        }
    }


}
