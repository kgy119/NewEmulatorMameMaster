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
        @SerializedName("check")
        private boolean check;

        @SerializedName("packageName")
        private String packageName;

        @SerializedName("nowVersionCode")
        private int nowVersionCode;

        @SerializedName("gameListVersion")
        private int gameListVersion;

        // 광고 설정 관련 필드들
        @SerializedName("adBannerUse")
        private boolean adBannerUse;

        @SerializedName("adFullCnt")
        private int adFullCnt;

        @SerializedName("adFullCoinCnt")
        private int adFullCoinCnt;

        @SerializedName("adNativeCnt")
        private int adNativeCnt;

        // 기존 getter/setter들
        public boolean isCheck() {
            return check;
        }

        public void setCheck(boolean check) {
            this.check = check;
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

        public int getGameListVersion() {
            return gameListVersion;
        }

        public void setGameListVersion(int gameListVersion) {
            this.gameListVersion = gameListVersion;
        }

        // 광고 설정 관련 getter/setter들
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

        public int getAdNativeCnt() {
            return adNativeCnt;
        }

        public void setAdNativeCnt(int adNativeCnt) {
            this.adNativeCnt = adNativeCnt;
        }

        @Override
        public String toString() {
            return "Root{" +
                    "check=" + check +
                    ", packageName='" + packageName + '\'' +
                    ", nowVersionCode=" + nowVersionCode +
                    ", gameListVersion=" + gameListVersion +
                    ", adBannerUse=" + adBannerUse +
                    ", adFullCnt=" + adFullCnt +
                    ", adFullCoinCnt=" + adFullCoinCnt +
                    ", adNativeCnt=" + adNativeCnt +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "VersionResponse{" +
                "root=" + root +
                '}';
    }
}