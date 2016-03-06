package com.afollestad.materialcamera;

/**
 * Created by tomiurankar on 04/03/16.
 */
public interface ICallback {
    /**
     * It is called when the background operation completes.
     * If the operation is successful, {@code exception} will be {@code null}.
     */
    void done(Exception exception);
}
