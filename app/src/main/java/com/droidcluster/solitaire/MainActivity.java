package com.droidcluster.solitaire;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.game.GameTimer;
import com.droidcluster.solitaire.game.JSONStorage;
import com.droidcluster.solitaire.game.Layout;
import com.droidcluster.solitaire.game.Solver;
import com.droidcluster.solitaire.model.Card;
import com.droidcluster.solitaire.model.Table;

public class MainActivity extends Activity {
    private final ImageView[] cardView = new ImageView[Card.values().length];
    private final Bitmap[] cardBitmap = new Bitmap[Card.values().length];
    private final boolean[] cardOpen = new boolean[Card.values().length];
    private Bitmap cardBack;

    private JSONStorage storage;
    // private ImageView gameBackground;
    private FrameLayout effectsView;
    private Table table;
    private final Layout layout = new Layout();
    private MenuController menuController;
    private StatsManager statsManager;
    private Mover mover;
    private final GameTimer timer = new GameTimer();
    private SettingsManager settingsManager;
    private PrefChangeToWhiteboardForwarder prefListener;
    private int animationTimeMs;
    private ScoreManager scoreManager;
    private final Solver solver = new Solver();
    private WelcomeController welcomeController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        effectsView = (FrameLayout) findViewById(R.id.effectsView);

        statsManager = new StatsManager(this);
        settingsManager = new SettingsManager(this);
        prefListener = new PrefChangeToWhiteboardForwarder(this);
        mover = new Mover(this);
        scoreManager = new ScoreManager(this);
        welcomeController = new WelcomeController(this);

        // wait for first layout
        ViewTreeObserver viewTreeObserver = effectsView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    //welcomeController.getWelcomeAnimation().start();
                    menuController = new MenuController(MainActivity.this);
                    effectsView.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                    new InitTask(MainActivity.this).execute();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.updateFullScreen(getWindow());
    }

    @Override
    protected void onDestroy() {
        pause();

        super.onDestroy();

        if (prefListener != null) {
            prefListener.destroy();
        }
        Whiteboard.destroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            unpause();
        } else {
            pause();
        }
    }

    @Override
    public void onBackPressed() {
        if (statsManager.isShowingStats()) {
            statsManager.toggleStats();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (statsManager.isShowingStats()) {
            statsManager.toggleStats();
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    void unpause() {
        timer.unpause();
        Whiteboard.post(Event.UNPAUSED);
    }

    private void pause() {
        if (timer.pause() && table != null) {
            table.setTime(timer.getTime());
            storage.saveTable(table);
        }
        Whiteboard.post(Event.PAUSED);
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public Layout getLayout() {
        return layout;
    }

    public ImageView getCardView(int tag) {
        return cardView[tag];
    }

    public void setCardView(int tag, ImageView view) {
        cardView[tag] = view;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public MenuController getMenuManager() {
        return menuController;
    }

    public Mover getMover() {
        return mover;
    }

    public boolean isCardRevealed(int tag) {
        return cardOpen[tag];
    }

    public void setCardOpen(int tag, boolean open) {
        cardOpen[tag] = open;
    }

    public Bitmap getCardBitmap(int tag) {
        return cardBitmap[tag];
    }

    public void setCardBitmap(int tag, Bitmap bitmap) {
        cardBitmap[tag] = bitmap;
    }

    public Bitmap getCardBack() {
        return cardBack;
    }

    public void setCardBack(Bitmap cardBack) {
        this.cardBack = cardBack;
    }

    public JSONStorage getStorage() {
        return storage;
    }

    public void setStorage(JSONStorage storage) {
        this.storage = storage;
    }

    public GameTimer getTimer() {
        return timer;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public int getAnimationTimeMs() {
        return animationTimeMs;
    }

    public void setAnimationTimeMs(int animationTimeMs) {
        this.animationTimeMs = animationTimeMs;
    }

    public Solver getSolver() {
        return solver;
    }

    public WelcomeController getWelcomeController() {
        return welcomeController;
    }

    public FrameLayout getEffectsView() {
        return effectsView;
    }

}
