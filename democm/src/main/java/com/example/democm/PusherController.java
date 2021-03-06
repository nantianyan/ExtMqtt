package com.example.democm;

import android.content.Context;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.alivc.player.AliVcMediaPlayer;
import com.alivc.player.MediaPlayer;
import com.example.cloudmedia.CloudMedia;
import com.example.cloudmedia.RemoteMediaNode;

import java.util.Map;

/**
 * Created by 阳旭东 on 2017/12/6.
 */

public class PusherController {
    private static final String TAG = "PusherController";
    private String mUrl;
    private PlayerStatus mStatus;
    private String mWhoareyou;
    private RemoteMediaNode mRemotePusher;
    private CloudMedia mCloudMedia;
    private AliVcMediaPlayer mPlayer;
    private SurfaceView mSurfaceView;
    private Context mContext;

    enum PlayerStatus{
        PLAYING,
        PAUSED,
        STOPED,
        SEEKING
    }

    public String getRtmpPlayUrl(){
        return mRemotePusher.getRtmpPlayUrl();
    }

    PusherController(CloudMedia cloudMedia, String whoareyou) {
        Log.d(TAG, "new PusherController");
        mCloudMedia = cloudMedia;
        mWhoareyou = whoareyou;
        mRemotePusher = mCloudMedia.declareRemoteMediaNode(whoareyou);
        mUrl = mRemotePusher.getRtmpPlayUrl();
        mStatus = PlayerStatus.STOPED;
    }

    void initPlayer(Context context, SurfaceView surface) {
        Log.d(TAG, "initPlayer");

        mContext = context;
        mSurfaceView = surface;
        initSurface();
        final String businessId = "";
        AliVcMediaPlayer.init(mContext, businessId);
        initVodPlayer();
    }

    private void initSurface(){
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
                holder.setKeepScreenOn(true);
                Log.d(TAG, "AlivcPlayer onSurfaceCreated." + mPlayer);

                // Important: surfaceView changed from background to front, we need reset surface to mediaplayer.
                // 对于从后台切换到前台,需要重设surface;部分手机锁屏也会做前后台切换的处理
                if (mPlayer != null) {
                    mPlayer.setVideoSurface(mSurfaceView.getHolder().getSurface());
                }

                Log.d(TAG, "AlivcPlayeron SurfaceCreated over.");
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
                Log.d(TAG, "onSurfaceChanged is valid ? " + holder.getSurface().isValid());
                if (mPlayer != null)
                    mPlayer.setSurfaceChanged();
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "onSurfaceDestroy.");
            }
        });
    }

    private void initVodPlayer() {
        mPlayer = new AliVcMediaPlayer(mContext, mSurfaceView);

        mPlayer.setPreparedListener(new MediaPlayer.MediaPlayerPreparedListener() {
            @Override
            public void onPrepared() {
                Toast.makeText(mContext, "prepare ok", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "w,h = " + mPlayer.getVideoWidth() + " , " + mPlayer.getVideoHeight());
            }
        });
        mPlayer.setFrameInfoListener(new MediaPlayer.MediaPlayerFrameInfoListener() {
            @Override
            public void onFrameInfoListener() {
                Map<String, String> debugInfo = mPlayer.getAllDebugInfo();
                long createPts = 0;
                if (debugInfo.get("create_player") != null) {
                    String time = debugInfo.get("create_player");
                    createPts = (long) Double.parseDouble(time);
                    Log.i(TAG, "create_player");
                }
                if (debugInfo.get("open-url") != null) {
                    String time = debugInfo.get("open-url");
                    long openPts = (long) Double.parseDouble(time) + createPts;
                    Log.i(TAG, "open-url");
                }
                if (debugInfo.get("find-stream") != null) {
                    String time = debugInfo.get("find-stream");
                    Log.d(TAG + "lfj0914", "find-Stream time =" + time + " , createpts = " + createPts);
                    long findPts = (long) Double.parseDouble(time) + createPts;
                    Log.i(TAG, "find-stream");
                }
                if (debugInfo.get("open-stream") != null) {
                    String time = debugInfo.get("open-stream");
                    Log.d(TAG + "lfj0914", "open-Stream time =" + time + " , createpts = " + createPts);
                    long openPts = (long) Double.parseDouble(time) + createPts;
                    Log.i(TAG, "open-stream");
                }
            }
        });
        mPlayer.setErrorListener(new MediaPlayer.MediaPlayerErrorListener() {
            @Override
            public void onError(int i, String msg) {
                Toast.makeText(mContext, "player error", Toast.LENGTH_SHORT).show();
            }
        });
        mPlayer.setCompletedListener(new MediaPlayer.MediaPlayerCompletedListener() {
            @Override
            public void onCompleted() {
                Log.d(TAG, "onCompleted--- ");
                //isCompleted = true;
            }
        });
        mPlayer.setSeekCompleteListener(new MediaPlayer.MediaPlayerSeekCompleteListener() {
            @Override
            public void onSeekCompleted() {
                Log.d(TAG, "onSeekCompleted--- ");
            }
        });
        mPlayer.setStoppedListener(new MediaPlayer.MediaPlayerStoppedListener() {
            @Override
            public void onStopped() {
                Log.d(TAG, "onStopped--- ");
            }
        });
        mPlayer.setBufferingUpdateListener(new MediaPlayer.MediaPlayerBufferingUpdateListener() {
            @Override
            public void onBufferingUpdateListener(int percent) {
                Log.d(TAG, "onBufferingUpdateListener--- " + percent);
            }
        });
        mPlayer.enableNativeLog();
    }

    public void testPlayer(String url){
        if(mPlayer != null)
            mPlayer.prepareAndPlay(url);
    }

    public void play(){
        if(mPlayer != null){
            Log.d(TAG, "prepareToPlay:" + mRemotePusher.getRtmpPlayUrl());
            mPlayer.prepareToPlay(mRemotePusher.getRtmpPlayUrl());
            mStatus = PlayerStatus.PLAYING;
        }
    }

    public void stop(){
        if(mPlayer != null){
            Log.d(TAG, "stop");

            mPlayer.stop();
            mStatus = PlayerStatus.STOPED;
        }
    }
    void startPushMedia(final CloudMedia.SimpleActionListener listener){
        Log.d(TAG, "startPushMedia");
        mRemotePusher.startPushMedia(listener);
    }

    void stopPushMedia(final CloudMedia.SimpleActionListener listener){
        Log.d(TAG, "stopPushMedia");

        mRemotePusher.stopPushMedia(new CloudMedia.SimpleActionListener() {
            @Override
            public boolean onResult(String result) {
                listener.onResult(result);

                stop();
                return true;
            }
        });
    }

    PlayerStatus getStatus() {
        return mStatus;
    }
    String getWhoareyou(){
        return mWhoareyou;
    }

    void onDestroy(){
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.destroy();
        }
    }
}
