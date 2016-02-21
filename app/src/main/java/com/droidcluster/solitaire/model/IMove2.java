package com.droidcluster.solitaire.model;

import java.util.List;

public interface IMove2 {

    void begin(Table table, Deck hand, List<Card> cardsToFlip);

    void abort(Table table, Deck hand);

    void complete(Table table, Deck hand, List<Card> cardsToFlip);

    void setTargetInternalDeckIndex(int targetInternalDeckIndex);

    int getFromDeckIndex();

    int getToDeckIndex();

    int getCardIndex();

    void setCardIndex(int cardIndex);

    void beginUndo(Table table, Deck hand, List<Card> cardsToFlip);

    void completeUndo(Table table, Deck hand, List<Card> cardsToFlip);

    int getPointsGiven();
}
