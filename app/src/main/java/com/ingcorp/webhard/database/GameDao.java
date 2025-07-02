package com.ingcorp.webhard.database;

@Dao
public interface GameDao {

    @Query("SELECT * FROM games ORDER BY game_name ASC")
    List<GameEntity> getAllGames();

    @Query("SELECT * FROM games WHERE game_rom = :gameRom LIMIT 1")
    GameEntity getGameByRom(String gameRom);

    @Query("SELECT * FROM games WHERE game_cate = :category ORDER BY game_name ASC")
    List<GameEntity> getGamesByCategory(String category);

    @Query("SELECT * FROM games WHERE game_name LIKE '%' || :searchTerm || '%' ORDER BY game_name ASC")
    List<GameEntity> searchGames(String searchTerm);

    @Query("SELECT DISTINCT game_cate FROM games WHERE game_cate IS NOT NULL AND game_cate != '' ORDER BY game_cate ASC")
    List<String> getAllCategories();

    @Query("SELECT COUNT(*) FROM games")
    int getGameCount();

    @Query("SELECT * FROM games ORDER BY CAST(game_cnt AS INTEGER) DESC LIMIT :limit")
    List<GameEntity> getPopularGames(int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertGame(GameEntity game);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertGames(List<GameEntity> games);

    @Update
    int updateGame(GameEntity game);

    @Delete
    int deleteGame(GameEntity game);

    @Query("DELETE FROM games")
    int deleteAllGames();

    @Query("DELETE FROM games WHERE game_rom = :gameRom")
    int deleteGameByRom(String gameRom);
}
