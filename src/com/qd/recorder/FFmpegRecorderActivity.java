
package com.qd.recorder;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qd.recorder.ProgressView.State;
import com.qd.recorder.helper.CameraWrapper;
import com.qd.recorder.helper.RecorderHelper;
import com.qd.recorder.helper.RuntimeHelper;
import com.qd.videorecorder.R;

import butterknife.InjectView;
import butterknife.OnClick;


public class FFmpegRecorderActivity extends BaseInjectActivity implements OnTouchListener {
    private final static String TAG = FFmpegRecorderActivity.class.getSimpleName();

    private PowerManager.WakeLock mWakeLock;
    private void ensureWakeLock() {
        if (null == mWakeLock) {
            mWakeLock = RuntimeHelper.acquireWakeLock(this);
        }
    }
    private void releaseWakeLock() {
        RuntimeHelper.releaseWakeLock(mWakeLock);
        mWakeLock = null;
    }

    private CameraWrapper mCameraProxy;
    private RecorderHelper mRecordHelper;

    //判断是否需要录制，手指按下继续，抬起时暂停
//    boolean recording = false;
    //判断是否开始了录制，第一次按下屏幕时设置为true
    boolean	isRecordingStarted = false;

//    TextView txtTimer, txtRecordingSize;
    //分别为闪光灯按钮、取消按钮、下一步按钮、转置摄像头按钮
//    @InjectView(R.id.recorder_cancel)Button cancelBtn;
    @InjectView(R.id.recorder_flashlight) Button flashIcon;
    @InjectView(R.id.recorder_next) Button nextBtn;
    @InjectView(R.id.recorder_frontcamera) Button switchCameraIcon;

    boolean nextEnabled = false;

    //预览的宽高和屏幕宽高
    private int previewWidth = 480;
    private int previewHeight = 480;

//    //音频的采样率，recorderParameters中会有默认值
//    private int sampleRate = 44100;
    //调用系统的录制音频类
//    private AudioRecord audioRecord;

    private CameraView cameraView;

    //Handler handler = new Handler();
	/*private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			if(rec)
				setTotalVideoTime();
			handler.postDelayed(this, 200);
		}
	};*/

//    private Dialog dialog = null;
    //包含显示摄像头数据的surfaceView
    @InjectView(R.id.recorder_surface_parent)
    RelativeLayout topLayout;

    //第一次按下屏幕时记录的时间
    long firstTime = 0;
    //手指抬起是的时间
    long startPauseTime = 0;
    //每次按下手指和抬起之间的暂停时间
    long totalPauseTime = 0;
    //手指抬起是的时间
    long pausedTime = 0;
    //总的暂停时间
    long stopPauseTime = 0;
    //录制的有效总时间
    long totalTime = 0;
    //提示换个场景
    private int recordingChangeTime = 3000;

    boolean recordFinish = false;
    private  Dialog creatingProgress;

    //以下两个只做同步标志，没有实际意义
    private final int[] mVideoRecordLock = new int[0];
    private long mLastAudioTimestamp = 0L;

    //时候保存过视频文件
    private boolean isRecordingSaved = false;
    private boolean isFinalizing = false;

    //进度条
    @InjectView(R.id.recorder_progress) ProgressView progressView;
    //捕获的第一幀的图片
    private String imagePath = null;
    private RecorderState currentRecorderState = RecorderState.PRESS;

    @InjectView(R.id.recorder_surface_state) ImageView stateImageView;

    private byte[] firstData = null;

