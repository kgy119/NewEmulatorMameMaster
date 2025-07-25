package com.ing.clubdamoim.model;

import com.ing.clubdamoim.database.entity.Game;

public class GameItem extends BaseItem {
    private Game game;

    public GameItem(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    @Override
    public int getItemType() {
        return TYPE_GAME;
    }
}
