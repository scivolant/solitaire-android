package com.droidcluster.solitaire.game;

import java.util.ArrayList;
import java.util.List;

import com.droidcluster.solitaire.model.Card;
import com.droidcluster.solitaire.model.Deck;
import com.droidcluster.solitaire.model.IMove2;
import com.droidcluster.solitaire.model.RecycleWasteMove;
import com.droidcluster.solitaire.model.Table;

public class Solver {

    // public static void main(String[] args) {
    // System.out.println("Start");
    // long startTime = System.currentTimeMillis();
    //
    // final int games = 10000;
    // final int maxMoves = 300;
    //
    // int solved = 0;
    // int bestMoves = maxMoves + 1;
    // for (int i = 0; i < games; i++) {
    // Table table = new Table();
    // // String asString = table.toString();
    //
    // Solver solver = new Solver();
    // if (solver.solve(table, maxMoves)) {
    // solved++;
    // if (table.getHistory().size() < bestMoves) {
    // bestMoves = table.getHistory().size();
    // }
    // // System.out.println(asString);
    // // System.out.println("Solved in " + table.getHistory().size()
    // // + " moves.");
    // }
    // // System.out.println(table);
    // }
    //
    // System.out.println("Solved " + solved + " out of " + games + " (" + 100 * (float) solved / games + "%)");
    // System.out.println("Best moves: " + bestMoves);
    // System.out.println("Done in " + (System.currentTimeMillis() - startTime) / 1000 + "s.");
    // }

    public void autoComplete(Table table) {
        if (!canAutoComplete(table)) {
            return;
        }

        Deck hand = new Deck();
        List<Card> cardsToFlip = new ArrayList<Card>();
        while (!table.isSolved()) {
            IMove2 m = getHint(table);
            m.begin(table, hand, cardsToFlip);
            m.complete(table, hand, cardsToFlip);
            cardsToFlip.clear();
            table.getHistory().add(m);
        }
    }

    public boolean canAutoComplete(Table table) {
        Deck waste = table.getWaste();
        Deck gameDeck = table.getGameDeck();
        if (table.isDrawThree() && waste.getCardsCount() + gameDeck.getCardsCount() > 1) {
            return false;
        }

        int allCount = 0;
        for (Deck deck : table.getTableau()) {
            int cardsCount = deck.getCardsCount();
            allCount += cardsCount;
            if (deck.getOpenCardsCount() < cardsCount) {
                return false;
            }
        }

        for (Deck d : table.getFoundations()) {
            allCount += d.getCardsCount();
        }
        allCount += gameDeck.getCardsCount();
        allCount += waste.getCardsCount();
        return allCount == 52;
    }

    private boolean solve(Table table, int maxMoves) {
        List<Card> cardsToFlip = new ArrayList<Card>();
        Deck hand;
        for (int i = 0; i < maxMoves; i++) {
            cardsToFlip.clear();
            hand = new Deck();

            IMove2 hint = getHint(table);
            if (hint == null) {
                return false;
            }

            // System.out.println("\n\n\n");
            // System.out.println(table);
            // System.out.println("hint: "
            // + hint
            // + " ("
            // + table.getDeck(hint.getFromDeckIndex()).getCardAt(
            // hint.getCardIndex()) + " -> "
            // + table.getDeck(hint.getToDeckIndex()).getCardAt(0) + ")");
            hint.begin(table, hand, cardsToFlip);
            hint.complete(table, hand, cardsToFlip);
            table.getHistory().add(hint);

            if (table.isSolved()) {
                return true;
            }
        }

        return false;
    }

    public IMove2 getHint(Table table) {
        List<IMove2> possibleMoves = table.getPossibleMoves();

        // something to foundation?
        for (IMove2 move : possibleMoves) {
            if (move.getToDeckIndex() < Table.FOUNDATION_DECKS_COUNT
                    && move.getFromDeckIndex() >= Table.FOUNDATION_DECKS_COUNT) {
                return move;
            }
        }

        // something leading to revealing a card?
        for (IMove2 move : possibleMoves) {
            if (Table.isInTableau(move.getFromDeckIndex())) {
                Deck fromDeck = table.getDeck(move.getFromDeckIndex());
                if (move.getCardIndex() + 1 == fromDeck.getOpenCardsCount()
                        && fromDeck.getOpenCardsCount() < fromDeck.getCardsCount()) {
                    return move;
                }
            }
        }

        // something leading to freeing a foundation deck?
        for (IMove2 move : possibleMoves) {
            if (Table.isInTableau(move.getFromDeckIndex())) {
                Deck fromDeck = table.getDeck(move.getFromDeckIndex());
                if (fromDeck.getCardAt(move.getCardIndex()).numberValue() < 13
                        && move.getCardIndex() + 1 == fromDeck.getCardsCount()) {
                    return move;
                }
            }
        }

        // something from waste to tableau?
        for (IMove2 move : possibleMoves) {
            if (move.getFromDeckIndex() == Table.WASTE_INDEX && Table.isInTableau(move.getToDeckIndex())) {
                return move;
            }
        }

        // game deck to waste?
        for (IMove2 move : possibleMoves) {
            if (move.getFromDeckIndex() == Table.GAME_DECK_INDEX) {
                return move;
            }
        }

        // recycle waste?
        for (IMove2 move : possibleMoves) {
            if (move instanceof RecycleWasteMove) {
                return move;
            }
        }

        return null;
    }

    public void initWinningGame(Table table) {
        Solver solver = new Solver();
        Table table2 = new Table();

        int tries = table.isDrawThree() ? 5 : 10;
        for (int i = 0; i < tries; i++) {
            table.init();
            table2.initFrom(table);
            if (solver.solve(table2, 300)) {
                break;
            }
            table.reset();
        }

        if (table.getDeck(Table.GAME_DECK_INDEX).getCardsCount() == 52) {
            // could not generate winning game, generating a random one
            table.init();
        }
    }
}
