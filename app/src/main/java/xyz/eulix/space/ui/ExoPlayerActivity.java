/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import xyz.eulix.space.R;
import xyz.eulix.space.event.LanStatusEvent;
import xyz.eulix.space.event.VideoSegmentLogEvent;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.network.video.EulixHttpDataSource;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.ToastUtil;
import xyz.eulix.space.util.share.ShareUtil;

/**
 * Author:      Zhu Fuyu
 * Description: ExoPlayer2 视频播放Activity
 * History:     2023/1/5
 */
public class ExoPlayerActivity extends Activity {
    private final static String KEY_M3U8_ROOT_PATH = "key_m3u8";
    private final static String KEY_LOCAL_VIDEO_PATH = "key_local_video_path";
    private final static String KEY_VIDE_NAME = "key_video_name";

    //局域网文件
    private final static String LAN_FILE_NAME = "index-lan.m3u8";
    //公网文件
    private final static String WAN_FILE_NAME = "index-wan.m3u8";

    private StyledPlayerView playerView;
    private StyledPlayerControlView controlView;
    private RelativeLayout layoutTitle;
    private ImageView imgBack;
    private TextView tvVideoName;
    private ImageView imgScreenOrientation;
    private ExoPlayer player;

    private RelativeLayout layoutLog;
    private TextView tvLanStatus;
    private TextView tvHttpCount;

    private String mVideoLocalPath;
    private String mM3u8RootPath;
    private String mVideoName;
    //是否正在使用局域网
    private boolean isUsingLan = false;
    //是否为在线播放
    private boolean isOnline = false;
    //是否为横屏
    private boolean mCurrentScreenLand = false;
    //是否需要恢复播放
    private boolean mNeedResumePlay = false;

    private int mHttpSegmentCount = 0;
    private int mP2PSegmentCount = 0;

    //播放在线视频
    public static void startOnline(Context context, String m3u8RootPath, String videoName) {
        Intent intent = new Intent(context, ExoPlayerActivity.class);
        intent.putExtra(KEY_M3U8_ROOT_PATH, m3u8RootPath);
        intent.putExtra(KEY_VIDE_NAME, videoName);
        context.startActivity(intent);
    }

