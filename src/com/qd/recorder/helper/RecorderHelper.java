package com.qd.recorder.helper;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core;
import com.qd.recorder.NewFFmpegFrameRecorder;
import com.qd.recorder.RecorderParameters;
import com.qd.recorder.Util;

import java.nio.Buffer;
import java.nio.ShortBuffer;

/**
 * Created by yangfeng on 14/12/22.
 */
public class RecorderHelper {
    //录制的最长时间
    private static final int recordingTime = 8000;
    //录制的最短时间
    private static final int recordingMinimumTime = 6000;

    //判断是否需要录制，点击下一步时暂停录制
    private boolean rec = false;
    private boolean recording = false;
    //录制视频和保存音频的类
    private volatile NewFFmpegFrameRecorder videoRecorder;
    //录制音频的线程
    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;
    //开启和停止录制音频的标记
    volatile boolean runAudioThread = true;

    //音频的采样率，recorderParameters中会有默认值
    private int sampleRate = 44100;

    //音频时间戳
    private volatile long mAudioTimestamp = 0L;
    private volatile long mAudioTimeRecorded;

    //视频时间戳
    private long mVideoTimestamp = 0L;

    private final int[] mAudioRecordLock = new int[0];

    public RecorderHelper(RecorderParameters recorderParameters, String filename, int imageWidth,
                          int imageHeight, int audioChannels) {
        sampleRate = recorderParameters.getAudioSamplingRate();

        videoRecorder = new NewFFmpegFrameRecorder(filename, imageWidth, imageHeight, audioChannels);
        videoRecorder.setFormat(recorderParameters.getVideoOutputFormat());
        videoRecorder.setSampleRate(recorderParameters.getAudioSamplingRate());
        videoRecorder.setFrameRate(recorderParameters.getVideoFrameRate());
        videoRecorder.setVideoCodec(recorderParameters.getVideoCodec());
        videoRecorder.setVideoQuality(recorderParameters.getVideoQuality());
        videoRecorder.setAudioQuality(recorderParameters.getVideoQuality());
        videoRecorder.setAudioCodec(recorderParameters.getAudioCodec());
        videoRecorder.setVideoBitrate(recorderParameters.getVideoBitrate());
        videoRecorder.setAudioBitrate(recorderParameters.getAudioBitrate());

        audioRecordRunnable = new AudioRecordRunnable();
        audioThread = new Thread(audioRecordRunnable);
    }

    public void destroy() {
        recording = false;
        runAudioThread = false;
    }

    public void start() {
        try {
            videoRecorder.start();
            audioThread.start();
        } catch (NewFFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void release() {
        try {
            if(videoRecorder != null) {
                videoRecorder.stop();
                videoRecorder.release();
            }
        } catch (com.googlecode.javacv.FrameRecorder.Exception e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        videoRecorder = null;
    }

    public void resetAudio() {
        runAudioThread = false;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording() {
        recording = true;
    }

    public void stopVideo() {
        videoRecorder = null;
    }

    public void setSize(int previewWidth, int previewHeight) {
        if(videoRecorder != null) {
            videoRecorder.setImageWidth(previewWidth);
            videoRecorder.setImageHeight(previewHeight);
        }

    }

    public void stopAudio() {
        runAudioThread = false;
    }

    public void record(opencv_core.IplImage yuvIplImage, long frameTime, long timeStamp) {
        try {

            mVideoTimestamp += frameTime;
            if(timeStamp > mVideoTimestamp) {
                mVideoTimestamp = timeStamp;
            }

            videoRecorder.setTimestamp(timeStamp);
            videoRecorder.record(yuvIplImage);
        } catch (com.googlecode.javacv.FrameRecorder.Exception e) {
            Log.i("recorder", "录制错误" + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean testStopVideo() {
        if (videoRecorder != null && recording) {
            recording = false;
            return true;
        }

        return false;
    }

    public long getAudioInterval() {
        return (System.nanoTime() - mAudioTimeRecorded) / 1000L;
    }

    public long getAudioTimeStamp() {
        return mAudioTimestamp;
    }

    public boolean isMaximumLimit(long totalTime) {
        return totalTime < recordingTime;
    }

    public boolean isMinimumLimit(long totalTime) {
        return totalTime >= recordingMinimumTime;
    }

    public void setNeedRecordFlag() {
        rec = true;
    }

    public void clearNeedRecordFlag() {
        rec = false;
    }

    public boolean needRecord() {
        return rec;
    }

    /**
     * 录制音频的线程
     * @author QD
     *
     */
    class AudioRecordRunnable implements Runnable {
        int bufferSize;
        short[] audioData;
        int bufferReadResult;
        private final AudioRecord audioRecord;
        public volatile boolean isInitialized;
        private int mCount =0;
        private AudioRecordRunnable() {
            bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,bufferSize);
            audioData = new short[bufferSize];
        }

        /**
         * shortBuffer包含了音频的数据和起始位置
         * @param shortBuffer
         */
        private void record(ShortBuffer shortBuffer) {
            try {
                synchronized (mAudioRecordLock) {
                    if (videoRecorder != null) {
                        this.mCount += shortBuffer.limit();
                        videoRecorder.record(0,new Buffer[] {shortBuffer});
                    }
                    return;
                }
            } catch (FrameRecorder.Exception localException){
                localException.printStackTrace();
            }
        }

        /**
         * 更新音频的时间戳
         */
        private void updateTimestamp() {
            if (videoRecorder != null) {
                int i = Util.getTimeStampInNsFromSampleCounted(this.mCount);
                if (mAudioTimestamp != i) {
                    mAudioTimestamp = i;
                    mAudioTimeRecorded =  System.nanoTime();
                }
            }
        }

        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            this.isInitialized = false;
            if(audioRecord != null) {
                //判断音频录制是否被初始化
                while (this.audioRecord.getState() == 0) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException localInterruptedException) {
                        localInterruptedException.printStackTrace();
                    }
                }

                this.isInitialized = true;
                this.audioRecord.startRecording();
                while (((runAudioThread) || (mVideoTimestamp > mAudioTimestamp)) &&
                        (mAudioTimestamp < (1000 * recordingTime))) {
                    updateTimestamp();
                    bufferReadResult = this.audioRecord.read(audioData, 0, audioData.length);
                    if ((bufferReadResult > 0) && ((recording && rec) || (mVideoTimestamp > mAudioTimestamp)))
                        record(ShortBuffer.wrap(audioData, 0, bufferReadResult));
                }

                this.audioRecord.stop();
                this.audioRecord.release();
            }
        }
    }

}
