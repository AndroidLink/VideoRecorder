package com.qd.recorder.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

/**
 * Created by yangfeng on 14/12/27.
 */
public class BitmapHelper {
    public static void showBitmapBackground(View view, String imgPath, int maxSize) {
        if (!TextUtils.isEmpty(imgPath)) {
            Bitmap bitmap = decodeBitmap(imgPath, maxSize);
            applyViewBackground(view, bitmap);
        }
//        if (null != view && !TextUtils.isEmpty(imgPath)) {
//            Bitmap bitmap = decodeBitmap(imgPath, maxSize);
//            BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
//                view.setBackgroundDrawable(drawable);
//            } else {
//                view.setBackground(drawable);
//            }
//        }
    }

    public static Bitmap decodeBitmap(String imgPath, int maxSize) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options);
        return bitmap;
    }

    public static int calculateInSampleSize(
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

    private static void applyViewBackground(View view, Bitmap bitmap) {
        if (null != view && null != bitmap) {
            BitmapDrawable drawable = new BitmapDrawable(view.getResources(), bitmap);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackgroundDrawable(drawable);
            } else {
                view.setBackground(drawable);
            }
        }
    }

}
