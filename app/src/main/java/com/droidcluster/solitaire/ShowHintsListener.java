package com.droidcluster.solitaire;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.Whiteboard.WhiteboardListener;
import com.droidcluster.solitaire.game.JSONStorage;
import com.droidcluster.solitaire.util.TouchHandler2;

public class ShowHintsListener implements WhiteboardListener {
    static final int HINT_AUTOFINISH = 2;
    private static final int HINT_WELCOME = 0;
    private static final int HINT_UNDO = 1;

    private final MainActivity mainActivity;
    private final View hintView;
    private final JSONStorage storage;

    private int moves;

    public ShowHintsListener(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        hintView = mainActivity.findViewById(R.id.hintView);
        storage = mainActivity.getStorage();
    }

    @Override
    public void whiteboardEventReceived(Event event) {
        switch (event) {
        case GAME_STARTED:
            if (!storage.isHintSeen(HINT_WELCOME)) {
                showHintWelcome();
                storage.setHintSeen(HINT_WELCOME);
            }
            Whiteboard.removeListener(this, Event.GAME_STARTED);
            break;

        case OFFERED_AUTOFINISH:
            if (!storage.isHintSeen(HINT_AUTOFINISH)) {
                showHintAutofinish();
                storage.setHintSeen(HINT_AUTOFINISH);
            }
            Whiteboard.removeListener(this, Event.OFFERED_AUTOFINISH);
            break;

        case MOVED:
            if (!storage.isHintSeen(HINT_UNDO)) {
                // show this hint after the third move
                if (moves < 3) {
                    moves++;
                    return;
                } else {
                    showHintUndo();
                    storage.setHintSeen(HINT_UNDO);
                }
            }
            Whiteboard.removeListener(this, Event.MOVED);
            break;
        default:
            break;
        }
    }

    private void showHintUndo() {
        String title = mainActivity.getString(R.string.hint2_title);
        String text = mainActivity.getString(R.string.hint2_text);
        Drawable image = mainActivity.getResources().getDrawable(R.drawable.btn_undo);

        showHint(title, text, image);
    }

    private void showHintAutofinish() {
        String title = mainActivity.getString(R.string.hint1_title);
        String text = mainActivity.getString(R.string.hint1_text);
        Drawable image = mainActivity.getResources().getDrawable(R.drawable.btn_autofinish);

        showHint(title, text, image);
        hintView.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                hideHint();
                mainActivity.getMenuManager().showAutofinishMenu();
            }
        });
    }

    private void showHintWelcome() {
        String title = mainActivity.getString(R.string.hint0_title);
        String text = mainActivity.getString(R.string.hint0_text);
        Drawable image = mainActivity.getResources().getDrawable(R.drawable.hint_touch);

        showHint(title, text, image);
        hintView.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                hideHint();
                mainActivity.getMenuManager().showLeftMenu();
            }
        });
    }

    private void showHint(String title, String text, Drawable image) {
        ((TextView) hintView.findViewById(R.id.hint_title)).setText(title);
        ((TextView) hintView.findViewById(R.id.hint_text)).setText(text);
        ((ImageView) hintView.findViewById(R.id.hint_image)).setImageDrawable(image);
        ObjectAnimator anim = ObjectAnimator.ofFloat(hintView, "alpha", 0, 1);
        anim.setDuration(3 * mainActivity.getAnimationTimeMs());
        hintView.setVisibility(View.VISIBLE);
        hintView.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                hideHint();
            }
        });

        anim.start();
    }

    private void hideHint() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(hintView, "alpha", 1, 0);
        anim.setDuration(3 * mainActivity.getAnimationTimeMs());
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                hintView.post(new Runnable() {
                    @Override
                    public void run() {
                        hintView.setOnTouchListener(null);
                    }
                });
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                hintView.setVisibility(View.GONE);
            }
        });
        anim.start();
    }
}
