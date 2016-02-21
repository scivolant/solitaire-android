package com.droidcluster.solitaire.model;

public class Stats {
    private int gamesPlayed;
    private int bestMoves;
    private int gamesWon;
    private long timePlayedSec;
    private long bestTime;
    private int totalMoves;
    private int bestPoints;
    private int strike;

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getBestMoves() {
        return bestMoves;
    }

    public void setBestMoves(int bestMoves) {
        this.bestMoves = bestMoves;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public long getTimePlayedSec() {
        return timePlayedSec;
    }

    public void setTimePlayedSec(long timePlayedSec) {
        this.timePlayedSec = timePlayedSec;
    }

    public long getBestTime() {
        return bestTime;
    }

    public void setBestTime(long bestTime) {
        this.bestTime = bestTime;
    }

    public int getTotalMoves() {
        return totalMoves;
    }

    public void setTotalMoves(int totalMoves) {
        this.totalMoves = totalMoves;
    }

    public int getBestPoints() {
        return bestPoints;
    }

    public void setBestPoints(int bestPoints) {
        this.bestPoints = bestPoints;
    }

    public int getStrike() {
        return strike;
    }

    public void setStrike(int strike) {
        this.strike = strike;
    }

    public void gameFinished(Table table) {
        gamesPlayed++;
        timePlayedSec += table.getTime();
        totalMoves += table.getHistory().size();

        if (table.isSolved()) {
            gamesWon++;
            if (bestMoves == 0 || bestMoves > table.getHistory().size()) {
                // TODO new best moves
                bestMoves = table.getHistory().size();
            }
            if (bestTime == 0 || bestTime > table.getTime()) {
                // TODO new best time
                bestTime = table.getTime();
            }
            if (bestPoints < table.getPoints()) {
                // TODO new best points
                bestPoints = table.getPoints();
            }

            strike = Math.max(0, strike + 1);
        } else {
            strike = Math.min(0, strike - 1);
        }
    }
}
