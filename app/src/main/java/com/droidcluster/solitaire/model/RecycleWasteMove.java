package com.droidcluster.solitaire.model;

import java.util.List;

public class RecycleWasteMove extends AbstractMove {
    private static final int POINTS = -100;

    @Override
    public void beginUndo(Table table, Deck hand, List<Card> cardsToFlip) {
        // nothing to do
    }

    @Override
    public void completeUndo(Table table, Deck hand, List<Card> cardsToFlip) {
        hand.clear();

        table.getWaste().putFirst(table.getGameDeck());
        table.getGameDeck().setOpenCardsCount(0);
        table.getWaste().setOpenCardsCount(52);
        table.getWaste().reverse();

        subtractPoints(table);

        // cardsToFlip
        for (int i = 0; i < table.getWaste().getCardsCount(); i++) {
            cardsToFlip.add(table.getWaste().getCardAt(i));
        }
    }

    @Override
    public void begin(Table table, Deck hand, List<Card> cardsToFlip) {
        hand.clear();
        cardIndex = table.getWaste().getCardsCount() - 1;
        Deck waste = table.getWaste();
        for (int i = 0; i < waste.getCardsCount(); i++) {
            cardsToFlip.add(waste.getCardAt(i));
        }
    }

    @Override
    public void complete(Table table, Deck hand, List<Card> cardsToFlip) {
        table.getGameDeck().putFirst(table.getWaste());
        table.getGameDeck().setOpenCardsCount(0);
        table.getWaste().setOpenCardsCount(52);
        table.getGameDeck().reverse();

        addPoints(POINTS, table);
    }

    @Override
    public int getFromDeckIndex() {
        return Table.WASTE_INDEX;
    }

    @Override
    public int getToDeckIndex() {
        return Table.GAME_DECK_INDEX;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getPointsGiven();
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
        RecycleWasteMove other = (RecycleWasteMove) obj;
        if (getPointsGiven() != other.getPointsGiven())
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Recycle waste";
    }
}
