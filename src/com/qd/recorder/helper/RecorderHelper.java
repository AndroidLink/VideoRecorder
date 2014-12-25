package com.qd.recorder.helper;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core;
import com.qd.recorder.CONSTANTS;
import com.qd.recorder.NewFFmpegFrameRecorder;
import com.qd.recorder.RecorderParameters;
import com.qd.recorder.SavedFrames;
import com.qd.recorder.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ShortBuffer;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

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
        strVideoPath = filename;
        fileVideoPath = new File(strVideoPath);

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

            yuvIplImage = null;
            lastSavedframe = null;
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

        //构建一个IplImage对象，用于录制视频
        //和opencv中的cvCreateImage方法一样
        if (null != yuvIplImage) {
            // todo: release it?
            yuvIplImage.release();
        }
        yuvIplImage = opencv_core.IplImage.create(previewHeight, previewWidth, IPL_DEPTH_8U, 2);
    }

    public void stopAudio() {
        runAudioThread = false;
    }

    public void record(long frameTime) {
        try {
            yuvIplImage.getByteBuffer().put(lastSavedframe.getFrameBytesData());
            long timeStamp = lastSavedframe.getTimeStamp();
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

    //视频文件的存放地址
    private String strVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "rec_video.mp4";
    //视频文件对象
    private File fileVideoPath = null;
    //视频文件在系统中存放的url
    private Uri uriVideoPath = null;

    public void onComplete(boolean isSuccess) {
        if(fileVideoPath != null && fileVideoPath.exists() && !isSuccess) {
            fileVideoPath.delete();
        }
    }

    public String getVideoPath() {
        return strVideoPath;
    }

    public Uri getVideoUri() {
        return uriVideoPath;
    }

    public long getVideoFileLength() {
        return new File(strVideoPath).length();
    }
    /**
     * 向系统注册我们录制的视频文件，这样文件才会在sd卡中显示
     */
    public void registerVideo(ContentResolver contentResolver) {
        Uri videoTable = Uri.parse(CONSTANTS.VIDEO_CONTENT_URI);

        Util.videoContentValues.put(MediaStore.Video.Media.SIZE, getVideoFileLength());
        try{
            uriVideoPath = contentResolver.insert(videoTable, Util.videoContentValues);
        } catch (Throwable e){
            uriVideoPath = null;
            strVideoPath = null;
            e.printStackTrace();
        } finally{
            // do nothing
        }
        Util.videoContentValues = null;
    }


    //IplImage对象,用于存储摄像头返回的byte[]，以及图片的宽高，depth，channel等
    private opencv_core.IplImage yuvIplImage = null;
    //每一幀的数据结构
    private SavedFrames lastSavedframe = new SavedFrames(null, 0L);

    public boolean isReadyToRecord() {
        return isRecording() && needRecord() && lastSavedframe != null &&
                lastSavedframe.getFrameBytesData() != null && yuvIplImage != null;
    }

    public void setSavedFrame(boolean isFront, byte[] data, long frameTimeStamp, int width, int height) {

        final byte[] tempData;
        if(isFront) {
            tempData = rotateYUV420Degree270(data, width, height);
        } else {
            tempData = rotateYUV420Degree90(data, width, height);
        }
        lastSavedframe = new SavedFrames(tempData, frameTimeStamp);
    }

    private static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++) {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2) {
            for(int y = 0;y < imageHeight/2;y++) {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
    }

    private static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        int i = 0;
        int count = 0;

        for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
            yuv[count] = data[i];
            count++;
        }

        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
                * imageHeight; i -= 2) {
            yuv[count++] = data[i - 1];
            yuv[count++] = data[i];
        }
        return yuv;
    }

    private static byte[] rotateYUV420Degree270(byte[] data, int imageWidth, int imageHeight) {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        int nWidth = 0, nHeight = 0;
        int wh = 0;
        int uvHeight = 0;
        if(imageWidth != nWidth || imageHeight != nHeight) {
            nWidth = imageWidth;
            nHeight = imageHeight;
            wh = imageWidth * imageHeight;
            uvHeight = imageHeight >> 1;//uvHeight = height / 2
        }

        //旋转Y
        int k = 0;
        for(int i = 0; i < imageWidth; i++) {
            int nPos = 0;
            for(int j = 0; j < imageHeight; j++) {
                yuv[k] = data[nPos + i];
                k++;
                nPos += imageWidth;
            }
        }

        for(int i = 0; i < imageWidth; i+=2){
            int nPos = wh;
            for(int j = 0; j < uvHeight; j++) {
                yuv[k] = data[nPos + i];
                yuv[k + 1] = data[nPos + i + 1];
                k += 2;
                nPos += imageWidth;
            }
        }
        //这一部分可以直接旋转270度，但是图像颜色不对
//	    // Rotate the Y luma
//	    int i = 0;
//	    for(int x = imageWidth-1;x >= 0;x--)
//	    {
//	        for(int y = 0;y < imageHeight;y++)
//	        {
//	            yuv[i] = data[y*imageWidth+x];
//	            i++;
//	        }
//
//	    }
//	    // Rotate the U and V color components
//		i = imageWidth*imageHeight;
//	    for(int x = imageWidth-1;x > 0;x=x-2)
//	    {
//	        for(int y = 0;y < imageHeight/2;y++)
//	        {
//	            yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
//	            i++;
//	            yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
//	            i++;
//	        }
//	    }
        return rotateYUV420Degree180(yuv,imageWidth,imageHeight);
    }

    public static byte[] cropYUV420(byte[] data,int imageW,int imageH,int newImageH){
        int cropH;
        int i,j,count,tmp;
        byte[] yuv = new byte[imageW*newImageH*3/2];

        cropH = (imageH - newImageH)/2;

        count = 0;
        for(j=cropH;j<cropH+newImageH;j++){
            for(i=0;i<imageW;i++){
                yuv[count++] = data[j*imageW+i];
            }
        }

        //Cr Cb
        tmp = imageH+cropH/2;
        for(j=tmp;j<tmp + newImageH/2;j++){
            for(i=0;i<imageW;i++){
                yuv[count++] = data[j*imageW+i];
            }
        }

        return yuv;
    }


    public interface PublishProgressInterface {
        public void onProgress(int progress);
    }

    public static String getFirstCapture(Context contexts, byte[] data, int width, int height,
                                          boolean isFacingFront,
                                          PublishProgressInterface asyncStopRecording) {
        asyncStopRecording.onProgress(10);

        String captureBitmapPath = Util.createImagePath(contexts);
        YuvImage localYuvImage = new YuvImage(data, 17, width, height, null);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileOutputStream outStream = null;

        asyncStopRecording.onProgress(50);

        try {
            File file = new File(captureBitmapPath);
            if(!file.exists()) {
                file.createNewFile();
            }

            localYuvImage.compressToJpeg(new Rect(0, 0, width, height),100, bos);
            Bitmap localBitmap1 = BitmapFactory.decodeByteArray(bos.toByteArray(),
                    0, bos.toByteArray().length);

            bos.close();

            Matrix localMatrix = new Matrix();
            if (isFacingFront) {
                localMatrix.setRotate(270.0F);
            } else {
                localMatrix.setRotate(90.0F);
            }

            Bitmap	localBitmap2 = Bitmap.createBitmap(localBitmap1, 0, 0,
                    localBitmap1.getHeight(),
                    localBitmap1.getHeight(),
                    localMatrix, true);

            asyncStopRecording.onProgress(70);

            ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
            localBitmap2.compress(Bitmap.CompressFormat.JPEG, 100, bos2);

            outStream = new FileOutputStream(captureBitmapPath);
            outStream.write(bos2.toByteArray());
            outStream.close();

            localBitmap1.recycle();
            localBitmap2.recycle();

            asyncStopRecording.onProgress(90);
        } catch (FileNotFoundException e) {
            captureBitmapPath = null;
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            captureBitmapPath = null;
        }
        return captureBitmapPath;
    }
}
