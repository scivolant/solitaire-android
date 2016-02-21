package com.droidcluster.solitaire;

import im.delight.apprater.AppRater;

import android.os.Handler;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.Whiteboard.WhiteboardListener;

public class UpdateStatsListener implements WhiteboardListener {

    private final MainActivity mainActivity;
    private final Handler handler;
    private final Runnable rateRunnable;

    public UpdateStatsListener(final MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        handler = new Handler(mainActivity.getMainLooper());
        rateRunnable = new Runnable() {
            private AppRater appRater;

            @Override
            public void run() {
                if (appRater == null) {
                    appRater = new AppRater(mainActivity);
                    appRater.setDaysBeforePrompt(0);
                    appRater.setLaunchesBeforePrompt(20);
                    appRater.setPhrases(R.string.app_rater_title, R.string.app_rater_explanation,
                            R.string.app_rater_button_now, R.string.app_rater_button_later,
                            R.string.app_rater_button_never);
                }
                appRater.show();
            }
        };
    }

    @Override
    public void whiteboardEventReceived(Event event) {
        mainActivity.getStatsManager().storeGameStats();
        if (event == Event.WON) {
            handler.postDelayed(rateRunnable, 1000);
        }
    }

}
