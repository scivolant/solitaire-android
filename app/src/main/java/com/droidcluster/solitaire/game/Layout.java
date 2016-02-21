package com.droidcluster.solitaire.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.droidcluster.solitaire.model.Deck;
import com.droidcluster.solitaire.model.Table;

public class Layout {

    public static class DragEndInfo {
        public static final int NOT_IN_AREA = -1;

        public int internalDeckIndex = NOT_IN_AREA;
        public boolean isInFoundationsArea;
        public boolean isInTableauArea;
    }

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    public int viewOrientation = HORIZONTAL;
    public final Point cardSize = new Point();
    /**
     * the rect-radius for the card rectangles
     */
    public final Point cardRadius = new Point();
    public final Point cardSpacing = new Point();
    public final Point cardSpacingBig = new Point();
    public final Point gameSize = new Point();
    public final Point gameLoc = new Point();
    public final Point availableSize = new Point();
    public int fontSize;
    public final Point[] deckLocations = new Point[Table.ALL_DECS_INTERNAL_COUNT];
    public final Rect statsLoc = new Rect();
    public Bitmap cardBackground;
    public Bitmap gameBackground;
    public final Bitmap[] suits = new Bitmap[4];

    public void initLayout(final int width, final int height, Settings settings, Context context) {
        Layout layout = this;
        layout.availableSize.x = width;
        layout.availableSize.y = height;

        layout.viewOrientation = width > height ? Layout.HORIZONTAL : Layout.VERTICAL;

        Point foundationsLoc = new Point();
        Point sourcesLoc = new Point();
        Point gameDeckLoc = new Point();
        Point tableauLoc = new Point();
        if (layout.viewOrientation == Layout.HORIZONTAL) {
            // card size
            layout.cardSize.x = (int) ((width - 20) / 12.5);
            layout.cardSize.y = (int) ((height - 20) / 4.75);
            adjustCardSizeAndRadius(layout);

            computeCardSpacing(layout);

            // center game on screen
            layout.gameSize.x = (int) (11.5 * layout.cardSize.x + 2 * layout.cardSpacingBig.x + 8 * layout.cardSpacing.x);
            layout.gameSize.y = (int) (4.5 * cardSize.y) + 4 * cardSpacing.y;
            layout.gameLoc.x = (width - layout.gameSize.x) / 2;
            layout.gameLoc.y = (height - layout.gameSize.y) / 2;

            // UI elements locations
            if (!settings.leftHand) {
                foundationsLoc.x = layout.gameLoc.x;
                foundationsLoc.y = layout.gameLoc.y;

                tableauLoc.x = (int) (foundationsLoc.x + 1.5 * layout.cardSize.x) + layout.cardSpacingBig.x;
                tableauLoc.y = foundationsLoc.y;

                gameDeckLoc.x = foundationsLoc.x + gameSize.x - cardSize.x;
                gameDeckLoc.y = foundationsLoc.y;
                // sourcesLoc.x = foundationsLoc.x + 8 * layout.cardSize.x + 6 * layout.cardSpacing.x + 2
                // * layout.cardSpacingBig.x;
                // sourcesLoc.y = foundationsLoc.y;
            } else {
                foundationsLoc.x = gameLoc.x + gameSize.x - cardSize.x;
                foundationsLoc.y = gameLoc.y;

                tableauLoc.x = (int) (foundationsLoc.x - cardSpacingBig.x - 7.5 * cardSize.x - 6 * cardSpacing.x);
                tableauLoc.y = gameLoc.y;

                gameDeckLoc.x = gameLoc.x;
                gameDeckLoc.y = gameLoc.y;
            }
        } else {
            // card size
            layout.cardSize.x = (int) (width / (7 + (float) 8 / 14));
            layout.cardSize.y = (height - 20) / 6;
            adjustCardSizeAndRadius(layout);

            computeCardSpacing(layout);

            // position game on screen
            layout.gameSize.x = 7 * layout.cardSize.x + 6 * layout.cardSpacing.x;
            layout.gameSize.y = 6 * layout.cardSize.y;
            layout.gameLoc.x = (width - layout.gameSize.x) / 2;
            layout.gameLoc.y = (height - layout.gameSize.y) / 3;

            // UI elements locations
            if (!settings.leftHand) {
                foundationsLoc.x = layout.gameLoc.x;
                foundationsLoc.y = layout.gameLoc.y;

                tableauLoc.x = foundationsLoc.x;
                tableauLoc.y = foundationsLoc.y + layout.cardSize.y + layout.cardSpacingBig.y;

                sourcesLoc.x = foundationsLoc.x + 5 * layout.cardSize.x + 5 * layout.cardSpacing.x;
                sourcesLoc.y = foundationsLoc.y;
            } else {
                sourcesLoc.x = layout.gameLoc.x;
                sourcesLoc.y = layout.gameLoc.y;

                tableauLoc.x = sourcesLoc.x;
                tableauLoc.y = sourcesLoc.y + layout.cardSize.y + layout.cardSpacingBig.y;

                foundationsLoc.x = sourcesLoc.x + 3 * layout.cardSize.x + 3 * layout.cardSpacing.x;
                foundationsLoc.y = sourcesLoc.y;
            }
        }

        // init deckLocations
        int i = 0;
        for (; i < Table.FOUNDATION_DECKS_COUNT; i++) {
            layout.deckLocations[i] = computeLocForIndex(i, foundationsLoc, getFoundationsOrientation(layout), layout);
        }
        for (; i < Table.FOUNDATION_DECKS_COUNT + Table.TABLEAU_DECKS_COUNT; i++) {
            layout.deckLocations[i] = computeLocForIndex(i - Table.FOUNDATION_DECKS_COUNT, tableauLoc,
                    Layout.HORIZONTAL, layout);
        }
        if (!settings.leftHand) {
            if (viewOrientation == HORIZONTAL) {
                layout.deckLocations[Table.GAME_DECK_INDEX] = gameDeckLoc;
                layout.deckLocations[Table.WASTE_INDEX] = new Point(gameDeckLoc);
                int tableauEnd = tableauLoc.x + 7 * cardSize.x + 6 * cardSpacing.x;
                layout.deckLocations[Table.WASTE_INDEX].x = tableauEnd + (gameDeckLoc.x - tableauEnd - cardSize.x) / 2;
            } else {
                layout.deckLocations[Table.WASTE_INDEX] = sourcesLoc;
                layout.deckLocations[Table.GAME_DECK_INDEX] = computeLocForIndex(1, sourcesLoc,
                        getSourcesOrientation(), layout);
                if (layout.viewOrientation == VERTICAL) {
                    layout.deckLocations[Table.WASTE_INDEX].x -= (layout.cardSize.x + layout.cardSpacing.x) / 2;
                }
            }
        } else {
            if (viewOrientation == HORIZONTAL) {
                layout.deckLocations[Table.GAME_DECK_INDEX] = gameDeckLoc;
                layout.deckLocations[Table.WASTE_INDEX] = new Point(gameDeckLoc);
                int gameDeckEnd = gameDeckLoc.x + cardSize.x;
                layout.deckLocations[Table.WASTE_INDEX].x = gameDeckEnd + (tableauLoc.x - gameDeckEnd - cardSize.x) / 2;
            } else {
                layout.deckLocations[Table.WASTE_INDEX] = computeLocForIndex(1, sourcesLoc, getSourcesOrientation(),
                        layout);
                layout.deckLocations[Table.GAME_DECK_INDEX] = sourcesLoc;
                if (layout.viewOrientation == VERTICAL) {
                    layout.deckLocations[Table.WASTE_INDEX].x += (layout.cardSize.x + layout.cardSpacing.x) / 2;
                }
            }
        }

        layout.statsLoc.top = layout.deckLocations[5].y + layout.cardSize.y;
        int statsLocWidth = 5 * (layout.cardSize.x + layout.cardSpacing.x);
        layout.statsLoc.left = (width - statsLocWidth) / 2;
        layout.statsLoc.right = layout.statsLoc.left + statsLocWidth;
    }

