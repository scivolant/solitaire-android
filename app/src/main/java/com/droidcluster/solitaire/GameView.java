package com.droidcluster.solitaire;

import com.droidcluster.solitaire.Whiteboard.Event;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class GameView extends FrameLayout {

    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Whiteboard.post(Event.RESIZED);
    }
}
