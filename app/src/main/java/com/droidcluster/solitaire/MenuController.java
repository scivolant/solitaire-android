package com.droidcluster.solitaire;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.game.JSONStorage;
import com.droidcluster.solitaire.model.Card;
import com.droidcluster.solitaire.model.Deck;
import com.droidcluster.solitaire.model.IMove2;
import com.droidcluster.solitaire.model.Table;
import com.droidcluster.solitaire.util.TouchHandler2;

public class MenuController {

    private static final float ACTIVE_ALPHA = 0.5f;
    private static final float INACTIVE_ALPHA = 1f;
    private final MainActivity mainActivity;
    private final View gameSubmenu;
    private final View btnReplay;
    private final View menuView;
    private final View leftMenu;
    private final View rightMenu;
    private final View scoreView;

    private final View btnSettings;
    private final View btnStats;
    private final View btnAutofinish;
    private final View btnUndo;
    private final ImageView btnGame;
    private final TextView btnNewGame;
    private final View btnDraw1;
    private final View btnDraw3;
    private final int animTime;

    private View menuVisible;
    private boolean showingWinMenu;
    private boolean disableMenu;

    public MenuController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        animTime = mainActivity.getResources().getInteger(android.R.integer.config_mediumAnimTime);

        menuView = mainActivity.findViewById(R.id.menuView);
        leftMenu = menuView.findViewById(R.id.menu_left);
        rightMenu = menuView.findViewById(R.id.menu_right);
        gameSubmenu = menuView.findViewById(R.id.game_submenu);
        scoreView = mainActivity.findViewById(R.id.scoreView);

        btnReplay = menuView.findViewById(R.id.menu_replay);
        btnSettings = menuView.findViewById(R.id.menu_settings_btn);
        btnStats = menuView.findViewById(R.id.menu_stats_btn);
        btnAutofinish = menuView.findViewById(R.id.menu_autofinish_btn);
        btnUndo = menuView.findViewById(R.id.menu_undo_btn);
        btnGame = (ImageView) menuView.findViewById(R.id.menu_game_btn);
        btnNewGame = (TextView) menuView.findViewById(R.id.menu_new_game);
        btnDraw1 = menuView.findViewById(R.id.menu_draw1);
        btnDraw3 = menuView.findViewById(R.id.menu_draw3);

        addListeners();
        updateDraw3().start();

