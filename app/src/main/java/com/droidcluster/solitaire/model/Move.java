package com.droidcluster.solitaire.model;

import java.util.ArrayList;
import java.util.List;

public class Move extends AbstractMove {
    public static class Flip {

        public final int internalDeckIndex;
        public final int oldOpenCardsCount;

        public Flip(int internalDeckIndex, int oldOpenCardsCount) {
            this.internalDeckIndex = internalDeckIndex;
            this.oldOpenCardsCount = oldOpenCardsCount;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + internalDeckIndex;
            result = prime * result + oldOpenCardsCount;
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
            Flip other = (Flip) obj;
            if (internalDeckIndex != other.internalDeckIndex)
                return false;
            if (oldOpenCardsCount != other.oldOpenCardsCount)
                return false;
            return true;
        }

    }

    public final int fromDeckIndex;
    public int toDeckIndex = -1;

    // set when performing the move
    public int cardsToMoveCount;
    public List<Flip> flips = new ArrayList<Move.Flip>();

    public Move(int sourceInternalDeckIndex, int cardIndex, int targetInternalDeckIndex) {
        this.fromDeckIndex = sourceInternalDeckIndex;
        this.cardIndex = cardIndex;
        this.toDeckIndex = targetInternalDeckIndex;
    }

    public Move(int sourceInternalDeckIndex, int cardIndex) {
        this.fromDeckIndex = sourceInternalDeckIndex;
        this.cardIndex = cardIndex;
    }

    @Override
    public String toString() {
        return "" + fromDeckIndex + '[' + cardIndex + "] -> " + toDeckIndex;
    }

    @Override
    public void begin(Table table, Deck hand, List<Card> cardsToFlip) {
        Deck fromDeck = table.getDeck(fromDeckIndex);

        boolean deck2waste = fromDeckIndex == Table.GAME_DECK_INDEX && toDeckIndex == Table.WASTE_INDEX;
        if (deck2waste) {
            cardIndex = table.isDrawThree() ? Math.min(2, fromDeck.getCardsCount() - 1) : 0;
        }

        hand.clear();
        cardsToMoveCount = cardIndex + 1;
        fromDeck.takeFromFirst(cardsToMoveCount, hand);

        if (deck2waste) {
            hand.reverse();
            hand.setOpenCardsCount(cardsToMoveCount);
            cardsToFlip.addAll(hand.getCardsAsList());
        }
    }

    @Override
    public void abort(Table table, Deck hand) {
        boolean deck2waste = fromDeckIndex == Table.GAME_DECK_INDEX && toDeckIndex == Table.WASTE_INDEX;
        if (deck2waste) {
            hand.reverse();
        }

        table.getDeck(fromDeckIndex).putFirst(hand);

        if (deck2waste) {
            table.getGameDeck().setOpenCardsCount(0);
        }
    }

    @Override
    public void complete(Table table, Deck hand, List<Card> cardsToFlip) {
        Deck fromSource = table.getDeck(fromDeckIndex);
        int sourceCoveredCardsBefore = fromSource.getCardsCount() - fromSource.getOpenCardsCount();
        Deck toTarget = table.getDeck(toDeckIndex);
        toTarget.putFirst(hand);

        // fix open cards counts
        flips = new ArrayList<Flip>();
        if (toDeckIndex == Table.WASTE_INDEX) {
            table.getWaste().setOpenCardsCount(52);
            flips.add(new Flip(Table.WASTE_INDEX, 52));
            flips.add(new Flip(Table.GAME_DECK_INDEX, 0));
        } else {
            flips.add(new Flip(fromDeckIndex, fromSource.getOpenCardsCount() + cardsToMoveCount));
            if (fromDeckIndex != Table.GAME_DECK_INDEX && fromSource.getCardsCount() > 0
                    && fromSource.getOpenCardsCount() == 0) {
                fromSource.setOpenCardsCount(1);
                cardsToFlip.add(fromSource.getCardAt(0));
            }

            flips.add(new Flip(toDeckIndex, toTarget.getOpenCardsCount() - cardsToMoveCount));
        }

        int sourceCoveredCardsNow = fromSource.getCardsCount() - fromSource.getOpenCardsCount();
        // points
        addPoints(determinePoints(sourceCoveredCardsBefore, sourceCoveredCardsNow), table);
        // table.setPoints(table.getPoints() + determinePoints(sourceCoveredCardsBefore, sourceCoveredCardsNow));
    }

