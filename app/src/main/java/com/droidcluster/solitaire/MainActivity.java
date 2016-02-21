package com.droidcluster.solitaire;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.Whiteboard.WhiteboardListener;
import com.droidcluster.solitaire.game.GameTimer;
import com.droidcluster.solitaire.game.JSONStorage;
import com.droidcluster.solitaire.game.Layout;
import com.droidcluster.solitaire.game.Settings;
import com.droidcluster.solitaire.game.Solver;
import com.droidcluster.solitaire.model.Card;
import com.droidcluster.solitaire.model.Deck;
import com.droidcluster.solitaire.model.IMove2;
import com.droidcluster.solitaire.model.Move;
import com.droidcluster.solitaire.model.RecycleWasteMove;
import com.droidcluster.solitaire.model.Stats;
import com.droidcluster.solitaire.model.Table;
import com.droidcluster.solitaire.render.TableRenderer;
import com.droidcluster.solitaire.util.ImageLoader;
import com.droidcluster.solitaire.util.TouchHandler2;

public class MainActivity extends Activity {
    private final ImageView[] cardView = new ImageView[Card.values().length];
    private final Bitmap[] cardBitmap = new Bitmap[Card.values().length];
    private final boolean[] cardOpen = new boolean[Card.values().length];
    private ImageView gameDeckView;
    private Bitmap cardBack;

