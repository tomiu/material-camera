package com.afollestad.materialcamera.internal;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.afollestad.materialcamera.R;
import com.afollestad.materialcamera.util.CameraUtil;
import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Created by tomiurankar on 04/03/16.
 */
public abstract class BaseGalleryFragment extends Fragment implements CameraUriInterface, View.OnClickListener {
    BaseCaptureInterface mInterface;
    int mPrimaryColor;
    String mOutputUri;
    View mControlsFrame;
    View mRetry;
    View mUseVideo;


    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mInterface = (BaseCaptureInterface) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mOutputUri = getArguments().getString("output_uri");
        mControlsFrame = view.findViewById(R.id.controlsFrame);
        mRetry = view.findViewById(R.id.retry);
        mUseVideo = view.findViewById(R.id.useVideo);

        mPrimaryColor = CameraUtil.darkenColor(getArguments().getInt(CameraIntentKey.PRIMARY_COLOR));
        int primaryColor = mPrimaryColor;
        primaryColor = Color.argb((int) (255 * 0.75f), Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor));
        mControlsFrame.setBackgroundColor(primaryColor);

        mRetry.setVisibility(getArguments().getBoolean(CameraIntentKey.ALLOW_RETRY, true) ? View.VISIBLE : View.GONE);

    }

    @Override
    public String getOutputUri() {
        return getArguments().getString("output_uri");
    }

    void showDialog( String title, String errorMsg) {
        new MaterialDialog.Builder(getActivity())
                .title(title)
                .content(errorMsg)
                .positiveText(android.R.string.ok)
                .show();
    }

}
