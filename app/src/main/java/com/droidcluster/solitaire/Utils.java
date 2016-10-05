package com.droidcluster.solitaire;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.Context;
import android.preference.PreferenceManager;
import android.view.Window;

import com.droidcluster.solitaire.util.Util;

public class Utils {

    static void updateFullScreen(Window window) {
        Context context = window.getContext();
        boolean fullScreen = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.pref_full_screen), true);
        Util.updateFullScreen(window, fullScreen);
    }

    static void showGameWonStuff(MainActivity mainActivity) {
        AnimatorSet effectGameWon = mainActivity.getMover().showGameWonEffect();
        effectGameWon.setStartDelay(1000);

        Animator anim = mainActivity.getStatsManager().showWinView(effectGameWon, mainActivity.getTable());
        Animator anim1 = mainActivity.getMenuController().showWinMenu();

        AnimatorSet set = new AnimatorSet();
        set.playTogether(anim, anim1);
        set.setDuration(5 * mainActivity.getAnimationTimeMs());
        set.start();
    }

}
