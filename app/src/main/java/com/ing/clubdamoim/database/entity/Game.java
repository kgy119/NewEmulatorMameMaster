package com.ing.clubdamoim.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

@Entity(tableName = "games")
public class Game {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id; // 자동 증가 Primary Key

    @ColumnInfo(name = "game_cate")
    private String gameCate;

    @ColumnInfo(name = "game_rom")
    private String gameRom;

    // 기본 생성자 (Room이 사용)
    public Game() {}

    // ✅ @Ignore 어노테이션 추가 - Room이 무시하도록 함
    @Ignore
    public Game(String gameCate, String gameRom) {
        this.gameCate = gameCate;
        this.gameRom = gameRom;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

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