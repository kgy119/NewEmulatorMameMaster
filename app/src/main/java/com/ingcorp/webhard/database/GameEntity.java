package com.ingcorp.webhard.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "games",
        indices = {@Index(value = "game_rom", unique = true)}
)
public class GameEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "game_id")
    private String gameId;

    @ColumnInfo(name = "game_name")
    private String gameName;

    @ColumnInfo(name = "game_cate")
    private String gameCategory;

    @ColumnInfo(name = "game_rom")
    private String gameRom;

    @ColumnInfo(name = "game_img")
    private String gameImg;

    @ColumnInfo(name = "game_cnt")
    private String gameCount;

    @ColumnInfo(name = "game_length")
    private String gameLength;

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    private long createdAt;

    // 생성자
    public GameEntity() {}

    public GameEntity(String gameId, String gameName, String gameCategory,
                      String gameRom, String gameImg, String gameCount, String gameLength) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.gameCategory = gameCategory;
        this.gameRom = gameRom;
        this.gameImg = gameImg;
        this.gameCount = gameCount;
        this.gameLength = gameLength;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }

    public String getGameCategory() { return gameCategory; }
    public void setGameCategory(String gameCategory) { this.gameCategory = gameCategory; }

    public String getGameRom() { return gameRom; }
    public void setGameRom(String gameRom) { this.gameRom = gameRom; }

    public String getGameImg() { return gameImg; }
    public void setGameImg(String gameImg) { this.gameImg = gameImg; }

    public String getGameCount() { return gameCount; }
    public void setGameCount(String gameCount) { this.gameCount = gameCount; }

    public String getGameLength() { return gameLength; }
    public void setGameLength(String gameLength) { this.gameLength = gameLength; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "GameEntity{" +
                "id=" + id +
                ", gameId='" + gameId + '\'' +
                ", gameName='" + gameName + '\'' +
                ", gameCategory='" + gameCategory + '\'' +
                ", gameRom='" + gameRom + '\'' +
                '}';
    }
}