    private static void computeCardSpacing(Layout layout) {
        layout.cardSpacing.x = layout.cardSize.x / 14;
        layout.cardSpacing.y = layout.cardSize.y / 14;
        layout.cardSpacingBig.x = (int) (2.5 * layout.cardSpacing.x);
        layout.cardSpacingBig.y = (int) (7.5 * layout.cardSpacing.y);
    }

    private static void adjustCardSizeAndRadius(Layout layout) {
        if (layout.cardSize.x * 3 / 2 > layout.cardSize.y) {
            layout.cardSize.x = layout.cardSize.y * 2 / 3;
        } else {
            layout.cardSize.y = layout.cardSize.x * 3 / 2;
        }

        layout.cardRadius.x = layout.cardRadius.y = (int) (layout.cardSize.x * 0.1);
        layout.fontSize = layout.cardSize.y / 3 - 6;
    }

    private static Point computeLocForIndex(int index, Point initialLoc, int orientation, Layout layout) {
        Point loc = new Point();
        loc.x = initialLoc.x;
        loc.y = initialLoc.y;
        if (orientation == HORIZONTAL) {
            loc.x += index * (layout.cardSize.x + layout.cardSpacing.x);
        } else {
            loc.y += index * (layout.cardSize.y + layout.cardSpacing.y);
        }
        return loc;
    }

    private static int getFoundationsOrientation(Layout layout) {
        return layout.viewOrientation == HORIZONTAL ? VERTICAL : HORIZONTAL;
    }

    private static int getSourcesOrientation() {
        return HORIZONTAL;
    }

