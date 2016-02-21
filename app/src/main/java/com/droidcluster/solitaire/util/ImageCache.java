package com.droidcluster.solitaire.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;

public class ImageCache {

    private Context context;

    public ImageCache(Context context) {
        this.context = context;
    }

    public void clear(final String prefix, boolean clear) {
        if (!clear) {
            return;
        }

        File[] toDelete = context.getCacheDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(prefix);
            }
        });

        for (File f : toDelete) {
            f.delete();
        }
    }

    /**
     * @return may return <code>null</code>
     */
    public Bitmap getImage(String fileName, int fallbackDrawableId, String cachePrefix, int reqW, int reqH,
            int cornerRadius) {

        File cachedImage = cachedImage(cachePrefix, reqW, reqH);
        if (cachedImage.isFile()) {
            // found in cache
            return ImageLoader.getImageFromFile(cachedImage.getAbsolutePath());
        }

        Bitmap result = null;
        if (fileName != null) {
            if (cornerRadius > 0) {
                result = ImageLoader.getRoundImageFromFile(fileName, reqW, reqH, cornerRadius, cornerRadius);
            } else {
                result = ImageLoader.getImageFromFile(fileName, reqW, reqH);
            }
        }
        if (result == null) {
            if (cornerRadius > 0) {
                result = ImageLoader.getRoundImageFromApp(fallbackDrawableId, reqW, reqH, cornerRadius, cornerRadius,
                        context.getResources());
            } else {
                result = ImageLoader.getBackgroundImageFromApp(fallbackDrawableId, reqW, reqH, context.getResources());
            }
        }
        cacheImage(result, cachedImage);

        return result;
    }

    private File cachedImage(String cachePrefix, int reqW, int reqH) {
        return new File(context.getCacheDir(), cachePrefix + reqW + "x" + reqH + ".png");
    }

    private void cacheImage(Bitmap bitmap, File file) {
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
        } catch (IOException e) {
            // Log.e("solitaire", "Could not cache image to file " + file.getAbsolutePath(), e);
        } finally {
            Util.close(fOut);
        }
    }
}