    private JSONStorage storage;
    // private ImageView gameBackground;
    private FrameLayout effectsView;
    private Table table;
    private final Layout layout = new Layout();
    private TableRenderer cardRenderer;
    private MenuManager menuManager2;
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
                    welcomeController.getWelcomeAnimation().start();
                    menuManager2 = new MenuManager(MainActivity.this);
                    effectsView.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                    new AsyncTask() {

                        @Override
                        protected Object doInBackground(Object... params) {
                            storage = new JSONStorage(getFilesDir());
                            animationTimeMs = getResources().getInteger(android.R.integer.config_shortAnimTime);
                            Settings settings = settingsManager.getSettings();
                            table = storage.loadOrCreateTable(settings.drawThree);
                            Stats stats = storage.loadOrCreateStats(table.isDrawThree());
                            if (table.getHistory().size() == 0 && stats.getGamesPlayed() == 0) {
                                // let the user win the first game
                                table.reset();
                                solver.initWinningGame(table);
                                storage.saveTable(table);
                            } else {
                                if (table.isSolved()) {
                                    table.reset();
                                    if (stats.getGamesPlayed() < 4) {
                                        // let the user win the first 3 games
                                        solver.initWinningGame(table);
                                    } else {
                                        table.init();
                                    }
                                    storage.saveTable(table);
                                }
                            }
                            timer.stop();
                            timer.setTime(table.getTime());
                            layout.initLayout(effectsView.getWidth(), effectsView.getHeight(), settings,
                                    MainActivity.this);
                            int x = layout.fontSize * 3 / 4;
                            layout.suits[0] = ImageLoader.getImageFromApp(R.drawable.suit0, x, x, getResources());
                            layout.suits[1] = ImageLoader.getImageFromApp(R.drawable.suit1, x, x, getResources());
                            layout.suits[2] = ImageLoader.getImageFromApp(R.drawable.suit2, x, x, getResources());
                            layout.suits[3] = ImageLoader.getImageFromApp(R.drawable.suit3, x, x, getResources());
                            cardRenderer = new TableRenderer();
                            cardRenderer.setResources(getResources());
                            cardRenderer.setLayout(layout);
                            new LoadImagesTask(MainActivity.this, layout, settings).doInBackground(false, false);
                            decorateGameBackground();

                            // draw cards
                            int width = layout.cardSize.x + 2;
                            int height = layout.cardSize.y + 2;
                            for (Card c : Card.values()) {
                                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                                Deck deck = new Deck(c);
                                deck.setOpenCardsCount(1);
                                cardRenderer.drawDeckCompact(deck, new Point(0, 0), new Canvas(bmp));
                                cardBitmap[c.ordinal()] = bmp;
                            }

                            cardBack = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            cardRenderer.drawDeckCompact(new Deck(Card.H1), new Point(0, 0), new Canvas(cardBack));

                            attachWhiteboardListeners();
                            return null;
                        }

                        private void decorateGameBackground() {
                            if (layout.gameBackground == null) {
                                return;
                            }
                            layout.gameBackground = layout.gameBackground.copy(Bitmap.Config.ARGB_8888, true);
                            Canvas bgCanvas = new Canvas(layout.gameBackground);
                            for (int i = 0; i < Table.FOUNDATION_DECKS_COUNT; i++) {
                                cardRenderer.drawFoundationSpot(layout.deckLocations[i], bgCanvas);
                            }
                        }

                        private void attachWhiteboardListeners() {
                            Whiteboard.addListener(new UpdateStatsListener(MainActivity.this), Event.WON, Event.LOST);
                            Whiteboard.addListener(new ShowAutofinishButtonListener(MainActivity.this),
                                    Event.GAME_STARTED, Event.MOVED);
                            Whiteboard.addListener(new ShowHintsListener(MainActivity.this), Event.OFFERED_AUTOFINISH,
                                    Event.MOVED, Event.GAME_STARTED);
                            Whiteboard.addListener(new WhiteboardListener() {
                                @Override
                                public void whiteboardEventReceived(Event event) {
                                    new LoadImagesTask(MainActivity.this, layout, settingsManager.getSettings()) {
                                        @Override
                                        protected void onPostExecute(String result) {
                                            decorateGameBackground();
                                            effectsView.setBackgroundDrawable(new BitmapDrawable(getResources(),
                                                    layout.gameBackground));
                                        };
                                    }.execute(true, false);
                                }
                            }, Event.GAME_BG_SET);
                            Whiteboard.addListener(new WhiteboardListener() {
                                @Override
                                public void whiteboardEventReceived(Event event) {
                                    new LoadImagesTask(MainActivity.this, layout, settingsManager.getSettings()) {
                                        @Override
                                        protected void onPostExecute(String result) {
                                            int width = layout.cardSize.x + 2;
                                            int height = layout.cardSize.y + 2;
                                            cardBack = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                                            cardRenderer.drawDeckCompact(new Deck(Card.H1), new Point(0, 0),
                                                    new Canvas(cardBack));

                                            for (int i = 0; i < cardView.length; i++) {
                                                if (!cardOpen[i]) {
                                                    cardView[i].setImageBitmap(cardBack);
                                                }
                                            }
                                        };
                                    }.execute(false, true);
                                }
                            }, Event.CARD_BG_SET);
                            Whiteboard.addListener(new WhiteboardListener() {
                                @Override
                                public void whiteboardEventReceived(Event event) {
                                    handleLayoutChange();
                                }
                            }, Event.LEFT_HAND_SET);
                            Whiteboard.addListener(new WhiteboardListener() {
                                @Override
                                public void whiteboardEventReceived(Event event) {
                                    table.setDrawThree(settingsManager.getSettings().drawThree);
                                    mover.fixWasteIfDrawThree(0).start();
                                }
                            }, Event.DRAW_THREE_SET);
                        }

                        @Override
                        protected void onPostExecute(Object result) {
                            Context context = MainActivity.this;
                            effectsView.addView(new View(context));
                            effectsView
                                    .setBackgroundDrawable(new BitmapDrawable(getResources(), layout.gameBackground));
                            menuManager2.updateMenu();

                            int width = layout.cardSize.x + 2;
                            int height = layout.cardSize.y + 2;
                            // create game deck location image
                            gameDeckView = new ImageView(context);
                            effectsView.addView(gameDeckView);
                            gameDeckView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
                            gameDeckView.setX(layout.deckLocations[Table.GAME_DECK_INDEX].x);
                            gameDeckView.setY(layout.deckLocations[Table.GAME_DECK_INDEX].y);
                            Bitmap emptyDeck = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            cardRenderer.drawDeckCompact(new Deck(), new Point(0, 0), new Canvas(emptyDeck));
                            gameDeckView.setImageBitmap(emptyDeck);
                            gameDeckView.setOnTouchListener(new TouchHandler2() {
                                protected void click(int x, int y) {
                                    if (table.getWaste().getCardsCount() > 0
                                            && table.getGameDeck().getCardsCount() == 0) {
                                        Animator moveAnim = mover.move(new RecycleWasteMove());
                                        moveAnim.start();
                                    } else if (table.getWaste().getCardsCount() == 0
                                            && table.getGameDeck().getCardsCount() > 0) {
                                        IMove2 move = new Move(Table.GAME_DECK_INDEX, 0, Table.WASTE_INDEX);
                                        Animator moveAnim = mover.move(move);
                                        moveAnim.start();
                                    }
                                }
                            });
                            attachResizeListener();

                            for (final Card c : Card.values()) {
                                final ImageView imageView = new ImageView(context);
                                effectsView.addView(imageView);
                                imageView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
                                imageView.setX(layout.deckLocations[Table.GAME_DECK_INDEX].x);
                                imageView.setY(layout.deckLocations[Table.GAME_DECK_INDEX].y);
                                imageView.setImageBitmap(cardBack);
                                imageView.setTag(c.ordinal());
                                CardTouchListener l = new CardTouchListener(MainActivity.this, c);
                                imageView.setOnTouchListener(l);
                                Whiteboard.addListener(l, Event.WON, Event.GAME_STARTED);
                                cardView[c.ordinal()] = imageView;
                            }

                            mover.restoreTableState();

                            // position win view
                            statsManager.centerWinViewContent();

                            unpause();
                            installScoreViewUpdater();

                            welcomeController.initComplete();
                        }

                        private void installScoreViewUpdater() {
                            ScoreViewUpdater u = new ScoreViewUpdater(MainActivity.this);
                            Whiteboard.addListener(u, Event.MOVED, Event.GAME_STARTED, Event.LOST, Event.WON,
                                    Event.PAUSED, Event.UNPAUSED);
                        }

                        private void attachResizeListener() {
                            Whiteboard.addListener(new WhiteboardListener() {
                                @Override
                                public void whiteboardEventReceived(Event event) {
                                    handleLayoutChange();
                                }
                            }, Event.RESIZED);
                        };

                        private void handleLayoutChange() {
                            layout.initLayout(effectsView.getWidth(), effectsView.getHeight(),
                                    settingsManager.getSettings(), MainActivity.this);
                            gameDeckView.setX(layout.deckLocations[Table.GAME_DECK_INDEX].x);
                            gameDeckView.setY(layout.deckLocations[Table.GAME_DECK_INDEX].y);
                            statsManager.centerWinViewContent();

                            new LoadImagesTask(MainActivity.this, layout, settingsManager.getSettings()) {
                                @Override
                                protected void onPostExecute(String result) {
                                    decorateGameBackground();
                                    effectsView.setBackgroundDrawable(new BitmapDrawable(getResources(),
                                            layout.gameBackground));
                                    mover.restoreTableState();
                                };
                            }.execute(true, false);
                        }
                    }.execute();
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

    private void unpause() {
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

    public Layout getLayout() {
        return layout;
    }

    public ImageView getCardView(int tag) {
        return cardView[tag];
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public MenuManager getMenuManager() {
        return menuManager2;
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

    public Bitmap getCardBack() {
        return cardBack;
    }

    public JSONStorage getStorage() {
        return storage;
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

    public Solver getSolver() {
        return solver;
    }
}
