package com.ingcorp.webhard.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.ingcorp.webhard.database.entity.Game;
import java.util.List;

@Dao
public interface GameDao {

    @Query("SELECT * FROM games")
    List<Game> getAllGames();

    @Query("SELECT * FROM games WHERE game_cate = :category")
    List<Game> getGamesByCategory(String category);

    @Query("SELECT * FROM games WHERE game_id = :gameId")
    Game getGameById(String gameId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGame(Game game);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGames(List<Game> games);

    @Query("DELETE FROM games")
    void deleteAllGames();

    @Query("SELECT COUNT(*) FROM games")
    int getGameCount();
}