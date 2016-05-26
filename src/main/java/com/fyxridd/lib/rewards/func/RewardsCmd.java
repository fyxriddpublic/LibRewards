package com.fyxridd.lib.rewards.func;

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
