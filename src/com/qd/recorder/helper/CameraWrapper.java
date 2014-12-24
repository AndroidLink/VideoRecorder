package com.qd.recorder.helper;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;

import com.qd.recorder.FFmpegRecorderActivity;
import com.qd.recorder.Util;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by yangfeng on 14/12/23.
 */
public class CameraWrapper {
    //摄像头以及它的参数
//    private Camera cameraDevice;
    private Camera.Parameters cameraParameters = null;
    private Camera mCamera;

    public void destroy() {
        if(mCamera != null){
            mCamera.setPreviewCallback(null);
            mCamera.release();
        }
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

    public void updateFrameRateAndOrientation(int frameRate, Activity activity) {
        final int orientation = Util.determineDisplayOrientation(activity, defaultCameraId);

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
        if (isPreviewOn) {
            if (mCamera != null) {
                mCamera.stopPreview();
                isPreviewOn = false;
                return true;
            }
        }
        return false;
    }

    public boolean startPreview() {
        if (!isPreviewOn) {
            if (null != mCamera) {
                mCamera.startPreview();
                isPreviewOn = true;
                return true;
            }
        }
        return false;
    }

    public static CameraWrapper open() {
        CameraWrapper wrapper = new CameraWrapper();
        wrapper.setCamera();
        return wrapper;
    }

    public boolean setCamera() {
        try {
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                int numberOfCameras = Camera.getNumberOfCameras();

                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == cameraSelection) {
                        defaultCameraId = i;
                    }
                }
            }
        } catch(Exception e) {
            return false;
        }

        if(mCamera != null) {
            mCamera.release();
        }

        if(defaultCameraId >= 0) {
            mCamera = Camera.open(defaultCameraId);
        } else {
            mCamera = Camera.open();
        }

        return true;
    }

    public void setPreviewCallBack(Camera.PreviewCallback previewCallback) {
        // todo : when does camera instant?
//        mCamera = camera;
        if (null != mCamera) {
            cameraParameters = mCamera.getParameters();
            mCamera.setPreviewCallback(previewCallback);
        }
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

    public static boolean hasCameraFlash(PackageManager packageManager) {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public static boolean hasFrontCamera(PackageManager packageManager) {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    /// camera & resolution begin
    //分别为 默认摄像头（后置）、默认调用摄像头的分辨率、被选择的摄像头（前置或者后置）
    private int defaultCameraId = -1;
    private int defaultScreenResolution = -1;
    private int cameraSelection = Camera.CameraInfo.CAMERA_FACING_BACK;

    public boolean isFacingFront() {
        return cameraSelection == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    public void swapCamera() {
        cameraSelection = isFacingFront() ? Camera.CameraInfo.CAMERA_FACING_BACK :
                Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    public Camera.Size getPreviewSize() {
        //获取摄像头的所有支持的分辨率
        List<Camera.Size> resolutionList = getResolutionList();
        Camera.Size previewSize = null;

        if (resolutionList != null && resolutionList.size() > 0) {
            Collections.sort(resolutionList, new Util.ResolutionComparator());
            if (defaultScreenResolution == -1) {
                //如果摄像头支持640*480，那么强制设为640*480
                for (int i = 0; i < resolutionList.size(); i++) {
                    Camera.Size size = resolutionList.get(i);
                    if (size != null && size.width == 640 && size.height == 480) {
                        previewSize = size;
                        break;
                    }
                }
                //如果不支持设为中间的那个
                if (null == previewSize) {
                    int mediumResolution = resolutionList.size() / 2;
                    if (mediumResolution >= resolutionList.size()) {
                        mediumResolution = resolutionList.size() - 1;
                    }
                    previewSize = resolutionList.get(mediumResolution);
                    defaultScreenResolution = mediumResolution;
                }
            } else {
                if (defaultScreenResolution >= resolutionList.size()) {
                    defaultScreenResolution = resolutionList.size() - 1;
                }
                previewSize = resolutionList.get(defaultScreenResolution);
            }
        }

        return previewSize;
    }

    /// camera & resolution end
    private boolean isPreviewOn = false;
}
