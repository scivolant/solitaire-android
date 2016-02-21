package com.droidcluster.solitaire.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.RectF;

public class TableRenderer extends CardRenderer {
    private Paint backgroundPaint;

    public TableRenderer() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Style.FILL);
    }

    public void drawFoundationSpot(Point loc, Canvas canvas) {
        int x = loc.x;
        int y = loc.y;
        canvas.drawRoundRect(new RectF(x, y, x + layout.cardSize.x, y + layout.cardSize.y), layout.cardRadius.x,
                layout.cardRadius.y, emptyDeckPaint);

        Bitmap b = layout.suits[2];
        canvas.drawBitmap(b, x + (layout.cardSize.x - b.getWidth()) / 2, y + layout.fontSize / 4, foundationPaint);
        b = layout.suits[0];
        canvas.drawBitmap(b, x + (layout.cardSize.x - b.getWidth()) / 2, y + layout.cardSize.y - layout.fontSize
                - layout.fontSize / 4, foundationPaint);
        int midY = y + (layout.cardSize.y - layout.fontSize) / 2;
        b = layout.suits[1];
        canvas.drawBitmap(b, x + (layout.cardSize.x / 2 - b.getWidth() * 5 / 4), midY, foundationPaint);
        b = layout.suits[3];
        canvas.drawBitmap(b, x + (layout.cardSize.x / 2 + b.getWidth() / 4), midY, foundationPaint);
    }
}
