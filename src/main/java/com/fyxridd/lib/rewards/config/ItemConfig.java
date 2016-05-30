package com.fyxridd.lib.rewards.config;

import com.fyxridd.lib.core.api.ItemApi;
import com.fyxridd.lib.core.api.UtilApi;
import com.fyxridd.lib.core.api.config.basic.Path;
import com.fyxridd.lib.core.api.config.convert.ConfigConvert;
import com.fyxridd.lib.core.api.config.pipe.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@Path("item")
public class ItemConfig {
    private static class ItemConverter implements ConfigConvert.ConfigConverter<ItemStack> {
        @Override
        public ItemStack convert(String plugin, ConfigurationSection config) throws Exception {
            //item
            int id, smallId;
            String[] temp = config.getString("item").split(":", 2);
            if (temp.length == 2) {
                id = Integer.parseInt(temp[0]);
                smallId = Integer.parseInt(temp[1]);
            }else {
                id = Integer.parseInt(temp[0]);
                smallId = 0;
            }
            ItemStack result = new ItemStack(id, 1, (short)smallId);
            ItemMeta im = ItemApi.EmptyIm.clone();
            //name
            {
                String displayName = config.getString("name");
                if (displayName != null) {
                    im.setDisplayName(UtilApi.convert(displayName));
                }
            }
            //lore
            {
                List<String> lore = config.getStringList("lore");
                if (lore != null && !lore.isEmpty()) {
                    for (int index=0;index<lore.size();index++) lore.set(index, UtilApi.convert(lore.get(index)));
                    im.setLore(lore);
                }
            }
            result.setItemMeta(im);
            return result;
        }
    }

    @Path("info")
    @ConfigConvert(ItemConverter.class)
    private ItemStack info;
    @Path("pre")
    @ConfigConvert(ItemConverter.class)
    private ItemStack pre;
    @Path("get")
    @ConfigConvert(ItemConverter.class)
    private ItemStack get;
    @Path("next")
    @ConfigConvert(ItemConverter.class)
    private ItemStack next;
    @Path("del")
    @ConfigConvert(ItemConverter.class)
    private ItemStack del;

    @Path("info.pos")
    private int infoPos;
    @Path("pre.pos")
    private int prePos;
    @Path("get.pos")
    private int getPos;
    @Path("next.pos")
    private int nextPos;
    @Path("del.pos")
    private int delPos;

    @Path("info.owner")
    @Color
    private String infoOwner;
    @Path("info.name")
    @Color
    private String infoName;
    @Path("info.gold")
    @Color
    private String infoGold;
    @Path("info.exp")
    @Color
    private String infoExp;
    @Path("info.level")
    @Color
    private String infoLevel;
    @Path("info.tip")
    @Color
    private String infoTip;

    public ItemStack getInfo() {
        return info;
    }

    public ItemStack getPre() {
        return pre;
    }

    public ItemStack getGet() {
        return get;
    }

    public ItemStack getNext() {
        return next;
    }

    public ItemStack getDel() {
        return del;
    }

    public int getInfoPos() {
        return infoPos;
    }

    public int getPrePos() {
        return prePos;
    }

    public int getGetPos() {
        return getPos;
    }

    public int getNextPos() {
        return nextPos;
    }

    public int getDelPos() {
        return delPos;
    }

    public String getInfoOwner() {
        return infoOwner;
    }

    public String getInfoName() {
        return infoName;
    }

    public String getInfoGold() {
        return infoGold;
    }

    public String getInfoExp() {
        return infoExp;
    }

    public String getInfoLevel() {
        return infoLevel;
    }

    public String getInfoTip() {
        return infoTip;
    }
}
