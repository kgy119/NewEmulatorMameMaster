package com.ingcorp.webhard.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GameDao {

    // ======================== 기본 조회 메소드 ========================

    @Query("SELECT * FROM games ORDER BY game_name ASC")
    List<GameEntity> getAllGames();

    @Query("SELECT * FROM games WHERE game_rom = :gameRom LIMIT 1")
    GameEntity getGameByRom(String gameRom);

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    GameEntity getGameById(long id);

    @Query("SELECT * FROM games WHERE game_id = :gameId LIMIT 1")
    GameEntity getGameByGameId(String gameId);

    // ======================== 카테고리 관련 메소드 ========================

    @Query("SELECT * FROM games WHERE game_cate = :category ORDER BY game_name ASC")
    List<GameEntity> getGamesByCategory(String category);

    @Query("SELECT DISTINCT game_cate FROM games WHERE game_cate IS NOT NULL AND game_cate != '' ORDER BY game_cate ASC")
    List<String> getAllCategories();

    @Query("SELECT COUNT(*) FROM games WHERE game_cate = :category")
    int getCategoryGameCount(String category);

    // ======================== 검색 관련 메소드 ========================

    @Query("SELECT * FROM games WHERE game_name LIKE '%' || :searchTerm || '%' ORDER BY game_name ASC")
    List<GameEntity> searchGames(String searchTerm);

    @Query("SELECT * FROM games WHERE " +
            "game_name LIKE '%' || :searchTerm || '%' OR " +
            "game_cate LIKE '%' || :searchTerm || '%' OR " +
            "game_rom LIKE '%' || :searchTerm || '%' " +
            "ORDER BY game_name ASC")
    List<GameEntity> searchGamesAdvanced(String searchTerm);

    @Query("SELECT * FROM games WHERE game_cate = :category AND " +
            "game_name LIKE '%' || :searchTerm || '%' ORDER BY game_name ASC")
    List<GameEntity> searchGamesInCategory(String category, String searchTerm);

    // ======================== 정렬 및 필터링 메소드 ========================

    // gameCount가 int라면 CAST 제거, String이라면 CAST 유지
    @Query("SELECT * FROM games ORDER BY CAST(game_cnt AS INTEGER) DESC LIMIT :limit")
    List<GameEntity> getPopularGames(int limit);

    @Query("SELECT * FROM games ORDER BY created_at DESC LIMIT :limit")
    List<GameEntity> getRecentlyAddedGames(int limit);

    @Query("SELECT * FROM games ORDER BY game_name ASC LIMIT :limit OFFSET :offset")
    List<GameEntity> getGamesPaged(int limit, int offset);

    @Query("SELECT * FROM games WHERE game_cate = :category ORDER BY game_name ASC LIMIT :limit OFFSET :offset")
    List<GameEntity> getGamesByCategoryPaged(String category, int limit, int offset);

    // ======================== 통계 메소드 ========================

    @Query("SELECT COUNT(*) FROM games")
    int getGameCount();

    @Query("SELECT COUNT(DISTINCT game_cate) FROM games WHERE game_cate IS NOT NULL AND game_cate != ''")
    int getCategoryCount();

    @Query("SELECT AVG(CAST(game_cnt AS INTEGER)) FROM games WHERE game_cnt IS NOT NULL AND game_cnt != ''")
    double getAverageGameCount();

    @Query("SELECT SUM(CAST(game_length AS INTEGER)) FROM games WHERE game_length IS NOT NULL AND game_length != ''")
    long getTotalGameLength();

    // ======================== 삽입/업데이트 메소드 ========================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertGame(GameEntity game);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertGames(List<GameEntity> games);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertGameIgnoreConflict(GameEntity game);

    @Update
    int updateGame(GameEntity game);

    @Update
    int updateGames(List<GameEntity> games);

    // ======================== 삭제 메소드 ========================

    @Delete
    int deleteGame(GameEntity game);

    @Delete
    int deleteGames(List<GameEntity> games);

    @Query("DELETE FROM games")
    int deleteAllGames();

    @Query("DELETE FROM games WHERE game_rom = :gameRom")
    int deleteGameByRom(String gameRom);

    @Query("DELETE FROM games WHERE game_cate = :category")
    int deleteGamesByCategory(String category);

    @Query("DELETE FROM games WHERE id IN (:ids)")
    int deleteGamesByIds(List<Long> ids);

    // ======================== 트랜잭션 메소드 ========================

    /**
     * 모든 게임을 삭제하고 새로운 게임 리스트를 삽입
     * 트랜잭션으로 처리하여 원자성 보장
     */
    @Transaction
    default int replaceAllGames(List<GameEntity> newGames) {
        deleteAllGames();
        insertGames(newGames);
        return newGames.size();
    }

    /**
     * 특정 카테고리의 게임들을 교체
     */
    @Transaction
    default int replaceCategoryGames(String category, List<GameEntity> newGames) {
        deleteGamesByCategory(category);
        insertGames(newGames);
        return newGames.size();
    }

    // ======================== 존재 여부 확인 메소드 ========================

    @Query("SELECT EXISTS(SELECT 1 FROM games WHERE game_rom = :gameRom)")
    boolean existsByRom(String gameRom);

    @Query("SELECT EXISTS(SELECT 1 FROM games WHERE game_id = :gameId)")
    boolean existsByGameId(String gameId);

    @Query("SELECT EXISTS(SELECT 1 FROM games WHERE id = :id)")
    boolean existsById(long id);

    // ======================== 커스텀 정렬 메소드 ========================

    @Query("SELECT * FROM games ORDER BY " +
            "CASE WHEN :sortBy = 'name' THEN game_name END ASC, " +
            "CASE WHEN :sortBy = 'category' THEN game_cate END ASC, " +
            "CASE WHEN :sortBy = 'count' THEN CAST(game_cnt AS INTEGER) END DESC, " +
            "CASE WHEN :sortBy = 'recent' THEN created_at END DESC")
    List<GameEntity> getGamesSorted(String sortBy);

    @Query("SELECT * FROM games WHERE game_cate = :category ORDER BY " +
            "CASE WHEN :sortBy = 'name' THEN game_name END ASC, " +
            "CASE WHEN :sortBy = 'count' THEN CAST(game_cnt AS INTEGER) END DESC, " +
            "CASE WHEN :sortBy = 'recent' THEN created_at END DESC")
    List<GameEntity> getGamesByCategorySorted(String category, String sortBy);

    // ======================== 랜덤 조회 메소드 ========================

    @Query("SELECT * FROM games ORDER BY RANDOM() LIMIT :limit")
    List<GameEntity> getRandomGames(int limit);

    @Query("SELECT * FROM games WHERE game_cate = :category ORDER BY RANDOM() LIMIT :limit")
    List<GameEntity> getRandomGamesByCategory(String category, int limit);

    // ======================== 게임 카운트 업데이트 메소드 ========================

    @Query("UPDATE games SET game_cnt = CAST((CAST(game_cnt AS INTEGER) + 1) AS TEXT) WHERE game_rom = :gameRom")
    int incrementGameCount(String gameRom);

    @Query("UPDATE games SET game_cnt = :newCount WHERE game_rom = :gameRom")
    int updateGameCount(String gameRom, String newCount);

    // ======================== 즐겨찾기 관련 (향후 확장용) ========================

    // Note: 향후 즐겨찾기 기능을 추가할 경우를 위한 예시
    // GameEntity에 favorite 필드가 추가되면 활성화
    /*
    @Query("SELECT * FROM games WHERE favorite = 1 ORDER BY game_name ASC")
    List<GameEntity> getFavoriteGames();

    @Query("UPDATE games SET favorite = :favorite WHERE game_rom = :gameRom")
    int updateFavoriteStatus(String gameRom, boolean favorite);
    */
}