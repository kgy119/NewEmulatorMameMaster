package com.ingcorp.webhard.model;

import com.google.gson.annotations.SerializedName;

public class VersionResponse {
    @SerializedName("root")
    private Root root;

    public Root getRoot() {
        return root;
    }

    public void setRoot(Root root) {
        this.root = root;
    }

    public static class Root {
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

        // Getters and Setters
        public boolean isCheck() {
            return isCheck;
        }

        public void setCheck(boolean check) {
            isCheck = check;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public int getNowVersionCode() {
            return nowVersionCode;
        }

        public void setNowVersionCode(int nowVersionCode) {
            this.nowVersionCode = nowVersionCode;
        }

        public boolean isAdBannerUse() {
            return adBannerUse;
        }

        public void setAdBannerUse(boolean adBannerUse) {
            this.adBannerUse = adBannerUse;
        }

        public int getAdFullCnt() {
            return adFullCnt;
        }

        public void setAdFullCnt(int adFullCnt) {
            this.adFullCnt = adFullCnt;
        }

        public int getAdFullCoinCnt() {
            return adFullCoinCnt;
        }

        public void setAdFullCoinCnt(int adFullCoinCnt) {
            this.adFullCoinCnt = adFullCoinCnt;
        }

        public int getGameListVersion() {
            return gameListVersion;
        }

        public void setGameListVersion(int gameListVersion) {
            this.gameListVersion = gameListVersion;
        }
    }
}