    private Handler mHandler;
    private void initHandler(){
        mHandler = new Handler(){
            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case 2:
                        int resId = 0;
                        if(currentRecorderState == RecorderState.PRESS){
                            resId = R.drawable.video_text01;
                        }else if(currentRecorderState == RecorderState.LOOSEN){
                            resId = R.drawable.video_text02;
                        }else if(currentRecorderState == RecorderState.CHANGE){
                            resId = R.drawable.video_text03;
                        }else if(currentRecorderState == RecorderState.SUCCESS){
                            resId = R.drawable.video_text04;
                        }
                        stateImageView.setImageResource(resId);
                        break;
                    case 3:
                        if(!mRecordHelper.isRecording()) {
                            initiateRecording(true);
                        } else {
                            //更新暂停的时间
                            stopPauseTime = System.currentTimeMillis();
                            totalPauseTime = stopPauseTime - startPauseTime - mRecordHelper.getMillisPerFrame();
                            pausedTime += totalPauseTime;
                        }
                        mRecordHelper.setNeedRecordFlag();
                        //开始进度条增长
                        progressView.setCurrentState(State.START);
                        //setTotalVideoTime();
                        break;
                    case 4:
                        //设置进度条暂停状态
                        progressView.setCurrentState(State.PAUSE);
                        //将暂停的时间戳添加到进度条的队列中
                        progressView.putProgressList((int) totalTime);
                        mRecordHelper.clearNeedRecordFlag();
                        startPauseTime = System.currentTimeMillis();
                        if(mRecordHelper.isMinimumLimit(totalTime)){
                            currentRecorderState = RecorderState.SUCCESS;
                            sendStateUpdateMessage();
                        } else if(totalTime >= recordingChangeTime) {
                            currentRecorderState = RecorderState.CHANGE;
                            sendStateUpdateMessage();
                        }
                        break;
                    case 5:
                        currentRecorderState = RecorderState.SUCCESS;
                        sendStateUpdateMessage();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void sendStateUpdateMessage() {
        mHandler.sendEmptyMessage(2);
    }
    private void clearTouchMessage() {
        mHandler.removeMessages(3);
        mHandler.removeMessages(4);
    }
    private void sendTouchDownMessage() {
        //如果MediaRecorder没有被初始化
        //执行初始化
        clearTouchMessage();
        mHandler.sendEmptyMessageDelayed(3, 300);
    }
    private void sendTouchUpMessage() {
        clearTouchMessage();
        if(mRecordHelper.needRecord()) {
            mHandler.sendEmptyMessage(4);
        }
    }

    private void sendMiniFilmReadyMessage() {
        mHandler.sendEmptyMessage(5);
    }

    //neon库对opencv做了优化
    static {
        System.loadLibrary("checkneon");
    }

    public native static int  checkNeonFromJNI();
    private boolean initSuccess = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recorder);

        ensureWakeLock();

        initHandler();

        if (CameraWrapper.hasFrontCamera(getPackageManager())) {
            switchCameraIcon.setVisibility(View.VISIBLE);
        } else {
            switchCameraIcon.setVisibility(View.GONE);
        }

        initCameraLayout();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(!initSuccess)
            return false;
        return super.dispatchTouchEvent(ev);
    }


    @Override
    protected void onResume() {
        super.onResume();
        sendStateUpdateMessage();

        ensureWakeLock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!isFinalizing) {
            finish();
            isFinalizing = true;
        }

        releaseWakeLock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Log.i("video", this.getLocalClassName()+"—destroy");

        if (null != mRecordHelper) {
            mRecordHelper.destroy();
        }

        if (null != mCameraProxy) {
            mCameraProxy.destroy();
        }

        releaseResources();

        if (cameraView != null) {
            cameraView.stopPreview();
        }
        cameraView = null;

        firstData = null;

