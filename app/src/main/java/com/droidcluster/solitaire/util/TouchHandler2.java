package com.droidcluster.solitaire.util;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public abstract class TouchHandler2 implements OnTouchListener {
    private int clickToDragTolerance = 20;
    private int touchX;
    private int touchY;
    private boolean dragging;

    public boolean onTouch(View v, MotionEvent event) {
        int x = Math.round(event.getX());
        int y = Math.round(event.getY());

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            touchX = x;
            touchY = y;
            break;
        case MotionEvent.ACTION_MOVE:
            if (!dragging) {
                // distinguish 'click' from 'drag'
                if (Math.abs(x - touchX) > clickToDragTolerance || Math.abs(y - touchY) > clickToDragTolerance) {
                    dragging = true;
                    dragStart(touchX, touchY);
                }
            }
            if (dragging) {
                drag(x, y);
            }
            break;
        case MotionEvent.ACTION_UP:
            if (dragging) {
                dragging = false;
                dragEnd();
            } else {
                click(x, y);
            }
            break;
        default:
            return false;
        }

        return true;
    }

    protected void dragStart(int touchX2, int touchY2) {
    };

    protected void drag(int x, int y) {
    };

    protected void dragEnd() {
    };

    protected void click(int x, int y) {
    };
}
