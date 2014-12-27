package com.qd.recorder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import org.bytedeco.javacpp.opencv_core.CvMat;

import com.qd.recorder.helper.BitmapHelper;
import com.qd.recorder.helper.ImageProcessor;
import com.qd.recorder.helper.RuntimeHelper;
import com.qd.videorecorder.R;

import butterknife.InjectView;
import butterknife.OnClick;

public class FFmpegPreviewActivity extends BaseInjectActivity implements OnCompletionListener,
        TextureView.SurfaceTextureListener {

	private String path;
    private String imgPath;
    private MediaPlayer mediaPlayer;

    @InjectView(R.id.preview_video_parent) RelativeLayout previewParent;
    @InjectView(R.id.preview_video) TextureView surfaceView;

	@InjectView(R.id.preview_play) ImageView imagePlay;
//    @InjectView(R.id.play_cancel) Button cancelBtn;


    @OnClick(R.id.preview_video)
    public void onVideoViewClicked() {
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            imagePlay.setVisibility(View.VISIBLE);
        } else {
            onPlayViewClicked();
        }
    }

    @OnClick(R.id.preview_play)
    public void onPlayViewClicked() {
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
        imagePlay.setVisibility(View.GONE);
    }

    @OnClick(R.id.play_cancel)
    public void onCancelViewClicked() {
        stop();
    }

    @OnClick(R.id.play_next)
    public void onNextViewClicked() {
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ffmpeg_preview);


        path = getIntent().getStringExtra(CONSTANTS.EXTRA_VIDEO_PATH);
        imgPath = getIntent().getStringExtra(CONSTANTS.EXTRA_SNAP_PATH);

        initParentSurfaceView();

        surfaceView.setSurfaceTextureListener(this);

		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
	}

    int maxSize;
    private void initParentSurfaceView() {
        int displayWidth = RuntimeHelper.getDisplayWidth();
        int displayHeight = RuntimeHelper.getDisplayHeight();
        LayoutParams layoutParams = (LayoutParams) previewParent.getLayoutParams();
        layoutParams.width = displayWidth;
        layoutParams.height = displayWidth;
        previewParent.setLayoutParams(layoutParams);
        maxSize = Math.min(displayWidth, displayHeight);
        BitmapHelper.showBitmapBackground(previewParent, imgPath, maxSize);
    }

	@Override
	protected void onStop() {
		if(mediaPlayer.isPlaying()){
			mediaPlayer.pause();
			imagePlay.setVisibility(View.GONE);
		}
		super.onStop();
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1,
			int arg2) {
        playMediaSource(mediaPlayer, new Surface(arg0), path);
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
		return false;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1,
			int arg2) {
		
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
		
	}

    private void stop(){
		mediaPlayer.stop();
		Intent intent = new Intent(this,FFmpegRecorderActivity.class);
		startActivity(intent);
		finish();
	}
	
	@Override
	public void onBackPressed() {
		stop();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		imagePlay.setVisibility(View.VISIBLE);
	}

    protected static void playMediaSource(MediaPlayer player, Surface view, String mediaPath) {
        try {
            player.reset();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 设置需要播放的视频
            player.setDataSource(mediaPath);
            // 把视频画面输出到Surface
            player.setSurface(view);
            player.setLooping(true);
            player.prepare();
            player.seekTo(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
