package com.droidcluster.solitaire;

import android.content.Context;
import android.content.Intent;

import com.droidcluster.solitaire.game.Settings;
import com.droidcluster.solitaire.util.Util;

public class SettingsManager {

    private final Settings settings = new Settings();
    private final MainActivity mainActivity;
    private boolean settingsInitialized;

    public SettingsManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public Settings getSettings() {
        if (!settingsInitialized) {
            initSettings();
            settingsInitialized = true;
        }
        return settings;
    }

    private void initSettings() {
        Context context = mainActivity;
        settings.leftHand = Util.getPrefBoolean(R.string.pref_left_hand, context);
        settings.drawThree = Util.getPrefBoolean(R.string.pref_draw_three, context);
        if (Util.getPrefBoolean(R.string.pref_card_background_enable, context)) {
            settings.cardBackground = Util.getPrefString(R.string.pref_card_background, context);
        }
        if (Util.getPrefBoolean(R.string.pref_game_background_enable, context)) {
            settings.gameBackground = Util.getPrefString(R.string.pref_game_background, context);
        }
    }

    public void showSettings() {
        mainActivity.startActivity(new Intent(mainActivity, SettingsActivity.class));
    }
}
