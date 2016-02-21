package com.droidcluster.solitaire.game;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.droidcluster.solitaire.model.AbstractMove;
import com.droidcluster.solitaire.model.Card;
import com.droidcluster.solitaire.model.Deck;
import com.droidcluster.solitaire.model.IMove2;
import com.droidcluster.solitaire.model.Move;
import com.droidcluster.solitaire.model.Move.Flip;
import com.droidcluster.solitaire.model.RecycleWasteMove;
import com.droidcluster.solitaire.model.Stats;
import com.droidcluster.solitaire.model.Table;

public class JSONSerializer {

    private static final String ALLOWED_RECYCLES = "allowedRecycles";
    private static final String JSON_KEY = "c";

    public JSONObject stats2json(Stats stats) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("gamesPlayed", stats.getGamesPlayed());
        result.put("bestMoves", stats.getBestMoves());
        result.put("gamesWon", stats.getGamesWon());
        result.put("timePlayedSec", stats.getTimePlayedSec());
        result.put("bestTime", stats.getBestTime());
        result.put("totalMoves", stats.getTotalMoves());
        result.put("bestPoints", stats.getBestPoints());
        result.put("strike", stats.getStrike());

        return result;
    }

    public Stats json2stats(JSONObject json) throws JSONException {
        Stats result = new Stats();
        result.setGamesPlayed(json.getInt("gamesPlayed"));
        result.setBestMoves(json.getInt("bestMoves"));
        result.setGamesWon(json.getInt("gamesWon"));
        result.setTimePlayedSec(json.getLong("timePlayedSec"));
        result.setBestTime(json.getLong("bestTime"));
        result.setTotalMoves(json.getInt("totalMoves"));
        result.setBestPoints(json.optInt("bestPoints"));
        result.setStrike(json.optInt("strike"));

        return result;
    }

    public JSONObject move2json(IMove2 move) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("points", ((AbstractMove) move).getPointsGiven());

        result.put("c", move.getCardIndex());
        if (move instanceof RecycleWasteMove) {
            result.put("r", 1);
            return result;
        }
        result.put("r", 0);
        Move m = (Move) move;
        result.put("src", m.fromDeckIndex);
        result.put("trg", m.toDeckIndex);
        result.put("count", m.cardsToMoveCount);

        JSONArray flips = new JSONArray();
        for (Flip f : m.flips) {
            flips.put(flip2json(f));
        }
        result.put("flips", flips);

        return result;
    }

    private JSONObject flip2json(Flip f) throws JSONException {
        JSONObject flip = new JSONObject();
        flip.put("i", f.internalDeckIndex);
        flip.put("open", f.oldOpenCardsCount);
        return flip;
    }

    public IMove2 json2move(JSONObject json) throws JSONException {
        if (json.getInt("r") == 1) {
            RecycleWasteMove result = new RecycleWasteMove();
            result.setPointsGiven(json.getInt("points"));
            int cardIndex = json.optInt("c");
            result.setCardIndex(cardIndex);
            return result;
        }

        int sourceInternalDeckIndex = json.getInt("src");
        int cardIndex = json.getInt("c");
        int foundationInternalDeckIndex = json.getInt("trg");
        Move result = new Move(sourceInternalDeckIndex, cardIndex, foundationInternalDeckIndex);
        result.cardsToMoveCount = json.getInt("count");

        JSONArray flips = json.getJSONArray("flips");
        for (int i = 0; i < flips.length(); i++) {
            result.flips.add(json2flip(flips.getJSONObject(i)));
        }
        result.setPointsGiven(json.getInt("points"));

        return result;
    }

    private Flip json2flip(JSONObject jsonObject) throws JSONException {
        int internalDeckIndex = jsonObject.getInt("i");
        int oldOpenCardsCount = jsonObject.getInt("open");
        Flip f = new Flip(internalDeckIndex, oldOpenCardsCount);
        return f;
    }

    public JSONObject deck2json(Deck deck) throws JSONException {
        JSONObject result = new JSONObject();

        result.put("open", deck.getOpenCardsCount());
        JSONArray cards = new JSONArray();
        for (int i = 0; i < deck.getCardsCount(); i++) {
            cards.put(card2json(deck.getCardAt(i)));
        }
        result.put("cards", cards);

        return result;
    }

    public void json2deck(JSONObject json, Deck target) throws JSONException {
        target.clear();
        target.setOpenCardsCount(json.getInt("open"));
        JSONArray cards = json.getJSONArray("cards");
        for (int i = 0; i < cards.length(); i++) {
            target.addCard(json2card(cards.getJSONObject(i)));
        }
    }

    public Table json2table(JSONObject json) throws JSONException {
        Table result = new Table();

        Deck[] allDecksInternal = result.getAllDecksInternal();
        JSONArray decks = json.getJSONArray("decks");
        for (int i = 0; i < Table.ALL_DECS_INTERNAL_COUNT; i++) {
            json2deck(decks.getJSONObject(i), allDecksInternal[i]);
        }

        JSONArray moves = json.getJSONArray("history");
        for (int i = 0; i < moves.length(); i++) {
            result.getHistory().add(json2move(moves.getJSONObject(i)));
        }

        result.setTime(json.optLong("time"));
        result.setPoints(json.optInt("points"));
        if (json.has(ALLOWED_RECYCLES)) {
            result.setAllowedRecycles(json.optInt(ALLOWED_RECYCLES));
        }

        return result;
    }

    public JSONObject table2json(Table table) throws JSONException {
        JSONObject json = new JSONObject();

        JSONArray decks = new JSONArray();
        for (Deck d : table.getAllDecksInternal()) {
            decks.put(deck2json(d));
        }
        json.put("decks", decks);

        JSONArray moves = new JSONArray();
        for (IMove2 m : table.getHistory()) {
            moves.put(move2json(m));
        }
        json.put("history", moves);
        json.put("time", table.getTime());
        json.put("points", table.getPoints());
        json.put(ALLOWED_RECYCLES, table.getAllowedRecycles());

        return json;
    }

    public JSONObject card2json(Card c) throws JSONException {
        JSONObject o = new JSONObject();
        o.put(JSON_KEY, c.name());
        return o;
    }

    public Card json2card(JSONObject json) throws JSONException {
        return Card.valueOf(json.getString(JSON_KEY));
    }

}
