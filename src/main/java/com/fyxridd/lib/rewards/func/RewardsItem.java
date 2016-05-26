package com.fyxridd.lib.rewards.func;

import com.fyxridd.lib.core.api.MessageApi;
import com.fyxridd.lib.core.api.PerApi;
import com.fyxridd.lib.core.api.PlayerApi;
import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.config.Setter;
import com.fyxridd.lib.core.api.fancymessage.FancyMessage;
import com.fyxridd.lib.core.realname.NotReadyException;
import com.fyxridd.lib.func.api.func.Default;
import com.fyxridd.lib.func.api.func.Func;
import com.fyxridd.lib.func.api.func.FuncType;
import com.fyxridd.lib.rewards.RewardsPlugin;
import com.fyxridd.lib.rewards.config.LangConfig;
import com.fyxridd.lib.rewards.config.RewardsConfig;
import com.fyxridd.lib.rewards.model.RewardsUser;
import com.fyxridd.lib.show.item.api.Info;
import com.fyxridd.lib.show.item.api.ShowApi;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@FuncType("item")
public class RewardsItem {
    private LangConfig langConfig;
    private RewardsConfig rewardsConfig;
    
    public RewardsItem() {
        //添加配置监听
        ConfigApi.addListener(RewardsPlugin.instance.pn, LangConfig.class, new Setter<LangConfig>() {
            @Override
            public void set(LangConfig value) {
                langConfig = value;
            }
        });
        ConfigApi.addListener(RewardsPlugin.instance.pn, RewardsConfig.class, new Setter<RewardsConfig>() {
            @Override
            public void set(RewardsConfig value) {
                rewardsConfig = value;
            }
        });
    }
    
    /**
     * 查看目标玩家的奖励列表
     */
    @Func("seeList")
    public void seeList(CommandSender sender, String tar, @Default("1") int page) {
        if (!(sender instanceof Player)) return;
        Player p = (Player) sender;
        
        //目标玩家存在性检测
        try {
            tar = PlayerApi.getRealName(p, tar);
        } catch (NotReadyException e) {
            return;
        }
        if (tar == null) return;
        //查看其它玩家奖励列表权限检测
        if (!p.getName().equals(tar) && !PerApi.checkHasPer(p.getName(), rewardsConfig.getInfoOtherPer())) return;
        //目标玩家没有未获取的奖励列表
        int maxPage = RewardsPlugin.instance.getRewardsManager().getRewardsUserSize(tar);
        if (maxPage <= 0) {
            MessageApi.send(p, get(p.getName(), 655), true);
            return;
        }
        //页面检测
        if (page < 1 || page > maxPage) {
            MessageApi.send(p, get(p.getName(), 705, maxPage), true);
            return;
        }
        Info info = getInfo(page-1, infHash);
        if (info != null) {
            String type = getKey(info, infHash);
            if (type == null) return;//异常
            try {
                RewardsUser ru = RewardsPlugin.instance.getRewardsManager().getRewardsUser(tar, type)

                //创建操作栏
                Inventory inv = Bukkit.createInventory(p, 9, "none");

                //提示物品
                ItemStack infoItem = new ItemStack(instance.infoItem, page, (short)instance.infoItemSmallId);
                ItemMeta im = infoItem.getItemMeta();
                im.setDisplayName(infoOwner+tar);
                List<String> lore = new ArrayList<>();
                lore.add(infoName+type);
                lore.add(infoGold+ru.getMoney());
                lore.add(infoExp+ru.getExp());
                lore.add(infoLevel+ru.getLevel());
                lore.add(infoTip+ru.getTip());
                im.setLore(lore);
                infoItem.setItemMeta(im);
                inv.setItem(infoPos, infoItem);

                //前一页
                if (page > 1) {
                    ItemStack preItem = pre.clone();
                    inv.setItem(prePos, preItem);
                }

                //获取奖励
                if (tar.equals(p.getName())) {
                    ItemStack getItem = get.clone();
                    inv.setItem(getPos, getItem);
                }

                //后一页
                if (page < maxPage) {
                    ItemStack nextItem = next.clone();
                    inv.setItem(nextPos, nextItem);
                }

                //删除奖励
                if (PerApi.has(p, adminPer)) {
                    ItemStack delItem = del.clone();
                    inv.setItem(delPos, delItem);
                }

                ShowApi.open(p, info, null, inv);
            } catch (Exception e) {
            }
        }
    }
    
    private FancyMessage get(String player, int id, Object... args) {
        return langConfig.getLang().get(player, id, args);
    }
}
