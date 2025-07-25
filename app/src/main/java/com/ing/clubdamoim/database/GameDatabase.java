package com.ing.clubdamoim.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import com.ing.clubdamoim.database.dao.GameDao;
import com.ing.clubdamoim.database.entity.Game;

@Database(
        entities = {Game.class},
        version = 1,
        exportSchema = false
)
public abstract class GameDatabase extends RoomDatabase {

    private static GameDatabase INSTANCE;

    public abstract GameDao gameDao();

    public static synchronized GameDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    GameDatabase.class,
                    "game_database"
            ).build();
        }
        return INSTANCE;
    }
}