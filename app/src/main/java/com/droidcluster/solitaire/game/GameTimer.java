package com.droidcluster.solitaire.game;

public class GameTimer {
    private boolean paused;
    private boolean stopped;

    private long startTime;
    private long pauseTime;

    public void start(int timeoutMillis) {
        startTime = System.currentTimeMillis() + timeoutMillis;
    }

    public void start() {
        start(0);
    }

    public void stop() {
        stopped = true;
        pause();
    }

    public void continueAndUnpause() {
        stopped = false;
        unpause();
    }

    public boolean pause() {
        if (paused) {
            return false;
        }

        paused = true;
        pauseTime = System.currentTimeMillis();
        return true;
    }

    public void unpause() {
        if (stopped || !paused) {
            return;
        }

        paused = false;
        startTime += (System.currentTimeMillis() - pauseTime);
    }

    public void setTime(long time) {
        long now = System.currentTimeMillis();
        startTime = now - time;
        if (paused) {
            pauseTime = now;
        }
    }

    public long getTime() {
        if (paused) {
            return Math.max(0, pauseTime - startTime);
        }
        return System.currentTimeMillis() - startTime;
    }
}
