package com.ingcorp.webhard.network;

import com.ingcorp.webhard.models.GameListResponse;
import com.ingcorp.webhard.models.VersionResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {

    /**
     * 버전 정보 조회
     */
    @GET("version.json")
    Call<VersionResponse> getVersionInfo();

    /**
     * 게임 리스트 조회
     */
    @GET("list.json")
    Call<GameListResponse> getGameList();
}