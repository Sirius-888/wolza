package com.wolza.arduinoapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    /**
     * Resizes and rotates a bitmap from a Uri safely to prevent OOM and orientation issues.
     */
    public static Bitmap handleImageUri(Context context, Uri uri, int maxSize) {
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            int inSampleSize = 1;
            while ((options.outWidth / inSampleSize) > maxSize || (options.outHeight / inSampleSize) > maxSize) {
                inSampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            is = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();

            if (bitmap == null) return null;

            return rotateImageIfRequired(context, bitmap, uri);

        } catch (Exception e) {
            Log.e(TAG, "Error handling image URI: " + e.getMessage());
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        if (input == null) return img;
        
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23) {
            ei = new ExifInterface(input);
        } else {
            ei = new ExifInterface(selectedImage.getPath());
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        input.close();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180: return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270: return rotateImage(img, 270);
            default: return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        if (rotatedImg != img) {
            img.recycle();
        }
        return rotatedImg;
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    public static String bitmapToBase64(Bitmap bitmap, int quality) {
        byte[] bytes = bitmapToByteArray(bitmap, quality);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
