package com.afollestad.materialcamera.internal;

import android.support.annotation.Nullable;

/**
 * @author Aidan Follestad (afollestad)
 */
public interface BaseCaptureInterface {

    void onRetry(@Nullable String outputUri);

    void onShowPreview(@Nullable String outputUri, boolean countdownIsAtZero);

    void onShowStillshot(String outputUri);

    void setRecordingStart(long start);

    void setRecordingEnd(long end);

    long getRecordingStart();

    long getRecordingEnd();

    boolean hasLengthLimit();

    boolean countdownImmediately();

    long getLengthLimit();

    void setCameraPosition(int position);

    void toggleCameraPosition();

    Object getCurrentCameraId();

    @BaseCaptureActivity.CameraPosition
    int getCurrentCameraPosition();

    void setFrontCamera(Object id);

    void setBackCamera(Object id);

    Object getFrontCamera();

    Object getBackCamera();

    void useVideo(String uri);

    boolean shouldAutoSubmit();

    boolean allowRetry();

    void setDidRecord(boolean didRecord);

    boolean didRecord();

    boolean restartTimerOnRetry();

    boolean continueTimerInPlayback();

    int videoBitRate();

    int videoFrameRate();

    int videoPreferredHeight();

    float videoPreferredAspect();

    /**
     *
     * @return true if we only want to take photographs instead of video capture
     */
    boolean useStillshot();

    void toggleFlashMode();

    @BaseCaptureActivity.FlashMode
    int getFlashMode();
}
