package com.fyxridd.lib.rewards.func;

import com.fyxridd.lib.core.api.MessageApi;
import com.fyxridd.lib.core.api.PerApi;
import com.fyxridd.lib.core.api.PlayerApi;
import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.config.Setter;
import com.fyxridd.lib.core.api.fancymessage.FancyMessage;
import com.fyxridd.lib.func.api.FuncApi;
import com.fyxridd.lib.func.api.func.Default;
import com.fyxridd.lib.func.api.func.Func;
import com.fyxridd.lib.func.api.func.FuncType;
import com.fyxridd.lib.rewards.RewardsPlugin;
import com.fyxridd.lib.rewards.config.ItemConfig;
import com.fyxridd.lib.rewards.config.LangConfig;
import com.fyxridd.lib.rewards.config.RewardsConfig;
import com.fyxridd.lib.rewards.model.RewardsUser;
import com.fyxridd.lib.show.item.api.Info;
import com.fyxridd.lib.show.item.api.OptionClickEvent;
import com.fyxridd.lib.show.item.api.OptionClickEventHandler;
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
public class RewardsItem implements OptionClickEventHandler{
    private LangConfig langConfig;
    private RewardsConfig rewardsConfig;
    private ItemConfig itemConfig;

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
        ConfigApi.addListener(RewardsPlugin.instance.pn, ItemConfig.class, new Setter<ItemConfig>() {
            @Override
            public void set(ItemConfig value) {
                itemConfig = value;
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
        tar = PlayerApi.getRealName(p, tar);
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
        String type = RewardsPlugin.instance.getRewardsManager().getType(tar, page-1);
        if (type != null) {
            Info info = RewardsPlugin.instance.getRewardsManager().getInfo(tar, type);
            if (info != null) {
                try {
                    RewardsUser ru = RewardsPlugin.instance.getRewardsManager().getRewardsUser(tar, type);

                    //创建操作栏
                    Inventory inv = Bukkit.createInventory(p, 9, "none");

                    //提示物品
                    ItemStack infoItem = itemConfig.getInfo().clone();
                    ItemMeta im = infoItem.getItemMeta();
                    im.setDisplayName(itemConfig.getInfoOwner()+tar);
                    List<String> lore = new ArrayList<>();
                    lore.add(itemConfig.getInfoName()+type);
                    lore.add(itemConfig.getInfoGold()+ru.getMoney());
                    lore.add(itemConfig.getInfoExp()+ru.getExp());
                    lore.add(itemConfig.getInfoLevel()+ru.getLevel());
                    lore.add(itemConfig.getInfoTip()+ru.getTip());
                    im.setLore(lore);
                    infoItem.setItemMeta(im);
                    inv.setItem(itemConfig.getInfoPos(), infoItem);

                    //前一页
                    if (page > 1) {
                        ItemStack preItem = itemConfig.getPre().clone();
                        inv.setItem(itemConfig.getPrePos(), preItem);
                    }

                    //获取奖励
                    if (tar.equals(p.getName())) {
                        ItemStack getItem = itemConfig.getGet().clone();
                        inv.setItem(itemConfig.getGetPos(), getItem);
                    }

                    //后一页
                    if (page < maxPage) {
                        ItemStack nextItem = itemConfig.getNext().clone();
                        inv.setItem(itemConfig.getNextPos(), nextItem);
                    }

                    //删除奖励
                    if (PerApi.has(p.getName(), rewardsConfig.getAdminPer())) {
                        ItemStack delItem = itemConfig.getDel().clone();
                        inv.setItem(itemConfig.getDelPos(), delItem);
                    }

                    ShowApi.open(p, info, null, inv);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onOptionClick(OptionClickEvent e) {
        try {
            Info info = e.getInfo();
            int cmd = e.getPos()-info.getInv().getSize();
            if (cmd >= 0) {
                Player p = e.getP();
                Inventory inv = info.getInv(p);
                int size = info.getInv().getSize();
                ItemStack infoItem = inv.getItem(size);
                String tar = infoItem.getItemMeta().getDisplayName().substring(itemConfig.getInfoOwner().length());
                String type = infoItem.getItemMeta().getLore().get(0).substring(itemConfig.getInfoName().length());
                if (cmd == itemConfig.getPrePos()) {
                    RewardsPlugin.instance.getRewardsManager().delayShow(p, tar, infoItem.getAmount()-1);
                    e.setWillClose(true);
                }else if (cmd == itemConfig.getNextPos()) {
                    RewardsPlugin.instance.getRewardsManager().delayShow(p, tar, infoItem.getAmount()+1);
                    e.setWillClose(true);
                }else if (cmd == itemConfig.getGetPos()) {
                    RewardsPlugin.instance.getRewardsManager().delayGet(p, type);
                    e.setWillClose(true);
                }else if (cmd == itemConfig.getDelPos()) {
                    FuncApi.onFunc(p, com.fyxridd.lib.show.cmd.api.ShowApi.CMD, RewardsPlugin.instance.pn, RewardsCmd.DELETE, tar+" "+type);
                    e.setWillClose(true);
                }
            }
        } catch (Exception e1) {
            //do nothing
        }
    }

    private FancyMessage get(String player, int id, Object... args) {
        return langConfig.getLang().get(player, id, args);
    }
}
