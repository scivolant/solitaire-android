package com.droidcluster.solitaire;

import com.droidcluster.solitaire.Whiteboard.Event;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;

public class WelcomeController {
    private final MainActivity mainActivity;

    public WelcomeController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void initComplete() {
        View welcomeView = mainActivity.findViewById(R.id.welcomeView);
        ObjectAnimator appear = ObjectAnimator.ofFloat(welcomeView, "alpha", 1, 0);
        appear.setDuration(mainActivity.getAnimationTimeMs());
        appear.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                Whiteboard.post(Event.GAME_STARTED);
            }
        });
        appear.start();
    }

    public void hideWelcomeView() {
        mainActivity.findViewById(R.id.welcomeView).setVisibility(View.GONE);
    }
}
