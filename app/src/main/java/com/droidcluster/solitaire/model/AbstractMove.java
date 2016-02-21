package com.droidcluster.solitaire.model;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMove implements IMove2 {

    private int pointsGiven;
    public int cardIndex = -1;

    @Override
    public int getPointsGiven() {
        return pointsGiven;
    }

    public void setPointsGiven(int pointsGiven) {
        this.pointsGiven = pointsGiven;
    }

    public void perform(Table table, Deck hand) {
        List<Card> list = new ArrayList<Card>();
        begin(table, hand, list);
        complete(table, hand, list);
    }

    protected void addPoints(int points, Table table) {
        int tablePoints = table.getPoints();
        pointsGiven = Math.max(-tablePoints, points);
        table.setPoints(tablePoints + pointsGiven);
    }

    protected void subtractPoints(Table table) {
        table.setPoints(table.getPoints() - pointsGiven);
    }

    @Override
    public void setTargetInternalDeckIndex(int targetInternalDeckIndex) {
        // TODO Auto-generated method stub

    }

    @Override
    public void abort(Table table, Deck hand) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getCardIndex() {
        return cardIndex;
    }

    @Override
    public void setCardIndex(int cardIndex) {
        this.cardIndex = cardIndex;
    }
}
