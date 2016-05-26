package com.fyxridd.lib.rewards.model;

public class RewardsInfo {
    private String plugin;
    private String type;

    private int minMoney, maxMoney;
    private int minExp, maxExp;
    private int minLevel, maxLevel;

    private String itemsPlugin, itemsGetType;
    private String enchantsPlugin, enchantsType;

    private String tip;

    public RewardsInfo(String plugin, String type,
                       int minMoney, int maxMoney,
                       int minExp, int maxExp,
                       int minLevel, int maxLevel,
                       String itemsPlugin, String itemsGetType,
                       String enchantsPlugin, String enchantsType,
                       String tip) {
        super();
        this.plugin = plugin;
        this.type = type;
        this.minMoney = minMoney;
        this.maxMoney = maxMoney;
        this.minExp = minExp;
        this.maxExp = maxExp;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.tip = tip;
        this.itemsPlugin = itemsPlugin;
        this.itemsGetType = itemsGetType;
        this.enchantsPlugin = enchantsPlugin;
        this.enchantsType = enchantsType;
    }

    public String getPlugin() {
        return plugin;
    }

    public String getType() {
        return type;
    }

    public int getMinMoney() {
        return minMoney;
    }

    public int getMaxMoney() {
        return maxMoney;
    }

    public int getMinExp() {
        return minExp;
    }

    public int getMaxExp() {
        return maxExp;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public String getItemsPlugin() {
        return itemsPlugin;
    }

    public String getItemsGetType() {
        return itemsGetType;
    }

    public String getEnchantsPlugin() {
        return enchantsPlugin;
    }

    public String getEnchantsType() {
        return enchantsType;
    }

    public String getTip() {
        return tip;
    }
}
