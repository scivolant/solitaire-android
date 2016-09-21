package com.droidcluster.solitaire;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.droidcluster.solitaire.game.Layout;
import com.droidcluster.solitaire.game.Settings;
import com.droidcluster.solitaire.util.ImageCache;
import com.droidcluster.solitaire.util.ImageLoader;

public class LoadImagesTask extends AsyncTask<Boolean, String, String> {
    private static final String GAME_BG_PREFIX = "game_bg_";
    private static final String CARD_BG_PREFIX = "card_bg_";

    private final Layout layout;
    private final Settings settings;
    private final ImageCache cache;

    public LoadImagesTask(Context context, Layout layout, Settings settings) {
        this.layout = layout;
        this.settings = settings;
        cache = new ImageCache(context);
    }

    @Override
    protected String doInBackground(Boolean... params) {
        cache.clear(GAME_BG_PREFIX, params[0]);
        cache.clear(CARD_BG_PREFIX, params[1]);

        int cornerRadius = layout.cardRadius.x;

        // game background
        Bitmap trash = layout.gameBackground;
        layout.gameBackground = cache.getImage(settings.gameBackground, R.drawable.gamebg, GAME_BG_PREFIX,
                layout.availableSize.x, layout.availableSize.y, cornerRadius);
        ImageLoader.recycle(trash);

        // card background
        trash = layout.cardBackground;
        layout.cardBackground = cache.getImage(settings.cardBackground, R.drawable.cardbg, CARD_BG_PREFIX,
                layout.cardSize.x, layout.cardSize.y, cornerRadius);
        ImageLoader.recycle(trash);

        return "ok";
    }

}
