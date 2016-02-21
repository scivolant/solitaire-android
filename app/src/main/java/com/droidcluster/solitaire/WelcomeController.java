package com.droidcluster.solitaire;

import com.droidcluster.solitaire.Whiteboard.Event;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public class WelcomeController {
    private final MainActivity mainActivity;
    private final View welcomeView;
    private final View logo;
    private final View title;
    private boolean initCompleted;
    private boolean welcomeCompleted;

    public WelcomeController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        welcomeView = mainActivity.findViewById(R.id.welcomeView);
        logo = welcomeView.findViewById(R.id.welcomeLogo);
        title = welcomeView.findViewById(R.id.welcomeTitle);
    }

    public Animator getWelcomeAnimation() {
        int startY = (welcomeView.getHeight() - logo.getHeight()) / 2;
        float endY = startY - 1.25f * logo.getHeight();
        ObjectAnimator logoAnim = ObjectAnimator.ofFloat(logo, "translationY", 0, endY - startY);
        logoAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        logoAnim.setDuration(500);
        logoAnim.setStartDelay(250);

        ObjectAnimator titleAnim = ObjectAnimator.ofFloat(title, "alpha", 0, 1);
        titleAnim.setInterpolator(new DecelerateInterpolator());
        titleAnim.setDuration(250);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(logoAnim, titleAnim);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                welcomeCompleted = true;
                checkHide();
            }
        });

        return set;
    }

    public void initComplete() {
        initCompleted = true;
        checkHide();
    }

    private void checkHide() {
        if (initCompleted && welcomeCompleted) {
            ObjectAnimator appear = ObjectAnimator.ofFloat(welcomeView, "alpha", 1, 0);
            appear.setDuration(mainActivity.getAnimationTimeMs());
            appear.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    Whiteboard.post(Event.GAME_STARTED);
                }
            });
            appear.start();
        }
    }
}
