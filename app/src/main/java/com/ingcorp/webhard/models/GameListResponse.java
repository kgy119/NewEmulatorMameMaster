package com.ingcorp.webhard.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GameListResponse {

    @SerializedName("root")
    private GameListRoot root;

    public GameListRoot getRoot() {
        return root;
    }

    public void setRoot(GameListRoot root) {
        this.root = root;
    }

    public static class GameListRoot {
        @SerializedName("result_code")
        private String resultCode;

        @SerializedName("all_total")
        private int allTotal;

        @SerializedName("list")
        private List<Game> list;

        public String getResultCode() {
            return resultCode;
        }

        public int getAllTotal() {
            return allTotal;
        }

        public List<Game> getList() {
            return list;
        }

        public void setResultCode(String resultCode) {
            this.resultCode = resultCode;
        }

        public void setAllTotal(int allTotal) {
            this.allTotal = allTotal;
        }

        public void setList(List<Game> list) {
            this.list = list;
        }

        @Override
        public String toString() {
            return "GameListRoot{" +
                    "resultCode='" + resultCode + '\'' +
                    ", allTotal=" + allTotal +
                    ", list=" + (list != null ? list.size() + " games" : "null") +
                    '}';
        }
    }
}
