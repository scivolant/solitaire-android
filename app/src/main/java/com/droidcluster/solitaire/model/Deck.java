package com.droidcluster.solitaire.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author shiba
 * 
 */
public class Deck {
    private static final int ARR_SIZE = 52;

    private final Card[] arr = new Card[ARR_SIZE];
    private int startIndex;
    private int cardsCount;
    private int openCardsCount;

    public Deck() {
        this(new Card[0]);
    }

    public Deck(Card... cards) {
        cardsCount = cards.length;
        System.arraycopy(cards, 0, this.arr, 0, cardsCount);
    }

    public void shuffle() {
        List<Card> list = getCardsAsList();
        Collections.shuffle(list);
        int i = 0;
        for (Card card : list) {
            arr[(startIndex + i++) % ARR_SIZE] = card;
        }
    }

    public void reverse() {
        List<Card> list = getCardsAsList();
        Collections.reverse(list);
        int i = 0;
        for (Card card : list) {
            arr[(startIndex + i++) % ARR_SIZE] = card;
        }
    }

    public void clear() {
        cardsCount = 0;
        openCardsCount = 0;
    }

    public void addCard(Card card) {
        arr[(startIndex + cardsCount) % ARR_SIZE] = card;
        cardsCount++;
    }

    public void takeFromFirst(int cardsCount, Deck hand) {
        if (cardsCount <= 0) {
            return;
        }

        if (cardsCount > this.cardsCount) {
            // take all cards
            cardsCount = this.cardsCount;
        }

        int nextHandIndex = hand.startIndex + hand.cardsCount;
        int newStartIndex = startIndex;
        for (int i = 0; i < cardsCount; i++) {
            hand.arr[(nextHandIndex++) % ARR_SIZE] = arr[(newStartIndex++) % ARR_SIZE];
        }
        hand.cardsCount += cardsCount;
        hand.openCardsCount = Math.min(openCardsCount, cardsCount);

        this.cardsCount -= cardsCount;
        startIndex = newStartIndex % ARR_SIZE;
        setOpenCardsCount(Math.min(this.cardsCount, openCardsCount - cardsCount));
    }

    /**
     * the same like {@link #putFirst(Deck)}, but keeps also the cards in the hand
     */
    public void copyToFirst(Deck hand) {
        int newStartIndex = startIndex;
        for (int i = hand.cardsCount - 1; i >= 0; i--) {
            if (--newStartIndex < 0) {
                newStartIndex += ARR_SIZE;
            }
            arr[newStartIndex] = hand.getCardAtUnchecked(i);
        }
        startIndex = newStartIndex;
        cardsCount += hand.cardsCount;
        openCardsCount += hand.openCardsCount;
    }

    public void putFirst(Deck hand) {
        copyToFirst(hand);
        hand.cardsCount = 0;
        hand.openCardsCount = 0;
    }

    public Card getCardAt(int i) {
        if (i >= cardsCount || i < 0) {
            return null;
        }
        return getCardAtUnchecked(i);
    }

    public boolean isCardOpen(int i) {
        return i < openCardsCount;
    }

    public int getCardsCount() {
        return cardsCount;
    }

    public int getOpenCardsCount() {
        return openCardsCount;
    }

    /**
     * @deprecated This method is inefficient!
     */
    public List<Card> getCardsAsList() {
        List<Card> list = new ArrayList<Card>();
        for (int i = 0; i < cardsCount; i++) {
            list.add(getCardAtUnchecked(i));
        }
        return list;
    }

    private Card getCardAtUnchecked(int i) {
        return arr[(startIndex + i) % ARR_SIZE];
    }

    public void setOpenCardsCount(int openCardsCount) {
        if (openCardsCount < 0) {
            openCardsCount = 0;
        }
        this.openCardsCount = openCardsCount;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Deck)) {
            return false;
        }
        Deck otherDeck = (Deck) obj;
        return openCardsCount == otherDeck.openCardsCount && getCardsAsList().equals(otherDeck.getCardsAsList());
    }

    @Override
    public int hashCode() {
        return (1 + openCardsCount) * getCardsAsList().hashCode();
    }

    @Override
    public String toString() {
        return getCardsAsList().toString();
    }
}
