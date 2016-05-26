package com.fyxridd.lib.rewards.func;

import com.fyxridd.lib.core.api.ItemApi;
import com.fyxridd.lib.rewards.model.RewardsUser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fyxridd.lib.core.api.MessageApi;
import com.fyxridd.lib.core.api.PerApi;
import com.fyxridd.lib.core.api.PlayerApi;
import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.config.Setter;
import com.fyxridd.lib.core.api.fancymessage.FancyMessage;
import com.fyxridd.lib.core.realname.NotReadyException;
import com.fyxridd.lib.func.api.func.Func;
import com.fyxridd.lib.func.api.func.FuncType;
import com.fyxridd.lib.rewards.RewardsPlugin;
import com.fyxridd.lib.rewards.config.LangConfig;
import com.fyxridd.lib.rewards.config.RewardsConfig;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

@FuncType("cmd")
public class RewardsCmd {
    private LangConfig langConfig;
    private RewardsConfig rewardsConfig;
    
    public RewardsCmd() {
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
     * 领取奖励
     */
    @Func("get")
    public void get(CommandSender sender, String type) {
        if (!(sender instanceof Player)) return;
        Player p = (Player) sender;
        String name = p.getName();
        //检测初始化
        checkInit(p.getName());
        //奖励类型不存在
        Map<String, RewardsUser> rewardsHash = userHash.get(name);
        RewardsUser rewardsUser = RewardsPlugin.instance.getRewardsManager().getRewardsUser(name, type);
        if (rewardsUser == null) {
            MessageApi.send(p, get(name, 635), true);
            return;
        }
        int money = rewardsUser.getMoney();
        int exp = rewardsUser.getExp();
        int level = rewardsUser.getLevel();
        Map<Integer, ItemStack> itemsHash = rewardsUser.getItemsHash();
        //背包空格检测
        PlayerInventory inv = p.getInventory();
        int emptySlots = ItemApi.getEmptySlots(inv);
        //背包空格不够
        if (emptySlots < itemsHash.size()) {
            MessageApi.send(p, get(name, 50, itemsHash.size()), true);
            return;
        }
        //成功
        //退出界面
        ShowApi.exit(p, false);
        //删除
        RewardsPlugin.instance.getRewardsManager().remove(name, type);
        //money
        if (money > 0) {
            EcoApi.add(p.getName(), money);
            MessageApi.send(p, get(name, 55, money), false);
        }
        //exp
        if (exp > 0) {
            p.giveExp(exp);
            MessageApi.send(p, get(name, 60, exp), false);
        }
        //level
        if (level > 0) {
            p.giveExpLevels(level);
            MessageApi.send(p, get(name, 65, level), false);
        }
        //item
        for (int i:itemsHash.keySet()) {
            ItemStack is = itemsHash.get(i);
            inv.addItem(is);
            MessageApi.send(p, get(name, 70, is.getAmount(), NamesApi.getItemName(is)), false);
        }
        //更新背包
        p.updateInventory();
        //tip
        ShowApi.tip(p, get(640), false);
        //检测显示下个列表
        if (rewardsHash.size() > 0) delayShow(p, name, 1);
    }
    
    /**
     * 给目标玩家添加奖励
     */
    @Func("add")
    public void add(CommandSender sender, String tar, String plugin, String type) {
        Player p = null;
        if (sender instanceof Player) p = (Player) sender;
        String name = null;
        if (p != null) name = p.getName();
        //权限检测
        if (p != null && !PerApi.checkHasPer(name, rewardsConfig.getAdminPer())) return;
        //目标玩家存在性检测
        try {
            tar = PlayerApi.getRealName(p, tar);
        } catch (NotReadyException e) {
            return;
        }
        if (tar == null) return;
        //添加
        if (RewardsPlugin.instance.getRewardsManager().addRewards(tar, plugin, type, null, true, false)) MessageApi.send(sender, get(name, 685), true);
        else MessageApi.send(sender, get(name, 690), true);
    }
    
    /**
     * 删除目标玩家的奖励
     */
    @Func("delete")
    public void delete(CommandSender sender, String tar, String type) {
        Player p = null;
        if (sender instanceof Player) p = (Player) sender;
        String name = null;
        if (p != null) name = p.getName();
        //权限检测
        if (p != null && !PerApi.checkHasPer(name, rewardsConfig.getAdminPer())) return;
        //目标玩家存在性检测
        try {
            tar = PlayerApi.getRealName(p, tar);
        } catch (NotReadyException e) {
            return;
        }
        if (tar == null) return;
        //移除
        if (RewardsPlugin.instance.getRewardsManager().remove(tar, type)) MessageApi.send(sender, get(name, 665), true);
        else MessageApi.send(sender, get(name, 670), true);
    }
    
    private FancyMessage get(String player, int id, Object... args) {
        return langConfig.getLang().get(player, id, args);
    }
}