        releaseWakeLock();
    }

    private void initCameraLayout() {
        new AsyncTask<String, Integer, Boolean>(){
            @Override
            protected Boolean doInBackground(String... params) {
                boolean result = setCamera();

                if(!initSuccess) {
                    initVideoRecorder();
                    startRecording();

                    initSuccess = true;
                }

                return result;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if(!result || mCameraProxy == null){
                    //FuncCore.showToast(FFmpegRecorderActivity.this, "无法连接到相机");
                    finish();
                    return;
                }

//                topLayout = (RelativeLayout) findViewById(R.id.recorder_surface_parent);
                if(topLayout != null && topLayout.getChildCount() > 0) {
                    topLayout.removeAllViews();
                }

                cameraView = new CameraView(FFmpegRecorderActivity.this, mCameraProxy);

                handleSurfaceChanged();

                //设置surface的宽高
                int width = RuntimeHelper.getDisplayWidth();
                int height = (int) (width * 1.0f * previewWidth / previewHeight);
                RelativeLayout.LayoutParams layoutParam1 = new RelativeLayout.LayoutParams(width, height);
                layoutParam1.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                //int margin = Util.calculateMargin(previewWidth, screenWidth);
                //layoutParam1.setMargins(0,margin,0,margin);

                RelativeLayout.LayoutParams layoutParam2 = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT);
                layoutParam2.topMargin = width;

                View view = new View(FFmpegRecorderActivity.this);
                view.setFocusable(false);
                view.setBackgroundColor(Color.BLACK);
                view.setFocusableInTouchMode(false);

                topLayout.addView(cameraView, layoutParam1);
                topLayout.addView(view,layoutParam2);

                topLayout.setOnTouchListener(FFmpegRecorderActivity.this);

//                switchCameraIcon.setOnClickListener(FFmpegRecorderActivity.this);
                if(mCameraProxy.isFacingFront()) {
                    flashIcon.setVisibility(View.GONE);
                } else {
                    flashIcon.setVisibility(View.VISIBLE);
                }
            }

        }.execute("start");
    }

    private boolean setCamera() {
        stopPreview();

        if (null == mCameraProxy) {
            mCameraProxy = CameraWrapper.open();
            return true;
        } else {
            return mCameraProxy.setCamera();
        }
    }


    private void initVideoRecorder() {
        if (mRecordHelper == null) {
            mRecordHelper = new RecorderHelper(Util.createFinalPath(this), 480, 480, 1);
        }
    }

    public void startRecording() {
        if (null != mRecordHelper) {
            mRecordHelper.start();
        }
    }

    /**
     * 停止录制
     * @author QD
     *
     */
    public class AsyncStopRecording extends AsyncTask<Void,Integer,Void> {
        private ProgressBar bar;
        private TextView progress;
        @Override
        protected void onPreExecute() {
            isFinalizing = true;
            recordFinish = true;
            if (null != mRecordHelper) {
                mRecordHelper.resetAudio();
            }

            //创建处理进度条
            creatingProgress= new Dialog(FFmpegRecorderActivity.this,R.style.Dialog_loading_noDim);
            Window dialogWindow = creatingProgress.getWindow();
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().density*240);
            lp.height = (int) (getResources().getDisplayMetrics().density*80);
            lp.gravity = Gravity.CENTER;
            dialogWindow.setAttributes(lp);
            creatingProgress.setCanceledOnTouchOutside(false);
            creatingProgress.setContentView(R.layout.activity_recorder_progress);

            progress = (TextView) creatingProgress.findViewById(R.id.recorder_progress_progresstext);
            bar = (ProgressBar) creatingProgress.findViewById(R.id.recorder_progress_progressbar);
            creatingProgress.show();

            //txtTimer.setVisibility(View.INVISIBLE);
            //handler.removeCallbacks(mUpdateTimeTask);
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress.setText(values[0] + "%");
            bar.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if(firstData != null) {
                getFirstCapture(new RecorderHelper.PublishProgressInterface() {
                    @Override
                    public void onProgress(int progress) {
                        publishProgress(progress);
                    }
                }, firstData);
            }

            isFinalizing = false;
            if (mRecordHelper.testStopVideo()) {
                releaseResources();
            }
            publishProgress(100);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            creatingProgress.dismiss();
            returnToCaller(true);
            mRecordHelper.registerVideo(getContentResolver());
            mRecordHelper.stopVideo();
        }
    }

    /**
     * 依据byte[]里的数据合成一张bitmap，
     * 截成480*480，并且旋转90度后，保存到文件
     * @param data
     */
    private void getFirstCapture(RecorderHelper.PublishProgressInterface asyncStopRecording, byte[] data) {
        String path = RecorderHelper.getFirstCapture(FFmpegRecorderActivity.this, data,
                previewWidth, previewHeight,
                mCameraProxy.isFacingFront(), asyncStopRecording);
        if (TextUtils.isEmpty(path)) {
            isFirstFrame = true;
        } else {
            isFirstFrame = false;
        }
    }

    /**
     * 放弃视频时弹出框
     */
    private void showCancellDialog(){
        Util.showDialog(FFmpegRecorderActivity.this, "提示", "确定要放弃本视频吗？", 2, new Handler(){
            @Override
            public void dispatchMessage(Message msg) {
                if(msg.what == 1)
                    videoTheEnd(false);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mRecordHelper.isRecording())
            showCancellDialog();
        else
            videoTheEnd(false);
    }

    //获取第一幀的图片
    private boolean isFirstFrame = true;

    /**
     * 显示摄像头的内容，以及返回摄像头的每一帧数据
     * @author QD
     *
     */
    class CameraView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {
        private SurfaceHolder mHolder;
        private CameraWrapper mCameraWrapper;

        public CameraView(Context context, CameraWrapper cameraProxy) {
            super(context);
            mHolder = getHolder();
            mHolder.addCallback(CameraView.this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            mCameraWrapper = cameraProxy;
            mCameraWrapper.setPreviewCallBack(CameraView.this);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            stopPreview();
            mCameraWrapper.setPreviewDisplay(holder);
        }

        public void surfaceChanged(SurfaceHolder  holder, int format, int width, int height) {
            mCameraWrapper.stopPreview();
            handleSurfaceChanged();
            startPreview();
            mCameraWrapper.autoFocus(null);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mHolder.addCallback(null);
            mCameraWrapper.setPreviewCallBack(null);
        }

        public void startPreview() {
            mCameraWrapper.startPreview();
        }

        public void stopPreview() {
            mCameraWrapper.stopPreview();
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //计算时间戳
            long frameTimeStamp = 0L;
            long audioTimestamp = mRecordHelper.getAudioTimeStamp();
            if (audioTimestamp == 0L && firstTime > 0L) {
                frameTimeStamp = 1000L * (System.currentTimeMillis() - firstTime);
            } else if (mLastAudioTimestamp == audioTimestamp) {
                frameTimeStamp = audioTimestamp + mRecordHelper.getNounSecondPerFrame();
            } else {
                long l2 = mRecordHelper.getAudioInterval();
                frameTimeStamp = l2 + audioTimestamp;
                mLastAudioTimestamp = audioTimestamp;
            }

            //录制视频
            synchronized (mVideoRecordLock) {
                if (mRecordHelper.isReadyToRecord()) {
                    //保存某一幀的图片
                    if(isFirstFrame){
                        isFirstFrame = false;
                        firstData = data;
					/*Message msg = mHandler.obtainMessage(1);
					msg.obj = data;
					msg.what = 1;
					mHandler.sendMessage(msg);*/
                    }

                    //超过最低时间时，下一步按钮可点击
                    totalTime = System.currentTimeMillis() - firstTime - pausedTime - mRecordHelper.getMillisPerFrame();
                    if(!nextEnabled && totalTime >= recordingChangeTime){
                        nextEnabled = true;
                        nextBtn.setEnabled(true);
                    }

                    if (nextEnabled && mRecordHelper.isMinimumLimit(totalTime)) {
                        sendMiniFilmReadyMessage();
                    }

                    if(currentRecorderState == RecorderState.PRESS && totalTime >= recordingChangeTime){
                        currentRecorderState = RecorderState.LOOSEN;
                        sendStateUpdateMessage();
                    }

                    mRecordHelper.record();
                }

                mRecordHelper.setSavedFrame(mCameraProxy.isFacingFront(), data, frameTimeStamp,
                        previewWidth, previewHeight);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!recordFinish) {
            if (mRecordHelper.isMaximumLimit(totalTime)) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sendTouchDownMessage();
                        break;
                    case MotionEvent.ACTION_UP:
                        sendTouchUpMessage();

                        break;
                }
            } else {
                //如果录制时间超过最大时间，保存视频
                saveRecording();
            }
        }
        return true;
    }
    /**
     * 关闭摄像头的预览
     */
    public void stopPreview() {
        if (null != mCameraProxy) {
            mCameraProxy.stopPreview();
        }
    }

    private void handleSurfaceChanged() {
        Camera.Size previewSize = mCameraProxy.getPreviewSize();
        if (previewSize == null) {
            //showToast(this, "无法连接到相机");
            finish();
            return;
        }

        //获取计算过的摄像头分辨率
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;
        mCameraProxy.setSize(previewWidth, previewHeight);

        mRecordHelper.setSize(previewWidth, previewHeight);

        mCameraProxy.updateFrameRateAndOrientation(mRecordHelper.getFrameRate(), this);
    }

    @OnClick(R.id.recorder_flashlight)
    public void onFlashButtonClicked() {
        if (!initSuccess) return;

        if(!CameraWrapper.hasCameraFlash(getPackageManager())){
            //showToast(this, "不能开启闪光灯");
            return;
        }

        if (null != mCameraProxy) {
            boolean flash = mCameraProxy.swapFlashMode();
            flashIcon.setSelected(flash);
        }
    }

    @OnClick(R.id.recorder_frontcamera)
    public void onCameraSwap() {
        if (!initSuccess) return;

        //转换摄像头
        mCameraProxy.swapCamera();
        if (mCameraProxy.isFacingFront()) {
            flashIcon.setVisibility(View.GONE);
        } else {
            flashIcon.setVisibility(View.VISIBLE);
        }

        initCameraLayout();
    }

    @OnClick(R.id.recorder_next)
    public void onNextButtonClicked() {
        if (isRecordingStarted) {
            saveRecording();
        } else {
            initiateRecording(false);
        }
    }
    @OnClick(R.id.recorder_cancel)
    public void onCancelButtonClicked() {
        if (mRecordHelper.isRecording()) {
            showCancellDialog();
        } else {
            videoTheEnd(false);
        }
    }


    /**
     * 结束录制
     * @param isSuccess
     */
    public void videoTheEnd(boolean isSuccess)
    {
        releaseResources();
        if (null != mRecordHelper) {
            mRecordHelper.onComplete(isSuccess);
        }

        returnToCaller(isSuccess);
    }

    /**
     * 设置返回结果
     * @param valid
     */
    private void returnToCaller(boolean valid) {
        try {
            setActivityResult(valid);
            if(valid) {
                Intent intent = new Intent(this, FFmpegEffectActivity.class);
                intent.putExtra(CONSTANTS.EXTRA_VIDEO_PATH, mRecordHelper.getVideoPath());
                intent.putExtra(CONSTANTS.EXTRA_SNAP_PATH, imagePath);
                startActivity(intent);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            finish();
        }
    }

    private void setActivityResult(boolean valid) {
        Intent resultIntent = new Intent();
        int resultCode;
        if (valid) {
            resultCode = RESULT_OK;
            resultIntent.setData(mRecordHelper.getVideoUri());
        } else {
            resultCode = RESULT_CANCELED;
        }

        setResult(resultCode, resultIntent);
    }

    /**
     * 保存录制的视频文件
     */
    private void saveRecording() {
        mRecordHelper.clearNeedRecordFlag();

        if (isRecordingStarted) {
            mRecordHelper.stopAudio();
            if(!isRecordingSaved){
                isRecordingSaved = true;
                new AsyncStopRecording().execute();
            }
        } else {
            videoTheEnd(false);
        }
    }

    /**
     * 求出录制的总时间

     private synchronized void setTotalVideoTime(){
     if(totalTime > 0)
     txtTimer.setText(Util.getRecordingTimeFromMillis(totalTime));

     } */

    /**
     * 释放资源，停止录制视频和音频
     */
    private void releaseResources(){
        isRecordingSaved = true;

        if (null != mRecordHelper) {
            mRecordHelper.release();
        }

        //progressView.putProgressList((int) totalTime);
        //停止刷新进度
        progressView.setCurrentState(State.PAUSE);
    }

    /**
     * 第一次按下时，初始化录制数据
     * @param isActionDown
     */
    private void initiateRecording(boolean isActionDown)  {
        isRecordingStarted = true;
        firstTime = System.currentTimeMillis();

        mRecordHelper.setRecording();
        totalPauseTime = 0;
        pausedTime = 0;

        //txtTimer.setVisibility(View.VISIBLE);
        //handler.removeCallbacks(mUpdateTimeTask);
        //handler.postDelayed(mUpdateTimeTask, 100);
    }

    public static enum RecorderState {
        PRESS(1),LOOSEN(2),CHANGE(3),SUCCESS(4);

        static RecorderState mapIntToValue(final int stateInt) {
            for (RecorderState value : RecorderState.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }
            return PRESS;
        }

        private int mIntValue;

        RecorderState(int intValue) {
            mIntValue = intValue;
        }

        int getIntValue() {
            return mIntValue;
        }
    }
}