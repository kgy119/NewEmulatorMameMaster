package com.ing.clubdamoim.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.ing.clubdamoim.database.entity.Game;
import java.util.List;

@Dao
public interface GameDao {

    @Query("SELECT * FROM games")
    List<Game> getAllGames();

    @Query("SELECT * FROM games WHERE game_cate = :category")
    List<Game> getGamesByCategory(String category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGame(Game game);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGames(List<Game> games);

    @Query("DELETE FROM games")
    void deleteAllGames();

    @Query("SELECT COUNT(*) FROM games")
    int getGameCount();
}