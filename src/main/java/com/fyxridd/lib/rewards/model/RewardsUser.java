package com.fyxridd.lib.rewards.model;

import com.fyxridd.lib.items.api.ItemsApi;
import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RewardsUser implements Serializable{
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String type;

    private int money;
    private int exp;
    private int level;
    private String tip;
    //可为空列表不为null
    private Map<Integer, String> itemsData;

    //临时,不保存到数据库

    //可为空列表不为null
    private Map<Integer,ItemStack> itemsHash;

    public RewardsUser(){}

    public RewardsUser(String name, String type, int money, int exp,
                       int level, String tip, Map<Integer, ItemStack> itemsHash) {
        super();
        this.name = name;
        this.type = type;
        this.money = money;
        this.exp = exp;
        this.level = level;
        this.tip = tip;
        this.itemsHash = itemsHash;
        updateItemsHashToItemsData();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    public Map<Integer, String> getItemsData() {
        return itemsData;
    }

    public void setItemsData(Map<Integer, String> itemsData) {
        this.itemsData = itemsData;
        updateItemsDataToItemsHash();
    }

    public Map<Integer, ItemStack> getItemsHash() {
        return itemsHash;
    }

    public void setItemsHash(Map<Integer, ItemStack> itemsHash) {
        this.itemsHash = itemsHash;
        updateItemsHashToItemsData();
    }

    /**
     * 根据itemsHash更新itemsData
     */
    private void updateItemsHashToItemsData() {
        this.itemsData = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry:itemsHash.entrySet()) itemsData.put(entry.getKey(), ItemsApi.saveItem(entry.getValue()));
    }

    /**
     * 根据itemsData更新itemsHash
     */
    private void updateItemsDataToItemsHash() {
        itemsHash = new HashMap<>();
        for (Map.Entry<Integer, String> entry:itemsData.entrySet()) itemsHash.put(entry.getKey(), ItemsApi.loadItem(entry.getValue()));
    }
}
