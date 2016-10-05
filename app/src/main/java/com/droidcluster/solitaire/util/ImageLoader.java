package com.droidcluster.solitaire.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;

public class ImageLoader {

    /**
     * @return may return <code>null</code>
     */
    public static Bitmap getImageFromFile(String filename) {
        return BitmapFactory.decodeFile(filename);
    }

    public static Bitmap scaleImage(Object image, int newWidth, int newHeight) {
        return Bitmap.createScaledBitmap((Bitmap) image, newWidth, newHeight, false);
    }

    /**
     * @return may return <code>null</code>
     */
    public static Bitmap getImageFromFile(String fileName, int reqWidth, int reqHeight) {
        if (fileName == null) {
            return null;
        }

        FileInputStream is = null;
        try {
            is = new FileInputStream(fileName);
            return streamToBackgroundImage(is, reqWidth, reqHeight);
        } catch (IOException e) {
            return null;
        } finally {
            Util.close(is);
        }
    }

    /**
     * @return may return <code>null</code>
     */
    public static Bitmap getBackgroundImageFromApp(int drawableId, int reqWidth, int reqHeight, Resources resources) {
        InputStream is = null;
        try {
            is = resources.openRawResource(drawableId);
            return streamToBackgroundImage(is, reqWidth, reqHeight);
        } catch (IOException e) {
            return null;
        } finally {
            Util.close(is);
        }
    }

    /**
     * @return may return <code>null</code>
     */
    public static Bitmap getImageFromApp(int resId, int reqWidth, int reqHeight, Resources res) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPurgeable = true;
        o.inInputShareable = true;
        o.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(res, resId, o);
        o.inSampleSize = calculateInSampleSize(o.outWidth, o.outHeight, reqWidth, reqHeight);
        o.inJustDecodeBounds = false;

        Bitmap b = BitmapFactory.decodeResource(res, resId, o);
        if (b == null) {
            return null;
        }

        if (reqWidth == -1) {
            reqWidth = (int) (((float) reqHeight / b.getHeight()) * b.getWidth());
        } else if (reqHeight == -1) {
            reqHeight = (int) (((float) reqWidth / b.getWidth()) * b.getHeight());
        }

        Bitmap result = Bitmap.createScaledBitmap(b, reqWidth, reqHeight, false);
        if(result != b) {
            recycleChecked(b);
        }
        return result;
    }

    private static Bitmap streamToBackgroundImage(InputStream is, int reqWidth, int reqHeight) throws IOException {
        if (reqWidth <= 0 || reqHeight <= 0) {
            throw new IllegalArgumentException("reqWidth and reqHeight must be > 0");
        }
        BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, true);
        if (decoder == null) {
            return null;
        }

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPurgeable = true;
        o.inInputShareable = true;
        o.inSampleSize = calculateInSampleSize(decoder.getWidth(), decoder.getHeight(), reqWidth, reqHeight);

        float q = Math.min(decoder.getWidth() / o.inSampleSize / (float) reqWidth, decoder.getHeight() / o.inSampleSize
                / (float) reqHeight);

        int qWidth = (int) (reqWidth * q);
        int qHeight = (int) (reqHeight * q);

        int left = (decoder.getWidth() / o.inSampleSize - qWidth) / 2;
        int top = (decoder.getHeight() / o.inSampleSize - qHeight) / 2;
        Rect rect = new Rect(left, top, left + o.inSampleSize * qWidth, top + o.inSampleSize * qHeight);
        Bitmap decodedRegion = decoder.decodeRegion(rect, o);

        if (decodedRegion != null) {
            Bitmap result = Bitmap.createScaledBitmap(decodedRegion, reqWidth, reqHeight, false);
            recycleChecked(decodedRegion);
            decoder.recycle();
            return result;
        }

        return null;
    }

    /**
     * @return may return <code>null</code>
     */
    public static Bitmap getRoundImageFromFile(String filename, int rx, int ry) {
        return getRoundedCornerBitmap(getImageFromFile(filename), rx, ry);
    }

    /**
     * @return may return <code>null</code>
     */
    public static Bitmap getRoundImageFromFile(String filename, int reqWidth, int reqHeight, int rx, int ry) {
        return getRoundedCornerBitmap(getImageFromFile(filename, reqWidth, reqHeight), rx, ry);
    }

    /**
     * @return may return <code>null</code>
     */
    public static Bitmap getRoundImageFromApp(int drawableId, int reqWidth, int reqHeight, int rx, int ry,
            Resources resources) {
        return getRoundedCornerBitmap(getBackgroundImageFromApp(drawableId, reqWidth, reqHeight, resources), rx, ry);
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        // Raw height and width of image
        int inSampleSize = 1;

        if (reqHeight != -1 && height > reqHeight || reqWidth != -1 && width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((reqHeight == -1 || (halfHeight / inSampleSize) > reqHeight)
                    && (reqWidth == -1 || (halfWidth / inSampleSize) > reqWidth)) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float rx, float ry) {
        if (rx <= 0 || ry <= 0 || bitmap == null) {
            return bitmap;
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, rx, ry, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        recycleChecked(bitmap);

        return output;
    }

    public static void recycleChecked(Bitmap bitmap) {
        if(bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}