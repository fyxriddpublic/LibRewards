package com.fyxridd.lib.rewards.manager;

import com.fyxridd.lib.core.api.SqlApi;
import com.fyxridd.lib.rewards.RewardsPlugin;

import java.io.File;

public class DaoManager {
    public DaoManager() {
        SqlApi.registerMapperXml(new File(RewardsPlugin.instance.dataPath, ""));
    }
}
