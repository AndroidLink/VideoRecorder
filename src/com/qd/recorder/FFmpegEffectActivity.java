package com.qd.recorder;

import android.os.Bundle;

import com.qd.videorecorder.R;

import butterknife.OnClick;

/**
 * Created by yangfeng on 14/12/22.
 */
public class FFmpegEffectActivity extends FFmpegPreviewActivity {
    private String mVideoPath;
    private String mSnapPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVideoPath = getIntent().getStringExtra(CONSTANTS.EXTRA_VIDEO_PATH);
        mSnapPath = getIntent().getStringExtra(CONSTANTS.EXTRA_SNAP_PATH);
    }

    @OnClick(R.id.play_next)
    public void onNextViewClicked() {
    }
}