    // TODO remove Table param
    public static DragEndInfo computeDragEndInfo(int x, int y, Table table, Layout layout) {
        DragEndInfo dragEndInfo = new DragEndInfo();

        Point wasteLoc = layout.deckLocations[Table.WASTE_INDEX];
        if (cardSizedRectangle(wasteLoc, layout).contains(x, y)) {
            if (table.getWaste().getCardsCount() > 0) {
                dragEndInfo.internalDeckIndex = Table.WASTE_INDEX;
            }
            return dragEndInfo;
        }

        Point gameDeckLoc = layout.deckLocations[Table.GAME_DECK_INDEX];
        if (cardSizedRectangle(gameDeckLoc, layout).contains(x, y)) {
            dragEndInfo.internalDeckIndex = Table.GAME_DECK_INDEX;
            return dragEndInfo;
        }

        // check tableau area
        Point tableauLoc = layout.deckLocations[Table.FOUNDATION_DECKS_COUNT];
        dragEndInfo.isInTableauArea = x >= tableauLoc.x
                && x <= tableauLoc.x + 7 * layout.cardSize.x + 6 * layout.cardSpacing.x && y >= tableauLoc.y;
        if (dragEndInfo.isInTableauArea) {
            for (int i = 0; i < Table.TABLEAU_DECKS_COUNT; i++) {
                int curDeckLoc = tableauLoc.x + i * (layout.cardSize.x + layout.cardSpacing.x);
                if (x >= curDeckLoc && x <= curDeckLoc + layout.cardSize.x) {
                    dragEndInfo.internalDeckIndex = i + Table.FOUNDATION_DECKS_COUNT;
                    return dragEndInfo;
                }
            }
        }

        // check foundations area
        Point foundationsLoc = layout.deckLocations[0];
        if (layout.viewOrientation == Layout.VERTICAL) {
            dragEndInfo.isInFoundationsArea = x >= foundationsLoc.x
                    && x <= foundationsLoc.x + 4 * layout.cardSize.x + 3 * layout.cardSpacing.x
                    && y <= foundationsLoc.y + layout.cardSize.y && y >= foundationsLoc.y;
        } else {
            dragEndInfo.isInFoundationsArea = x >= foundationsLoc.x && x <= foundationsLoc.x + layout.cardSize.x
                    && y >= foundationsLoc.y
                    && y <= foundationsLoc.y + 4 * layout.cardSize.y + 3 * layout.cardSpacing.y;
        }
        if (dragEndInfo.isInFoundationsArea) {
            if (layout.viewOrientation == Layout.VERTICAL) {
                for (int i = 0; i < Table.FOUNDATION_DECKS_COUNT; i++) {
                    int curDeckLoc = foundationsLoc.x + i * (layout.cardSize.x + layout.cardSpacing.x);
                    if (x >= curDeckLoc && x <= curDeckLoc + layout.cardSize.x) {
                        dragEndInfo.internalDeckIndex = i;
                        return dragEndInfo;
                    }
                }
            } else {
                for (int i = 0; i < Table.FOUNDATION_DECKS_COUNT; i++) {
                    int curDeckLoc = foundationsLoc.y + i * (layout.cardSize.y + layout.cardSpacing.y);
                    if (y >= curDeckLoc && y <= curDeckLoc + layout.cardSize.y) {
                        dragEndInfo.internalDeckIndex = i;
                        return dragEndInfo;
                    }
                }
            }
        }

        return dragEndInfo;
    }

    public static Rect cardSizedRectangle(Point loc, Layout layout) {
        return new Rect(loc.x, loc.y, loc.x + layout.cardSize.x, loc.y + layout.cardSize.y);
    }

    public int computeCardIndexByYOffset(Deck deck, int clickY, int deckLocY) {
        if (clickY < deckLocY) {
            return -1;
        }

        int curY = deckLocY;
        for (int i = deck.getCardsCount() - 1; i >= 0; i--) {
            int cardHeight;
            if (deck.isCardOpen(i)) {
                cardHeight = getPartiallyCoveredOpenCard();
            } else {
                cardHeight = getPartiallyCoveredBackCard();
            }
            if (i == 0) {
                cardHeight = cardSize.y;
            }

            if (clickY >= curY && clickY < curY + cardHeight) {
                return i;
            }
            curY += cardHeight;
        }

        return -1;
    }

    public int computeDeckYOffset(Deck deck, int cardIndex) {
        int openCards = deck.getOpenCardsCount();
        int coveredCards = deck.getCardsCount() - openCards;
        openCards -= (cardIndex + 1);
        if (openCards < 0) {
            coveredCards += openCards;
            openCards = 0;
        }

        return coveredCards * getPartiallyCoveredBackCard() + openCards * getPartiallyCoveredOpenCard();
    }

    public int getPartiallyCoveredBackCard() {
        return cardSize.y / 5;
    }

    public int getPartiallyCoveredOpenCard() {
        return cardSize.y / 3;
    }

    public int getDrawThreeWasteOffset() {
        return cardSize.x / 3;
    }
}