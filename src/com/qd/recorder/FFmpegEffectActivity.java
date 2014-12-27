package com.qd.recorder;

import android.os.Bundle;
import android.text.TextUtils;

import org.bytedeco.javacpp.opencv_core;
import com.qd.recorder.helper.BitmapHelper;
import com.qd.recorder.helper.ImageProcessor;
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
        testImageEffect();
    }


    private static class Mat extends opencv_core.CvMat {
    }

    private Mat mat1,mat2;
    private void applyWatermark() {
//        // 初始化数据
//        mat1 = new Mat();
//        mat2 = new Mat();
//        Mat mat1Sub = new Mat();
//
//        // 加载图片
//        final int minSize = Math.min(RuntimeHelper.getDisplayWidth(), RuntimeHelper.getDisplayHeight());
//        Bitmap bt1 = decodeBitmap(snapPath, minSize);
//        Bitmap bt2 = BitmapFactory.decodeResource(getResources(),
//                R.drawable.im_flower3);
//        Bitmap bt3 = null;
//
//        // 转换数据
//        Utils.bitmapToMat(bt1, mat1);
//        Utils.bitmapToMat(bt2, mat2);
//
//        /** 方法一加权 高级方式 可实现水印效果*********/
//
//        // mat1Sub=mat1.submat(0, mat2.rows(), 0, mat2.cols());
//        // Core.addWeighted(mat1Sub, 1, mat2, 0.3, 0., mat1Sub);
//
//        /** 方法二 求差 ********/
//
//        // submat(y坐标, 图片2的高, x坐标，图片2的宽);
//        // mat1Sub=mat1.submat(0, mat2.rows(), 0, mat2.cols());
//        // mat2.copyTo(mat1Sub);
//
//        /*** 方法三兴趣区域裁剪 **/
//        // 定义感兴趣区域Rect(x坐标，y坐标,图片2的宽，图片2的高)
//        Rect rec = new Rect(0, 0, mat2.cols(), mat2.rows());
//        // submat(y坐标, 图片2的高, x坐标，图片2的宽);
//        mat1Sub = mat1.submat(rec);
//        mat2.copyTo(mat1Sub);
//        //转化为android识别的图像数据注意bt3的宽高要和mat1一至
//        bt3 = Bitmap.createBitmap(mat1.cols(), mat1.rows(), Bitmap.Config.RGB_565);
//        Utils.matToBitmap(mat1, bt3);
////        iv3.setImageBitmap(bt3);
//        applyViewBackground(previewParent, bt3);
    }


    private static final String EFFECT_SUFFIX = "_effect";
    private int mOperatorIndex = 0;
    private Object mProcessingLock = new Object();
    private boolean mIsProcessing = false;

    private void testImageEffect() {
        synchronized (mProcessingLock) {
            if (mIsProcessing) {
                return;
            }
            mIsProcessing = true;
        }

        String effectPath = Util.duplicate(mSnapPath, EFFECT_SUFFIX);
        if (TextUtils.isEmpty(effectPath)) {
            // todo: error met.
            return;
        }
//        ImageProcessor.clone(mSnapPath, effectPath);

        switch (mOperatorIndex) {
            default:
                ImageProcessor.smooth(effectPath);
                break;
            case 1:
                ImageProcessor.padding(effectPath);
                break;
            case 2:
                ImageProcessor.pyramid_up(effectPath);
                break;
            case 3:
                ImageProcessor.pyramid_down(effectPath);
                break;
            case 4:
                ImageProcessor.morphology_Dilation(effectPath, 0);
                break;
            case 5:
                ImageProcessor.morphology_Dilation(effectPath, 1);
                break;
            case 6:
                ImageProcessor.morphology_Dilation(effectPath, 2);
            case 7:
                ImageProcessor.morphology_Erosion(effectPath, 0);
                break;
            case 8:
                ImageProcessor.morphology_Erosion(effectPath, 1);
                break;
            case 9:
                ImageProcessor.morphology_Erosion(effectPath, 2);
                break;
            case 10:
                ImageProcessor.thresholding(effectPath, 0);
                break;
            case 11:
                ImageProcessor.thresholding(effectPath, 1);
                break;
            case 12:
                ImageProcessor.thresholding(effectPath, 2);
                break;
            case 13:
                ImageProcessor.thresholding(effectPath, 3);
                break;
            case 14:
                ImageProcessor.thresholding(effectPath, 4);
                break;
        }

        synchronized (mProcessingLock) {
            BitmapHelper.showBitmapBackground(previewParent, effectPath, maxSize);

            mOperatorIndex++;
            mOperatorIndex %= (14);
            mIsProcessing = false;
        }
    }
}
