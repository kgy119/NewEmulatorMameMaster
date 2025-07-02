package com.ingcorp.webhard.models;

import com.google.gson.annotations.SerializedName;

public class VersionResponse {

    @SerializedName("root")
    private VersionRoot root;

    public VersionRoot getRoot() { return root; }
    public void setRoot(VersionRoot root) { this.root = root; }

    public static class VersionRoot {
        @SerializedName("result_code")
        private String resultCode;

        @SerializedName("isCheck")
        private String isCheck;

        @SerializedName("nowVersionCode")
        private String nowVersionCode;

        @SerializedName("packageName")
        private String packageName;

        @SerializedName("adBannerUse")
        private String adBannerUse;

        @SerializedName("adFullCnt")
        private String adFullCnt;

        @SerializedName("adFullCoinCnt")
        private String adFullCoinCnt;

        @SerializedName("gameListVersion")
        private String gameListVersion;

        // Getters
        public String getResultCode() { return resultCode; }
        public String getIsCheck() { return isCheck; }
        public String getNowVersionCode() { return nowVersionCode; }
        public String getPackageName() { return packageName; }
        public String getAdBannerUse() { return adBannerUse; }
        public String getAdFullCnt() { return adFullCnt; }
        public String getAdFullCoinCnt() { return adFullCoinCnt; }
        public String getGameListVersion() { return gameListVersion; }

        // 편의 메소드
        public int getNowVersionCodeInt() {
            try {
                return Integer.parseInt(nowVersionCode);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public boolean isCheckEnabled() {
            return "Y".equals(isCheck);
        }

        public boolean isAdBannerEnabled() {
            return "Y".equals(adBannerUse);
        }

        @Override
        public String toString() {
            return "VersionRoot{" +
                    "resultCode='" + resultCode + '\'' +
                    ", isCheck='" + isCheck + '\'' +
                    ", nowVersionCode='" + nowVersionCode + '\'' +
                    ", packageName='" + packageName + '\'' +
                    ", gameListVersion='" + gameListVersion + '\'' +
                    '}';
        }
    }
}

