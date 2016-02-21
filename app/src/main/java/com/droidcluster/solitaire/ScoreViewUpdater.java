package com.droidcluster.solitaire;

import android.view.View;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.Whiteboard.WhiteboardListener;

public class ScoreViewUpdater implements WhiteboardListener {
    private static final long UPDATE_INTERVAL_MS = 500;

    private final MainActivity mainActivity;
    private final View moves;
    private final Runnable updateRunnable;

    private boolean active;

    public ScoreViewUpdater(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        View scoreView = mainActivity.findViewById(R.id.scoreView);
        moves = scoreView.findViewById(R.id.score_moves);

        updateRunnable = new Runnable() {
            public void run() {
                if (!active) {
                    return;
                }
                update();
                moves.postDelayed(this, UPDATE_INTERVAL_MS);
            };
        };

        start();
    }

    private void update() {
        mainActivity.getScoreManager().updateScore();
    }

    private void start() {
        if (active) {
            return;
        }
        active = true;
        moves.post(updateRunnable);
    }

    private void stop() {
        active = false;
    }

    @Override
    public void whiteboardEventReceived(Event event) {
        switch (event) {
        case PAUSED:
            stop();
            break;
        case UNPAUSED:
            start();
            break;
        default:
            update();
        }
    }

}
