package com.fyxridd.lib.rewards;

import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.plugin.SimplePlugin;
import com.fyxridd.lib.rewards.config.LangConfig;
import com.fyxridd.lib.rewards.config.RewardsConfig;
import com.fyxridd.lib.rewards.manager.DaoManager;
import com.fyxridd.lib.rewards.manager.RewardsManager;

public class RewardsPlugin extends SimplePlugin{
    public static RewardsPlugin instance;
    
    private RewardsManager rewardsManager;
    private DaoManager daoManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        //注册配置
        ConfigApi.register(pn, LangConfig.class);
        ConfigApi.register(pn, RewardsConfig.class);
        
        rewardsManager = new RewardsManager();
        daoManager = new DaoManager();
        
        super.onEnable();
    }

    public RewardsManager getRewardsManager() {
        return rewardsManager;
    }

    public DaoManager getDaoManager() {
        return daoManager;
    }
}