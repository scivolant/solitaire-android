package com.droidcluster.solitaire;

import com.droidcluster.solitaire.model.Table;
import com.droidcluster.solitaire.util.TimeConverter;

import android.view.View;
import android.widget.TextView;

public class ScoreManager {

    private MainActivity mainActivity;
    private View scoreView;
    private TextView moves;
    private TextView points;
    private TextView time;

    public ScoreManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;

        scoreView = mainActivity.findViewById(R.id.scoreView);
        moves = (TextView) scoreView.findViewById(R.id.score_moves);
        points = (TextView) scoreView.findViewById(R.id.score_points);
        time = (TextView) scoreView.findViewById(R.id.score_time);
    }

    public void updateScore() {
        Table table = mainActivity.getTable();
        moves.setText(String.valueOf(table.getHistory().size()));
        points.setText(String.valueOf(table.getPoints()));
        time.setText(TimeConverter.timeToString(mainActivity.getTimer().getTime()));
    }
}
