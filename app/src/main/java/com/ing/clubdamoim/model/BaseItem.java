package com.ing.clubdamoim.model;

public abstract class BaseItem {
    public static final int TYPE_GAME = 0;
    public static final int TYPE_AD = 1;

    public abstract int getItemType();
}
