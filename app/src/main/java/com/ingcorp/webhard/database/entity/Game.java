package com.ingcorp.webhard.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

@Entity(tableName = "games")
public class Game {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id; // 자동 증가 Primary Key

    @ColumnInfo(name = "game_id")
    private String gameId;

    @ColumnInfo(name = "game_name")
    private String gameName;

    @ColumnInfo(name = "game_cate")
    private String gameCate;

    @ColumnInfo(name = "game_rom")
    private String gameRom;

    @ColumnInfo(name = "game_img")
    private String gameImg;

    @ColumnInfo(name = "game_cnt")
    private String gameCnt;

    @ColumnInfo(name = "game_length")
    private String gameLength;

    // 기본 생성자 (Room이 사용)
    public Game() {}

    // ✅ @Ignore 어노테이션 추가 - Room이 무시하도록 함
    @Ignore
    public Game(String gameId, String gameName, String gameCate, String gameRom,
                String gameImg, String gameCnt, String gameLength) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.gameCate = gameCate;
        this.gameRom = gameRom;
        this.gameImg = gameImg;
        this.gameCnt = gameCnt;
        this.gameLength = gameLength;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

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