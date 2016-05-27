package com.fyxridd.lib.rewards.model;

import com.fyxridd.lib.core.api.getter.MultiRandomInt;

public class RewardsInfo {
    private String plugin;
    private String type;

    //可为null
    private MultiRandomInt money;
    //可为null
    private MultiRandomInt exp;
    //可为null
    private MultiRandomInt level;

    private String itemsPlugin, itemsGetType;
    private String enchantsPlugin, enchantsType;

    private String tip;

    public RewardsInfo(String plugin, String type,
                       MultiRandomInt money,
                       MultiRandomInt exp,
                       MultiRandomInt level,
                       String itemsPlugin, String itemsGetType,
                       String enchantsPlugin, String enchantsType,
                       String tip) {
        super();
        this.plugin = plugin;
        this.type = type;
        this.money = money;
        this.exp = exp;
        this.level = level;
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

    public MultiRandomInt getMoney() {
        return money;
    }

    public MultiRandomInt getExp() {
        return exp;
    }

    public MultiRandomInt getLevel() {
        return level;
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
