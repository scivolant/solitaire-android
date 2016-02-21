package com.droidcluster.solitaire.game;

import java.util.Properties;

import com.droidcluster.solitaire.model.Stats;
import com.droidcluster.solitaire.model.Table;

public interface Storage {

    void saveTable(Table table);

    Table loadOrCreateTable(boolean drawThree);

    void saveStats(Stats stats, boolean drawThree);

    Stats loadOrCreateStats(boolean drawThree);

    void saveConfig(Properties config);

    Properties loadOrCreateConfig();

    boolean isHintSeen(int i);

    void setHintSeen(int i);
}