    protected int determinePoints(int sourceCoveredCardsBefore, int sourceCoveredCardsNow) {
        if (fromDeckIndex == Table.WASTE_INDEX) {
            // waste to foundation
            if (toDeckIndex < Table.FOUNDATION_DECKS_COUNT) {
                return 10;
            } else {
                // waste to tableau
                return 5;
            }
        }

        // foundation to tableau
        if (fromDeckIndex < Table.FOUNDATION_DECKS_COUNT && Table.isInTableau(toDeckIndex)) {
            return -15;
        }

        int result = 0;

        // tableau to foundation
        if (Table.isInTableau(fromDeckIndex) && toDeckIndex < Table.FOUNDATION_DECKS_COUNT) {
            result += 10;
        }

        // 5 points per flip
        if (sourceCoveredCardsBefore != sourceCoveredCardsNow) {
            for (Flip f : flips) {
                if (fromDeckIndex == f.internalDeckIndex && Table.isInTableau(fromDeckIndex)) {
                    result += 5;
                }
            }
        }

        return result;
    }

    @Override
    public void setTargetInternalDeckIndex(int targetInternalDeckIndex) {
        this.toDeckIndex = targetInternalDeckIndex;
    }

    @Override
    public void beginUndo(Table table, Deck hand, List<Card> cardsToFlip) {
        hand.clear();
        Deck moveTarget = table.getDeck(toDeckIndex);
        moveTarget.takeFromFirst(cardsToMoveCount, hand);

        // cardsToFlip
        if (Table.isInTableau(fromDeckIndex)) {
            // source was on the tableau -> at most one card was flipped
            if (flips != null) {
                for (Flip f : flips) {
                    if (f.internalDeckIndex == fromDeckIndex && f.oldOpenCardsCount == cardIndex + 1
                            && table.getDeck(fromDeckIndex).getCardsCount() > 0) {
                        cardsToFlip.add(table.getDeck(fromDeckIndex).getCardAt(0));
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void completeUndo(Table table, Deck hand, List<Card> cardsToFlip) {
        boolean deck2waste = fromDeckIndex == Table.GAME_DECK_INDEX && toDeckIndex == Table.WASTE_INDEX;
        if (deck2waste) {
            hand.reverse();
            cardsToFlip.addAll(hand.getCardsAsList());
        }

        Deck fromSource = table.getDeck(fromDeckIndex);
        fromSource.putFirst(hand);

        // revert open cards count
        if (flips != null) {
            for (Flip f : flips) {
                table.getDeck(f.internalDeckIndex).setOpenCardsCount(f.oldOpenCardsCount);
            }
        }

        // points
        subtractPoints(table);
    }

    @Override
    public int getFromDeckIndex() {
        return fromDeckIndex;
    }

    @Override
    public int getToDeckIndex() {
        return toDeckIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + cardIndex;
        result = prime * result + cardsToMoveCount;
        result = prime * result + ((flips == null) ? 0 : flips.hashCode());
        result = prime * result + getPointsGiven();
        result = prime * result + fromDeckIndex;
        result = prime * result + toDeckIndex;
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
        Move other = (Move) obj;
        if (cardIndex != other.cardIndex)
            return false;
        if (cardsToMoveCount != other.cardsToMoveCount)
            return false;
        if (flips == null) {
            if (other.flips != null)
                return false;
        } else if (!flips.equals(other.flips))
            return false;
        if (getPointsGiven() != other.getPointsGiven())
            return false;
        if (fromDeckIndex != other.fromDeckIndex)
            return false;
        if (toDeckIndex != other.toDeckIndex)
            return false;
        return true;
    }
}
