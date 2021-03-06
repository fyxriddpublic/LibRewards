package com.fyxridd.lib.rewards.config;

import com.fyxridd.lib.core.api.config.basic.Path;

public class RewardsConfig {
    @Path("adminPer")
    private String adminPer;

    @Path("usePer")
    private String usePer;

    @Path("infoOtherPer")
    private String infoOtherPer;

    @Path("tipRewards")
    private boolean tipRewards;

    public String getAdminPer() {
        return adminPer;
    }

    public String getUsePer() {
        return usePer;
    }

    public String getInfoOtherPer() {
        return infoOtherPer;
    }

    public boolean isTipRewards() {
        return tipRewards;
    }
}
