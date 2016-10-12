package com.droidcluster.solitaire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.droidcluster.solitaire.Whiteboard.Event;
import com.droidcluster.solitaire.game.Layout;
import com.droidcluster.solitaire.model.Card;
import com.droidcluster.solitaire.model.Deck;
import com.droidcluster.solitaire.model.IMove2;
import com.droidcluster.solitaire.model.RecycleWasteMove;
import com.droidcluster.solitaire.model.Table;

public class Mover {

    private final MainActivity mainActivity;
    private AnimatorSet activeWinAnimation;
    private Animator autofinishAnimation;

    public Mover(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    private Animator recycleWaste(RecycleWasteMove move) {
        Table table = mainActivity.getTable();
        Layout layout = mainActivity.getLayout();
        Deck waste = table.getWaste();
        List<Animator> anims = new ArrayList<Animator>();
        for (int i = 0; i < waste.getCardsCount(); i++) {
            ImageView cardView = mainActivity.getCardView(waste.getCardAt(i).ordinal());
            anims.add(ObjectAnimator.ofFloat(cardView, "x", cardView.getX(),
                    layout.deckLocations[Table.GAME_DECK_INDEX].x));
            Animator flip = flipCard(waste.getCardAt(i).ordinal(), isLeftHand());
            flip.setDuration(mainActivity.getAnimationTimeMs());
            anims.add(flip);
        }

        move.perform(table, new Deck());
        table.getHistory().add(move);
        table.setTime(mainActivity.getTimer().getTime());
        mainActivity.getStorage().saveTable(table);
        Whiteboard.post(Event.MOVED);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(anims);
        set.setDuration(mainActivity.getAnimationTimeMs());
        set.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                updateGameDeck();
                invalidatePreKitkat();
            };
        });

        return set;
    }

    private boolean isLeftHand() {
        return mainActivity.getSettingsManager().getSettings().leftHand;
    }

    public Animator undo() {
        Table table = mainActivity.getTable();
        IMove2 move = table.getHistory().pop();

        Deck hand = new Deck();
        List<Card> cardsToFlip = new ArrayList<Card>();
        List<Animator> anims = new ArrayList<Animator>();

        // first step -> flip cards BEFORE moving
        move.beginUndo(table, hand, cardsToFlip);
        AnimatorSet initialFlips = null;
        if (!cardsToFlip.isEmpty()) {
            mainActivity.getMover().createFlipAnims(cardsToFlip, anims, true);
            initialFlips = new AnimatorSet();
            initialFlips.playTogether(anims);
        }

        anims.clear();
        cardsToFlip.clear();
        // second step -> flip cards WHILE moving
        move.completeUndo(table, hand, cardsToFlip);
        mainActivity.getStorage().saveTable(table);
        mainActivity.getMover().createFlipAnims(cardsToFlip, anims, move instanceof RecycleWasteMove ^ isLeftHand());
        Deck fromDeck = table.getDeck(move.getFromDeckIndex());
        Layout layout = mainActivity.getLayout();
        int toX = layout.deckLocations[move.getFromDeckIndex()].x;
        float toY = layout.deckLocations[move.getFromDeckIndex()].y;
        if (Table.isInTableau(move.getFromDeckIndex())) {
            toY += layout.computeDeckYOffset(fromDeck, move.getCardIndex());
        }
        if (move.getFromDeckIndex() == Table.WASTE_INDEX && table.isDrawThree()) {
            toX -= layout.getDrawThreeWasteOffset();
        }
        for (int i = move.getCardIndex(); i >= 0; i--) {
            ImageView view = mainActivity.getCardView(fromDeck.getCardAt(i).ordinal());
            view.bringToFront();
            anims.add(ObjectAnimator.ofFloat(view, "x", view.getX(), toX));
            anims.add(ObjectAnimator.ofFloat(view, "y", view.getY(), toY));
            if (Table.isInTableau(move.getFromDeckIndex())) {
                toY += layout.getPartiallyCoveredOpenCard();
            }
        }
        if ((move.getFromDeckIndex() == Table.WASTE_INDEX || move.getToDeckIndex() == Table.WASTE_INDEX)
                && table.isDrawThree()) {
            anims.add(fixWasteIfDrawThree(0));
        }

        AnimatorSet moveAnims = new AnimatorSet();
        moveAnims.playTogether(anims);
        moveAnims.setDuration(mainActivity.getAnimationTimeMs());

        Animator result;
        if (initialFlips != null) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(initialFlips, moveAnims);
            result = set;
        } else {
            result = moveAnims;
        }

        result.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                invalidatePreKitkat();
            }
        });

        return result;
    }

    public Animator move(IMove2 move) {
        mainActivity.getTimer().continueAndUnpause();
        if (move instanceof RecycleWasteMove) {
            return recycleWaste((RecycleWasteMove) move);
        }

        int toDeckIndex = move.getToDeckIndex();
        int cardsToMove = move.getCardIndex() + 1;
        final Layout layout = mainActivity.getLayout();
        final Table table = mainActivity.getTable();
        Point toDeckLoc = layout.deckLocations[toDeckIndex];
        List<Animator> moveAnims = new ArrayList<Animator>();
        Deck fromDeck = table.getDeck(move.getFromDeckIndex());
        final boolean drawingThree = table.isDrawThree() && toDeckIndex == Table.WASTE_INDEX;

        if (!drawingThree) {
            // animate cards movement
            Deck toDeck = table.getDeck(toDeckIndex);
            for (int i = cardsToMove - 1; i >= 0; i--) {
                ImageView cardView = mainActivity.getCardView(fromDeck.getCardAt(i).ordinal());
                moveAnims.add(ObjectAnimator.ofFloat(cardView, "x", cardView.getX(), toDeckLoc.x));

                float toY = toDeckLoc.y;
                if (Table.isInTableau(toDeckIndex)) {
                    toY += layout.computeDeckYOffset(toDeck, i - cardsToMove);
                }
                moveAnims.add(ObjectAnimator.ofFloat(cardView, "y", cardView.getY(), toY));

                cardView.bringToFront();
            }
        }

        Deck hand = new Deck();
        List<Card> cardsToFlip = new ArrayList<Card>();
        move.begin(table, hand, cardsToFlip);
        createFlipAnims(cardsToFlip, moveAnims, !isLeftHand());

        cardsToFlip.clear();
        move.complete(table, hand, cardsToFlip);
        table.getHistory().add(move);
        table.setTime(mainActivity.getTimer().getTime());
        mainActivity.getStorage().saveTable(table);
        if (table.isSolved()) {
            mainActivity.getTimer().pause();
            Whiteboard.post(Event.WON);
        }
        Whiteboard.post(Event.MOVED);

        if (drawingThree) {
            Deck waste = table.getWaste();
            int centerX = layout.deckLocations[Table.WASTE_INDEX].x;
            ImageView view0 = mainActivity.getCardView(waste.getCardAt(0).ordinal());
            moveAnims.add(ObjectAnimator.ofFloat(view0, "y", view0.getY(), toDeckLoc.y));
            switch (waste.getCardsCount()) {
            case 1:
                moveAnims.add(ObjectAnimator.ofFloat(view0, "x", view0.getX(), centerX));
                view0.bringToFront();
                break;
            case 2:
                moveAnims.add(ObjectAnimator.ofFloat(view0, "x", view0.getX(), centerX + layout.cardSize.x / 3));
                ImageView view1 = mainActivity.getCardView(waste.getCardAt(1).ordinal());
                moveAnims.add(ObjectAnimator.ofFloat(view1, "y", view1.getY(), toDeckLoc.y));
                moveAnims.add(ObjectAnimator.ofFloat(view1, "x", view1.getX(), centerX - layout.cardSize.x / 3));
                view1.bringToFront();
                view0.bringToFront();
                break;
            default:
                moveAnims.add(ObjectAnimator.ofFloat(view0, "x", view0.getX(), centerX + layout.cardSize.x / 3));
                view1 = mainActivity.getCardView(waste.getCardAt(1).ordinal());
                moveAnims.add(ObjectAnimator.ofFloat(view1, "y", view1.getY(), toDeckLoc.y));
                moveAnims.add(ObjectAnimator.ofFloat(view1, "x", view1.getX(), centerX));
                ImageView view2 = mainActivity.getCardView(waste.getCardAt(2).ordinal());
                moveAnims.add(ObjectAnimator.ofFloat(view2, "y", view2.getY(), toDeckLoc.y));
                moveAnims.add(ObjectAnimator.ofFloat(view2, "x", view2.getX(), centerX - layout.cardSize.x / 3));
                view2.bringToFront();
                view1.bringToFront();
                view0.bringToFront();
            }

        }
        AnimatorSet moveCard = new AnimatorSet();
        moveCard.playTogether(moveAnims);
        moveCard.setDuration(mainActivity.getAnimationTimeMs());
        Animator moveAnimator;
        if (cardsToFlip.isEmpty()) {
            moveAnimator = moveCard;
        } else {
            List<Animator> flipAnims = new ArrayList<Animator>();
            createFlipAnims(cardsToFlip, flipAnims, false);
            AnimatorSet flipCard = new AnimatorSet();
            flipCard.playTogether(flipAnims);

            AnimatorSet set = new AnimatorSet();
            set.playSequentially(moveCard, flipCard);
            moveAnimator = set;
        }

        if (table.isDrawThree() && move.getFromDeckIndex() == Table.WASTE_INDEX) {
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(moveAnimator, fixWasteIfDrawThree(0));
            animatorSet.setDuration(mainActivity.getAnimationTimeMs());
            moveAnimator = animatorSet;
        }

        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (drawingThree) {
                    Deck waste = table.getWaste();
                    for (int cardIndex = 3; cardIndex < waste.getCardsCount(); cardIndex++) {
                        int centerX = layout.deckLocations[Table.WASTE_INDEX].x;
                        ImageView view = mainActivity.getCardView(waste.getCardAt(cardIndex).ordinal());
                        view.setX(centerX - layout.cardSize.x / 3);
                    }
                }

                if (mainActivity.getTable().isSolved()) {
                    mainActivity.getStatsManager().storeGameStats();

                    Utils.showGameWonStuff(mainActivity);
                }
                invalidatePreKitkat();
            }

        });
        return moveAnimator;
    }

    public Animator deal() {
        ArrayList<Animator> anims = new ArrayList<Animator>();
        ArrayList<Animator> openAnims = new ArrayList<Animator>();

        for (int i = 0; i < Table.TABLEAU_DECKS_COUNT; i++) {
            Deck deck = mainActivity.getTable().getTableau()[i];
            Layout layout = mainActivity.getLayout();
            Point endLoc = layout.deckLocations[Table.FOUNDATION_DECKS_COUNT + i];
            Point p = new Point(endLoc);
            ImageView lastImageView = null;
            for (int cardIndex = deck.getCardsCount() - 1; cardIndex >= 0; cardIndex--) {
                ImageView imageView = mainActivity.getCardView(deck.getCardAt(cardIndex).ordinal());
                imageView.bringToFront();

                ObjectAnimator mx = ObjectAnimator.ofFloat(imageView, "x", imageView.getX(), p.x);
                mx.setInterpolator(new DecelerateInterpolator());
                long startDelay = (i - cardIndex) * mainActivity.getAnimationTimeMs() / 3;
                mx.setStartDelay(startDelay);
                anims.add(mx);
                ObjectAnimator my = ObjectAnimator.ofFloat(imageView, "y", imageView.getY(), p.y);
                my.setInterpolator(new DecelerateInterpolator());
                my.setStartDelay(startDelay);
                anims.add(my);

                p.y += layout.getPartiallyCoveredBackCard();
                lastImageView = imageView;
            }

            if (lastImageView == null) {
                continue;
            }

            int startDelay = i * mainActivity.getAnimationTimeMs() / 2;
            Animator animator = flipCard((Integer) lastImageView.getTag(), false);
            animator.setStartDelay(startDelay);
            openAnims.add(animator);
        }

        AnimatorSet animSetXY = new AnimatorSet();
        animSetXY.playTogether(anims);
        animSetXY.setDuration(2 * mainActivity.getAnimationTimeMs());

        AnimatorSet animSetOpen = new AnimatorSet();
        animSetOpen.setInterpolator(new LinearInterpolator());
        animSetOpen.playTogether(openAnims);

        AnimatorSet set = new AnimatorSet();
        set.playSequentially(animSetXY, animSetOpen);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                updateGameDeck();
                invalidatePreKitkat();
            }
        });
        mainActivity.getMenuController().updateMenu();

        return set;
    }

    public AnimatorSet showGameWonEffect() {
        List<Animator> anims = new ArrayList<Animator>();
        Layout layout = mainActivity.getLayout();

        for (int i = 0; i < Card.values().length; i++) {
            int suit = i / 13;

            final ImageView imageView = mainActivity.getCardView(i);
            List<Animator> cardAnims = new ArrayList<Animator>();
            int cardIndex = i % 13;
            ObjectAnimator down = ObjectAnimator.ofFloat(imageView, "y", imageView.getY(), layout.availableSize.y
                    + layout.cardSize.y);
            down.setDuration(20 * mainActivity.getAnimationTimeMs());
            cardAnims.add(down);

            ObjectAnimator right = ObjectAnimator.ofFloat(imageView, "x", imageView.getX(),
                    (((float) (1 + 2 * suit) * layout.availableSize.x) - 4 * layout.cardSize.x) / 8);
            right.setDuration(down.getDuration() / 5);
            right.setRepeatCount(4);
            right.setRepeatMode(ValueAnimator.REVERSE);
            cardAnims.add(right);

            Animator flipCard = flipCard((Integer) imageView.getTag(), false);
            flipCard.setDuration(down.getDuration());
            cardAnims.add(flipCard);
            ObjectAnimator rotX = ObjectAnimator.ofFloat(imageView, "rotationX", 0, 85);
            rotX.setDuration(down.getDuration());
            cardAnims.add(rotX);

            AnimatorSet cardSet = new AnimatorSet();
            cardSet.playTogether(cardAnims);
            cardSet.setStartDelay(cardIndex * down.getDuration() / 2 + suit * down.getDuration() / 5);
            anims.add(cardSet);
        }

        final AnimatorSet set = new AnimatorSet();
        set.playTogether(anims);

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                activeWinAnimation = set;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                activeWinAnimation = null;
            }
        });

        return set;
    }

    public Animator collectCards() {
        List<Animator> anims = new ArrayList<Animator>();
        Layout layout = mainActivity.getLayout();
        Random random = new Random();
        for (int i = 0; i < Card.values().length; i++) {
            int startDelay = random.nextInt(mainActivity.getAnimationTimeMs());

            ImageView v = mainActivity.getCardView(i);
            ObjectAnimator mx = ObjectAnimator.ofFloat(v, "x", v.getX(), layout.deckLocations[Table.GAME_DECK_INDEX].x);
            mx.setStartDelay(startDelay);
            anims.add(mx);
            ObjectAnimator my = ObjectAnimator.ofFloat(v, "y", v.getY(), layout.deckLocations[Table.GAME_DECK_INDEX].y);
            my.setStartDelay(startDelay);
            anims.add(my);
            ObjectAnimator rotX = ObjectAnimator.ofFloat(v, "rotationX", v.getRotationX(), 0);
            rotX.setStartDelay(startDelay);
            anims.add(rotX);
            if (mainActivity.isCardRevealed(i)) {
                Animator flipCard = flipCard(i, false);
                flipCard.setStartDelay(startDelay);
                anims.add(flipCard);
            } else {
                ObjectAnimator rotY = ObjectAnimator.ofFloat(v, "rotationY", v.getRotationY(), 0);
                rotY.setStartDelay(startDelay);
                anims.add(rotY);
            }
        }

        AnimatorSet set = new AnimatorSet();
        set.playTogether(anims);
        set.setInterpolator(new AccelerateInterpolator(2));
        set.setDuration(2 * mainActivity.getAnimationTimeMs());

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mainActivity.getStatsManager().hideWinView().start();
                Animator winAnim = activeWinAnimation;
                if (winAnim != null) {
                    winAnim.cancel();
                }
            }
        });

        return set;
    }

    void createFlipAnims(List<Card> cardsToFlip, List<Animator> flipAnims, boolean backward) {
        for (Card c : cardsToFlip) {
            Animator flip = flipCard(c.ordinal(), backward);
            flip.setDuration(mainActivity.getAnimationTimeMs());
            flipAnims.add(flip);
        }
    }

    private void updateGameDeck() {
        Deck deck = mainActivity.getTable().getGameDeck();
        for (int i = deck.getCardsCount() - 1; i >= 0; i--) {
            mainActivity.getCardView(deck.getCardAt(i).ordinal()).bringToFront();
        }
    }

    private Animator flipCard(final int tag, boolean backward) {
        ObjectAnimator animation;
        if (backward) {
            if (mainActivity.isCardRevealed(tag)) {
                animation = ObjectAnimator.ofFloat(mainActivity.getCardView(tag), "rotationY", 0, -180);
            } else {
                animation = ObjectAnimator.ofFloat(mainActivity.getCardView(tag), "rotationY", 180, 0);
            }
        } else {
            if (mainActivity.isCardRevealed(tag)) {
                animation = ObjectAnimator.ofFloat(mainActivity.getCardView(tag), "rotationY", 0, 180);
            } else {
                animation = ObjectAnimator.ofFloat(mainActivity.getCardView(tag), "rotationY", -180, 0);
            }
        }
        animation.setDuration(2 * mainActivity.getAnimationTimeMs());
        animation.addUpdateListener(new AnimatorUpdateListener() {
            private boolean done = false;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (!done && animation.getAnimatedFraction() > 0.5f) {
                    done = true;
                    mainActivity.getCardView(tag).setImageBitmap(
                            mainActivity.isCardRevealed(tag) ? mainActivity.getCardBack() : mainActivity
                                    .getCardBitmap(tag));
                    mainActivity.setCardOpen(tag, !mainActivity.isCardRevealed(tag));
                }
            }
        });

        return animation;
    }

    public Animator returnCards(ArrayList<ImageView> views, int deckIndex, int cardIndex) {
        Layout layout = mainActivity.getLayout();
        ArrayList<Animator> anims = new ArrayList<Animator>();
        int toX = layout.deckLocations[deckIndex].x;
        int toY = layout.deckLocations[deckIndex].y;
        if (Table.isInTableau(deckIndex)) {
            toY += layout.computeDeckYOffset(mainActivity.getTable().getDeck(deckIndex), cardIndex);
        }
        if (mainActivity.getTable().isDrawThree() && deckIndex == Table.WASTE_INDEX) {
            toX += layout.cardSize.x / 3;
        }

        for (int i = views.size() - 1; i >= 0; i--) {
            ImageView view = views.get(i);
            anims.add(ObjectAnimator.ofFloat(view, "x", view.getX(), toX));
            anims.add(ObjectAnimator.ofFloat(view, "y", view.getY(), toY));
            toY += mainActivity.getLayout().getPartiallyCoveredOpenCard();
        }

        if (deckIndex == Table.WASTE_INDEX && mainActivity.getTable().isDrawThree()) {
            anims.add(fixWasteIfDrawThree(0));
        }

        AnimatorSet set = new AnimatorSet();
        set.playTogether(anims);
        set.setDuration(mainActivity.getAnimationTimeMs());
        return set;
    }

    public void restoreTableState() {
        Table table = mainActivity.getTable();
        Layout layout = mainActivity.getLayout();

        if(autofinishAnimation != null) {
            autofinishAnimation.end();
        }

        Deck[] decks = table.getAllDecksInternal();
        for (int deckIndex = 0; deckIndex < decks.length; deckIndex++) {
            Deck deck = decks[deckIndex];
            int x = layout.deckLocations[deckIndex].x;
            int y = layout.deckLocations[deckIndex].y;
            for (int cardIndex = deck.getCardsCount() - 1; cardIndex >= 0; cardIndex--) {
                int tag = deck.getCardAt(cardIndex).ordinal();
                ImageView cardView = mainActivity.getCardView(tag);
                cardView.bringToFront();
                cardView.setX(x);
                cardView.setY(y);

                boolean open = deck.getOpenCardsCount() > cardIndex;
                mainActivity.setCardOpen(tag, open);
                Bitmap bm = open ? mainActivity.getCardBitmap(tag) : mainActivity.getCardBack();
                cardView.setImageBitmap(bm);

                if (Table.isInTableau(deckIndex)) {
                    y += open ? layout.getPartiallyCoveredOpenCard() : layout.getPartiallyCoveredBackCard();
                }
            }
        }

        if (table.isDrawThree()) {
            Deck waste = table.getWaste();
            int centerX = layout.deckLocations[Table.WASTE_INDEX].x;
            switch (waste.getCardsCount()) {
            case 0:
            case 1:
                return;
            case 2:
                mainActivity.getCardView(waste.getCardAt(0).ordinal()).setX(centerX + layout.cardSize.x / 3);
                mainActivity.getCardView(waste.getCardAt(1).ordinal()).setX(centerX - layout.cardSize.x / 3);
                break;
            default:
                mainActivity.getCardView(waste.getCardAt(0).ordinal()).setX(centerX + layout.cardSize.x / 3);
                for (int i = 2; i < waste.getCardsCount(); i++) {
                    mainActivity.getCardView(waste.getCardAt(i).ordinal()).setX(centerX - layout.cardSize.x / 3);
                }
            }
        }
    }

    public Animator animateAutoFinish() {
        final Table table = mainActivity.getTable();

        int[] deckIndex = new int[Card.values().length];
        for (int i = Table.FOUNDATION_DECKS_COUNT; i < Table.ALL_DECS_INTERNAL_COUNT; i++) {
            Deck deck = table.getDeck(i);
            for (int j = 0; j < deck.getCardsCount(); j++) {
                deckIndex[deck.getCardAt(j).ordinal()] = i;
            }
        }

        // which suit goes to which foundation deck?

        int[] suitDeck = new int[Table.FOUNDATION_DECKS_COUNT];
        List<Integer> freeDecks = new ArrayList<Integer>();
        List<Integer> allSuits = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3));
        // if there are cards on the foundation, remember their suits
        for (int i = 0; i < Table.FOUNDATION_DECKS_COUNT; i++) {
            Card card = table.getFoundations()[i].getCardAt(0);
            if (card != null) {
                suitDeck[card.getSuit()] = i;
                allSuits.remove((Integer) card.getSuit());
            } else {
                freeDecks.add(i);
            }
        }
        // distribute the 'remaining' suits to the 'free' foundation decks
        while (!freeDecks.isEmpty()) {
            suitDeck[allSuits.remove(0)] = freeDecks.remove(0);
        }

        mainActivity.getSolver().autoComplete(table);

        int timeOffset = 0;
        Layout layout = mainActivity.getLayout();
        ArrayList<Animator> anims = new ArrayList<Animator>();
        final int autofinishTimeStep = getAutofinishTimeStep();
        for (int value = 0; value < 13; value++) {
            for (int suit = 0; suit < Card.SUITS_COUNT; suit++) {
                final int i = value + 13 * suit;
                if (deckIndex[i] == 0) {
                    continue;
                }

                final ImageView view = mainActivity.getCardView(i);
                ObjectAnimator animX = ObjectAnimator.ofFloat(view, "x", view.getX(),
                        layout.deckLocations[suitDeck[suit]].x);
                ObjectAnimator animY = ObjectAnimator.ofFloat(view, "y", view.getY(),
                        layout.deckLocations[suitDeck[suit]].y);
                animX.setDuration(mainActivity.getAnimationTimeMs());
                animY.setDuration(mainActivity.getAnimationTimeMs());
                animX.setStartDelay(timeOffset);
                animY.setStartDelay(timeOffset);
                anims.add(animX);
                anims.add(animY);
                if (!mainActivity.isCardRevealed(i)) {
                    Animator flip = flipCard(i, false);
                    flip.setStartDelay(timeOffset);
                    flip.setDuration(mainActivity.getAnimationTimeMs());
                    anims.add(flip);
                }

                animX.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        view.bringToFront();
                    }
                });

                // card is not in place -> animate
                timeOffset += autofinishTimeStep;
            }
        }

        AnimatorSet set = new AnimatorSet();
        set.playTogether(anims);

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                autofinishAnimation = animation;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                autofinishAnimation = null;
            }
        });

        return set;
    }

    private int getAutofinishTimeStep() {
        return mainActivity.getAnimationTimeMs() / 3;
    }

    public Animator fixWasteIfDrawThree(int startCardIndex) {
        Table table = mainActivity.getTable();
        Deck waste = table.getWaste();
        int center = mainActivity.getLayout().deckLocations[Table.WASTE_INDEX].x;
        ArrayList<Animator> anims = new ArrayList<Animator>();

        if (!table.isDrawThree()) {
            // if drawing one, move all cards to the center
            for (int i = startCardIndex; i < waste.getCardsCount(); i++) {
                ImageView view = mainActivity.getCardView(waste.getCardAt(i).ordinal());
                if (view.getX() != center) {
                    ObjectAnimator a = ObjectAnimator.ofFloat(view, "x", view.getX(), center);
                    a.setDuration(mainActivity.getAnimationTimeMs());
                    anims.add(a);
                }
            }
        } else if (waste.getCardsCount() > startCardIndex + 1) {
            if (waste.getCardsCount() > startCardIndex) {
                ImageView view = mainActivity.getCardView(waste.getCardAt(startCardIndex).ordinal());
                ObjectAnimator a = ObjectAnimator.ofFloat(view, "x", view.getX(), center
                        + mainActivity.getLayout().getDrawThreeWasteOffset());
                a.setDuration(mainActivity.getAnimationTimeMs());
                anims.add(a);
            }

            if (waste.getCardsCount() > startCardIndex + 2) {
                ImageView view1 = mainActivity.getCardView(waste.getCardAt(startCardIndex + 1).ordinal());
                ObjectAnimator a1 = ObjectAnimator.ofFloat(view1, "x", view1.getX(), center);
                a1.setDuration(mainActivity.getAnimationTimeMs());
                anims.add(a1);
                ImageView view2 = mainActivity.getCardView(waste.getCardAt(startCardIndex + 2).ordinal());
                ObjectAnimator a2 = ObjectAnimator.ofFloat(view2, "x", view2.getX(), center
                        - mainActivity.getLayout().getDrawThreeWasteOffset());
                a2.setDuration(mainActivity.getAnimationTimeMs());
                anims.add(a2);
            } else if (waste.getCardsCount() > startCardIndex + 1) {
                ImageView view1 = mainActivity.getCardView(waste.getCardAt(startCardIndex + 1).ordinal());
                ObjectAnimator a = ObjectAnimator.ofFloat(view1, "x", view1.getX(), center
                        - mainActivity.getLayout().getDrawThreeWasteOffset());
                a.setDuration(mainActivity.getAnimationTimeMs());
                anims.add(a);
            }
        }

        AnimatorSet set = new AnimatorSet();
        set.playTogether(anims);
        return set;
    }

    private void invalidatePreKitkat() {
        ViewParent parent = mainActivity.getCardView(0).getParent();
        if (Build.VERSION.SDK_INT < 19 /* KITKAT */&& parent instanceof View) {
            ((View) parent).invalidate();
            parent.requestLayout();
        }
    }
}
