package com.droidcluster.solitaire;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.Whiteboard.WhiteboardListener;

public class ShowAutofinishButtonListener implements WhiteboardListener {

    private MainActivity mainActivity;

    private boolean offeredAutofinish;
    private int actionsShowing;

    public ShowAutofinishButtonListener(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void whiteboardEventReceived(Event event) {
        if (event == Event.MOVED) {
            if (mainActivity.getSolver().canAutoComplete(mainActivity.getTable())) {
                if (!offeredAutofinish) {
                    offeredAutofinish = true;
                    actionsShowing = 0;
                    if (mainActivity.getStorage().isHintSeen(ShowHintsListener.HINT_AUTOFINISH)) {
                        // if the hint is to be shown, autofinish will be offered when the hint closes
                        mainActivity.getMenuManager().showAutofinishMenu();
                        return;
                    } else {
                        Whiteboard.post(Event.OFFERED_AUTOFINISH);
                    }
                } else if (actionsShowing < 3) {
                    actionsShowing++;
                    return;
                }
            }

            Animator hideMenu = mainActivity.getMenuManager().hideMenu();
            hideMenu.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mainActivity.getMenuManager().updateMenu();
                }
            });
            hideMenu.start();
            return;
        }

        if (event == Event.GAME_STARTED) {
            offeredAutofinish = false;
        }
    }
}
