package com.ingcorp.webhard.models;

import com.google.gson.annotations.SerializedName;

public class VersionResponse {

    @SerializedName("root")
    private VersionRoot root;

    public VersionRoot getRoot() {
        return root;
    }

    public void setRoot(VersionRoot root) {
        this.root = root;
    }

    public static class VersionRoot {
        @SerializedName("isCheck")
        private boolean isCheck;

        @SerializedName("packageName")
        private String packageName;

        @SerializedName("nowVersionCode")
        private int nowVersionCode;

        @SerializedName("adBannerUse")
        private boolean adBannerUse;

        @SerializedName("adFullCnt")
        private int adFullCnt;

        @SerializedName("adFullCoinCnt")
        private int adFullCoinCnt;

        @SerializedName("gameListVersion")
        private int gameListVersion;

        // Getters
        public boolean isCheckEnabled() {
            return isCheck;
        }

        public String getPackageName() {
            return packageName;
        }

        public int getNowVersionCode() {
            return nowVersionCode;
        }

        public int getNowVersionCodeInt() {
            return nowVersionCode;
        }

        public boolean isAdBannerEnabled() {
            return adBannerUse;
        }

        public int getAdFullCnt() {
            return adFullCnt;
        }

        public int getAdFullCoinCnt() {
            return adFullCoinCnt;
        }


        public int getGameListVersionInt() {
            return gameListVersion;
        }

        // Setters
        public void setIsCheck(boolean isCheck) {
            this.isCheck = isCheck;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public void setNowVersionCode(int nowVersionCode) {
            this.nowVersionCode = nowVersionCode;
        }

        public void setAdBannerUse(boolean adBannerUse) {
            this.adBannerUse = adBannerUse;
        }

        public void setAdFullCnt(int adFullCnt) {
            this.adFullCnt = adFullCnt;
        }

        public void setAdFullCoinCnt(int adFullCoinCnt) {
            this.adFullCoinCnt = adFullCoinCnt;
        }

        public void setGameListVersion(int gameListVersion) {
            this.gameListVersion = gameListVersion;
        }

        @Override
        public String toString() {
            return "VersionRoot{" +
                    "isCheck=" + isCheck +
                    ", packageName='" + packageName + '\'' +
                    ", nowVersionCode=" + nowVersionCode +
                    ", adBannerUse=" + adBannerUse +
                    ", adFullCnt=" + adFullCnt +
                    ", adFullCoinCnt=" + adFullCoinCnt +
                    ", gameListVersion=" + gameListVersion +
                    '}';
        }
    }
}