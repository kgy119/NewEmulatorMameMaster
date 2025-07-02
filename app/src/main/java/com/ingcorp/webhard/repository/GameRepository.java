
package com.ingcorp.webhard.repository;

import android.content.Context;
import android.util.Log;

import com.ingcorp.webhard.database.AppDatabase;
import com.ingcorp.webhard.database.GameDao;
import com.ingcorp.webhard.database.GameEntity;
import com.ingcorp.webhard.models.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameRepository {

    private static final String TAG = "GameRepository";
    private final GameDao gameDao;
    private final ExecutorService executor;

    public GameRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        gameDao = database.gameDao();
        executor = Executors.newFixedThreadPool(4);
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    /**
     * 모든 게임 조회
     */
    public void getAllGames(RepositoryCallback<List<GameEntity>> callback) {
        executor.execute(() -> {
            try {
                List<GameEntity> games = gameDao.getAllGames();
                callback.onSuccess(games);
            } catch (Exception e) {
                Log.e(TAG, "Error getting all games", e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * 게임 리스트 전체 교체
     */
    public void replaceAllGames(List<Game> newGames, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                // 기존 데이터 삭제
                int deletedCount = gameDao.deleteAllGames();
                Log.d(TAG, "Deleted " + deletedCount + " games");

                // 새 데이터 삽입
                List<GameEntity> gameEntities = new ArrayList<>();
                for (Game game : newGames) {
                    GameEntity entity = new GameEntity(
                            game.getGameId(),
                            game.getGameName(),
                            game.getGameCategory(),
                            game.getGameRom(),
                            game.getGameImg(),
                            game.getGameCount(),
                            game.getGameLength()
                    );
                    gameEntities.add(entity);
                }

                List<Long> insertedIds = gameDao.insertGames(gameEntities);
                Log.d(TAG, "Inserted " + insertedIds.size() + " games");

                callback.onSuccess(insertedIds.size());

            } catch (Exception e) {
                Log.e(TAG, "Error replacing games", e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * ROM 파일명으로 게임 검색
     */
    public void getGameByRom(String gameRom, RepositoryCallback<GameEntity> callback) {
        executor.execute(() -> {
            try {
                GameEntity game = gameDao.getGameByRom(gameRom);
                callback.onSuccess(game);
            } catch (Exception e) {
                Log.e(TAG, "Error getting game by rom: " + gameRom, e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * 인기 게임 조회
     */
    public void getPopularGames(int limit, RepositoryCallback<List<GameEntity>> callback) {
        executor.execute(() -> {
            try {
                List<GameEntity> games = gameDao.getPopularGames(limit);
                callback.onSuccess(games);
            } catch (Exception e) {
                Log.e(TAG, "Error getting popular games", e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * 게임 검색
     */
    public void searchGames(String searchTerm, RepositoryCallback<List<GameEntity>> callback) {
        executor.execute(() -> {
            try {
                List<GameEntity> games = gameDao.searchGames(searchTerm);
                callback.onSuccess(games);
            } catch (Exception e) {
                Log.e(TAG, "Error searching games: " + searchTerm, e);
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * 리소스 정리
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}