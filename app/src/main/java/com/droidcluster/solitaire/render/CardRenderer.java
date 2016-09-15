package com.droidcluster.solitaire.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.renderscript.Type;

import com.droidcluster.solitaire.R;
import com.droidcluster.solitaire.game.Layout;
import com.droidcluster.solitaire.model.Card;
import com.droidcluster.solitaire.model.Deck;
import com.droidcluster.solitaire.util.ImageLoader;

public class CardRenderer extends Renderer {

    protected Paint emptyDeckPaint;
    private Paint cardBgBasePaint;
    private Paint cardTextRedPaint;
    private Paint cardTextBlackPaint;
    private Paint cardBorderPaint;
    private Paint gradientPaint;
    private final RectF cardRect = new RectF();
    private final RectF gradientRect = new RectF();
    protected Paint foundationPaint;

    public CardRenderer() {
        emptyDeckPaint = new Paint();
        emptyDeckPaint.setColor(Color.WHITE);
        emptyDeckPaint.setAlpha(64);
        emptyDeckPaint.setStyle(Style.STROKE);

        foundationPaint = new Paint(emptyDeckPaint);
        emptyDeckPaint.setStrokeWidth(5);

        gradientPaint = new Paint();
        gradientPaint.setStyle(Style.FILL);

        cardBgBasePaint = new Paint();
        cardBgBasePaint.setColor(Color.WHITE);
        cardBgBasePaint.setStyle(Style.FILL);

        cardTextRedPaint = new Paint();
        cardTextRedPaint.setColor(Color.RED);

        cardTextBlackPaint = new Paint();
        cardTextBlackPaint.setColor(Color.BLACK);

        cardBorderPaint = new Paint();
        cardBorderPaint.setColor(Color.BLACK);
        cardBorderPaint.setStyle(Style.STROKE);
    }

    @Override
    public void setLayout(Layout layout) {
        super.setLayout(layout);

        float[] positions = { 0.5f, 0.99f };
        gradientPaint.setShader(new RadialGradient(3 * layout.cardSize.x / 5, layout.cardSize.y / 10,
                layout.cardSize.y, new int[] { 0x00000000, 0x40000000 }, positions, Shader.TileMode.CLAMP));

        cardTextBlackPaint.setTextSize(layout.fontSize);
        cardTextBlackPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        cardTextRedPaint.setTextSize(layout.fontSize);
        cardTextRedPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        foundationPaint.setTextSize(layout.fontSize);
    }

    public void drawDeckCompact(Deck deck, Point loc, Canvas canvas) {
        if (deck.getCardsCount() == 0) {
            drawEmptyDeck(loc, canvas);
        } else if (deck.getOpenCardsCount() == 0) {
            drawCardBack(loc, canvas);
        } else {
            drawCard(deck.getCardAt(0), loc, canvas);
        }
    }

    public void drawDeckPartiallyShown(Deck deck, Point loc, Canvas canvas) {
        final int cardsCount = deck.getCardsCount();
        if (cardsCount == 0) {
            drawEmptyDeck(loc, canvas);
        } else {
            for (int j = cardsCount - 1; j >= 0; j--) {
                boolean isCardOpen = j < deck.getOpenCardsCount();
                int y = computeYForCardIndex(deck, j, loc);
                if (isCardOpen) {
                    drawCard(deck.getCardAt(j), loc.x, y, canvas);
                } else {
                    drawCardBack(loc.x, y, canvas);
                }
            }
        }
    }

    private void drawCard(Card card, Point loc, Canvas canvas) {
        drawCard(card, loc.x, loc.y, canvas);
    }

