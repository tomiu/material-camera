package com.afollestad.materialcamera.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.afollestad.materialcamera.ICallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by tomiurankar on 06/03/16.
 */
public class ImageUtils {
    /**
     * Saves byte[] array to disk
     * @param input byte array
     * @param output path to output file
     * @param callback will always return in originating thread
     */
    public static void saveToDiskAsync(final byte[] input, final File output, final ICallback callback) {
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                try {
                    FileOutputStream outputStream = new FileOutputStream(output);
                    outputStream.write(input);
                    outputStream.flush();
                    outputStream.close();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(null);
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(e);
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * Rotates the bitmap per their EXIF flag.
     *
     * @param inputFile Expects an JPEG file if corrected orientation wants to be set.
     * @return rotated bitmap or null
     */
    @Nullable
    public static Bitmap getRotatedBitmap(String inputFile, int reqWidth, int reqHeight) {
        final int rotationInDegrees = getExifDegreesFromJpeg(inputFile);

        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(inputFile, opts);
        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
        opts.inJustDecodeBounds = false;
        final Bitmap origBitmap = BitmapFactory.decodeFile(inputFile, opts);

        if (origBitmap == null)
            return null;

        Matrix matrix = new Matrix();
        matrix.preRotate(rotationInDegrees);
        // we need not check if the rotation is not needed, since the below function will then return the same bitmap. Thus no memory loss occurs.
        Bitmap rotatedBitmap = Bitmap.createBitmap(origBitmap, 0, 0, origBitmap.getWidth(), origBitmap.getHeight(), matrix, true);

        return rotatedBitmap;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static int getExifDegreesFromJpeg(String inputFile) {
        try {
            ExifInterface exif = new ExifInterface(inputFile);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);


            if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }

        } catch (IOException e) {
            Log.e("exif", "Error when trying to get exif data from : " + inputFile, e);
        }

        return 0;
    }

}