    //播放本地视频
    public static void startLocal(Context context, String videoLocalPath, String videoName) {
        Intent intent = new Intent(context, ExoPlayerActivity.class);
        intent.putExtra(KEY_LOCAL_VIDEO_PATH, videoLocalPath);
        intent.putExtra(KEY_VIDE_NAME, videoName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exo_player);

        initData();

        playerView = findViewById(R.id.player_view);
        controlView = findViewById(R.id.exo_controller);

        layoutLog = findViewById(R.id.layout_log);
        tvLanStatus = findViewById(R.id.tv_lan_status);
        tvHttpCount = findViewById(R.id.tv_log_http_count);

        // Create a player instance.
        player = new ExoPlayer.Builder(this).build();

        playerView.setPlayer(player);
        controlView.setPlayer(player);

        setCustomPlayerController();
        mCurrentScreenLand = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        setScreenUi();

        MediaSource mediaSource;
        if (isOnline) {
            isUsingLan = LanManager.getInstance().isLanEnable();
            mediaSource = getOnlineMediaSource(isUsingLan);
        } else {
            mediaSource = getLocalMediaSource();
        }

        if (mediaSource == null) {
            ToastUtil.showToast(getResources().getString(R.string.video_resource_not_exist));
            finish();
            return;
        }

        // Set the media source to be played.
        player.setMediaSource(mediaSource);
        // Prepare the player.
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_READY:
                        playerView.hideController();
                        //打开屏幕常亮：
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        break;
                    case Player.STATE_IDLE:
                        break;
                    case Player.STATE_ENDED:
                        //关闭屏幕常亮：
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        break;
                    default:
                }
                Player.Listener.super.onPlaybackStateChanged(playbackState);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                Logger.d("zfy", "onPlayerError " + error.errorCode + ",msg:" + error.getMessage());
//                if (!isOnline) {
//                    //非在线播放失败，调用第三方播放器
//                    SystemMediaUtils.openMediaFile(EulixSpaceApplication.getContext(), mVideoLocalPath);
//                }
            }

            @Override
            public void onEvents(Player player, Player.Events events) {
                if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                    //打开屏幕常亮：
                    if (player.isPlaying()) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        //关闭屏幕常亮：
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                }
                Player.Listener.super.onEvents(player, events);
            }
        });

        EventBusUtil.register(this);
    }

    private void initData() {
        mVideoLocalPath = getIntent().getStringExtra(KEY_LOCAL_VIDEO_PATH);
        mM3u8RootPath = getIntent().getStringExtra(KEY_M3U8_ROOT_PATH);
        mVideoName = getIntent().getStringExtra(KEY_VIDE_NAME);
        if (!TextUtils.isEmpty(mM3u8RootPath)) {
            isOnline = true;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // land do nothing is ok
            mCurrentScreenLand = true;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            // port do nothing is ok
            mCurrentScreenLand = false;
        }
        setScreenUi();
    }

    //自定义控制面板样式
    private void setCustomPlayerController() {
        layoutTitle = controlView.findViewById(R.id.layout_title);
        imgBack = controlView.findViewById(R.id.img_back);
        tvVideoName = controlView.findViewById(R.id.tv_video_name);
        imgScreenOrientation = controlView.findViewById(R.id.exo_screen_orientation);

        if (layoutTitle != null) {
            layoutTitle.setOnClickListener(v -> {
                if (PreferenceUtil.getLoggerSwitch(ExoPlayerActivity.this)) {
                    if (layoutLog.getVisibility() == View.VISIBLE) {
                        layoutLog.setVisibility(View.GONE);
                    } else {
                        layoutLog.setVisibility(View.VISIBLE);
                        refreshLogData();
                    }
                }
            });
        }

        if (imgBack != null) {
            imgBack.setOnClickListener(v -> {
                finish();
            });
        }

        if (tvVideoName != null && !TextUtils.isEmpty(mVideoName)) {
            tvVideoName.setText(mVideoName);
        }

        if (imgScreenOrientation != null) {
            imgScreenOrientation.setOnClickListener(v -> {
                if (mCurrentScreenLand) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                setScreenUi();
            });
        }

    }

    private void setScreenUi() {
        if (mCurrentScreenLand) {
            //设置横屏样式
            if (imgScreenOrientation != null) {
                imgScreenOrientation.setImageResource(R.drawable.icon_video_screen_land);
            }
            if (layoutTitle != null) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) layoutTitle.getLayoutParams();
                layoutParams.setMargins(layoutParams.leftMargin, getResources().getDimensionPixelOffset(R.dimen.dp_0),
                        layoutParams.rightMargin, layoutParams.bottomMargin);
                layoutTitle.setLayoutParams(layoutParams);
                layoutTitle.setGravity(Gravity.LEFT);
            }
        } else {
            //设置竖屏样式
            if (imgScreenOrientation != null) {
                imgScreenOrientation.setImageResource(R.drawable.icon_video_screen_portal);
            }
            if (layoutTitle != null) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) layoutTitle.getLayoutParams();
                layoutParams.setMargins(layoutParams.leftMargin, getResources().getDimensionPixelOffset(R.dimen.dp_42),
                        layoutParams.rightMargin, layoutParams.bottomMargin);
                layoutTitle.setLayoutParams(layoutParams);
                layoutTitle.setGravity(Gravity.CENTER);
            }
        }
    }

    //获取在线播放mediaSource
    private MediaSource getOnlineMediaSource(boolean isLan) {
        String m3u8FileName;
        if (isLan) {
            m3u8FileName = LAN_FILE_NAME;
        } else {
            m3u8FileName = WAN_FILE_NAME;
        }
        File file = new File(mM3u8RootPath, m3u8FileName);
        Uri mediaUrl = null;
        if (file.exists()) {
            mediaUrl = ShareUtil.getFileUri(this, file);
        }
        if (mediaUrl == null) {
            return null;
        }

        // Create a data source factory.
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, new EulixHttpDataSource.Factory(mVideoName));
        // Create a HLS media source pointing to a playlist uri.
        HlsMediaSource hlsMediaSource =
                new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(mediaUrl));

        return hlsMediaSource;
    }

    //获取本地视频mediaSource
    private MediaSource getLocalMediaSource() {
        File file = new File(mVideoLocalPath);
        Uri mediaUrl = null;
        if (file.exists()) {
            mediaUrl = ShareUtil.getFileUri(this, file);
        }
        if (mediaUrl == null) {
            return null;
        }

        // Create a data source factory.
        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
        // Create media source pointing to a playlist uri.
        MediaSource mediaSource =
                new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(mediaUrl));

        return mediaSource;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LanStatusEvent event) {
        Logger.d("zfy", "receive LanStatusEvent " + event.isLanEnable);
        if (isOnline) {
            changeLanSourceStatus(event.isLanEnable);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(VideoSegmentLogEvent event) {
        Logger.d("zfy", "receive VideoSegmentLogEvent " + event.videoName + ",sourceType=" + event.sourceType);
        if (event.videoName != null && event.videoName.equals(mVideoName)) {
            if (event.sourceType == VideoSegmentLogEvent.SOURCE_TYPE_P2P) {
                mP2PSegmentCount++;
            } else {
                mHttpSegmentCount++;
            }
            refreshLogData();
        }
    }

    private void refreshLogData() {
        if (layoutLog != null && layoutLog.getVisibility() == View.VISIBLE) {
            tvLanStatus.setText(LanManager.getInstance().isLanEnable() ? "True" : "False");
            tvHttpCount.setText(mHttpSegmentCount + "");
        }
    }

    //切换公网、局域网视频源
    private void changeLanSourceStatus(boolean isLanEnable) {
        if (isLanEnable == isUsingLan || player == null) {
            //no need to change lan status
            return;
        }
        boolean isCurrentPlaying = player.isPlaying();
        MediaSource mediaSource = getOnlineMediaSource(isLanEnable);
        if (mediaSource != null) {
            isUsingLan = isLanEnable;
            Logger.d("zfy", "change video source to " + (isLanEnable ? "lan" : "wan"));
            long currentPosition = player.getCurrentPosition();
            player.setMediaSource(mediaSource);
            player.prepare();
            if (isCurrentPlaying) {
                player.play();
            }
            player.seekTo(currentPosition);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            if (player.isPlaying()) {
                mNeedResumePlay = true;
                player.pause();
            } else {
                mNeedResumePlay = false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNeedResumePlay && player != null) {
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }

        EventBusUtil.unRegister(this);
    }
}