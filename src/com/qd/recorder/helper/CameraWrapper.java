package com.qd.recorder.helper;

import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;

import com.qd.recorder.FFmpegRecorderActivity;
import com.qd.recorder.Util;

import java.io.IOException;
import java.util.List;

/**
 * Created by yangfeng on 14/12/23.
 */
public class CameraWrapper {
    //摄像头以及它的参数
    private Camera cameraDevice;
    private Camera.Parameters cameraParameters = null;
    private Camera mCamera;

    public void destroy() {
        if(cameraDevice != null){
            cameraDevice.setPreviewCallback(null);
            cameraDevice.release();
        }
        cameraDevice = null;
        mCamera = null;
    }

    public void setFlashMode(String flashMode) {
//        if (null != cameraParameters && null != mCamera) {
            cameraParameters.setFlashMode(flashMode);
            mCamera.setParameters(cameraParameters);
//        }
    }

    public List<Camera.Size> getResolutionList() {
        if (mCamera == null) {
            return null;
        }
        return Util.getResolutionList(mCamera);
    }

    public void setSize(int previewWidth, int previewHeight) {
        cameraParameters.setPreviewSize(previewWidth, previewHeight);
    }

    public void updateFrameRateAndOrientation(int frameRate, int orientation) {
        //设置预览帧率
        cameraParameters.setPreviewFrameRate(frameRate);
        //系统版本为8一下的不支持这种对焦
        if(Build.VERSION.SDK_INT >  Build.VERSION_CODES.FROYO)
        {
            mCamera.setDisplayOrientation(orientation);
            List<String> focusModes = cameraParameters.getSupportedFocusModes();
            if(focusModes != null){
                Log.i("video", Build.MODEL);
                if (((Build.MODEL.startsWith("GT-I950"))
                        || (Build.MODEL.endsWith("SCH-I959"))
                        || (Build.MODEL.endsWith("MEIZU MX3"))) &&
                        focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){

                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else {
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                        cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                    } else {
                        cameraParameters.setFocusMode(focusModes.get(0));
                    }
                }
            }
        } else {
            mCamera.setDisplayOrientation(90);
        }
        mCamera.setParameters(cameraParameters);
    }

    public boolean stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            return true;
        }
        return false;
    }

    public boolean startPreview() {
        if (null != mCamera) {
            mCamera.startPreview();
            return true;
        }
        return false;
    }

    public void setCamera(int defaultCameraId) {
        if(mCamera != null) {
            mCamera.release();
        }

        if(defaultCameraId >= 0) {
            cameraDevice = Camera.open(defaultCameraId);
        } else {
            cameraDevice = Camera.open();
        }
    }

    public void setPreviewCallBack(Camera.PreviewCallback previewCallback) {
        // todo : when does camera instant?
//        mCamera = camera;
        cameraParameters = mCamera.getParameters();
        mCamera.setPreviewCallback(previewCallback);
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException exception) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void autoFocus(Camera.AutoFocusCallback callback) {
        mCamera.autoFocus(callback);
    }
}
