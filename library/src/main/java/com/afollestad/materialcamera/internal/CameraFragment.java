package com.afollestad.materialcamera.internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialcamera.ICallback;
import com.afollestad.materialcamera.R;
import com.afollestad.materialcamera.util.CameraUtil;
import com.afollestad.materialcamera.util.Degrees;
import com.afollestad.materialcamera.util.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.afollestad.materialcamera.internal.BaseCaptureActivity.CAMERA_POSITION_BACK;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.CAMERA_POSITION_FRONT;
import static com.afollestad.materialcamera.internal.BaseCaptureActivity.CAMERA_POSITION_UNKNOWN;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class CameraFragment extends BaseCameraFragment implements View.OnClickListener {

    CameraPreview mPreviewView;
    RelativeLayout mPreviewFrame;

    private Camera.Size mVideoSize;
    private Camera mCamera;
    private Point mWindowSize;
    private int mDisplayOrientation;

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    private static Camera.Size chooseVideoSize(BaseCaptureInterface ci, List<Camera.Size> choices) {
        Camera.Size backupSize = null;
        for (Camera.Size size : choices) {
            if (size.height <= ci.videoPreferredHeight()) {
                if (size.width == size.height * ci.videoPreferredAspect())
                    return size;
                if (ci.videoPreferredHeight() >= size.height)
                    backupSize = size;
            }
        }
        if (backupSize != null) return backupSize;
        LOG(CameraFragment.class, "Couldn't find any suitable video size");
        return choices.get(choices.size() - 1);
    }

    private static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int width, int height, Camera.Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.width;
        int h = aspectRatio.height;
        for (Camera.Size option : choices) {
            if (option.height == width * h / w &&
                    option.width >= width && option.height >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            LOG(CameraFragment.class, "Couldn't find any suitable preview size");
            return choices.get(0);
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreviewFrame = (RelativeLayout) view.findViewById(R.id.rootFrame);
        mPreviewFrame.setOnClickListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPreviewFrame = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    public void onPause() {
        if (mCamera != null) mCamera.lock();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.rootFrame) {
            if (mCamera == null) return;
            mCamera.cancelAutoFocus();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (!success)
                        Toast.makeText(getActivity(), "Unable to auto-focus!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            super.onClick(view);
        }
    }

    @Override
    public void openCamera() {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) return;
        try {
            final int mBackCameraId = mInterface.getBackCamera() != null ? (Integer) mInterface.getBackCamera() : -1;
            final int mFrontCameraId = mInterface.getFrontCamera() != null ? (Integer) mInterface.getFrontCamera() : -1;
            if (mBackCameraId == -1 || mFrontCameraId == -1) {
                int numberOfCameras = Camera.getNumberOfCameras();
                if (numberOfCameras == 0) {
                    throwError(new Exception("No cameras are available on this device."));
                    return;
                }

                for (int i = 0; i < numberOfCameras; i++) {
                    //noinspection ConstantConditions
                    if (mFrontCameraId != -1 && mBackCameraId != -1) break;
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mFrontCameraId == -1) {
                        mInterface.setFrontCamera(i);
                    } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK && mBackCameraId == -1) {
                        mInterface.setBackCamera(i);
                    }
                }
            }
            if (getCurrentCameraPosition() == CAMERA_POSITION_UNKNOWN) {
                if (getArguments().getBoolean(CameraIntentKey.DEFAULT_TO_FRONT_FACING, false)) {
                    // Check front facing first
                    if (mInterface.getFrontCamera() != null && (Integer) mInterface.getFrontCamera() != -1) {
                        mButtonFacing.setImageDrawable(VC.get(this, R.drawable.mcam_camera_rear));
                        mInterface.setCameraPosition(CAMERA_POSITION_FRONT);
                    } else {
                        mButtonFacing.setImageDrawable(VC.get(this, R.drawable.mcam_camera_front));
                        if (mInterface.getBackCamera() != null && (Integer) mInterface.getBackCamera() != -1)
                            mInterface.setCameraPosition(CAMERA_POSITION_BACK);
                        else mInterface.setCameraPosition(CAMERA_POSITION_UNKNOWN);
                    }
                } else {
                    // Check back facing first
                    if (mInterface.getBackCamera() != null && (Integer) mInterface.getBackCamera() != -1) {
                        mButtonFacing.setImageDrawable(VC.get(this, R.drawable.mcam_camera_front));
                        mInterface.setCameraPosition(CAMERA_POSITION_BACK);
                    } else {
                        mButtonFacing.setImageDrawable(VC.get(this, R.drawable.mcam_camera_rear));
                        if (mInterface.getFrontCamera() != null && (Integer) mInterface.getFrontCamera() != -1)
                            mInterface.setCameraPosition(CAMERA_POSITION_FRONT);
                        else mInterface.setCameraPosition(CAMERA_POSITION_UNKNOWN);
                    }
                }
            }

            if (mWindowSize == null)
                mWindowSize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(mWindowSize);
            final int toOpen = getCurrentCameraId();
            mCamera = Camera.open(toOpen == -1 ? 0 : toOpen);
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> videoSizes = parameters.getSupportedVideoSizes();
            if (videoSizes == null || videoSizes.size() == 0)
                videoSizes = parameters.getSupportedPreviewSizes();
            mVideoSize = chooseVideoSize((BaseCaptureActivity) activity, videoSizes);
            Camera.Size previewSize = chooseOptimalSize(parameters.getSupportedPreviewSizes(),
                    mWindowSize.x, mWindowSize.y, mVideoSize);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                parameters.setRecordingHint(true);
            Camera.Size mStillShotSize = getHighestSupportedStillShotSize(parameters.getSupportedPictureSizes());
            parameters.setPictureSize(mStillShotSize.width, mStillShotSize.height);
            setCameraDisplayOrientation(parameters);
            mCamera.setParameters(parameters);
            createPreview();
            mMediaRecorder = new MediaRecorder();
        } catch (IllegalStateException e) {
            throwError(new Exception("Cannot access the camera.", e));
        } catch (RuntimeException e2) {
            throwError(new Exception("Cannot access the camera, you may need to restart your device.", e2));
        }
    }

    private Camera.Size getHighestSupportedStillShotSize(List<Camera.Size> supportedPictureSizes) {
        Camera.Size maxSize = supportedPictureSizes.get(supportedPictureSizes.size() - 1); //TODO: find the biggest resolution
        return maxSize;
    }

    @SuppressWarnings("WrongConstant")
    private void setCameraDisplayOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(getCurrentCameraId(), info);
        final int deviceOrientation = Degrees.getDisplayRotation(getActivity());
        mDisplayOrientation = Degrees.getDisplayOrientation(
                info.orientation, deviceOrientation, info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
        Log.d("CameraFragment", String.format("Orientations: Sensor = %d˚, Device = %d˚, Display = %d˚",
                info.orientation, deviceOrientation, mDisplayOrientation));

        int previewOrientation;
        int jpegOrientation;
        if (CameraUtil.isArcWelder()) {
            previewOrientation = 0;
            jpegOrientation = 0;
        } else {
            jpegOrientation = previewOrientation = mDisplayOrientation;

            if (Degrees.isPortrait(deviceOrientation) && getCurrentCameraPosition() == CAMERA_POSITION_FRONT)
                previewOrientation = Degrees.mirror(mDisplayOrientation);
        }

        parameters.setRotation(jpegOrientation);
        mCamera.setDisplayOrientation(previewOrientation);
    }

    private void createPreview() {
        Activity activity = getActivity();
        if (activity == null) return;
        if (mWindowSize == null)
            mWindowSize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(mWindowSize);
        mPreviewView = new CameraPreview(getActivity(), mCamera);
        if (mPreviewFrame.getChildCount() > 0 && mPreviewFrame.getChildAt(0) instanceof CameraPreview)
            mPreviewFrame.removeViewAt(0);
        mPreviewFrame.addView(mPreviewView, 0);
        mPreviewView.setAspectRatio(mWindowSize.x, mWindowSize.y);
    }

    @Override
    public void closeCamera() {
        try {
            if (mCamera != null) {
                try {
                    mCamera.lock();
                } catch (Throwable ignored) {
                }
                mCamera.release();
                mCamera = null;
            }
        } catch (IllegalStateException e) {
            throwError(new Exception("Illegal state while trying to close camera.", e));
        }
    }

    private boolean prepareMediaRecorder() {
        return prepareMediaRecorder(CamcorderProfile.QUALITY_480P);
    }

    private boolean prepareMediaRecorder(int forceQuality) {
        try {
            final Activity activity = getActivity();
            if (null == activity) return false;
            final BaseCaptureInterface captureInterface = (BaseCaptureInterface) activity;

            setCameraDisplayOrientation(mCamera.getParameters());
            mMediaRecorder = new MediaRecorder();
            mCamera.stopPreview();
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);

            Log.d("Camera1Fragment", String.format(
                    "Bit rate: %d, Frame rate: %d, Resolution: %s",
                    captureInterface.videoBitRate(), captureInterface.videoFrameRate(),
                    String.format(Locale.getDefault(), "%dx%d", mVideoSize.width, mVideoSize.height)));

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
            mMediaRecorder.setProfile(CamcorderProfile.get(getCurrentCameraId(), forceQuality));
            mMediaRecorder.setVideoSize(mVideoSize.width, mVideoSize.height);
            if (captureInterface.videoFrameRate() > 0)
                mMediaRecorder.setVideoFrameRate(captureInterface.videoFrameRate());
            if (captureInterface.videoBitRate() > 0)
                mMediaRecorder.setVideoEncodingBitRate(captureInterface.videoBitRate());

            Uri uri = Uri.fromFile(getOutputMediaFile());
            mOutputUri = uri.toString();
            mMediaRecorder.setOutputFile(uri.getPath());

            mMediaRecorder.setOrientationHint(mDisplayOrientation);
            mMediaRecorder.setPreviewDisplay(mPreviewView.getHolder().getSurface());

            try {
                mMediaRecorder.prepare();
                return true;
            } catch (Throwable e) {
                throwError(new Exception("Failed to prepare the media recorder: " + e.getMessage(), e));
                return false;
            }
        } catch (Throwable t) {
            try {
                mCamera.lock();
            } catch (IllegalStateException e) {
                throwError(new Exception("Failed to re-lock camera: " + e.getMessage(), e));
                return false;
            }
            if (forceQuality == CamcorderProfile.QUALITY_480P)
                return prepareMediaRecorder(CamcorderProfile.QUALITY_720P);
            else if (forceQuality == CamcorderProfile.QUALITY_720P)
                return prepareMediaRecorder(CamcorderProfile.QUALITY_LOW);
            else if (forceQuality == CamcorderProfile.QUALITY_LOW) {
                return prepareMediaRecorder(CamcorderProfile.QUALITY_1080P);
            }
            throwError(new Exception("Failed to begin recording: " + t.getMessage(), t));
            return false;
        }
    }

    @Override
    public boolean startRecordingVideo() {
        super.startRecordingVideo();
        if (prepareMediaRecorder()) {
            try {
                // UI
                mButtonVideo.setImageDrawable(VC.get(this, R.drawable.mcam_action_stop));
                if (!CameraUtil.isArcWelder())
                    mButtonFacing.setVisibility(View.GONE);

                // Only start counter if count down wasn't already started
                if (!mInterface.hasLengthLimit()) {
                    mInterface.setRecordingStart(System.currentTimeMillis());
                    startCounter();
                }

                // Start recording
                mMediaRecorder.start();

                mButtonVideo.setEnabled(false);
                mButtonVideo.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mButtonVideo.setEnabled(true);
                    }
                }, 200);

                return true;
            } catch (Throwable t) {
                t.printStackTrace();
                mInterface.setRecordingStart(-1);
                stopRecordingVideo(false);
                throwError(new Exception("Failed to start recording: " + t.getMessage(), t));
            }
        }
        return false;
    }

    @Override
    public void stopRecordingVideo(final boolean reachedZero) {
        super.stopRecordingVideo(reachedZero);

        if (mInterface.hasLengthLimit() && mInterface.shouldAutoSubmit() &&
                (mInterface.getRecordingStart() < 0 || mMediaRecorder == null)) {
            stopCounter();
            if (mCamera != null) {
                try {
                    mCamera.lock();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            releaseRecorder();
            closeCamera();
            mButtonFacing.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mInterface.onShowPreview(mOutputUri, reachedZero);
                }
            }, 100);
            return;
        }

        if (mCamera != null)
            mCamera.lock();
        releaseRecorder();
        closeCamera();

        if (!mInterface.didRecord())
            mOutputUri = null;

        mButtonVideo.setImageDrawable(VC.get(this, R.drawable.mcam_action_capture));
        if (!CameraUtil.isArcWelder())
            mButtonFacing.setVisibility(View.VISIBLE);
        if (mInterface.getRecordingStart() > -1 && getActivity() != null)
            mInterface.onShowPreview(mOutputUri, reachedZero);

        stopCounter();
    }

    @Override
    public void takeStillshot() {
        super.takeStillshot();
        //TODO: add flash light

        //https://github.com/josnidhin/Android-Camera-Example/blob/master/src/com/example/cam/CamTestActivity.java
        final String TAG = "takeStillShot";

        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
//                			 Log.d(TAG, "onShutter'd");
            }
        };

        Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
//                			 Log.d(TAG, "onPictureTaken - raw. Raw is null: " + (data == null));
            }
        };

        Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(final byte[] data, Camera camera) {
//                Log.d(TAG, "onPictureTaken - jpeg, size: " + data.length);

                final File outputPic = getOutputPictureFile();

                // lets save the image to disk
                ImageUtils.saveToDiskAsync(data, outputPic, new ICallback() {
                    @Override
                    public void done(Exception e) {
                        if (e == null) {
                            Log.d(TAG, "picture saved to disk - jpeg, size: " + data.length);
                            mOutputUri = Uri.fromFile(outputPic).toString();
                            mInterface.onShowStillshot(mOutputUri);

//                            mCamera.startPreview();
                            mButtonStillshot.setEnabled(true);
                        } else {
                            throwError(e);
                        }
                    }
                });
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //We could have configurable shutter sound here

            //mCamera.enableShutterSound(false);
        }
        
        mButtonStillshot.setEnabled(false);
        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    static class CompareSizesByArea implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.width * lhs.height -
                    (long) rhs.width * rhs.height);
        }
    }
}