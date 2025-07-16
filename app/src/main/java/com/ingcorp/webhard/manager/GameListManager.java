package com.ingcorp.webhard.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.ingcorp.webhard.database.GameDatabase;
import com.ingcorp.webhard.database.entity.Game;
import com.ingcorp.webhard.helpers.UtilHelper;
import com.ingcorp.webhard.model.GameListResponse;
import com.ingcorp.webhard.network.NetworkClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameListManager {
    private static final String TAG = "mame00";
    private static final String PREFS_NAME = "game_list_prefs";
    private static final String KEY_GAME_LIST_VERSION = "game_list_version";

    private Context context;
    private GameDatabase database;
    private SharedPreferences preferences;
    private ExecutorService executorService;
    private Handler mainHandler;
    private UtilHelper utilHelper;


    public interface GameListUpdateListener {
        void onUpdateStarted();
        void onUpdateCompleted();
        void onUpdateFailed(String error);
    }

    public GameListManager(Context context) {
        this.context = context;
        this.database = GameDatabase.getInstance(context);
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.utilHelper = UtilHelper.getInstance(context);
    }

    /**
     * 게임리스트 버전 저장
     */
    private void saveGameListVersion(int version) {
        preferences.edit().putInt(KEY_GAME_LIST_VERSION, version).apply();
    }

    /**
     * 게임리스트 업데이트 필요성 체크
     */
    public boolean needsUpdate(int serverVersion) {
        int savedVersion = utilHelper.getSavedGameListVersion();
        return serverVersion != savedVersion;
    }

    public int getCurrentGameListVersion() {
        return utilHelper.getSavedGameListVersion();
    }


    /**
     * 게임리스트 업데이트 수행 (Retrofit 콜백 사용)
     */
    public void updateGameList(int newVersion, GameListUpdateListener listener) {
        if (listener != null) {
            listener.onUpdateStarted();
        }

        NetworkClient.getApiService().getGameList().enqueue(new Callback<GameListResponse>() {
            @Override
            public void onResponse(Call<GameListResponse> call, Response<GameListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GameListResponse gameListResponse = response.body();

                    if ("0000".equals(gameListResponse.getResultCode())) {
                        Log.e(TAG, "response.isSuccessful: " + response.body());
                        // 백그라운드에서 데이터베이스 작업 수행 (Executor 사용)
                        saveGameListToDatabase(gameListResponse, newVersion, new GameListUpdateListener() {
                            @Override
                            public void onUpdateStarted() {
                                // 생략 가능
                            }

                            @Override
                            public void onUpdateCompleted() {
                                // ✅ 게임 리스트 업데이트 성공 시 버전 저장
                                UtilHelper.getInstance(context).saveGameListVersion(newVersion);
                                if (listener != null) {
                                    listener.onUpdateCompleted();
                                }
                            }

                            @Override
                            public void onUpdateFailed(String error) {
                                if (listener != null) {
                                    listener.onUpdateFailed(error);
                                }
                            }
                        });

                    } else {
                        Log.e(TAG, "Server returned error code: " + gameListResponse.getResultCode());
                        if (listener != null) {
                            listener.onUpdateFailed("Server error: " + gameListResponse.getResultCode());
                        }
                    }
                } else {
                    Log.e(TAG, "Game list update failed: " + response.code());
                    if (listener != null) {
                        listener.onUpdateFailed("Network error: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<GameListResponse> call, Throwable t) {
                Log.e(TAG, "Game list update error", t);
                if (listener != null) {
                    listener.onUpdateFailed("Network error: " + t.getMessage());
                }
            }
        });
    }


    /**
     * 게임리스트를 데이터베이스에 저장 (Executor 사용)
     */
    private void saveGameListToDatabase(GameListResponse gameListResponse, int newVersion,
                                        GameListUpdateListener listener) {
        executorService.execute(() -> {
            try {
                // 기존 데이터 모두 삭제
                database.gameDao().deleteAllGames();

                // 새로운 게임 리스트 저장
                List<Game> games = new ArrayList<>();
                for (GameListResponse.GameItem item : gameListResponse.getList()) {
                    Game game = new Game(
                            item.getGameId(),
                            item.getGameName(),
                            item.getGameCate(),
                            item.getGameRom(),
                            item.getGameImg(),
                            item.getGameCnt(),
                            item.getGameLength()
                    );
                    games.add(game);
                }

                database.gameDao().insertGames(games);

                // 버전 정보 저장
                saveGameListVersion(newVersion);

                Log.d(TAG, "Game list updated successfully. " + games.size() + " games saved.");

                // 메인 스레드에서 콜백 호출
                if (listener != null) {
                    mainHandler.post(() -> listener.onUpdateCompleted());
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to save game list", e);

                // 메인 스레드에서 에러 콜백 호출
                if (listener != null) {
                    mainHandler.post(() -> listener.onUpdateFailed("Database save failed: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * 저장된 게임 개수 가져오기 (Executor 사용)
     */
    public void getGameCount(GameCountListener listener) {
        executorService.execute(() -> {
            try {
                int count = database.gameDao().getGameCount();

                // 메인 스레드에서 콜백 호출
                if (listener != null) {
                    mainHandler.post(() -> listener.onGameCountLoaded(count));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get game count", e);
                if (listener != null) {
                    mainHandler.post(() -> listener.onGameCountLoaded(0));
                }
            }
        });
    }

    /**
     * 카테고리별 게임 목록 가져오기
     */
    public void getGamesByCategory(String category, GameListListener listener) {
        executorService.execute(() -> {
            try {
                List<Game> games;
                if ("ALL".equals(category)) {
                    games = database.gameDao().getAllGames();
                } else {
                    games = database.gameDao().getGamesByCategory(category);
                }

                // 메인 스레드에서 콜백 호출
                if (listener != null) {
                    mainHandler.post(() -> listener.onGamesLoaded(games));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get games by category", e);
                if (listener != null) {
                    mainHandler.post(() -> listener.onGamesLoaded(new ArrayList<>()));
                }
            }
        });
    }

    /**
     * 리소스 정리
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public interface GameCountListener {
        void onGameCountLoaded(int count);
    }

    public interface GameListListener {
        void onGamesLoaded(List<Game> games);
    }
}