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
import android.view.View.OnClickListener;
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

public class MenuManager {

    private static final float ACTIVE_ALPHA = 0.5f;
    private static final float INACTIVE_ALPHA = 1f;
    private final MainActivity mainActivity;
    private final View gameSubmenu;
    private final View buttonsView;
    private final View btnReplay;
    private final View menuView;
    private final View scoreView;

    private final View btnSettings;
    private final View btnStats;
    private final View btnAutofinish;
    private final View btnUndo;
    private final ImageView btnGame;
    private final TextView btnNewGame;
    private final View btnDraw1;
    private final View btnDraw3;
    private final View titleDraw3;
    private final View titleGame;

    private boolean menuVisible;
    private boolean showingWinMenu;
    private boolean disableMenu;

    public MenuManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        menuView = mainActivity.findViewById(R.id.menuView);
        buttonsView = menuView.findViewById(R.id.menu_buttons);
        gameSubmenu = menuView.findViewById(R.id.game_submenu);
        scoreView = mainActivity.findViewById(R.id.scoreView);

        btnReplay = menuView.findViewById(R.id.menu_replay_btn);
        btnSettings = menuView.findViewById(R.id.menu_settings_btn);
        btnStats = menuView.findViewById(R.id.menu_stats_btn);
        btnAutofinish = menuView.findViewById(R.id.menu_autofinish_btn);
        btnUndo = menuView.findViewById(R.id.menu_undo_btn);
        btnGame = (ImageView) menuView.findViewById(R.id.menu_game_btn);
        btnNewGame = (TextView) menuView.findViewById(R.id.menu_new_game_btn);
        btnDraw1 = menuView.findViewById(R.id.menu_draw1_btn);
        btnDraw3 = menuView.findViewById(R.id.menu_draw3_btn);
        titleDraw3 = menuView.findViewById(R.id.menu_draw3_title);
        titleGame = menuView.findViewById(R.id.menu_game_title);

        addListeners();
        updateDraw3().start();

        menuView.setTranslationY(buttonsView.getHeight());
    }

    private void addListeners() {
        // attach to background
        View gameBackground = mainActivity.findViewById(R.id.effectsView);
        gameBackground.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (showingWinMenu || disableMenu) {
                    return;
                }
                toggleMenu();
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
                hideMenu().start();
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
                    gameSubmenu.setVisibility(View.VISIBLE);
                } else {
                    gameSubmenu.setVisibility(View.GONE);
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
        buttonsView.setVisibility(View.INVISIBLE);
        gameSubmenu.setVisibility(View.VISIBLE);
        btnReplay.setVisibility(View.GONE);
        btnDraw1.setVisibility(View.GONE);
        btnDraw3.setVisibility(View.GONE);
        titleDraw3.setVisibility(View.GONE);
        titleGame.setVisibility(View.GONE);
        // btnNewGame.setBackgroundResource(R.drawable.button_light_bg);
        // btnNewGame.setTextColor(mainActivity.getResources().getColor(R.color.textColor));
        // gameSubmenu.setBackgroundResource(0);
        menuView.setAlpha(0);
        menuView.setVisibility(View.VISIBLE);
        menuView.setTranslationY(buttonsView.getHeight());

        ObjectAnimator anim = ObjectAnimator.ofFloat(menuView, "alpha", 0, 1);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                scoreView.setVisibility(View.GONE);
                menuVisible = true;
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
        titleDraw3.setVisibility(View.VISIBLE);
        titleGame.setVisibility(View.VISIBLE);

        mainActivity.findViewById(R.id.menu_autofinish_btn).setVisibility(
                mainActivity.getSolver().canAutoComplete(table) ? View.VISIBLE : View.GONE);
        mainActivity.findViewById(R.id.menu_undo_btn).setVisibility(
                table.getHistory().isEmpty() ? View.GONE : View.VISIBLE);
    }

    public void toggleMenu() {
        if (menuVisible) {
            Animator hideMenu = hideMenu();
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
            showMenu();
        }
    }

    public void showMenu() {
        buttonsView.setVisibility(View.VISIBLE);
        menuView.setAlpha(1);
        ObjectAnimator anim = ObjectAnimator.ofFloat(menuView, "translationY",
                Math.min(menuView.getTranslationY(), buttonsView.getHeight()), 0);
        anim.setDuration(mainActivity.getAnimationTimeMs());
        menuView.bringToFront();
        menuView.setVisibility(View.VISIBLE);
        anim.start();
        scoreView.setVisibility(View.GONE);
        menuVisible = true;
    }

    public Animator hideMenu() {
        return hideMenu(false);
    }

    private Animator hideMenu(final boolean hideScore) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(menuView, "translationY", menuView.getTranslationY(),
                menuView.getHeight());
        anim.setDuration(mainActivity.getAnimationTimeMs());
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                menuVisible = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                gameSubmenu.setVisibility(View.GONE);
                menuView.setVisibility(View.GONE);
                if (!hideScore) {
                    scoreView.setVisibility(View.VISIBLE);
                }
            }
        });
        return anim;
    }

    public void showAutofinishMenu() {
        btnAutofinish.setVisibility(View.VISIBLE);
        btnGame.setVisibility(View.INVISIBLE);
        btnSettings.setVisibility(View.INVISIBLE);
        btnStats.setVisibility(View.INVISIBLE);
        btnUndo.setVisibility(View.INVISIBLE);

        showMenu();
    }

    private void autofinish() {
        hideMenu(true).start();
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

    private void newGame() {
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
        hideMenu().start();

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
        hideMenu().start();

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
}
