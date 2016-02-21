package com.droidcluster.solitaire;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unchecked")
public class Whiteboard {
    public enum Event {
        // game
        GAME_STARTED, MOVED, WON, LOST, PAUSED, UNPAUSED,
        // prefs
        CARD_BG_SET, GAME_BG_SET, LEFT_HAND_SET, RESIZED, DRAW_THREE_SET,
        // hints
        OFFERED_AUTOFINISH,
    }

    public interface WhiteboardListener {
        void whiteboardEventReceived(Event event);
    }

    private static final Set<WhiteboardListener>[] listeners;
    static {
        listeners = new Set[Event.values().length];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new HashSet<WhiteboardListener>();
        }
    }

    public static void post(Event event) {
        for (WhiteboardListener listener : listeners[event.ordinal()]) {
            listener.whiteboardEventReceived(event);
        }
    }

    public static void addListener(WhiteboardListener listener, Event... events) {
        for (Event event : events) {
            HashSet<WhiteboardListener> set = new HashSet<WhiteboardListener>(listeners[event.ordinal()]);
            if (set.add(listener)) {
                listeners[event.ordinal()] = set;
            }
        }
    }

    public static void removeListener(WhiteboardListener listener, Event... events) {
        for (Event event : events) {
            HashSet<WhiteboardListener> set = new HashSet<WhiteboardListener>(listeners[event.ordinal()]);
            if (set.remove(listener)) {
                listeners[event.ordinal()] = set;
            }
        }
    }

    public static void destroy() {
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].clear();
        }
    }
}
