package com.droidcluster.solitaire.model;

public enum Card {
    //
    T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13,
    //
    K1, K2, K3, K4, K5, K6, K7, K8, K9, K10, K11, K12, K13,
    //
    H1, H2, H3, H4, H5, H6, H7, H8, H9, H10, H11, H12, H13,
    //
    P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13;

    public static final int ACE_VALUE = 0;
    public static final int VALE_VALUE = 10;
    public static final int KING_VALUE = 12;
    public static final int SUITS_COUNT = 4;

    public int numberValue() {
        return ordinal() % 13 + 1;
    }

    public int getSuit() {
        return ordinal() / 13;
    }

    public boolean isRed() {
        int i = ordinal();
        return i >= 13 && i <= 38;
    }

    public String getSuitSymbolUTF8() {
        switch (getSuit()) {
        case 0:
            return "\u2663";
        case 1:
            return "\u2666";
        case 2:
            return "\u2665";
        case 3:
            return "\u2660";
        }

        return "?";
    }

    public String getNumberSymbol() {
        int numberValue = numberValue();
        switch (numberValue) {
        case 1:
            return "A";
        case 11:
            return "J";
        case 12:
            return "Q";
        case 13:
            return "K";
        default:
            return String.valueOf(numberValue);
        }
    }
}