    private void drawCard(Card card, int locx, int locy, Canvas canvas) {
        cardRect.set(locx, locy, locx + layout.cardSize.x, locy + layout.cardSize.y);
        gradientRect.set(locx, locy, locx + layout.cardSize.x, locy + layout.cardSize.y / 2);

        // background
        canvas.drawRoundRect(cardRect, layout.cardRadius.x, layout.cardRadius.y, cardBgBasePaint);

        // text
        int padding = 2;
        int y = locy + layout.fontSize - 2 * padding;
        int x = locx + padding;
        canvas.drawText(card.getNumberSymbol(), x, y, card.isRed() ? cardTextRedPaint : cardTextBlackPaint);
        Bitmap sb = layout.suits[card.getSuit()];
        canvas.drawBitmap(sb, locx + layout.cardSize.x - sb.getWidth() - 2 * padding, locy + 2 * padding, null);
        // canvas.drawBitmap(sb, x, locy + 10 * padding + sb.getHeight(), null);
        // canvas.drawBitmap(sb, x, locy + layout.cardSize.y - 2 * padding - sb.getHeight(), null);

        // draw picture
        int reqWidth = layout.cardSize.x - sb.getWidth();
        int imgLocx = locx + (layout.cardSize.x - reqWidth) / 2;
        // if (card.ordinal() % 13 == Card.ACE_VALUE) {
        int drawableId = getArtDrawableId(card.ordinal());
        Bitmap bigSuitImage = ImageLoader.getImageFromApp(drawableId, reqWidth, -1, resources);
        if (bigSuitImage != null) {
            canvas.drawBitmap(bigSuitImage, imgLocx, locy + (layout.cardSize.y + y - locy - bigSuitImage.getHeight())
                    / 2, null);
        }

        // if (card.ordinal() % 13 == Card.KING_VALUE) {
        // int reqHeight = layout.cardSize.y - layout.fontSize * 3 / 4 - padding;
        // Bitmap b = ImageLoader.getImageFromApp(card.isRed() ? R.drawable.k2 : R.drawable.k9, -1, reqHeight,
        // resources);
        //
        // x = locx + (layout.cardSize.x - b.getWidth()) / 2; // center picture on card
        // y -= layout.fontSize / 4;
        // canvas.drawBitmap(b, x, y, null);

        // } else if (card.ordinal() % 13 == Card.VALE_VALUE) {
        // Bitmap valeBitmap = ImageLoader.getImageFromApp(R.drawable.vale0, 3 * reqWidth, -1, resources);
        // if (valeBitmap != null) {
        // int imgLoc = locx + (layout.cardSize.x - 3 * reqWidth) / 2;
        // canvas.drawBitmap(valeBitmap, imgLoc, locy + layout.getPartiallyCoveredOpenCard(), null);
        // }
        // } else {
        // y += 2 * layout.fontSize;
        // float textWidth = cardHugeTextRedPaint.measureText(card.getNumberSymbol());
        // Paint motivePaint = card.isRed() ? cardHugeTextRedPaint : cardHugeTextBlackPaint;
        // canvas.drawText(card.getNumberSymbol(), locx + layout.cardSize.x - textWidth, y, motivePaint);
        // cardRect.top = y;
        // canvas.drawRoundRect(cardRect, layout.cardRadius.x, layout.cardRadius.y, motivePaint);
        // }

        // border
        cardRect.top = locy;
        canvas.drawRoundRect(cardRect, layout.cardRadius.x, layout.cardRadius.y, cardBorderPaint);

        // gradient
        canvas.drawRoundRect(cardRect, layout.cardRadius.x, layout.cardRadius.y, gradientPaint);
    }

    private int getArtDrawableId(int cardValue) {
        switch (cardValue / 13) {
        case 0:
            return R.drawable.suit0;
        case 1:
            return R.drawable.suit1;
        case 2:
            return R.drawable.suit2;
        default:
            return R.drawable.suit3;
        }
    }

    private void drawCardBack(Point loc, Canvas canvas) {
        drawCardBack(loc.x, loc.y, canvas);
    }

    private void drawCardBack(int locx, int locy, Canvas canvas) {
        RectF cardRect = new RectF(locx, locy, locx + layout.cardSize.x, locy + layout.cardSize.y);

        if (layout.cardBackground != null) {
            canvas.drawBitmap(layout.cardBackground, locx, locy, null);
        } else {
            // background
            canvas.drawRoundRect(cardRect, layout.cardRadius.x, layout.cardRadius.y, cardBgBasePaint);

            // motive
            canvas.drawLine(locx + layout.cardRadius.x, locy + layout.cardRadius.y, locx + layout.cardSize.x
                    - layout.cardRadius.x, locy + layout.cardSize.y - layout.cardRadius.y, cardBorderPaint);
            canvas.drawLine(locx + layout.cardRadius.x, locy + layout.cardSize.y - layout.cardRadius.y, locx
                    + layout.cardSize.x - layout.cardRadius.x, locy + layout.cardRadius.y, cardBorderPaint);
        }

        // border
        canvas.drawRoundRect(cardRect, layout.cardRadius.x, layout.cardRadius.y, cardBorderPaint);

        // gradient
        canvas.drawRoundRect(cardRect, layout.cardRadius.x, layout.cardRadius.y, gradientPaint);
    }

    private void drawEmptyDeck(Point loc, Canvas canvas) {
        canvas.drawRoundRect(new RectF(loc.x, loc.y, loc.x + layout.cardSize.x, loc.y + layout.cardSize.y),
                layout.cardRadius.x, layout.cardRadius.y, emptyDeckPaint);
    }

    private int computeYForCardIndex(Deck deck, int cardIndex, Point deckLoc) {
        return deckLoc.y + layout.computeDeckYOffset(deck, cardIndex);
    }

}
