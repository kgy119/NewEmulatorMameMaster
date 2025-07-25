package com.ing.clubdamoim.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class NetworkClient {
    private static final String BASE_URL = "http://retrogamemaster.net/";
    private static Retrofit retrofit;

    // 일반 API 호출용 (기존)
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // 로깅 인터셉터 설정
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttpClient 설정
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Retrofit 빌더
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // 프로그레스 있는 다운로드용 (새로 추가)
    public static Retrofit getProgressRetrofitInstance(ProgressInterceptor.ProgressListener progressListener) {
        // 로깅 인터셉터 설정 (기존과 동일)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC); // 다운로드시에는 BASIC으로 설정

        // 프로그레스 인터셉터 생성
        ProgressInterceptor progressInterceptor = new ProgressInterceptor(progressListener);

        // 프로그레스 지원 OkHttpClient 설정
        OkHttpClient progressClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
                .addNetworkInterceptor(progressInterceptor) // 프로그레스 인터셉터 추가
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS) // 다운로드용으로 읽기 타임아웃 증가
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(progressClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // 기존 API 서비스 (변경 없음)
    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }

    // 프로그레스 지원 API 서비스 (새로 추가)
    public static ApiService getProgressApiService(ProgressInterceptor.ProgressListener progressListener) {
        return getProgressRetrofitInstance(progressListener).create(ApiService.class);
    }
}