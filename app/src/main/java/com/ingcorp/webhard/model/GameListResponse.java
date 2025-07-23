package com.ingcorp.webhard.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GameListResponse {
    @SerializedName("list")
    private List<GameItem> list;

    public List<GameItem> getList() {
        return list;
    }

    public void setList(List<GameItem> list) {
        this.list = list;
    }

    public static class GameItem {

        @SerializedName("game_cate")
        private String gameCate;

        @SerializedName("game_rom")
        private String gameRom;

        public String getGameCate() {
            return gameCate;
        }

        public void setGameCate(String gameCate) {
            this.gameCate = gameCate;
        }

        public String getGameRom() {
            return gameRom;
        }

        public void setGameRom(String gameRom) {
            this.gameRom = gameRom;
        }
    }
}