package com.droidcluster.solitaire;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.droidcluster.solitaire.game.JSONStorage;
import com.droidcluster.solitaire.game.Layout;
import com.droidcluster.solitaire.model.Stats;
import com.droidcluster.solitaire.model.Table;
import com.droidcluster.solitaire.util.TimeConverter;
import com.droidcluster.solitaire.util.TouchHandler2;

public class StatsManager {

    private final MainActivity mainActivity;
    private final View effectsView;
    private final View statsView;
    private final TextView bestScore;
    private final TextView bestTime;
    private final TextView bestMoves;
    private final TextView totalGames;
    private final ProgressBar winLossProgress;
    private final View winView;
    private final View winViewContent;
    private final TextView winScore;
    private final TextView winTime;
    private final TextView winMoves;
    private final Handler uiHandler;
    private final TextView bestWinScore;
    private final TextView bestWinTime;
    private final TextView bestWinMoves;
    private boolean hideWinViewRequested;
    private boolean hideWinView;
    private final TextView winBestTitle;
    private final TextView titleWinTime;
    private final TextView titleWinScore;
    private final TextView titleWinMoves;
    private final TextView gamesWonPercent;

    private ValueAnimator bestAnimation;
    private Animator showWinViewAnimation;

    public StatsManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        uiHandler = new Handler(Looper.getMainLooper());

        // init stats view
        effectsView = mainActivity.findViewById(R.id.effectsView);
        statsView = mainActivity.findViewById(R.id.statsView);
        bestScore = (TextView) statsView.findViewById(R.id.bestScore);
        bestTime = (TextView) statsView.findViewById(R.id.bestTime);
        bestMoves = (TextView) statsView.findViewById(R.id.bestMoves);
        totalGames = (TextView) statsView.findViewById(R.id.totalGames);
        winLossProgress = (ProgressBar) statsView.findViewById(R.id.winLossProgress);
        gamesWonPercent = (TextView) statsView.findViewById(R.id.gamesWonPercent);

        // init win view
        winView = mainActivity.findViewById(R.id.winView);
        winViewContent = winView.findViewById(R.id.winViewContent);
        View titleRow = winView.findViewById(R.id.winTitleRow);
        titleRow.setBackgroundResource(0);
        winBestTitle = (TextView) titleRow.findViewById(R.id.winRowBest);

        View scoreRow = winView.findViewById(R.id.winScoreRow);
        titleWinScore = (TextView) scoreRow.findViewById(R.id.winRowProperty);
        titleWinScore.setText(mainActivity.getString(R.string.score_points));
        winScore = (TextView) scoreRow.findViewById(R.id.winRowValue);
        bestWinScore = (TextView) scoreRow.findViewById(R.id.winRowBest);
        View timeRow = winView.findViewById(R.id.winTimeRow);
        titleWinTime = (TextView) timeRow.findViewById(R.id.winRowProperty);
        titleWinTime.setText(mainActivity.getString(R.string.score_time));
        winTime = (TextView) timeRow.findViewById(R.id.winRowValue);
        bestWinTime = (TextView) timeRow.findViewById(R.id.winRowBest);
        View movesRow = winView.findViewById(R.id.winMovesRow);
        titleWinMoves = (TextView) movesRow.findViewById(R.id.winRowProperty);
        titleWinMoves.setText(mainActivity.getString(R.string.score_moves));
        winMoves = (TextView) movesRow.findViewById(R.id.winRowValue);
        bestWinMoves = (TextView) movesRow.findViewById(R.id.winRowBest);

