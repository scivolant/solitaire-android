package com.droidcluster.solitaire;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.widget.TextView;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.Whiteboard.WhiteboardListener;
import com.droidcluster.solitaire.game.Layout;
import com.droidcluster.solitaire.game.Storage;
import com.droidcluster.solitaire.util.TouchHandler2;

public class HintController implements WhiteboardListener {
    private static int HIDE_TIMEOUT = 6000;

    static final int HINT_AUTOFINISH = 2;
    private static final int CENTER = -1;
    private static final int HINT_WELCOME = 0;
    private static final int HINT_UNDO = 1;
    private static final int HINT_MENU = 3;

    private final MainActivity mainActivity;
    private final Storage storage;
    private final int animDuration;

    private int moves;

    public HintController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        animDuration = mainActivity.getResources().getInteger(android.R.integer.config_longAnimTime);
        storage = mainActivity.getStorage();
    }

    @Override
    public void whiteboardEventReceived(Event event) {
        switch (event) {
            case GAME_STARTED:
                if (!storage.isHintSeen(HINT_WELCOME)) {
                    showHintWelcome();
                    storage.setHintSeen(HINT_WELCOME);
                } else if (!storage.isHintSeen(HINT_MENU)) {
                    showHintMainMenu();
                }
                Whiteboard.removeListener(this, Event.GAME_STARTED);
                break;

            case OFFERED_AUTOFINISH:
                if (!storage.isHintSeen(HINT_AUTOFINISH)) {
                    mainActivity.getEffectsView().post(new Runnable() {
                        @Override
                        public void run() {
                            showHintAutofinish();
                        }
                    });
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
                        mainActivity.getMenuController().updateMenu();
                        mainActivity.getEffectsView().post(new Runnable() {
                            @Override
                            public void run() {
                                showHintUndo();
                            }
                        });
                        storage.setHintSeen(HINT_UNDO);
                    }
                } else {
                    Whiteboard.removeListener(this, Event.MOVED);
                }
                break;
            default:
                break;
        }
    }

    private void showHintUndo() {
        String text = mainActivity.getString(R.string.hint2);

        Layout layout = mainActivity.getLayout();
        int x = (int) (mainActivity.findViewById(R.id.menu_right).getX()
                - mainActivity.getResources().getDimension(R.dimen.activity_horizontal_margin) / 2);
        int y = layout.availableSize.y - mainActivity.findViewById(R.id.menu_game_btn).getHeight()
                - (int) mainActivity.getResources().getDimension(R.dimen.activity_vertical_margin);

        mainActivity.getMenuController().showRightMenu();
        showHint(text, x, y, true);
    }

    private void showHintAutofinish() {
        String text = mainActivity.getString(R.string.hint1);

        Layout layout = mainActivity.getLayout();
        int x = (int) (mainActivity.findViewById(R.id.menu_right).getX()
                - mainActivity.getResources().getDimension(R.dimen.activity_horizontal_margin) / 2);
        int y = layout.availableSize.y - mainActivity.findViewById(R.id.menu_game_btn).getHeight()
                - (int) mainActivity.getResources().getDimension(R.dimen.activity_vertical_margin);

        showHint(text, x, y, true);
    }

    private void showHintWelcome() {
        String text = mainActivity.getString(R.string.hint0);
        Layout layout = mainActivity.getLayout();
        int x = (int) (mainActivity.findViewById(R.id.menu_right).getX()
                - mainActivity.getResources().getDimension(R.dimen.activity_horizontal_margin) / 2);
        int y = layout.availableSize.y - mainActivity.findViewById(R.id.menu_game_btn).getHeight()
                - (int) mainActivity.getResources().getDimension(R.dimen.activity_vertical_margin);

        showHint(text, x, y, true);
        mainActivity.getMenuController().showRightMenu();
        mainActivity.findViewById(R.id.hint).setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                Animator hide = hideHint();
                hide.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        showHintMainMenu();
                    }
                });
                mainActivity.getMenuController().hideMenuNow();
                hide.start();
            }
        });
    }

    private void showHintMainMenu() {
        String text = mainActivity.getString(R.string.hint3);

        Layout layout = mainActivity.getLayout();
        int x = (int) (mainActivity.findViewById(R.id.menu_settings_btn).getX()
                + mainActivity.getResources().getDimension(R.dimen.activity_horizontal_margin))
                + mainActivity.findViewById(R.id.menu_settings_btn).getWidth();
        int y = layout.availableSize.y - mainActivity.findViewById(R.id.menu_game_btn).getHeight()
                - (int) mainActivity.getResources().getDimension(R.dimen.activity_vertical_margin);

        showHint(text, x, y, false);
        mainActivity.getMenuController().showLeftMenu();
        mainActivity.findViewById(R.id.hint).setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                mainActivity.getMenuController().hideMenuNow();
                hideHintNow();
            }
        });
        storage.setHintSeen(HINT_MENU);
    }

    private void showHint(String text, final int x, final int y, final boolean right) {
        final TextView hint = (TextView) mainActivity.findViewById(R.id.hint);
        hint.setText(text);
        hint.setBackgroundResource(right ? R.drawable.bubble_white_r : R.drawable.bubble_white_l);
        final ObjectAnimator anim = ObjectAnimator.ofFloat(hint, "alpha", 0, 1);
        anim.setDuration(animDuration);
        hint.setVisibility(View.VISIBLE);

        hint.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                hideHintNow();
            }
        });

        hint.post(new Runnable() {
            @Override
            public void run() {
                int xpos = x;
                if (x == CENTER) {
                    xpos = (mainActivity.getLayout().availableSize.x - hint.getWidth()) / 2;
                } else if (right) {
                    xpos -= hint.getWidth();
                }
                int ypos = y;
                if (y == CENTER) {
                    ypos = (mainActivity.getLayout().availableSize.y - hint.getHeight()) / 2;
                }
                hint.setX(xpos);
                hint.setY(ypos);
            }
        });

        hint.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(hint.getAlpha() == 1) {
                    hideHintNow();
                }
            }
        }, HIDE_TIMEOUT);

        anim.start();
    }

    public void hideHintNow() {
        hideHint().start();
    }

    private Animator hideHint() {
        final View hint = mainActivity.findViewById(R.id.hint);
        ObjectAnimator anim = ObjectAnimator.ofFloat(hint, "alpha", 1, 0);
        anim.setDuration(animDuration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                hint.post(new Runnable() {
                    @Override
                    public void run() {
                        hint.setOnTouchListener(null);
                    }
                });
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                hint.setVisibility(View.GONE);
            }
        });
        return anim;
    }
}
