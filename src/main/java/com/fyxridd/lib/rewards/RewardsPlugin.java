package com.fyxridd.lib.rewards;

import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.plugin.SimplePlugin;
import com.fyxridd.lib.func.api.FuncApi;
import com.fyxridd.lib.rewards.config.ItemConfig;
import com.fyxridd.lib.rewards.config.LangConfig;
import com.fyxridd.lib.rewards.config.RewardsConfig;
import com.fyxridd.lib.rewards.func.RewardsCmd;
import com.fyxridd.lib.rewards.func.RewardsItem;
import com.fyxridd.lib.rewards.manager.DaoManager;
import com.fyxridd.lib.rewards.manager.RewardsManager;

public class RewardsPlugin extends SimplePlugin{
    public static RewardsPlugin instance;
    
    private RewardsManager rewardsManager;
    private DaoManager daoManager;

    private RewardsCmd rewardsCmd;
    private RewardsItem rewardsItem;
    
    @Override
    public void onEnable() {
        instance = this;
        
        //注册配置
        ConfigApi.register(pn, LangConfig.class);
        ConfigApi.register(pn, RewardsConfig.class);
        ConfigApi.register(pn, ItemConfig.class);
        
        rewardsManager = new RewardsManager();
        daoManager = new DaoManager();

        //注册功能
        rewardsCmd = new RewardsCmd();
        rewardsItem = new RewardsItem();
        FuncApi.register(RewardsPlugin.instance.pn, rewardsCmd);
        FuncApi.register(RewardsPlugin.instance.pn, rewardsItem);

        super.onEnable();
    }

    public RewardsManager getRewardsManager() {
        return rewardsManager;
    }

    public DaoManager getDaoManager() {
        return daoManager;
    }

    public RewardsCmd getRewardsCmd() {
        return rewardsCmd;
    }

    public RewardsItem getRewardsItem() {
        return rewardsItem;
    }
}