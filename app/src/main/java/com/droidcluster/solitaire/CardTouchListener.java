package com.droidcluster.solitaire;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.Whiteboard.WhiteboardListener;
import com.droidcluster.solitaire.game.Layout;
import com.droidcluster.solitaire.game.Layout.DragEndInfo;
import com.droidcluster.solitaire.model.Card;
import com.droidcluster.solitaire.model.Move;
import com.droidcluster.solitaire.model.Table;
import com.droidcluster.solitaire.util.TouchHandler2;

public class CardTouchListener extends TouchHandler2 implements WhiteboardListener {

    private final MainActivity mainActivity;
    private final Card card;

    private int[] startPos = new int[2];
    private int correctionX;
    private int correctionY;
    private int curX;
    private int curY;
    private int[] findCard;
    private boolean disabled;

    public CardTouchListener(MainActivity mainActivity, Card card) {
        this.mainActivity = mainActivity;
        this.card = card;
    }

    @Override
    protected void dragStart(int startX, int startY) {
        findCard = mainActivity.getTable().findCard(card);

        if (!canDrag()) {
            return;
        }
        mainActivity.getCardView(card.ordinal()).getLocationOnScreen(startPos);

        correctionX = startX;
        correctionY = startY;

        int sourceDeckIndex = findCard[0];
        if (sourceDeckIndex == Table.WASTE_INDEX) {
            mainActivity.getMover().fixWasteIfDrawThree(1).start();
        }

        int cardIndex = findCard[1];
        for (int i = cardIndex; i >= 0; i--) {
            Card card = mainActivity.getTable().getDeck(sourceDeckIndex).getCardAt(i);
            mainActivity.getCardView(card.ordinal()).bringToFront();
        }
    }

    private boolean canDrag() {
        if (!mainActivity.isCardRevealed(card.ordinal())) {
            return false;
        }

        int fromDeckIndex = findCard[0];
        if (fromDeckIndex == Table.GAME_DECK_INDEX) {
            return false;
        }

        int cardIndex = findCard[1];
        return cardIndex == 0 || Table.isInTableau(fromDeckIndex);
    }

    @Override
    protected void drag(int x, int y) {
        if (!canDrag()) {
            return;
        }

        int[] pos = new int[2];
        mainActivity.getCardView(card.ordinal()).getLocationOnScreen(pos);

        curX = pos[0] + x;
        curY = pos[1] + y;

        int deltaX = curX - (startPos[0] + correctionX);
        int deltaY = curY - (startPos[1] + correctionY);

        int sourceDeckIndex = findCard[0];
        int cardIndex = findCard[1];
        for (int i = cardIndex; i >= 0; i--) {
            Card card = mainActivity.getTable().getDeck(sourceDeckIndex).getCardAt(i);
            ImageView view = mainActivity.getCardView(card.ordinal());
            view.setX(startPos[0] + deltaX);
            view.setY(startPos[1] + deltaY + (cardIndex - i) * mainActivity.getLayout().getPartiallyCoveredOpenCard());
        }
    }

    @Override
    protected void dragEnd() {
        if (!canDrag()) {
            return;
        }

        Table table = mainActivity.getTable();
        DragEndInfo dragEndInfo = Layout.computeDragEndInfo(curX, curY, table, mainActivity.getLayout());

        boolean m = false;
        int sourceDeckIndex = findCard[0];
        if (dragEndInfo.isInFoundationsArea || dragEndInfo.isInTableauArea) {
            m = dragEndInfo.internalDeckIndex != -1 && dragEndInfo.internalDeckIndex != sourceDeckIndex;
        }

        int cardIndex = findCard[1];
        if (m && table.isMovePossible(sourceDeckIndex, cardIndex, dragEndInfo.internalDeckIndex)) {
            Move move = new Move(sourceDeckIndex, cardIndex, dragEndInfo.internalDeckIndex);
            Animator moveAnimator = mainActivity.getMover().move(move);
            moveAnimator.start();
        } else {
            ArrayList<ImageView> views = new ArrayList<ImageView>();
            // Point findCard = table.findCard(card);
            int deckIndex = findCard[0];
            for (int i = 0; i <= cardIndex; i++) {
                Card each = table.getDeck(deckIndex).getCardAt(i);
                ImageView view = mainActivity.getCardView(each.ordinal());
                views.add(view);
            }
            Animator returnCards = mainActivity.getMover().returnCards(views, deckIndex, cardIndex);
            returnCards.start();
        }
    }

    @Override
    protected void click(int x, int y) {
        Table table = mainActivity.getTable();
        findCard = table.findCard(card);
        int deckIndex = findCard[0];
        int cardIndex = findCard[1];

        if (deckIndex == Table.WASTE_INDEX && cardIndex > 0) {
            return;
        }

        if (Table.isInTableau(deckIndex) && !mainActivity.isCardRevealed(card.ordinal())) {
            return;
        }

        if (deckIndex == Table.GAME_DECK_INDEX) {
            Move move = new Move(deckIndex, 0, Table.WASTE_INDEX);
            Animator animator = mainActivity.getMover().move(move);
            animator.start();

            return;
        }

        List<Integer> possibleMoveTargets = table.getPossibleMoveTargets(deckIndex, card);
        if (!possibleMoveTargets.isEmpty()) {
            Move move = new Move(deckIndex, cardIndex, possibleMoveTargets.get(0));
            Animator animator = mainActivity.getMover().move(move);
            animator.start();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (disabled) {
            return false;
        }
        return super.onTouch(v, event);
    }

    @Override
    public void whiteboardEventReceived(Event event) {
        disabled = event == Event.WON;
    }
}
