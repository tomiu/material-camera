package com.afollestad.materialcamera;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

import com.afollestad.materialcamera.internal.CameraIntentKey;
import com.afollestad.materialcamera.util.CameraUtil;
import com.afollestad.materialdialogs.util.DialogUtils;

import java.io.File;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MaterialCamera {

    public static final String ERROR_EXTRA = "mcam_error";
    public static final String STATUS_EXTRA = "mcam_status";

    public static final int STATUS_RECORDED = 1;
    public static final int STATUS_RETRY = 2;

    private Activity mContext;
    private long mLengthLimit = -1;
    private boolean mAllowRetry = true;
    private boolean mAutoSubmit = false;
    private String mSaveDir;
    private int mPrimaryColor;
    private boolean mShowPortraitWarning = true;
    private boolean mDefaultToFrontFacing = false;
    private boolean mCountdownImmediately = false;
    private boolean mRetryExists = false;
    private boolean mRestartTimerOnRetry = false;
    private boolean mContinueTimerInPlayback = true;
    private boolean mForceCamera1 = false;
    private boolean mStillShot;


    private int mVideoBitRate = -1;
    private int mVideoFrameRate = -1;
    private int mVideoPreferredHeight = -1;
    private float mVideoPreferredAspect = -1f;

    public MaterialCamera(@NonNull Activity context) {
        mContext = context;
        mPrimaryColor = DialogUtils.resolveColor(context, R.attr.colorPrimary);
    }

    public MaterialCamera countdownMillis(long lengthLimitMs) {
        mLengthLimit = lengthLimitMs;
        return this;
    }

    /**
     * @deprecated use {@link #countdownMillis(long)} instead.
     */
    @Deprecated
    public MaterialCamera lengthLimitMillis(long lengthLimitMs) {
        mLengthLimit = lengthLimitMs;
        return this;
    }

    public MaterialCamera countdownSeconds(float lengthLimitSec) {
        return countdownMillis((int) (lengthLimitSec * 1000f));
    }

    /**
     * @deprecated use {@link #countdownSeconds(float)} instead.
     */
    @Deprecated
    public MaterialCamera lengthLimitSeconds(float lengthLimitSec) {
        return countdownMillis((int) (lengthLimitSec * 1000f));
    }

    public MaterialCamera countdownMinutes(float lengthLimitMin) {
        return countdownMillis((int) (lengthLimitMin * 1000f * 60f));
    }

    /**
     * @deprecated use {@link #countdownMinutes(float)} instead.
     */
    @Deprecated
    public MaterialCamera lengthLimitMinutes(float lengthLimitMin) {
        return countdownMillis((int) (lengthLimitMin * 1000f * 60f));
    }

    public MaterialCamera allowRetry(boolean allowRetry) {
        mAllowRetry = allowRetry;
        return this;
    }

    public MaterialCamera autoSubmit(boolean autoSubmit) {
        mAutoSubmit = autoSubmit;
        return this;
    }

    public MaterialCamera countdownImmediately(boolean immediately) {
        mCountdownImmediately = immediately;
        return this;
    }

    public MaterialCamera saveDir(@Nullable File dir) {
        if (dir == null) return saveDir((String) null);
        return saveDir(dir.getAbsolutePath());
    }

    public MaterialCamera saveDir(@Nullable String dir) {
        mSaveDir = dir;
        return this;
    }

    public MaterialCamera primaryColor(@ColorInt int color) {
        mPrimaryColor = color;
        return this;
    }

    public MaterialCamera primaryColorRes(@ColorRes int colorRes) {
        return primaryColor(ContextCompat.getColor(mContext, colorRes));
    }

    public MaterialCamera primaryColorAttr(@AttrRes int colorAttr) {
        return primaryColor(DialogUtils.resolveColor(mContext, colorAttr));
    }

    public MaterialCamera showPortraitWarning(boolean show) {
        mShowPortraitWarning = show;
        return this;
    }

    public MaterialCamera defaultToFrontFacing(boolean frontFacing) {
        mDefaultToFrontFacing = frontFacing;
        return this;
    }

    public MaterialCamera retryExits(boolean exits) {
        mRetryExists = exits;
        return this;
    }

    public MaterialCamera restartTimerOnRetry(boolean restart) {
        mRestartTimerOnRetry = restart;
        return this;
    }

    public MaterialCamera continueTimerInPlayback(boolean continueTimer) {
        mContinueTimerInPlayback = continueTimer;
        return this;
    }

    public MaterialCamera forceCamera1() {
        mForceCamera1 = true;
        return this;
    }

    public MaterialCamera videoBitRate(@IntRange(from = 1, to = Integer.MAX_VALUE) int rate) {
        mVideoBitRate = rate;
        return this;
    }

    public MaterialCamera videoFrameRate(@IntRange(from = 1, to = Integer.MAX_VALUE) int rate) {
        mVideoFrameRate = rate;
        return this;
    }

    public MaterialCamera videoPreferredHeight(@IntRange(from = 1, to = Integer.MAX_VALUE) int height) {
        mVideoPreferredHeight = height;
        return this;
    }

    public MaterialCamera videoPreferredAspect(@FloatRange(from = 0.1, to = Float.MAX_VALUE) float ratio) {
        mVideoPreferredAspect = ratio;
        return this;
    }

    /**
     * Will take a still shot instead of recording
     * Note: Current implementation will default to using Camera1 API.
     * Also the library owner has chosen to disregard the Camera2 API regardless of settings, so
     * this is a non issue anyway.
     * @return
     */
    public MaterialCamera stillShot() {
        mStillShot = true;
        return this;
    }


    public Intent getIntent() {
        final Class<?> cls = !mStillShot && !mForceCamera1 && CameraUtil.hasCamera2(mContext) ?
                CaptureActivity2.class : CaptureActivity.class;
        Intent intent = new Intent(mContext, cls)
                .putExtra(CameraIntentKey.LENGTH_LIMIT, mLengthLimit)
                .putExtra(CameraIntentKey.ALLOW_RETRY, mAllowRetry)
                .putExtra(CameraIntentKey.AUTO_SUBMIT, mAutoSubmit)
                .putExtra(CameraIntentKey.SAVE_DIR, mSaveDir)
                .putExtra(CameraIntentKey.PRIMARY_COLOR, mPrimaryColor)
                .putExtra(CameraIntentKey.SHOW_PORTRAIT_WARNING, mShowPortraitWarning)
                .putExtra(CameraIntentKey.DEFAULT_TO_FRONT_FACING, mDefaultToFrontFacing)
                .putExtra(CameraIntentKey.COUNTDOWN_IMMEDIATELY, mCountdownImmediately)
                .putExtra(CameraIntentKey.RETRY_EXITS, mRetryExists)
                .putExtra(CameraIntentKey.RESTART_TIMER_ON_RETRY, mRestartTimerOnRetry)
                .putExtra(CameraIntentKey.CONTINUE_TIMER_IN_PLAYBACK, mContinueTimerInPlayback)
                .putExtra(CameraIntentKey.STILL_SHOT, mStillShot);

        if (mVideoBitRate > 0)
            intent.putExtra(CameraIntentKey.VIDEO_BIT_RATE, mVideoBitRate);
        if (mVideoFrameRate > 0)
            intent.putExtra(CameraIntentKey.VIDEO_FRAME_RATE, mVideoFrameRate);
        if (mVideoPreferredHeight > 0)
            intent.putExtra(CameraIntentKey.VIDEO_PREFERRED_HEIGHT, mVideoPreferredHeight);
        if (mVideoPreferredAspect > 0f)
            intent.putExtra(CameraIntentKey.VIDEO_PREFERRED_ASPECT, mVideoPreferredAspect);
        return intent;
    }

    public void start(int requestCode) {
        mContext.startActivityForResult(getIntent(), requestCode);
    }
}