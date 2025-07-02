package com.ingcorp.webhard.models;

import com.google.gson.annotations.SerializedName;

public class Game {

    @SerializedName("game_id")
    private String gameId;

    @SerializedName("game_name")
    private String gameName;

    @SerializedName("game_cate")
    private String gameCategory;

    @SerializedName("game_rom")
    private String gameRom;

    @SerializedName("game_img")
    private String gameImg;

    @SerializedName("game_cnt")
    private String gameCount;

    @SerializedName("game_length")
    private String gameLength;

    // 기본 생성자
    public Game() {
    }

    // 모든 필드 생성자
    public Game(String gameId, String gameName, String gameCategory,
                String gameRom, String gameImg, String gameCount, String gameLength) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.gameCategory = gameCategory;
        this.gameRom = gameRom;
        this.gameImg = gameImg;
        this.gameCount = gameCount;
        this.gameLength = gameLength;
    }

    // Getters
    public String getGameId() {
        return gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public String getGameCategory() {
        return gameCategory;
    }

    public String getGameRom() {
        return gameRom;
    }

    public String getGameImg() {
        return gameImg;
    }

    public String getGameCount() {
        return gameCount;
    }

    public String getGameLength() {
        return gameLength;
    }

    // Setters
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public void setGameCategory(String gameCategory) {
        this.gameCategory = gameCategory;
    }

    public void setGameRom(String gameRom) {
        this.gameRom = gameRom;
    }

    public void setGameImg(String gameImg) {
        this.gameImg = gameImg;
    }

    public void setGameCount(String gameCount) {
        this.gameCount = gameCount;
    }

    public void setGameLength(String gameLength) {
        this.gameLength = gameLength;
    }

    // 편의 메소드
    public int getGameCountInt() {
        try {
            return Integer.parseInt(gameCount);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public long getGameLengthLong() {
        try {
            return Long.parseLong(gameLength);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 파일 크기를 MB 단위로 반환
    public String getGameSizeMB() {
        long bytes = getGameLengthLong();
        if (bytes == 0) return "0 MB";

        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.1f MB", mb);
    }

    // 카테고리를 한글로 변환
    public String getGameCategoryKorean() {
        switch (gameCategory != null ? gameCategory.toUpperCase() : "") {
            case "FIGHT":
                return "격투";
            case "ACTION":
                return "액션";
            case "SHOOTING":
                return "슈팅";
            case "SPORTS":
                return "스포츠";
            case "ARCADE":
                return "아케이드";
            case "PUZZLE":
                return "퍼즐";
            default:
                return gameCategory != null ? gameCategory : "기타";
        }
    }

    @Override
    public String toString() {
        return "Game{" +
                "gameId='" + gameId + '\'' +
                ", gameName='" + gameName + '\'' +
                ", gameCategory='" + gameCategory + '\'' +
                ", gameRom='" + gameRom + '\'' +
                ", gameCount='" + gameCount + '\'' +
                ", gameLength='" + gameLength + '\'' +
                '}';
    }
}
