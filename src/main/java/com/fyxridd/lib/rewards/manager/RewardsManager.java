package com.fyxridd.lib.rewards.manager;

import com.fyxridd.lib.core.api.*;
import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.config.Setter;
import com.fyxridd.lib.core.api.fancymessage.FancyMessage;
import com.fyxridd.lib.core.api.getter.MultiRandomInt;
import com.fyxridd.lib.enchants.api.EnchantsApi;
import com.fyxridd.lib.func.api.FuncApi;
import com.fyxridd.lib.items.api.ItemsApi;
import com.fyxridd.lib.names.api.NamesApi;
import com.fyxridd.lib.rewards.RewardsPlugin;
import com.fyxridd.lib.rewards.config.LangConfig;
import com.fyxridd.lib.rewards.config.RewardsConfig;
import com.fyxridd.lib.rewards.func.RewardsCmd;
import com.fyxridd.lib.rewards.model.RewardsInfo;
import com.fyxridd.lib.rewards.model.RewardsUser;
import com.fyxridd.lib.show.item.api.Info;
import com.fyxridd.lib.show.item.api.ShowApi;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.EventExecutor;

import java.io.File;
import java.util.*;

public class RewardsManager {
    //配置
    private LangConfig langConfig;
    private RewardsConfig rewardsConfig;

    //插件,类型,奖励信息
    private Map<String, Map<String, RewardsInfo>> rewardsHash = new HashMap<>();

    //缓存

    //RewardsUser读取规则: 玩家一加入游戏,就检测从数据库中读取所有的RewardsUser到缓存
    //动态读取
    //玩家名,类型名,奖励
    private Map<String, Map<String, RewardsUser>> userHash = new HashMap<>();
    //玩家名,类型名,奖励页面(与userHash同步)
    private Map<String, Map<String, Info>> infoHash = new HashMap<>();

    //需要更新的列表
    private Set<RewardsUser> needUpdateList = new HashSet<>();
    //需要删除的列表
    private Map<String, Set<RewardsUser>> needDeleteList = new HashMap<>();

    public RewardsManager() {
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

        //计时器: 更新
        Bukkit.getScheduler().scheduleSyncRepeatingTask(RewardsPlugin.instance, new Runnable() {
            @Override
            public void run() {
                saveAll();
            }
        }, 436, 436);

        //注册事件
        {
            //插件停止
            Bukkit.getPluginManager().registerEvent(PluginDisableEvent.class, RewardsPlugin.instance, EventPriority.NORMAL, new EventExecutor() {
                @Override
                public void execute(Listener listener, Event e) throws EventException {
                    if (e instanceof PluginDisableEvent) {
                        PluginDisableEvent event = (PluginDisableEvent) e;
                        if (event.getPlugin().getName().equals(RewardsPlugin.instance.pn)) saveAll();
                    }
                }
            }, RewardsPlugin.instance);
            //玩家加入
            Bukkit.getPluginManager().registerEvent(PlayerJoinEvent.class, RewardsPlugin.instance, EventPriority.LOWEST, new EventExecutor() {
                @Override
                public void execute(Listener listener, Event e) throws EventException {
                    if (e instanceof PlayerJoinEvent) {
                        PlayerJoinEvent event = (PlayerJoinEvent) e;
                        //检测初始化
                        checkInit(event.getPlayer().getName());
                        //提示领取奖励
                        if (rewardsConfig.isTipRewards() && getRewardsUserSize(event.getPlayer().getName()) > 0) MessageApi.send(event.getPlayer(), get(event.getPlayer().getName(), 660), true);
                    }
                }
            }, RewardsPlugin.instance);
        }
    }