        statsView.setOnTouchListener(new TouchHandler2() {
            @Override
            protected void click(int x, int y) {
                toggleStats();
            }
        });
    }

    public void centerWinViewContent() {
        Layout layout = mainActivity.getLayout();
        winViewContent.setY(layout.statsLoc.top);
        winViewContent.getLayoutParams().width = layout.statsLoc.width();
    }

    void toggleStats() {
        if (isShowingStats()) {
            ObjectAnimator statsViewAlpha = ObjectAnimator.ofFloat(statsView, "alpha", 1, 0);
            statsViewAlpha.setDuration(mainActivity.getAnimationTimeMs());
            statsViewAlpha.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    statsView.setVisibility(View.GONE);
                }
            });

            ObjectAnimator rotGameView = ObjectAnimator
                    .ofFloat(effectsView, "rotationX", effectsView.getRotationX(), 0);
            ObjectAnimator moveGameView = ObjectAnimator.ofFloat(effectsView, "y", effectsView.getY(), 0);
            ObjectAnimator scaleGameViewX = ObjectAnimator.ofFloat(effectsView, "scaleX", effectsView.getScaleX(), 1);
            ObjectAnimator scaleGameViewY = ObjectAnimator.ofFloat(effectsView, "scaleY", effectsView.getScaleY(), 1);

            AnimatorSet gameViewSet = new AnimatorSet();
            gameViewSet.playTogether(rotGameView, moveGameView, scaleGameViewX, scaleGameViewY);
            gameViewSet.setDuration(3 * mainActivity.getAnimationTimeMs());

            AnimatorSet set = new AnimatorSet();
            set.playTogether(statsViewAlpha, gameViewSet);
            // set.playSequentially(statsViewAlpha, gameViewSet);
            set.start();
        } else {
            effectsView.setPivotX(effectsView.getWidth() / 2);
            effectsView.setPivotY(0);
            ObjectAnimator rotGameView = ObjectAnimator.ofFloat(effectsView, "rotationX", 0, -45);
            int y = effectsView.getHeight() * 3 / 4;
            ObjectAnimator moveGameView = ObjectAnimator.ofFloat(effectsView, "y", 0, y);
            ObjectAnimator scaleGameViewX = ObjectAnimator.ofFloat(effectsView, "scaleX", 1, 0.9f);
            ObjectAnimator scaleGameViewY = ObjectAnimator.ofFloat(effectsView, "scaleY", 1, 0.9f);

            AnimatorSet gameViewSet = new AnimatorSet();
            gameViewSet.playTogether(rotGameView, moveGameView, scaleGameViewX, scaleGameViewY);
            gameViewSet.setDuration(3 * mainActivity.getAnimationTimeMs());

            statsView.setAlpha(0);
            statsView.setVisibility(View.VISIBLE);
            statsView.setY(Math.max(0, (y - statsView.getHeight()) / 2));
            ObjectAnimator statsViewAlpha = ObjectAnimator.ofFloat(statsView, "alpha", 0, 1);
            statsViewAlpha.setDuration(mainActivity.getAnimationTimeMs());
            statsViewAlpha.setStartDelay(gameViewSet.getDuration() - statsViewAlpha.getDuration());

            AnimatorSet set = new AnimatorSet();
            Animator updateStats = updateStats();
            updateStats.setStartDelay(statsViewAlpha.getDuration() / 2);
            set.playTogether(gameViewSet, statsViewAlpha, updateStats);
            set.start();
        }
    }

    boolean isShowingStats() {
        return statsView.getVisibility() == View.VISIBLE;
    }

    private Animator updateStats() {
        final Stats stats = mainActivity.getStorage().loadOrCreateStats(mainActivity.getTable().isDrawThree());
        bestScore.setText(String.valueOf(stats.getBestPoints()));
        bestTime.setText(TimeConverter.timeToString(stats.getBestTime()));
        bestMoves.setText(String.valueOf(stats.getBestMoves()));
        totalGames.setText(String.valueOf(stats.getGamesPlayed()));

        // animate progress smoothly
        int smoothFactor = 100 / (1 + stats.getGamesWon() / 10);
        winLossProgress.setMax(smoothFactor * stats.getGamesPlayed());
        ObjectAnimator res = ObjectAnimator.ofInt(winLossProgress, "progress", 0, smoothFactor * stats.getGamesWon());

        res.setDuration(10 * mainActivity.getAnimationTimeMs());
        res.setInterpolator(new DecelerateInterpolator());

        res.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                gamesWonPercent.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(stats.getGamesPlayed() == 0) {
                    return;
                }
                int percentWon = 100 * stats.getGamesWon() / stats.getGamesPlayed();
                int textWidth = gamesWonPercent.getWidth();
                int progressRight = winLossProgress.getWidth();
                int x = progressRight * percentWon / 100;
                x = Math.max(0, x);
                x = Math.min(progressRight - textWidth / 2, x);
                gamesWonPercent.setTranslationX(x - textWidth / 2);
                gamesWonPercent.setVisibility(View.VISIBLE);
                gamesWonPercent.setText(String.valueOf(percentWon) + " %");
            }
        });
        return res;
    }

    public Animator hideWinView() {
        hideWinViewRequested = true;
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(winView, "alpha", winView.getAlpha(), 0);
        alphaAnim.setDuration(mainActivity.getAnimationTimeMs());
        alphaAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (showWinViewAnimation != null) {
                    showWinViewAnimation.end();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                hideWinView = true;
                if (bestAnimation != null) {
                    bestAnimation.cancel();
                    bestAnimation = null;
                }
                // reset all win view colors
                int color = mainActivity.getResources().getColor(R.color.textColor);
                titleWinScore.setTextColor(color);
                winScore.setTextColor(color);
                bestWinScore.setTextColor(color);
                titleWinTime.setTextColor(color);
                winTime.setTextColor(color);
                bestWinTime.setTextColor(color);
                titleWinMoves.setTextColor(color);
                winMoves.setTextColor(color);
                bestWinMoves.setTextColor(color);
            }
        });
        return alphaAnim;
    }

    public Animator showWinView(final Animator gameWonAnimation, final Table table) {
        hideWinViewRequested = false;

        winView.setAlpha(0);
        winView.setVisibility(View.VISIBLE);

        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(winView, "alpha", 0, 0.75f);
        alphaAnim.setDuration(3 * mainActivity.getAnimationTimeMs());
        alphaAnim.setInterpolator(new DecelerateInterpolator());

        final Stats stats = mainActivity.getStorage().loadOrCreateStats(mainActivity.getTable().isDrawThree());
        if (stats.getGamesWon() < 2) {
            winBestTitle.setText("");
            bestWinScore.setText("");
            bestWinMoves.setText("");
            bestWinTime.setText("");
        } else {
            winBestTitle.setText(mainActivity.getString(R.string.score_best));
            bestWinScore.setText(String.valueOf(stats.getBestPoints()));
            bestWinMoves.setText(String.valueOf(stats.getBestMoves()));
            bestWinTime.setText(TimeConverter.timeToString(stats.getBestTime()));
        }
        final int bestPoints = stats.getBestPoints();
        final int bestMoves = stats.getBestMoves();
        final long bestTime = stats.getBestTime();

        alphaAnim.addListener(new AnimatorListenerAdapter() {
            private static final float STEPS = 150;

            @Override
            public void onAnimationStart(Animator animation) {
                hideWinView = false;
                showWinViewAnimation = animation;

                final int points = table.getPoints();
                final int moves = table.getHistory().size();
                final long time = table.getTime();

                new Runnable() {
                    private int i = 1;

                    @Override
                    public void run() {
                        if (i++ > STEPS) {
                            winScore.setText(String.valueOf(points));
                            winMoves.setText(String.valueOf(moves));
                            winTime.setText(TimeConverter.timeToString(time));
                            if (!hideWinViewRequested) {

                                if (stats.getGamesWon() >= 2
                                        && (time == bestTime || points == bestPoints || moves == bestMoves)) {
                                    Integer colorFrom = mainActivity.getResources().getColor(R.color.textColor);
                                    Integer colorTo = Color.RED;
                                    bestAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                                    bestAnimation.addUpdateListener(new AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animator) {
                                            if (points == bestPoints) {
                                                titleWinScore.setTextColor((Integer) animator.getAnimatedValue());
                                                winScore.setTextColor((Integer) animator.getAnimatedValue());
                                                bestWinScore.setTextColor((Integer) animator.getAnimatedValue());
                                            }
                                            if (time == bestTime) {
                                                titleWinTime.setTextColor((Integer) animator.getAnimatedValue());
                                                winTime.setTextColor((Integer) animator.getAnimatedValue());
                                                bestWinTime.setTextColor((Integer) animator.getAnimatedValue());
                                            }
                                            if (moves == bestMoves) {
                                                titleWinMoves.setTextColor((Integer) animator.getAnimatedValue());
                                                winMoves.setTextColor((Integer) animator.getAnimatedValue());
                                                bestWinMoves.setTextColor((Integer) animator.getAnimatedValue());
                                            }
                                        }
                                    });
                                    bestAnimation.setRepeatMode(ValueAnimator.REVERSE);
                                    bestAnimation.setRepeatCount(4);
                                    bestAnimation.setDuration(5 * mainActivity.getAnimationTimeMs());
                                    bestAnimation.start();

                                    gameWonAnimation.setStartDelay(30 * mainActivity.getAnimationTimeMs());
                                }

                                gameWonAnimation.start();
                            }
                        } else {
                            winScore.setText(String.valueOf((int) (i * points / STEPS)));
                            winMoves.setText(String.valueOf((int) (i * moves / STEPS)));
                            winTime.setText(TimeConverter.timeToString((long) (i * time / STEPS)));
                            if (!hideWinView) {
                                uiHandler.postDelayed(this, mainActivity.getAnimationTimeMs() / 16);
                            }
                        }
                    }
                }.run();

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                showWinViewAnimation = null;
            }
        });
        return alphaAnim;
    }

    public void storeGameStats() {
        final Table table = mainActivity.getTable();
        final JSONStorage storage = mainActivity.getStorage();
        final Stats stats = storage.loadOrCreateStats(table.isDrawThree());
        stats.gameFinished(table);
        storage.saveStats(stats, table.isDrawThree());
    }
}
