package com.droidcluster.solitaire;

import android.animation.Animator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.droidcluster.solitaire.game.JSONStorage;
import com.droidcluster.solitaire.game.Layout;
import com.droidcluster.solitaire.game.Settings;
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

/**
 * Created by jordan on 19.05.2016.
 */
public class InitTask extends AsyncTask<MainActivity, Void, Object> {
    private final MainActivity mainActivity;
    private TableRenderer cardRenderer;
    private ImageView gameDeckView;

    public InitTask(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected Object doInBackground(MainActivity... params) {
        JSONStorage storage = new JSONStorage(mainActivity.getFilesDir());
        mainActivity.setStorage(storage);
        int animationTimeMs = mainActivity.getResources().getInteger(android.R.integer.config_shortAnimTime);
        mainActivity.setAnimationTimeMs(animationTimeMs);
        Settings settings = mainActivity.getSettingsManager().getSettings();
        Table table = storage.loadOrCreateTable(settings.drawThree);
        mainActivity.setTable(table);
        Stats stats = storage.loadOrCreateStats(table.isDrawThree());
        if (table.getHistory().size() == 0 && stats.getGamesPlayed() == 0) {
            // let the user win the first game
            table.reset();
            mainActivity.getSolver().initWinningGame(table);
            storage.saveTable(table);
        } else {
            if (table.isSolved()) {
                table.reset();
                if (stats.getGamesPlayed() < 4) {
                    // let the user win the first 3 games
                    mainActivity.getSolver().initWinningGame(table);
                } else {
                    table.init();
                }
                storage.saveTable(table);
            }
        }
        mainActivity.getTimer().stop();
        mainActivity.getTimer().setTime(table.getTime());
        Layout layout = mainActivity.getLayout();
        layout.initLayout(mainActivity.getEffectsView().getWidth(), mainActivity.getEffectsView().getHeight(), settings);
        int x = layout.fontSize * 3 / 4;
        layout.suits[0] = ImageLoader.getImageFromApp(R.drawable.suit0, x, x, mainActivity.getResources());
        layout.suits[1] = ImageLoader.getImageFromApp(R.drawable.suit1, x, x, mainActivity.getResources());
        layout.suits[2] = ImageLoader.getImageFromApp(R.drawable.suit2, x, x, mainActivity.getResources());
        layout.suits[3] = ImageLoader.getImageFromApp(R.drawable.suit3, x, x, mainActivity.getResources());
        cardRenderer = new TableRenderer();
        cardRenderer.setResources(mainActivity.getResources());
        cardRenderer.setLayout(layout);
        new LoadImagesTask(mainActivity, layout, settings).doInBackground(false, false);
        decorateGameBackground();

        // draw cards
        int width = layout.cardSize.x + 2;
        int height = layout.cardSize.y + 2;
        for (Card c : Card.values()) {
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Deck deck = new Deck(c);
            deck.setOpenCardsCount(1);
            cardRenderer.drawDeckCompact(deck, new Point(0, 0), new Canvas(bmp));
            mainActivity.setCardBitmap(c.ordinal(), bmp);
        }

        Bitmap cardBack = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        cardRenderer.drawDeckCompact(new Deck(Card.H1), new Point(0, 0), new Canvas(cardBack));
        mainActivity.setCardBack(cardBack);

        attachWhiteboardListeners();
        return null;
    }

    private void decorateGameBackground() {
        Layout layout = mainActivity.getLayout();
        if (layout.gameBackground == null) {
            return;
        }
        Bitmap copy = layout.gameBackground.copy(Bitmap.Config.ARGB_8888, true);
        ImageLoader.recycleChecked(layout.gameBackground);
        layout.gameBackground = copy;
        Canvas bgCanvas = new Canvas(layout.gameBackground);
        for (int i = 0; i < Table.FOUNDATION_DECKS_COUNT; i++) {
            cardRenderer.drawFoundationSpot(layout.deckLocations[i], bgCanvas);
        }
    }

    private void attachWhiteboardListeners() {
        Whiteboard.addListener(new UpdateStatsListener(mainActivity), Whiteboard.Event.WON, Whiteboard.Event.LOST);
        Whiteboard.addListener(new ShowAutofinishButtonListener(mainActivity),
                Whiteboard.Event.GAME_STARTED, Whiteboard.Event.MOVED);
        Whiteboard.addListener(new HintController(mainActivity), Whiteboard.Event.OFFERED_AUTOFINISH,
                Whiteboard.Event.MOVED, Whiteboard.Event.GAME_STARTED);
        Whiteboard.addListener(new Whiteboard.WhiteboardListener() {
            @Override
            public void whiteboardEventReceived(Whiteboard.Event event) {
                new LoadImagesTask(mainActivity, mainActivity.getLayout(), mainActivity.getSettingsManager().getSettings()) {
                    @Override
                    protected void onPostExecute(String result) {
                        decorateGameBackground();
                        mainActivity.getEffectsView().setBackgroundDrawable(new BitmapDrawable(mainActivity.getResources(),
                                mainActivity.getLayout().gameBackground));
                    }
                }.execute(true, false);
            }
        }, Whiteboard.Event.GAME_BG_SET);
        Whiteboard.addListener(new Whiteboard.WhiteboardListener() {
            @Override
            public void whiteboardEventReceived(Whiteboard.Event event) {
                new LoadImagesTask(mainActivity, mainActivity.getLayout(), mainActivity.getSettingsManager().getSettings()) {
                    @Override
                    protected void onPostExecute(String result) {
                        decorateGameBackground();
                        mainActivity.getEffectsView().setBackgroundDrawable(new BitmapDrawable(mainActivity.getResources(),
                                mainActivity.getLayout().gameBackground));

                        int width = mainActivity.getLayout().cardSize.x + 2;
                        int height = mainActivity.getLayout().cardSize.y + 2;
                        Bitmap cardBack = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        cardRenderer.drawDeckCompact(new Deck(Card.H1), new Point(0, 0), new Canvas(cardBack));
                        mainActivity.setCardBack(cardBack);

                        for (int i = 0; i < Card.values().length; i++) {
                            if (!mainActivity.isCardRevealed(i)) {
                                mainActivity.getCardView(i).setImageBitmap(cardBack);
                            }
                        }
                    }
                }.execute(false, true);
            }
        }, Whiteboard.Event.CARD_BG_SET);
        Whiteboard.addListener(new Whiteboard.WhiteboardListener() {
            @Override
            public void whiteboardEventReceived(Whiteboard.Event event) {
                handleLayoutChange();
            }
        }, Whiteboard.Event.LEFT_HAND_SET);
        Whiteboard.addListener(new Whiteboard.WhiteboardListener() {
            @Override
            public void whiteboardEventReceived(Whiteboard.Event event) {
                mainActivity.getTable().setDrawThree(mainActivity.getSettingsManager().getSettings().drawThree);
                mainActivity.getMover().fixWasteIfDrawThree(0).start();
            }
        }, Whiteboard.Event.DRAW_THREE_SET);
    }

    @Override
    protected void onPostExecute(Object result) {
        Layout layout = mainActivity.getLayout();
        FrameLayout effectsView = mainActivity.getEffectsView();

        effectsView.addView(new View(mainActivity));
        effectsView
                .setBackgroundDrawable(new BitmapDrawable(mainActivity.getResources(), layout.gameBackground));
        mainActivity.getMenuController().updateMenu();

        int width = layout.cardSize.x + 2;
        int height = layout.cardSize.y + 2;
        // create game deck location image
        gameDeckView = new ImageView(mainActivity);
        effectsView.addView(gameDeckView);
        gameDeckView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
        gameDeckView.setX(layout.deckLocations[Table.GAME_DECK_INDEX].x);
        gameDeckView.setY(layout.deckLocations[Table.GAME_DECK_INDEX].y);
        Bitmap emptyDeck = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        cardRenderer.drawDeckCompact(new Deck(), new Point(0, 0), new Canvas(emptyDeck));
        gameDeckView.setImageBitmap(emptyDeck);
        gameDeckView.setOnTouchListener(new TouchHandler2() {
            protected void click(int x, int y) {
                Table table = mainActivity.getTable();
                if (table.getWaste().getCardsCount() > 0
                        && table.getGameDeck().getCardsCount() == 0) {
                    Animator moveAnim = mainActivity.getMover().move(new RecycleWasteMove());
                    moveAnim.start();
                } else if (table.getWaste().getCardsCount() == 0
                        && table.getGameDeck().getCardsCount() > 0) {
                    IMove2 move = new Move(Table.GAME_DECK_INDEX, 0, Table.WASTE_INDEX);
                    Animator moveAnim = mainActivity.getMover().move(move);
                    moveAnim.start();
                }
            }
        });
        attachResizeListener();

        for (final Card c : Card.values()) {
            final ImageView imageView = new ImageView(mainActivity);
            effectsView.addView(imageView);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
            imageView.setX(layout.deckLocations[Table.GAME_DECK_INDEX].x);
            imageView.setY(layout.deckLocations[Table.GAME_DECK_INDEX].y);
            imageView.setImageBitmap(mainActivity.getCardBack());
            imageView.setTag(c.ordinal());
            CardTouchListener l = new CardTouchListener(mainActivity, c);
            imageView.setOnTouchListener(l);
            Whiteboard.addListener(l, Whiteboard.Event.WON, Whiteboard.Event.GAME_STARTED);
            mainActivity.setCardView(c.ordinal(), imageView);
        }

        mainActivity.getMover().restoreTableState();

        // position win view
        mainActivity.getStatsManager().centerWinViewContent();

        mainActivity.unpause();
        installScoreViewUpdater();

        mainActivity.getWelcomeController().initComplete();
    }

    private void installScoreViewUpdater() {
        ScoreViewUpdater u = new ScoreViewUpdater(mainActivity);
        Whiteboard.addListener(u, Whiteboard.Event.MOVED, Whiteboard.Event.GAME_STARTED, Whiteboard.Event.LOST, Whiteboard.Event.WON,
                Whiteboard.Event.PAUSED, Whiteboard.Event.UNPAUSED);
    }

    private void attachResizeListener() {
        Whiteboard.addListener(new Whiteboard.WhiteboardListener() {
            @Override
            public void whiteboardEventReceived(Whiteboard.Event event) {
                handleLayoutChange();
            }
        }, Whiteboard.Event.RESIZED);
    }

    private void handleLayoutChange() {
        mainActivity.findViewById(R.id.hint).setVisibility(View.GONE);
        mainActivity.findViewById(R.id.game_submenu).setVisibility(View.GONE);
        final Layout layout = mainActivity.getLayout();
        final View effectsView = mainActivity.getEffectsView();
        layout.initLayout(effectsView.getWidth(), effectsView.getHeight(),
                mainActivity.getSettingsManager().getSettings());
        gameDeckView.setX(layout.deckLocations[Table.GAME_DECK_INDEX].x);
        gameDeckView.setY(layout.deckLocations[Table.GAME_DECK_INDEX].y);
        mainActivity.getMover().restoreTableState();
        mainActivity.getStatsManager().centerWinViewContent();
        effectsView.setBackgroundDrawable(null);

        new LoadImagesTask(mainActivity, layout, mainActivity.getSettingsManager().getSettings()) {
            @Override
            protected void onPostExecute(String result) {
                decorateGameBackground();
                effectsView.setBackgroundDrawable(new BitmapDrawable(mainActivity.getResources(),
                        layout.gameBackground));
            }
        }.execute(true, false);
    }
}