    /**
     * @see com.fyxridd.lib.rewards.api.RewardsApi#reloadRewards(String)
     */
    public void reloadRewards(String plugin) {
        if (plugin == null) return;
        YamlConfiguration config = UtilApi.loadConfigByUTF8(new File(new File(CoreApi.pluginPath, plugin), "rewards.yml"));
        if (config == null) return;
        Map<String, RewardsInfo> map = new HashMap<>();
        rewardsHash.put(plugin, map);

        for (String type : config.getValues(false).keySet()) {
            MemorySection ms = (MemorySection) config.get(type);
            //money
            MultiRandomInt money = null;
            {
                String str = ms.getString("money");
                if (str != null && !str.isEmpty()) money = new MultiRandomInt(str);
            }
            //exp
            MultiRandomInt exp = null;
            {
                String str = ms.getString("exp");
                if (str != null && !str.isEmpty()) exp = new MultiRandomInt(str);
            }
            //level
            MultiRandomInt level = null;
            {
                String str = ms.getString("level");
                if (str != null && !str.isEmpty()) level = new MultiRandomInt(str);
            }
            //items
            String s = ms.getString("itemsType");
            String itemsPlugin = null, itemsGetType = null;
            if (s != null) {
                String[] ss = s.split(":");
                switch (ss.length) {
                    case 1:
                        itemsPlugin = plugin;
                        itemsGetType = ss[0];
                        break;
                    case 2:
                        itemsPlugin = ss[0];
                        itemsGetType = ss[1];
                        break;
                }
            }
            //enchants
            s = ms.getString("enchantsType");
            String enchantsPlugin = null, enchantsType = null;
            if (s != null) {
                String[] ss = s.split(":");
                switch (ss.length) {
                    case 1:
                        enchantsPlugin = plugin;
                        enchantsType = ss[0];
                        break;
                    case 2:
                        enchantsPlugin = ss[0];
                        enchantsType = ss[1];
                        break;
                }
            }
            //tip
            String tip = ms.getString("tip");
            //添加
            map.put(type, new RewardsInfo(plugin, type, money, exp, level, itemsPlugin, itemsGetType, enchantsPlugin, enchantsType, tip));
        }
    }

