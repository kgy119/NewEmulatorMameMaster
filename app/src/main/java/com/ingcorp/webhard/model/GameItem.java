package com.ingcorp.webhard.model;

import com.ingcorp.webhard.database.entity.Game;

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