        rightMenu.setTranslationY(rightMenu.getHeight());
        leftMenu.setTranslationX(-leftMenu.getWidth() - leftMenu.getX());
    }

    private void addListeners() {
        // attach to background
        final View gameBackground = mainActivity.findViewById(R.id.effectsView);
        gameBackground.setOnTouchListener(new TouchHandler2() {
            private final int SWIPE_TOLERANCE = 2* (int) mainActivity.getResources().getDimension(R.dimen.activity_horizontal_margin);
            private int dragStartX = -1;
            private int dragX;

            @Override
            protected void click(int x, int y) {
                if (showingWinMenu || disableMenu) {
                    return;
                }
                toggleMenu(rightMenu);
            }

            @Override
            protected void dragStart(int x, int y) {
                dragStartX = x;
            }

            @Override
            protected void drag(int x, int y) {
                dragX = x;
            }

            @Override
            protected void dragEnd() {
                if(dragStartX < SWIPE_TOLERANCE && dragX - dragStartX > SWIPE_TOLERANCE) { // swiped right
                    toggleMenu(leftMenu);
                } else {
                    hideMenuNow();
                }
                dragStartX = -1;
            }
        });

        // create menu items
        btnSettings.setOnTouchListener(new TouchHandler2() {
            @Override
            public void click(int x, int y) {
                mainActivity.getSettingsManager().showSettings();
            }
        });
        btnStats.setOnTouchListener(new TouchHandler2() {
            @Override
            public void click(int x, int y) {
                hideMenuNow();
                mainActivity.getStatsManager().toggleStats();
            }
        });
        btnAutofinish.setOnTouchListener(new TouchHandler2() {
            @Override
            public void click(int x, int y) {
                autofinish();
            }
        });
        btnUndo.setOnTouchListener(new TouchHandler2() {
            @Override
            public void click(int x, int y) {
                Table table = mainActivity.getTable();
                if (table.getHistory().isEmpty()) {
                    return;
                }
                Animator anim = mainActivity.getMover().undo();
                anim.start();
                updateMenu();
            }
        });
        btnGame.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                if (gameSubmenu.getVisibility() == View.GONE) {
                    gameSubmenu.setAlpha(0);
                    gameSubmenu.setVisibility(View.VISIBLE);
                    gameSubmenu.post(new Runnable() {
                        @Override
                        public void run() {
                            gameSubmenu.setX(rightMenu.getX() - gameSubmenu.getWidth()
                                    - mainActivity.getResources().getDimension(R.dimen.activity_horizontal_margin));
                            gameSubmenu.setY(Math.max(0, rightMenu.getY() + btnGame.getHeight() - gameSubmenu.getHeight()));
                            gameSubmenu.setAlpha(1);
                        }
                    });
                } else {
                    gameSubmenu.setVisibility(View.GONE);
                    gameSubmenu.setAlpha(0);
                }
            }
        });
        btnNewGame.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                newGame();
            }
        });
        btnReplay.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                replay();
            }
        });
        btnDraw1.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                setDrawThree(false);
            }
        });
        btnDraw3.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                setDrawThree(true);
            }
        });
    }

    private void setDrawThree(boolean drawThree) {
        boolean curDrawThree = mainActivity.getSettingsManager().getSettings().drawThree;
        if (drawThree == curDrawThree) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        Editor editor = prefs.edit();
        editor.putBoolean(mainActivity.getString(R.string.pref_draw_three), drawThree);
        editor.commit();

        updateDraw3().start();
    }

    private Animator updateDraw3() {

        boolean drawThree = mainActivity.getSettingsManager().getSettings().drawThree;
        List<Animator> anims = new ArrayList<Animator>();
        final int[] curDrawThreeImageId = new int[1]; // hack, but this has to be final
        if (drawThree) {
            anims.add(ObjectAnimator.ofFloat(btnDraw1, "alpha", btnDraw1.getAlpha(), INACTIVE_ALPHA));
            anims.add(ObjectAnimator.ofFloat(btnDraw3, "alpha", btnDraw3.getAlpha(), ACTIVE_ALPHA));
            curDrawThreeImageId[0] = R.drawable.draw3;
        } else {
            anims.add(ObjectAnimator.ofFloat(btnDraw1, "alpha", btnDraw1.getAlpha(), ACTIVE_ALPHA));
            anims.add(ObjectAnimator.ofFloat(btnDraw3, "alpha", btnDraw3.getAlpha(), INACTIVE_ALPHA));
            curDrawThreeImageId[0] = R.drawable.draw1;
        }

        AnimatorSet res = new AnimatorSet();
        res.playTogether(anims);
        res.setDuration(mainActivity.getAnimationTimeMs());
        res.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                btnGame.setImageResource(curDrawThreeImageId[0]);
            }
        });
        return res;
    }

    public Animator showWinMenu() {
        gameSubmenu.setVisibility(View.VISIBLE);
        btnReplay.setVisibility(View.GONE);
        btnDraw1.setVisibility(View.GONE);
        btnDraw3.setVisibility(View.GONE);
        menuView.setAlpha(0);
        menuView.setVisibility(View.VISIBLE);

        gameSubmenu.post(new Runnable() {
            @Override
            public void run() {
                gameSubmenu.setX((mainActivity.getLayout().availableSize.x - gameSubmenu.getWidth()) / 2);
                gameSubmenu.setY(mainActivity.getLayout().availableSize.y - gameSubmenu.getHeight());
            }
        });
        ObjectAnimator anim = ObjectAnimator.ofFloat(menuView, "alpha", 0, 1);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                scoreView.setVisibility(View.GONE);
                menuVisible = rightMenu;
                showingWinMenu = true;
            }
        });
        return anim;
    }

    public void updateMenu() {
        Table table = mainActivity.getTable();
        if (table == null) {
            return;
        }

        btnGame.setVisibility(View.VISIBLE);
        btnSettings.setVisibility(View.VISIBLE);
        btnStats.setVisibility(View.VISIBLE);
        btnReplay.setVisibility(table.getHistory().isEmpty() ? View.GONE : View.VISIBLE);
        btnDraw1.setVisibility(View.VISIBLE);
        btnDraw3.setVisibility(View.VISIBLE);

        if(mainActivity.getSolver().canAutoComplete(table)) {
            btnAutofinish.setAlpha(1);
            btnAutofinish.setVisibility(View.VISIBLE);
        } else {
            hideItemNow(btnAutofinish);
        }
        if(!table.getHistory().isEmpty()) {
            btnUndo.setAlpha(1);
            btnUndo.setVisibility(View.VISIBLE);
        } else {
            hideItemNow(btnUndo);
        }
    }

    private void hideItemNow(final View item) {
        if(menuVisible == null) {
            item.setVisibility(View.GONE);
            return;
        }

        Animator fadeOut = ObjectAnimator.ofFloat(item, "alpha", 1, 0);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                item.setVisibility(View.GONE);
            }
        });
        fadeOut.setDuration(2*animTime);
        fadeOut.start();
    }

    public void toggleMenu(View menu) {
        Animator hideMenu = hideMenu();
        if (hideMenu != null) {
            hideMenu.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    updateMenu();
                }
            });
            hideMenu.start();
        } else {
            updateMenu();
            showMenu(menu);
        }
    }

    public void showLeftMenu() {
        showMenu(leftMenu);
    }

    public void showRightMenu() {
        showMenu(rightMenu);
    }

    public void showMenu(final View menu) {
        ObjectAnimator moveIn;
        if(menu.getId() == R.id.menu_left) {
            moveIn = ObjectAnimator.ofFloat(menu, "translationX", menu.getTranslationX(), 0);
        } else {
            moveIn = ObjectAnimator.ofFloat(menu, "translationY", menu.getHeight(), 0);
        }

        moveIn.setDuration(animTime);
        moveIn.setInterpolator(new AnticipateOvershootInterpolator(1.5f));
        menu.bringToFront();
        menu.setVisibility(View.VISIBLE);
        menu.setAlpha(0);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(menu, "alpha", 0, 1);
        fadeIn.setDuration(animTime);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(moveIn, fadeIn);
        set.start();
        scoreView.setVisibility(View.GONE);
        menuVisible = menu;
    }

    public void hideMenuNow() {
        Animator hide = hideMenu();
        if(hide != null) {
            hide.start();
        }
    }

    /**
     *
     * @return may return <tt>null</tt>
     */
    public Animator hideMenu() {
        return menuVisible == null ? null : hideMenu(menuVisible, false);
    }

    private Animator hideMenu(final View menu, final boolean hideScore) {
        //final View menu = rightMenu;

        ObjectAnimator moveOut;
        if(menu.getId() == R.id.menu_left) {
            moveOut = ObjectAnimator.ofFloat(menu, "translationX", 0, -menu.getWidth());
        } else {
            moveOut = ObjectAnimator.ofFloat(menu, "translationY", menu.getTranslationY(), menu.getHeight());
        }

        moveOut.setDuration(animTime);
        moveOut.setInterpolator(new AccelerateInterpolator(1.5f));

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(menu, "alpha", 1, 0);
        fadeOut.setDuration(animTime);

        AnimatorSet set = new AnimatorSet();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                menuVisible = null;
                gameSubmenu.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                menu.setVisibility(View.GONE);
                if (!hideScore) {
                    scoreView.setVisibility(View.VISIBLE);
                }
            }
        });
        set.playTogether(moveOut, fadeOut);

        return set;
    }

    public void showAutofinishMenu() {
        btnAutofinish.setVisibility(View.VISIBLE);
        btnGame.setVisibility(View.INVISIBLE);
        btnSettings.setVisibility(View.INVISIBLE);
        btnStats.setVisibility(View.INVISIBLE);
        btnUndo.setVisibility(View.INVISIBLE);

        menuView.post(new Runnable() {
            @Override
            public void run() {
                showMenu(rightMenu);
            }
        });
    }

    private void autofinish() {
        hideMenu(rightMenu, true).start();
        final Table table = mainActivity.getTable();
        Animator autoFinish = mainActivity.getMover().animateAutoFinish();
        mainActivity.getStorage().saveTable(table);
        Whiteboard.post(Event.WON);

        autoFinish.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                disableMenu = false;
                Utils.showGameWonStuff(mainActivity);
            }
        });
        disableMenu = true;
        autoFinish.start();
    }

    public void newGame() {
        Table table = mainActivity.getTable();
        showingWinMenu = false;

        boolean lost = false;
        if (!table.isSolved()) {
            Whiteboard.post(Event.LOST);
            lost = true;
        }

        table.reset();
        if (lost && mainActivity.getStorage().loadOrCreateStats(table.isDrawThree()).getStrike() < -2) {
            // don't make the user sad and generate a wining game
            mainActivity.getSolver().initWinningGame(table);
        } else {
            table.init();
        }

        mainActivity.getTimer().pause();
        mainActivity.getTimer().setTime(0);
        JSONStorage storage = mainActivity.getStorage();
        storage.saveTable(table);
        Whiteboard.post(Event.GAME_STARTED);
        hideMenuNow();

        resetAndDeal();
    }

    private void replay() {
        Table table = mainActivity.getTable();

        if (table.getHistory().isEmpty()) {
            return;
        }

        Deck hand = new Deck();
        List<Card> cardsToFlip = new ArrayList<Card>();
        while (!table.getHistory().isEmpty()) {
            IMove2 move = table.getHistory().pop();
            move.beginUndo(table, hand, cardsToFlip);
            move.completeUndo(table, hand, cardsToFlip);
        }
        table.setTime(mainActivity.getTimer().getTime());
        JSONStorage storage = mainActivity.getStorage();
        storage.saveTable(table);
        hideMenuNow();

        resetAndDeal();
    }

    private void resetAndDeal() {
        Animator reset = mainActivity.getMover().collectCards();
        reset.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Animator nu = mainActivity.getMover().deal();
                nu.setStartDelay(mainActivity.getAnimationTimeMs() / 3);
                // nu.setInterpolator(new AccelerateInterpolator());
                nu.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        disableMenu = false;
                    }
                });
                nu.start();
            }
        });
        reset.setInterpolator(new DecelerateInterpolator());
        disableMenu = true;
        reset.start();
    }

    public boolean isShowingWinMenu() {
        return showingWinMenu;
    }
}