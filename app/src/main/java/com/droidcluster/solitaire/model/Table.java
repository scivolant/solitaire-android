package com.droidcluster.solitaire.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class Table {
    public static final int FOUNDATION_DECKS_COUNT = 4;
    public static final int TABLEAU_DECKS_COUNT = 7;
    public static final int ALL_DECS_INTERNAL_COUNT = FOUNDATION_DECKS_COUNT + TABLEAU_DECKS_COUNT + 2;

    public static final int WASTE_INDEX = ALL_DECS_INTERNAL_COUNT - 2;
    public static final int GAME_DECK_INDEX = ALL_DECS_INTERNAL_COUNT - 1;

    public static boolean isInTableau(int internalDeckIndex) {
        return internalDeckIndex >= FOUNDATION_DECKS_COUNT
                && internalDeckIndex < FOUNDATION_DECKS_COUNT + TABLEAU_DECKS_COUNT;
    }

    private static final int UNLIMITED = -1;

    private boolean drawThree = false;
    private int allowedRecycles = UNLIMITED;

    private Deck gameDeck;
    private Deck waste;
    private Deck[] foundations;
    private Deck[] tableau;
    private final Stack<IMove2> history = new Stack<IMove2>();
    private long time;
    private int points;

    public Table() {
        reset();
        init();
    }

    public void reset() {
        history.clear();
        gameDeck = new Deck(Card.values());
        waste = new Deck();
        // 4
        foundations = new Deck[] { new Deck(), new Deck(), new Deck(), new Deck() };
        // 7
        tableau = new Deck[] { new Deck(), new Deck(), new Deck(), new Deck(), new Deck(), new Deck(), new Deck() };
        // set cards
        gameDeck.shuffle();
        time = 0;
        points = 0;
    }

    public void init() {
        for (int i = 0; i < Table.TABLEAU_DECKS_COUNT; i++) {
            for (int j = i; j < Table.TABLEAU_DECKS_COUNT; j++) {
                gameDeck.takeFromFirst(1, tableau[j]);
            }
            tableau[i].setOpenCardsCount(1);
            // TODO this is lame, optimize takeFromFirst
            tableau[i].reverse();
        }
    }

    public void initFrom(Table table) {
        for (int i = 0; i < ALL_DECS_INTERNAL_COUNT; i++) {
            getDeck(i).clear();
            getDeck(i).copyToFirst(table.getDeck(i));
        }
        drawThree = table.isDrawThree();
    }

    public void setDrawThree(boolean drawThree) {
        this.drawThree = drawThree;
    }

    public boolean isDrawThree() {
        return drawThree;
    }

    public int getAllowedRecycles() {
        return allowedRecycles;
    }

    public void setAllowedRecycles(int allowedRecycles) {
        this.allowedRecycles = allowedRecycles;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        if (points < 0) {
            this.points = 0;
        } else {
            this.points = points;
        }
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Deck getGameDeck() {
        return gameDeck;
    }

    public Deck getWaste() {
        return waste;
    }

    public Deck[] getFoundations() {
        return foundations;
    }

    public Deck[] getTableau() {
        return tableau;
    }

    public List<IMove2> getPossibleMoves() {
        Deck[] decks = getAllDecksInternal();
        List<IMove2> result = new ArrayList<IMove2>();
        for (int sourceDeckIndex = 0; sourceDeckIndex < decks.length; sourceDeckIndex++) {
            Deck deck = decks[sourceDeckIndex];
            if (deck.getCardsCount() == 0) {
                continue;
            }
            if (Table.isInTableau(sourceDeckIndex)) {
                for (int cardIndex = 0; cardIndex < deck.getOpenCardsCount(); cardIndex++) {
                    Card card = deck.getCardAt(cardIndex);
                    for (Integer foundationInternalDeckIndex : getPossibleMoveTargets(sourceDeckIndex, card)) {
                        result.add(new Move(sourceDeckIndex, cardIndex, foundationInternalDeckIndex));
                    }
                }
            } else {
                int cardIndex = 0;
                Card card = deck.getCardAt(cardIndex);
                for (Integer foundationInternalDeckIndex : getPossibleMoveTargets(sourceDeckIndex, card)) {
                    result.add(new Move(sourceDeckIndex, cardIndex, foundationInternalDeckIndex));
                }
            }
        }
        if (waste.getCardsCount() > 0 && gameDeck.getCardsCount() == 0) {
            result.add(new RecycleWasteMove());
        }
        return result;
    }

    /**
     * @return the information about the made undo or <code>null</code> if no move has been made on this table
     */
    public IMove2 undo() {
        if (history.isEmpty()) {
            return null;
        }

        IMove2 m = history.pop();
        Deck hand = new Deck();
        List<Card> cardsToFlip = new ArrayList<Card>();
        m.beginUndo(this, hand, cardsToFlip);
        m.completeUndo(this, hand, cardsToFlip);
        return m;
    }

    /**
     * @return <code>true</code> if all cards are on the foundation (every suit foundation contains 13 cards)
     */
    public boolean isSolved() {
        for (Deck f : getFoundations()) {
            if (f.getCardsCount() != 13) {
                return false;
            }
        }
        return true;
    }

    public boolean isMovePossible(int fromDeckIndex, int cardIndex, int toDeckIndex) {
        int cardsCount = cardIndex + 1;
        if (cardsCount > 1 && (!isInTableau(fromDeckIndex) || !isInTableau(toDeckIndex))) {
            // moving several cards is possible only across tableau decks
            return false;
        }

        Card card = getDeck(fromDeckIndex).getCardAt(cardIndex);
        Deck toDeck = getDeck(toDeckIndex);

        if (toDeckIndex < FOUNDATION_DECKS_COUNT && cardsCount == 1) {
            if (toDeck.getCardsCount() == 0) {
                return card.numberValue() == 1; // ace
            }

            return toDeck.getCardAt(0).getSuit() == card.getSuit()
                    && toDeck.getCardAt(0).numberValue() + 1 == card.numberValue();
        }

        if (isInTableau(toDeckIndex)) {
            if (toDeck.getCardsCount() == 0) {
                return card.numberValue() == 13; // king
            }

            Card top = toDeck.getCardAt(0);
            boolean differentColors = card.isRed() ^ top.isRed();
            return differentColors && top.numberValue() == card.numberValue() + 1;
        }

        return false;
    }

    public int[] findCard(Card card) {
        int[] p = new int[2];
        Deck[] allDecksInternal = getAllDecksInternal();
        for (p[0] = 0; p[0] < allDecksInternal.length; p[0]++) {
            Deck deck = allDecksInternal[p[0]];
            for (p[1] = 0; p[1] < deck.getCardsCount(); p[1]++) {
                if (deck.getCardAt(p[1]) == card) {
                    return p;
                }
            }
        }

        return null;
    }

    public List<Integer> getPossibleMoveTargets(int sourceDeckIndex, int cardIndex) {
        Card card = getAllDecksInternal()[sourceDeckIndex].getCardAt(cardIndex);
        return getPossibleMoveTargets(sourceDeckIndex, card);
    }

    public List<Integer> getPossibleMoveTargets(int sourceDeckIndex, Card card) {
        // for the source
        if (sourceDeckIndex == 12) {
            // open covered source
            return Arrays.asList(11);
        }
        Deck[] decks = getAllDecksInternal();
        List<Integer> result = new ArrayList<Integer>();

        // for the foundations
        if (decks[sourceDeckIndex].getCardAt(0) == card) {
            // we can only move the top card to the foundation
            for (int i = 0; i < FOUNDATION_DECKS_COUNT; i++) {
                Deck deck = decks[i];
                if (deck.getCardsCount() == 0 && card.numberValue() == 1) {
                    result.add(i);
                    continue;
                } else if (deck.getCardsCount() > 0 && deck.getCardAt(0).getSuit() == card.getSuit()
                        && deck.getCardAt(0).numberValue() + 1 == card.numberValue()) {
                    result.add(i);
                }
            }
        }

        // for the tableau
        for (int i = 4; i < 11; i++) {
            Deck deck = decks[i];
            if (deck.getCardsCount() == 0) {
                if (card.numberValue() == 13) {
                    // king opens new deck
                    result.add(i);
                }
                continue;
            }
            Card top = deck.getCardAt(0);
            boolean differentColors = card.isRed() ^ top.isRed();
            if (top.numberValue() == card.numberValue() + 1 && differentColors) {
                result.add(i);
            }
        }
        return result;
    }

    public Deck[] getAllDecksInternal() {
        return new Deck[] {
                // - the 4 foundations
                foundations[0], foundations[1], foundations[2], foundations[3],
                // - the 7 tableau decks
                tableau[0], tableau[1], tableau[2], tableau[3], tableau[4], tableau[5], tableau[6],
                // - source open
                waste,
                // - source covered
                gameDeck, };
    }

    public Deck getDeck(int internalDeckIndex) {
        return getAllDecksInternal()[internalDeckIndex];
    }

    public Stack<IMove2> getHistory() {
        return history;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('\n');
        boolean haveCards = true;
        for (int i = 0; haveCards && i < 52; i++) {
            haveCards = false;
            if (i < 4) {
                Card c = foundations[i].getCardAt(0);
                b.append(c != null ? c : "[]");
            }
            b.append("\t\t");
            for (Deck d : tableau) {
                int cardIndex = d.getCardsCount() - i - 1;
                Card c = d.getCardAt(cardIndex);
                if (c != null) {
                    haveCards = true;
                    b.append(cardIndex < d.getOpenCardsCount() ? c : "^" + c);
                }
                b.append('\t');
            }
            if (i == 0) {
                b.append('\t');
                Card c = waste.getCardAt(0);
                b.append(c != null ? c : "[]");
                b.append('\t');
                c = gameDeck.getCardAt(0);
                b.append(c != null ? "^" + c : "[]");
            }
            b.append('\n');
            haveCards |= i < 4;
        }
        return b.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(foundations);
        result = prime * result + ((gameDeck == null) ? 0 : gameDeck.hashCode());
        result = prime * result + ((history == null) ? 0 : history.hashCode());
        result = prime * result + points;
        result = prime * result + Arrays.hashCode(tableau);
        result = prime * result + (int) (time ^ (time >>> 32));
        result = prime * result + ((waste == null) ? 0 : waste.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Table other = (Table) obj;
        if (!Arrays.equals(foundations, other.foundations))
            return false;
        if (gameDeck == null) {
            if (other.gameDeck != null)
                return false;
        } else if (!gameDeck.equals(other.gameDeck))
            return false;
        if (history == null) {
            if (other.history != null)
                return false;
        } else if (!history.equals(other.history))
            return false;
        if (points != other.points)
            return false;
        if (!Arrays.equals(tableau, other.tableau))
            return false;
        if (time != other.time)
            return false;
        if (waste == null) {
            if (other.waste != null)
                return false;
        } else if (!waste.equals(other.waste))
            return false;
        return true;
    }
}
