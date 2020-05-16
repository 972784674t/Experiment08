package com.example.experiment08;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


import me.bogerchan.niervisualizer.NierVisualizerManager;
import me.bogerchan.niervisualizer.renderer.IRenderer;
import me.bogerchan.niervisualizer.renderer.circle.CircleBarRenderer;
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType1Renderer;
import me.bogerchan.niervisualizer.util.NierAnimator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "MusicPlayerActivity";

    //广播
    private static final String PLAY_MUSIC = "com.ciom.playMusic";
    private static final String MUSIC_DURATION = "com.ciom.musicDuration";
    private static final String NEXT_MUSIC = "com.ciom.nextMusic";
    IntentFilter filter;

    RecyclerView recyclerView;
    MusicAdapter musicAdapter;
    @SuppressLint("StaticFieldLeak")
    private static TextView endTime;
    @SuppressLint("StaticFieldLeak")
    static TextView nowTime;
    ImageView playbtn, lastbtn, nextbtn;
    @SuppressLint("StaticFieldLeak")
    static SeekBar songseek;
    SurfaceView nvsurfaceview;
    SimpleDateFormat simpleDateFormat;

    //mediaPlayer的autoseeeionid
    static int AUTO_SESSION_ID = 0;

    //数据源
    List<Music> musicList;

    //当前播放第几首音乐
    int currentPlayPosition = 0;

    //暂停音乐时进度条的位置
    int currentPausePosition = 0;

    //NierVisualizerManager绘图
    NierVisualizerManager visualizerManager;

    //播放状态管理: -1 未在播放状态  1 正在播放状态  2 暂停状态
    static int PLAYING_STATUS = -1;

    //播放进度条是否被拖动
    static boolean isSeekBarChanging = true;

    LinearLayoutManager linearLayoutManager;

    //activity绑定service
    public static MusicPlayerService.MusicBinder musicBinder;
    Intent MusicPlayerServiceIntent;

    //注册广播
    MusicReceiver musicReceiver;

    //当前播放进度
    static int playProgress;

    //获取音乐播放进度
    //运用Handler中的handleMessage方法接收service传递的信息
    public static Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            //super.handleMessage(msg);
            // 将SeekBar位置设置到当前播放位置
            if (isSeekBarChanging) {
                songseek.setProgress(msg.arg1);
                playProgress = msg.arg1;
                //音乐的当前播放进度
                nowTime.setText(new SimpleDateFormat("mm:ss", Locale.getDefault()).format(new Date(msg.arg1)));
            }
            return false;
        }
    });

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate activity");

        //注册广播
        musicReceiver = new MusicReceiver();
        filter = new IntentFilter();
        filter.addAction(PLAY_MUSIC);
        filter.addAction(MUSIC_DURATION);
        filter.addAction(NEXT_MUSIC);
        registerReceiver(musicReceiver, filter);

        //启动并绑定服务
        MusicPlayerServiceIntent = new Intent(this, MusicPlayerService.class);
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                musicBinder = (MusicPlayerService.MusicBinder) service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        startService(MusicPlayerServiceIntent);
        bindService(MusicPlayerServiceIntent, connection, Service.BIND_AUTO_CREATE);

        init();

        setEventListener();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy activity");
        currentPausePosition = 0;
        //音频可视化销毁
        visualizerManager.release();
        Log.d(TAG, "visualizerManager.release()");
        registerReceiver(musicReceiver, filter);
        //解绑并停止服务
        //unbindService(connection);
        //stopService(MusicPlayerServiceIntent);
    }

    /**
     * 系统初始化
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void init() {
        Log.d(TAG, " Thread -> " + android.os.Process.myTid() + " name " + Thread.currentThread().getName());
        //状态栏自适应
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        //获取限权
        checkPermission();
        //定义数据格式
        simpleDateFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());
        //View初始化
        recyclerView = findViewById(R.id.muiscRcycle);
        endTime = findViewById(R.id.endTime);
        nowTime = findViewById(R.id.newTime);
        songseek = findViewById(R.id.songseek);
        playbtn = findViewById(R.id.playbtn);
        playbtn.bringToFront();
        lastbtn = findViewById(R.id.lastsong);
        nextbtn = findViewById(R.id.nextsong);

        //SurfaceView设置透明背景
        nvsurfaceview = findViewById(R.id.nvsurfaceview);
        nvsurfaceview.setZOrderOnTop(true);
        nvsurfaceview.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        //获取数据源并设置recyclerView布局适配器
        musicList = MusicUtils.getMusicList(this);
        musicAdapter = new MusicAdapter();
        musicAdapter.setMusics(musicList);
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        //数据源变化，适配器更新
        musicAdapter.notifyDataSetChanged();
        recyclerView.setItemViewCacheSize(musicList.size() - 4);
        recyclerView.setAdapter(musicAdapter);

        //控件点击事件初始化
        playbtn.setOnClickListener(this);
        lastbtn.setOnClickListener(this);
        nextbtn.setOnClickListener(this);
        nvsurfaceview.setOnClickListener(this);

        //音频可视化控件初始化
        visualizerManager = new NierVisualizerManager();
        //获取自定义样式
        CircleBarRenderer circleBarRenderer = getMyCircleBarRenderer();
        //开始渲染
        visualizerManager.start(nvsurfaceview, new IRenderer[]{circleBarRenderer, new ColumnarType1Renderer()});
        Log.d(TAG, "visualizerManager -> Create");

        //进度条滑动事件
        songseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentPausePosition = progress;
                nowTime.setText(simpleDateFormat.format(new Date(progress)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekBarChanging = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekBarChanging = true;
                musicBinder.callPlayMusicByPausePosition(currentPausePosition);
            }
        });

    }

    /**
     * 接口回调，设置recyclerView中item点击的监听事件
     */
    private void setEventListener() {
        musicAdapter.setOnItemClickListener(new MusicAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                currentPlayPosition = position;
                Log.d(TAG, "播放第 " + currentPlayPosition + " 首");
                musicBinder.callPlayMusicInPosition(currentPlayPosition);
                PLAYING_STATUS = 1;
            }
        });
    }

    /**
     * 对三个按钮的监听
     *
     * @param v 点击
     */
    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            //播放或暂停
            case R.id.playbtn:
                playOrPause();
                break;

            //上一首
            case R.id.lastsong:
                callLastSong();
                break;

            //下一首
            case R.id.nextsong:
                callNextSong();
                break;

            default:
                break;
        }

    }

    /**
     * 暂停或播放逻辑设计
     */
    private void playOrPause() {
        //如果没有在播放音乐,播放第一首音乐
        if (PLAYING_STATUS == -1) {
            musicBinder.callPlayMusicInPosition(currentPlayPosition);
            last_nextSongUIUpdate(currentPlayPosition, 0);
            return;
        }
        //如果在播放音乐则暂停
        if (PLAYING_STATUS == 1) {
            musicBinder.callPauseMusic();
            PLAYING_STATUS = 2;
            return;
        }
        //如果在暂停则播放音乐
        if (PLAYING_STATUS == 2) {
            musicBinder.callPlayMusic();
            PLAYING_STATUS = 1;
        }
    }



    /**
     * 播放上一首歌曲逻辑设计，同时更新UI
     */
    private void callLastSong() {
        if (currentPlayPosition == 0) {
            Toast.makeText(this, "已经是第一首了哦", Toast.LENGTH_SHORT).show();
            return;
        }
        //如果播放时间少于2500毫米，则不更新UI
        if (playProgress < 2500) {
            musicBinder.callLastMusic();
            return;
        } else {
            musicBinder.callLastMusic();
        }
        //更新选择item的UI
        if (currentPlayPosition >= 0) {
            last_nextSongUIUpdate(currentPlayPosition - 1, 1);
        }
    }

    /**
     * 播放下一首歌曲逻辑设计，同时更新UI
     */
    private void callNextSong() {
        if (currentPlayPosition == musicList.size() - 1) {
            Toast.makeText(this, "已经是最后一首音乐了哦", Toast.LENGTH_SHORT).show();
            return;
        } else {
            musicBinder.callNextMusic();
            //更新选择item的UI
            last_nextSongUIUpdate(currentPlayPosition + 1, -1);
        }
    }

    /**
     * 点击下一曲或上一曲按钮时，recyclerView中的UI更新
     *
     * @param songPosition 当前歌曲位置
     * @param last_or_next 上一曲： 1 或： 下一曲 -1 当前位置： 0
     */
    public void last_nextSongUIUpdate(int songPosition, int last_or_next) {
        //recycleView定位
        final TopSmoothScroller mScroller = new TopSmoothScroller(this);
        mScroller.setTargetPosition(songPosition);
        linearLayoutManager.startSmoothScroll(mScroller);

        //更新UI
        View view = Objects.requireNonNull(recyclerView.getLayoutManager()).findViewByPosition(songPosition);
        assert view != null;
        TextView textView = view.findViewById(R.id.itemchoice);
        textView.setVisibility(View.VISIBLE);
        if (last_or_next != 0) {
            View viewLast = recyclerView.getLayoutManager().findViewByPosition(songPosition + last_or_next);
            assert viewLast != null;
            viewLast.findViewById(R.id.itemchoice).setVisibility(View.GONE);
        }
        currentPlayPosition = songPosition;
        playbtn.bringToFront();
    }



    /**
     * 音频可视化UI自定义绘制样式 1
     *
     * @return 圆形可视化UI
     */
    private CircleBarRenderer getMyCircleBarRenderer() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#08090A"));
        return new CircleBarRenderer(
                paint, 4,
                CircleBarRenderer.Type.TYPE_A,
                0.9f,
                .8f,
                new NierAnimator(
                        new LinearInterpolator(),
                        20000,
                        new float[]{0f, -360f},
                        true)
        );
    }

    /**
     * 更新音乐可视化UI
     *
     * @param autoSessionID mediaPlayer.getAudioSessionId()
     */
    void updateMusicVisualization(int autoSessionID) {
        //音频可视化控件初始化
        visualizerManager = new NierVisualizerManager();
        visualizerManager.init(autoSessionID);
        Log.d(TAG, "visualizerManager.init()");
        //获取自定义样式
        CircleBarRenderer circleBarRenderer = getMyCircleBarRenderer();
        //开始渲染
        visualizerManager.start(nvsurfaceview, new IRenderer[]{circleBarRenderer, new ColumnarType1Renderer()});
        Log.d(TAG, "visualizerManager.start()");
        visualizerManager.resume();
        Log.d(TAG, "visualizerManager.resume()");
    }


    /**
     * 音乐广播接收器
     */
    class MusicReceiver extends BroadcastReceiver {
        public static final String TAGB = "MusicPlay 广播接收结果 ";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) {
                Log.d(TAGB, "没有收到任何广播");
            } else {
                switch (intent.getAction()) {
                    case PLAY_MUSIC:
                        //获取autoSessionID绑定到音乐可视化UI
                        int autoSessionID = Objects.requireNonNull(intent.getExtras()).getInt("autoSessionID");
                        Log.d(TAGB, "autoSessionID -> " + autoSessionID);
                        updateMusicVisualization(autoSessionID);
                        AUTO_SESSION_ID = autoSessionID;
                        break;
                    case MUSIC_DURATION:
                        String musicDuration = simpleDateFormat.format(new Date(Objects.requireNonNull(intent.getExtras()).getInt("musicDuration")));
                        Log.d(TAGB, "MUSIC_DURATION -> " + musicDuration);
                        endTime.setText(musicDuration);
                        songseek.setMax(intent.getExtras().getInt("musicDuration"));
                        break;
                    case NEXT_MUSIC:
                        int next = intent.getExtras().getInt("isPlayNextMusic");
                        Log.d(TAG,"nextSong"+next);
                        //last_nextSongUIUpdate(currentPlayPosition,-1);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * 获取权限
     */
    public void checkPermission() {
        boolean isGranted = true;
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //如果没有写sd卡权限
                isGranted = false;
            }
            if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
            }
            if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
            }
            Log.i("权限获取", " ： " + isGranted);
            if (!isGranted) {
                this.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission
                                .ACCESS_FINE_LOCATION,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO
                        },
                        102);
            }
        }
    }

    /**
     * 重写LinearSmoothScroller,点击下一首时，recycleView滚动到歌曲位置
     */
    public class TopSmoothScroller extends LinearSmoothScroller {
        TopSmoothScroller(Context context) {
            super(context);
        }
        @Override
        protected int getHorizontalSnapPreference() {
            return SNAP_TO_START;
        }
        @Override
        protected int getVerticalSnapPreference() {
            return SNAP_TO_START;
        }
    }

}
