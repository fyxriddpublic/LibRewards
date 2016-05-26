package com.fyxridd.lib.rewards;

import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.plugin.SimplePlugin;
import com.fyxridd.lib.msg.config.LangConfig;
import com.fyxridd.lib.msg.config.MsgConfig;
import com.fyxridd.lib.msg.config.ScoreboardConfig;
import com.fyxridd.lib.msg.manager.InfoManager;
import com.fyxridd.lib.msg.manager.MsgManager;
import com.fyxridd.lib.msg.manager.OuterManager;
import com.fyxridd.lib.msg.manager.ScoreboardManager;

public class RewardsPlugin extends SimplePlugin{
    public static MsgPlugin instance;
    public static boolean libParamsHook;

    private MsgManager msgManager;
    private ScoreboardManager scoreboardManager;
    private InfoManager infoManager;
    
    @Override
    public void onEnable() {
        instance = this;
        try {
            Class.forName("com.fyxridd.lib.params.ParamsPlugin");
            libParamsHook = true;
        } catch (Exception e) {
        }

        //注册配置
        ConfigApi.register(pn, LangConfig.class);
        ConfigApi.register(pn, ScoreboardConfig.class);
        ConfigApi.register(pn, MsgConfig.class);
        
        msgManager = new MsgManager();
        scoreboardManager = new ScoreboardManager();
        infoManager = new InfoManager();
        if (libParamsHook) new OuterManager();
        
        super.onEnable();
    }

    public MsgManager getMsgManager() {
        return msgManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public InfoManager getInfoManager() {
        return infoManager;
    }
}