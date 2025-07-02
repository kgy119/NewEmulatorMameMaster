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

    // 수정: int 타입으로 변경 (게임 카운트는 숫자여야 함)
    @ColumnInfo(name = "game_cnt")
    private int gameCount;

    // 수정: int 타입으로 변경 (게임 길이는 숫자여야 함)
    @ColumnInfo(name = "game_length")
    private int gameLength;

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    private long createdAt;

    // 기본 생성자
    public GameEntity() {}

    // 수정: 파라미터 타입을 int로 변경
    public GameEntity(String gameId, String gameName, String gameCategory,
                      String gameRom, String gameImg, int gameCount, int gameLength) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.gameCategory = gameCategory;
        this.gameRom = gameRom;
        this.gameImg = gameImg;
        this.gameCount = gameCount;
        this.gameLength = gameLength;
        this.createdAt = System.currentTimeMillis();
    }

    // String 파라미터를 받는 생성자 (기존 코드와의 호환성을 위해)
    public GameEntity(String gameId, String gameName, String gameCategory,
                      String gameRom, String gameImg, String gameCount, String gameLength) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.gameCategory = gameCategory;
        this.gameRom = gameRom;
        this.gameImg = gameImg;
        // String을 int로 안전하게 변환
        this.gameCount = parseIntSafely(gameCount, 0);
        this.gameLength = parseIntSafely(gameLength, 0);
        this.createdAt = System.currentTimeMillis();
    }

    // 안전한 int 파싱을 위한 헬퍼 메소드
    private int parseIntSafely(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public String getGameCategory() {
        return gameCategory;
    }

    public void setGameCategory(String gameCategory) {
        this.gameCategory = gameCategory;
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

    // 수정: int 타입 반환
    public int getGameCount() {
        return gameCount;
    }

    public void setGameCount(int gameCount) {
        this.gameCount = gameCount;
    }

    // 호환성을 위한 String 버전 setter
    public void setGameCount(String gameCount) {
        this.gameCount = parseIntSafely(gameCount, 0);
    }

    // 수정: int 타입 반환
    public int getGameLength() {
        return gameLength;
    }

    public void setGameLength(int gameLength) {
        this.gameLength = gameLength;
    }

    // 호환성을 위한 String 버전 setter
    public void setGameLength(String gameLength) {
        this.gameLength = parseIntSafely(gameLength, 0);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "GameEntity{" +
                "id=" + id +
                ", gameId='" + gameId + '\'' +
                ", gameName='" + gameName + '\'' +
                ", gameCategory='" + gameCategory + '\'' +
                ", gameRom='" + gameRom + '\'' +
                ", gameCount=" + gameCount +
                ", gameLength=" + gameLength +
                ", createdAt=" + createdAt +
                '}';
    }

    // equals와 hashCode 메소드 추가 (데이터 비교를 위해)
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        GameEntity that = (GameEntity) obj;

        if (gameRom != null ? !gameRom.equals(that.gameRom) : that.gameRom != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return gameRom != null ? gameRom.hashCode() : 0;
    }
}