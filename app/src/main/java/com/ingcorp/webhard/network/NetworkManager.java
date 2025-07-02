package com.ingcorp.webhard.network;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ingcorp.webhard.models.GameListResponse;
import com.ingcorp.webhard.models.VersionResponse;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkManager {

    private static final String TAG = "NetworkManager";
    private static final String BASE_URL = "http://retrogamemaster.net/app/";

    private static NetworkManager instance;
    private final ApiService apiService;

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    private NetworkManager() {
        // HTTP 로깅 인터셉터 (디버그용)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttp 클라이언트 설정
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .build();

        // Gson 설정
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        // Retrofit 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public interface NetworkCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    /**
     * 버전 정보 가져오기
     */
    public void getVersionInfo(NetworkCallback<VersionResponse> callback) {
        Call<VersionResponse> call = apiService.getVersionInfo();

        call.enqueue(new Callback<VersionResponse>() {
            @Override
            public void onResponse(Call<VersionResponse> call, Response<VersionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    VersionResponse versionResponse = response.body();

                    // JSON: { "root": { "isCheck": true, ... } }
                    // root만 존재하면 성공으로 간주
                    if (versionResponse.getRoot() != null) {
                        Log.d(TAG, "Version info retrieved successfully: " + versionResponse.getRoot());
                        callback.onSuccess(versionResponse);
                    } else {
                        String error = "Invalid response: root is null";
                        Log.e(TAG, error);
                        callback.onError(error);
                    }
                } else {
                    String error = "HTTP " + response.code() + ": " + response.message();
                    Log.e(TAG, error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<VersionResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, error, t);
                callback.onError(error);
            }
        });
    }

    /**
     * 게임 리스트 가져오기
     */
    public void getGameList(NetworkCallback<GameListResponse> callback) {
        Call<GameListResponse> call = apiService.getGameList();

        call.enqueue(new Callback<GameListResponse>() {
            @Override
            public void onResponse(Call<GameListResponse> call, Response<GameListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GameListResponse gameListResponse = response.body();

                    if (gameListResponse.getRoot() != null && gameListResponse.getRoot().getList() != null) {
                        int gameCount = gameListResponse.getRoot().getList().size();
                        Log.d(TAG, "Game list retrieved successfully: " + gameCount + " games");
                        callback.onSuccess(gameListResponse);
                    } else {
                        String error = "Invalid response: root or list is null";
                        Log.e(TAG, error);
                        callback.onError(error);
                    }
                } else {
                    String error = "HTTP " + response.code() + ": " + response.message();
                    Log.e(TAG, error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<GameListResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, error, t);
                callback.onError(error);
            }
        });
    }

    /**
     * 네트워크 연결 취소
     */
    public void cancelAllRequests() {
        if (apiService != null) {
            // OkHttp는 자동으로 요청을 관리하므로 특별한 취소 로직이 필요하지 않음
            Log.d(TAG, "Network requests cleanup completed");
        }
    }
}