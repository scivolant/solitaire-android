package com.droidcluster.solitaire;

import java.util.Arrays;
import java.util.List;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.game.Settings;
import com.droidcluster.solitaire.util.Util;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

public class PrefChangeToWhiteboardForwarder implements OnSharedPreferenceChangeListener {
    private MainActivity mainActivity;

    private final List<String> cardBgPrefs;
    private final List<String> gameBgPrefs;

    public PrefChangeToWhiteboardForwarder(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        cardBgPrefs = Arrays.asList(mainActivity.getString(R.string.pref_card_background),
                mainActivity.getString(R.string.pref_card_background_enable));
        gameBgPrefs = Arrays.asList(mainActivity.getString(R.string.pref_game_background),
                mainActivity.getString(R.string.pref_game_background_enable));

        PreferenceManager.getDefaultSharedPreferences(mainActivity).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        Settings settings = mainActivity.getSettingsManager().getSettings();

        if (cardBgPrefs.contains(key)) {
            if (Util.getPrefBoolean(R.string.pref_card_background_enable, mainActivity)) {
                settings.cardBackground = Util.getPrefString(R.string.pref_card_background, mainActivity);
            } else {
                settings.cardBackground = null;
            }
            Whiteboard.post(Event.CARD_BG_SET);
        }

        if (gameBgPrefs.contains(key)) {
            if (Util.getPrefBoolean(R.string.pref_game_background_enable, mainActivity)) {
                settings.gameBackground = Util.getPrefString(R.string.pref_game_background, mainActivity);
            } else {
                settings.gameBackground = null;
            }
            Whiteboard.post(Event.GAME_BG_SET);
        }

        if (mainActivity.getString(R.string.pref_left_hand).equals(key)) {
            settings.leftHand = prefs.getBoolean(key, false);
            Whiteboard.post(Event.LEFT_HAND_SET);
        }

        if (mainActivity.getString(R.string.pref_draw_three).equals(key)) {
            settings.drawThree = prefs.getBoolean(key, false);
            Whiteboard.post(Event.DRAW_THREE_SET);
        }
    }

    public void destroy() {
        PreferenceManager.getDefaultSharedPreferences(mainActivity).unregisterOnSharedPreferenceChangeListener(this);
    }

}