    /**
     * 获取奖励信息
     * @param plugin 插件,不为null
     * @param type 类型,不为null
     * @return 奖励信息,没有返回null
     */
    public RewardsInfo getRewardsInfo(String plugin, String type) {
        try {
            return rewardsHash.get(plugin).get(type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取玩家未领取的奖励数量
     * @return >=0
     */
    public int getRewardsUserSize(String name) {
        checkInit(name);
        return userHash.get(name).size();
    }

    /**
     * @return 可能为null
     */
    public RewardsUser getRewardsUser(String name, String type) {
        checkInit(name);
        return userHash.get(name).get(type);
    }

    /**
     * 获取玩家指定位置的奖励类型
     * @param name 玩家
     * @param pos 位置,0到hash的长度-1(超出会自动调整)
     * @return 出错返回null
     */
    public String getType(String name, int pos) {
        Map<String, RewardsUser> hash = userHash.get(name);
        if (hash == null) return null;
        if (hash.size() <= 0) return null;
        if (pos < 0) pos = 0;
        if (pos >= hash.size()) pos = hash.size() -1;
        return (String) hash.keySet().toArray()[pos];
    }

    /**
     * 获取玩家指定类型的Info
     * @return 不存在返回null
     */
    public Info getInfo(String name, String type) {
        Map<String, Info> hash = infoHash.get(name);
        if (hash == null) return null;
        return hash.get(type);
    }

    /**
     * @see com.fyxridd.lib.rewards.api.RewardsApi#addRewards(String, String, String, String, boolean, boolean)
     */
    public boolean addRewards(String tar, String plugin, String type, String show, boolean force, boolean direct) {
        if (tar == null || type == null) return false;
        if (plugin == null) plugin = RewardsPlugin.instance.pn;
        //目标玩家存在性检测
        tar = PlayerApi.getRealName(null, tar);
        if (tar == null) return false;
        //获取奖励信息
        RewardsInfo info = getRewardsInfo(plugin, type);
        if (info == null) return false;
        //添加
        Map<Integer, ItemStack> itemsHash = null;
        if (info.getItemsPlugin() != null) {
            itemsHash = new HashMap<>();
            List<ItemStack> itemsList = ItemsApi.getItems(info.getItemsPlugin(), info.getItemsGetType());
            for (int index=0;index<itemsList.size();index++) {
                //附魔
                ItemStack is = itemsList.get(index);
                EnchantsApi.addEnchant(info.getEnchantsPlugin(), info.getEnchantsType(), is);
                itemsHash.put(index, is);
            }
        }
        int money = info.getMoney() != null?info.getMoney().get(0):0;
        int exp = info.getExp() != null?info.getExp().get(0):0;
        int level = info.getLevel() != null?info.getLevel().get(0):0;
        return addRewards(plugin, show, tar,
                money, exp, level,
                info.getTip(), itemsHash, force, direct);
    }

    /**
     * @see com.fyxridd.lib.rewards.api.RewardsApi#addRewards(String, String, String, int, int, int, String, java.util.HashMap, boolean, boolean)
     */
    public boolean addRewards(String plugin, String type, String tar, int money, int exp, int level, String tip, Map<Integer, ItemStack> itemsHash, boolean force, boolean direct) {
        if (tar == null || money < 0 || exp < 0 || level < 0 || PlayerApi.getRealName(null, tar) == null) return false;
        //修正
        if (plugin == null) plugin = RewardsPlugin.instance.pn;
        if (money < 0) money = 0;
        if (exp < 0) exp = 0;
        if (level < 0) level = 0;
        if (itemsHash == null) itemsHash = new HashMap<>();
        //检测奖励是否为空
        if (money <= 0 && exp <= 0 && level <= 0 && itemsHash.isEmpty()) {
            MessageApi.send(tar, get(tar, 80), false);
            return true;
        }
        //直接添加
        if (direct) {
            final Player tarP = Bukkit.getPlayerExact(tar);
            if (tarP != null && !tarP.isDead()) {
                //背包空格检测
                PlayerInventory pi = tarP.getInventory();
                if (ItemApi.getEmptySlots(pi) >= itemsHash.size()) {//背包空格足够
                    //money
                    if (money > 0) {
                        EcoApi.add(tarP.getName(), money);
                        MessageApi.send(tarP, get(tar, 55, money), false);
                    }
                    //exp
                    if (exp > 0) {
                        tarP.giveExp(exp);
                        MessageApi.send(tarP, get(tar, 60, exp), false);
                    }
                    //level
                    if (level > 0) {
                        tarP.giveExpLevels(level);
                        MessageApi.send(tarP, get(tar, 65, level), false);
                    }
                    //item
                    for (int i:itemsHash.keySet()) {
                        ItemStack is = itemsHash.get(i);
                        pi.addItem(is);
                        MessageApi.send(tarP, get(tar, 70, is.getAmount(), NamesApi.getItemName(is)), false);
                    }
                    //延时更新背包
                    PlayerApi.updateInventoryDelay(tarP);
                    //提示
                    MessageApi.send(tarP, get(tar, 640), false);
                    return true;
                }
                //背包空格不够,添加到奖励列表
                MessageApi.send(tarP, get(tar, 90), true);
            }
        }

        //type修正
        if (type == null) type = getNextUnusedName(plugin, tar);
        else type = plugin+"-"+type;
        if (tip == null) tip = get(tar, 645).getText();
        //保存
        RewardsUser rewardsUser = getRewardsUser(tar, type);
        if (rewardsUser != null && !force) return false;
        if (rewardsUser != null) {
            rewardsUser.setMoney(money);
            rewardsUser.setExp(exp);
            rewardsUser.setLevel(level);
            rewardsUser.setTip(tip);
            rewardsUser.setItemsHash(itemsHash);
        }else {
            if (isInDelList(tar, type)) saveAll();//防不同对象同时在删除列表里的bug
            rewardsUser = new RewardsUser(tar, type, money, exp, level, tip, itemsHash);
        }
        //添加缓存
        addToHash(rewardsUser);
        //添加更新
        Set<RewardsUser> dels = needDeleteList.get(tar);
        if (dels != null) {
            dels.remove(rewardsUser);
            if (dels.isEmpty()) needDeleteList.remove(tar);
        }
        needUpdateList.add(rewardsUser);
        //提示
        MessageApi.send(tar, get(tar, 695), false);
        return true;
    }

    /**
     * 移除指定玩家的指定类型奖励
     * @param name 玩家(必须是存在的),不为null
     * @param type 奖励名,不为null
     * @return 是否移除成功
     */
    public boolean remove(String name, String type) {
        //检测初始化
        checkInit(name);
        //从缓存中删除
        RewardsUser user = userHash.get(name).remove(type);
        if (user == null) return false;
        try {
            infoHash.get(name).remove(type);
        } catch (Exception e) {
            //do nothing
        }
        //添加更新
        Set<RewardsUser> dels = needDeleteList.get(name);
        if (dels == null) {
            dels = new HashSet<>();
            needDeleteList.put(name, dels);
        }
        dels.add(user);
        needUpdateList.remove(user);
        return true;
    }

    /**
     * 延时0秒显示
     * @param p 玩家
     * @param tar 查看的目标玩家
     * @param page 页面
     */
    public void delayShow(final Player p, final String tar, final int page) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(RewardsPlugin.instance, new Runnable() {
            @Override
            public void run() {
                if (p.isOnline()) p.chat("/f re c " + tar + " " + page);
            }
        });
    }

    /**
     * 延时0秒获取
     * @param p 玩家
     * @param type 奖励类型
     */
    public void delayGet(final Player p, final String type) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(RewardsPlugin.instance, new Runnable() {
            @Override
            public void run() {
                if (p.isOnline()) FuncApi.onFunc(p, com.fyxridd.lib.show.cmd.api.ShowApi.CMD, RewardsPlugin.instance.pn, RewardsCmd.GET, type);
            }
        });
    }

    /**
     * 获取下一个未被使用的名字
     * @param plugin 添加奖励的插件名
     * @param tar 玩家名
     * @return 下一个未被使用的名字(完全)
     */
    private String getNextUnusedName(String plugin, String tar) {
        //检测初始化
        checkInit(tar);
        //获取
        Map<String, RewardsUser> rewardsHash = userHash.get(tar);
        int index = 1;
        while (rewardsHash.containsKey(plugin+"-"+index) || isInDelList(tar, plugin+"-"+index)) index ++;
        return plugin+"-"+index;
    }

    /**
     * 检测是否在删除列表里
     * @param name 玩家名
     * @param type 类型
     */
    private boolean isInDelList(String name, String type) {
        Set<RewardsUser> dels = needDeleteList.get(name);
        if (dels != null) {
            for (RewardsUser user:dels) {
                if (user.getName().equals(name) && user.getType().equals(type)) return true;
            }
        }
        return false;
    }

    /**
     * 检测初始化玩家
     */
    private void checkInit(String name) {
        if (!userHash.containsKey(name)) {
            userHash.put(name, new HashMap<String, RewardsUser>());
            infoHash.put(name, new HashMap<String, Info>());
            //从数据库中读取玩家所有的RewardsUser
            for (RewardsUser user:RewardsPlugin.instance.getDaoManager().getRewardsUsers(name)) addToHash(user);
        }
    }

    /**
     * 添加RewardsUser到缓存
     * 同时更新Info
     */
    private void addToHash(RewardsUser rewardsUser) {
        //初始化
        Map<String, RewardsUser> rewardsHash = userHash.get(rewardsUser.getName());
        if (rewardsHash == null) {
            rewardsHash = new HashMap<>();
            userHash.put(rewardsUser.getName(), rewardsHash);
        }
        Map<String, Info> infHash = infoHash.get(rewardsUser.getName());
        if (infHash == null) {
            infHash = new HashMap<>();
            infoHash.put(rewardsUser.getName(), infHash);
        }
        //添加
        rewardsHash.put(rewardsUser.getType(), rewardsUser);
        Info info = ShowApi.register(get(rewardsUser.getName(), 700, rewardsUser.getName()).getText(), 36, false, RewardsPlugin.instance.getRewardsItem());
        infHash.put(rewardsUser.getType(), info);
        for (Map.Entry<Integer, ItemStack> entry: rewardsUser.getItemsHash().entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < 36) info.setItem(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 更新
     */
    private void saveAll() {
        //删除
        if (!needDeleteList.isEmpty()) {
            Set<RewardsUser> set = new HashSet<>();
            for (Set<RewardsUser> s:needDeleteList.values()) set.addAll(s);
            RewardsPlugin.instance.getDaoManager().deletes(set);
            needDeleteList.clear();
        }
        //保存
        if (!needUpdateList.isEmpty()) {
            RewardsPlugin.instance.getDaoManager().saveOrUpdates(needUpdateList);
            needUpdateList.clear();
        }
    }

    private FancyMessage get(String player, int id, Object... args) {
        return langConfig.getLang().get(player, id, args);
    }
}
