package com.ingcorp.webhard.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GameListResponse {
    @SerializedName("result_code")
    private String resultCode;

    @SerializedName("all_total")
    private int allTotal;

    @SerializedName("list")
    private List<GameItem> list;

    // Getters and Setters
    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public int getAllTotal() {
        return allTotal;
    }

    public void setAllTotal(int allTotal) {
        this.allTotal = allTotal;
    }

    public List<GameItem> getList() {
        return list;
    }

    public void setList(List<GameItem> list) {
        this.list = list;
    }

    public static class GameItem {
        @SerializedName("game_id")
        private String gameId;

        @SerializedName("game_name")
        private String gameName;

        @SerializedName("game_cate")
        private String gameCate;

        @SerializedName("game_rom")
        private String gameRom;

        @SerializedName("game_img")
        private String gameImg;

        @SerializedName("game_cnt")
        private String gameCnt;

        @SerializedName("game_length")
        private String gameLength;

        // Getters and Setters
        public String getGameId() {
            return gameId;
        }

        public void setGameId(String gameId) {
            this.gameId = gameId;
        }

        public String getGameName() {
            return gameName;
        }

        public void setGameName(String gameName) {
            this.gameName = gameName;
        }

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

        public String getGameImg() {
            return gameImg;
        }

        public void setGameImg(String gameImg) {
            this.gameImg = gameImg;
        }

        public String getGameCnt() {
            return gameCnt;
        }

        public void setGameCnt(String gameCnt) {
            this.gameCnt = gameCnt;
        }

        public String getGameLength() {
            return gameLength;
        }

        public void setGameLength(String gameLength) {
            this.gameLength = gameLength;
        }
    }
}