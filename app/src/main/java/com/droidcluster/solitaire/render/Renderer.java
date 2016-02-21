package com.droidcluster.solitaire.render;

import android.content.res.Resources;

import com.droidcluster.solitaire.game.Layout;

public class Renderer {

    protected Layout layout;
    protected Resources resources;

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }
}
