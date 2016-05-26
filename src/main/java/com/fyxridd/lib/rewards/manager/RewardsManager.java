package com.fyxridd.lib.rewards.manager;

import com.fyxridd.lib.core.api.CoreApi;
import com.fyxridd.lib.core.api.ItemApi;
import com.fyxridd.lib.core.api.MessageApi;
import com.fyxridd.lib.core.api.UtilApi;
import com.fyxridd.lib.core.api.config.ConfigApi;
import com.fyxridd.lib.core.api.config.Setter;
import com.fyxridd.lib.core.api.fancymessage.FancyMessage;
import com.fyxridd.lib.enchants.api.EnchantsApi;
import com.fyxridd.lib.func.api.FuncApi;
import com.fyxridd.lib.items.api.ItemsApi;
import com.fyxridd.lib.rewards.RewardsPlugin;
import com.fyxridd.lib.rewards.config.LangConfig;
import com.fyxridd.lib.rewards.func.RewardsCmd;
import com.fyxridd.lib.rewards.func.RewardsItem;
import com.fyxridd.lib.rewards.model.RewardsInfo;
import com.fyxridd.lib.rewards.model.RewardsUser;
import com.fyxridd.lib.show.item.api.Info;
import com.fyxridd.lib.show.item.api.OptionClickEventHandler;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.EventExecutor;

import java.io.File;
import java.util.*;

public class RewardsManager implements OptionClickEventHandler {
    //RewardsUser读取规则:
    //玩家一加入游戏,就检测从数据库中读取所有的RewardsUser到缓存

    //配置
    private LangConfig langConfig;

    private boolean tipRewards;
    private ItemStack pre,get,next,del;
    private int infoPos, prePos, getPos, nextPos, delPos;
    private int infoItem, infoItemSmallId;
    private String infoOwner, infoName, infoGold, infoExp, infoLevel, infoTip;

    //插件,类型,奖励信息
    private Map<String, Map<String, RewardsInfo>> rewardsHash = new HashMap<>();

    //缓存

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

        //注册功能
        FuncApi.register(RewardsPlugin.instance.pn, new RewardsCmd());
        FuncApi.register(RewardsPlugin.instance.pn, new RewardsItem());

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
                    saveAll();
                }
            }, RewardsPlugin.instance);
            //玩家加入
            Bukkit.getPluginManager().registerEvent(PlayerJoinEvent.class, RewardsPlugin.instance, EventPriority.LOWEST, new EventExecutor() {
                @Override
                public void execute(Listener listener, Event e) throws EventException {
                    PlayerJoinEvent event = (PlayerJoinEvent) e;
                    //检测初始化
                    checkInit(event.getPlayer().getName());
                    //提示领取奖励
                    if (tipRewards && getRewardsUserSize(event.getPlayer().getName()) > 0) MessageApi.send(event.getPlayer(), get(event.getPlayer().getName(), 660), true);
                }
            }, RewardsPlugin.instance);
        }
    }

    @Override
    public void onOptionClick(OptionClickEvent e) {
        try {
            Info info = e.getInfo();
            int cmd = e.getPos()-info.getInv().getSize();
            if (cmd >= 0) {
                if (cmd == prePos) {
                    Player p = e.getP();
                    Inventory inv = info.getInv(p);
                    int size = info.getInv().getSize();
                    ItemStack infoItem = inv.getItem(size);
                    String tar = infoItem.getItemMeta().getDisplayName().substring(infoOwner.length());
                    int page = infoItem.getAmount()-1;
                    delayShow(p, tar, page);
                    e.setWillClose(true);
                }else if (cmd == getPos) {
                    Player p = e.getP();
                    Inventory inv = info.getInv(p);
                    int size = info.getInv().getSize();
                    ItemStack infoItem = inv.getItem(size);
                    String type = infoItem.getItemMeta().getLore().get(0).substring(infoName.length());
                    delayGet(p, type);
                    e.setWillClose(true);
                }else if (cmd == nextPos) {
                    Player p = e.getP();
                    Inventory inv = info.getInv(p);
                    int size = info.getInv().getSize();
                    ItemStack infoItem = inv.getItem(size);
                    String tar = infoItem.getItemMeta().getDisplayName().substring(infoOwner.length());
                    int page = infoItem.getAmount()+1;
                    delayShow(p, tar, page);
                    e.setWillClose(true);
                }else if (cmd == delPos) {
                    ItemStack infoItem = info.getInv(e.getP()).getItem(info.getInv().getSize());
                    String tar = infoItem.getItemMeta().getDisplayName().substring(infoOwner.length());
                    String type = infoItem.getItemMeta().getLore().get(0).substring(infoName.length());
                    e.getP().chat("/f tp Lib_rewards del "+tar+" "+type);
                    e.setWillClose(true);
                }
            }
        } catch (Exception e1) {
            //do nothing
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
            String str;
            //minMoney,maxMoney
            int minMoney, maxMoney;
            str = ms.getString("money");
            if (str == null || str.isEmpty()) {
                minMoney = 0;
                maxMoney = 0;
            }else {
                minMoney = Integer.parseInt(str.split("\\-")[0]);
                maxMoney = Integer.parseInt(str.split("\\-")[1]);
            }
            //minExp,maxExp
            int minExp, maxExp;
            str = ms.getString("exp");
            if (str == null || str.isEmpty()) {
                minExp = 0;
                maxExp = 0;
            }else {
                minExp = Integer.parseInt(str.split("\\-")[0]);
                maxExp = Integer.parseInt(str.split("\\-")[1]);
            }
            //minLevel,maxLevel
            int minLevel, maxLevel;
            str = ms.getString("level");
            if (str == null || str.isEmpty()) {
                minLevel = 0;
                maxLevel = 0;
            }else {
                minLevel = Integer.parseInt(str.split("\\-")[0]);
                maxLevel = Integer.parseInt(str.split("\\-")[1]);
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
            map.put(type, new RewardsInfo(plugin, type, minMoney, maxMoney, minExp, maxExp, minLevel, maxLevel, itemsPlugin, itemsGetType, enchantsPlugin, enchantsType, tip));
        }
    }

    /**
     * @see com.fyxridd.lib.rewards.api.RewardsApi#addRewards(String, String, String, String, boolean, boolean)
     */
    public boolean addRewards(String tar, String plugin, String type, String show, boolean force, boolean direct) {
        if (tar == null || type == null) return false;
        if (plugin == null) plugin = RewardsPlugin.pn;
        //目标玩家存在性检测
        tar = CoreApi.getRealName(null, tar);
        if (tar == null) return false;
        //获取奖励信息
        RewardsInfo info = getRewardsInfo(plugin, type);
        if (info == null) return false;
        //添加
        HashMap<Integer, ItemStack> itemsHash;
        if (info.itemsPlugin == null) itemsHash = null;
        else {
            itemsHash = new HashMap<>();
            List<ItemStack> itemsList = ItemsApi.getItems(info.itemsPlugin, info.itemsGetType);
            int index = 0;
            for (ItemStack is:itemsList) {
                //附魔
                EnchantsApi.addEnchant(info.enchantsPlugin, info.enchantsType, is);
                itemsHash.put(index++, is);
            }
        }
        return addRewards(plugin, show, tar,
                CoreApi.Random.nextInt(info.maxMoney-info.minMoney+1)+info.minMoney,
                CoreApi.Random.nextInt(info.maxExp-info.minExp+1)+info.minExp,
                CoreApi.Random.nextInt(info.maxLevel-info.minLevel+1)+info.minLevel,
                info.tip, itemsHash, force, direct);
    }

    /**
     * @see com.fyxridd.lib.rewards.api.RewardsApi#addRewards(String, String, String, int, int, int, String, java.util.HashMap, boolean, boolean)
     */
    public boolean addRewards(String plugin, String type, String tar, int money, int exp, int level, String tip, Map<Integer, ItemStack> itemsHash, boolean force, boolean direct) {
        if (tar == null || money < 0 || exp < 0 || level < 0 || CoreApi.getRealName(null, tar) == null) return false;
        //修正
        if (plugin == null) plugin = RewardsPlugin.pn;
        if (money < 0) money = 0;
        if (exp < 0) exp = 0;
        if (level < 0) level = 0;
        if (itemsHash == null) itemsHash = new HashMap<>();
        //检测奖励是否为空
        if (money <= 0 && exp <= 0 && level <= 0 && itemsHash.isEmpty()) {
            CoreApi.sendMsg(tar, get(80), false);
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
                        ShowApi.tip(tarP, get(55, money), false);
                    }
                    //exp
                    if (exp > 0) {
                        tarP.giveExp(exp);
                        ShowApi.tip(tarP, get(60, exp), false);
                    }
                    //level
                    if (level > 0) {
                        tarP.giveExpLevels(level);
                        ShowApi.tip(tarP, get(65, level), false);
                    }
                    //item
                    for (int i:itemsHash.keySet()) {
                        ItemStack is = itemsHash.get(i);
                        pi.addItem(is);
                        ShowApi.tip(tarP, get(70, is.getAmount(), NamesApi.getItemName(is)), false);
                    }
                    //延时更新背包
                    CoreApi.updateInventoryDelay(tarP);
                    //提示
                    ShowApi.tip(tarP, get(640), false);
                    return true;
                }
                //背包空格不够,添加到奖励列表
                ShowApi.tip(tarP, get(90), true);
            }
        }

        //type修正
        if (type == null) type = getNextName(plugin, tar);
        else type = plugin+"-"+type;
        if (tip == null) tip = get(645).getText();
        //保存
        RewardsUser rewardsUser = getRewardsUser(tar, type);
        if (rewardsUser != null && !force) return false;
        if (rewardsUser != null) {
            rewardsUser.setMoney(money);
            rewardsUser.setExp(exp);
            rewardsUser.setLevel(level);
            rewardsUser.setTip(tip);
            rewardsUser.setItemsHash(itemsHash);
            rewardsUser.updateItems();
        }else {
            if (isInDelList(tar, type)) saveAll();//防不同对象同时在删除列表里的bug
            rewardsUser = new RewardsUser(tar, type, money, exp, level, tip, itemsHash);
        }
        //添加缓存
        addToHash(rewardsUser);
        //添加更新
        HashSet<RewardsUser> dels = needDeleteList.get(tar);
        if (dels != null) {
            dels.remove(rewardsUser);
            if (dels.isEmpty()) needDeleteList.remove(tar);
        }
        needUpdateList.add(rewardsUser);
        //提示
        CoreApi.sendMsg(tar, get(695), false);
        return true;
    }

    /**
     * 获取奖励信息
     * @param plugin 插件,不为null
     * @param type 类型,不为null
     * @return 奖励信息,没有返回null
     */
    private RewardsInfo getRewardsInfo(String plugin, String type) {
        try {
            return rewardsHash.get(plugin).get(type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 给玩家添加奖励<br>
     * 包括物品编辑器中的物品
     * @param tar 目标玩家,必须是存在的
     * @param money 钱,>=0
     * @param exp 经验,>=0
     * @param level 等级,>=0
     * @param itemsHash 物品
     * @param tip 说明,可为null
     * @return 是否添加成功
     */
    public boolean give(String tar, int money, int exp, int level, Map<Integer, ItemStack> itemsHash, String tip) {
        //修正
        if (money < 0) money = 0;
        if (exp < 0) exp = 0;
        if (level < 0) level = 0;
        if (tip == null) tip = "";
        //添加
        return addRewards(RewardsPlugin.instance.pn, null, tar, money, exp, level, tip, itemsHash , true, false);
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
        Map<String, RewardsUser> rewardsHash = userHash.get(name);
        RewardsUser user = rewardsHash.remove(type);
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
     * 获取下一个未被使用的名字
     * @param plugin 添加奖励的插件名
     * @param tar 玩家名
     * @return 下一个未被使用的名字(完全)
     */
    private String getNextName(String plugin, String tar) {
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
     * 获取值对应的键
     * @param info 内容,不为null
     * @param hash hash
     * @return 出错返回null
     */
    private String getKey(Info info, HashMap<String, Info> hash) {
        for (Map.Entry<String, Info> entry:hash.entrySet()) {
            if (entry.getValue().equals(info)) return entry.getKey();
        }
        return null;
    }

    /**
     * 获取指定位置的Info
     * @param pos 位置,0到hash的长度-1
     * @param hash hash
     * @return 出错返回null
     */
    private Info getInfo(int pos, HashMap<String, Info> hash) {
        if (hash.size() <= 0) return null;
        if (pos < 0) pos = 0;
        if (pos >= hash.size()) pos = hash.size() -1;
        String key = (String) hash.keySet().toArray()[pos];
        return hash.get(key);
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
    private void delayGet(final Player p, final String type) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(RewardsPlugin.instance, new Runnable() {
            @Override
            public void run() {
                if (p.isOnline()) p.chat("/f re f "+type);
            }
        });
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
     * 检测初始化玩家
     */
    private void checkInit(String name) {
        if (!userHash.containsKey(name)) {
            userHash.put(name, new HashMap<String, RewardsUser>());
            infoHash.put(name, new HashMap<String, Info>());
            //从数据库中读取玩家所有的RewardsUser
            for (RewardsUser user:RewardsPlugin.instance.getDaoManager().getRewardsUsers(name)) {
                //解析
                Map<Integer, ItemStack> itemHash = new HashMap<>();
                for (Map.Entry<Integer, String> entry:user.getItemsData().entrySet()) {
                    itemHash.put(entry.getKey(), ItemsApi.loadItem(entry.getValue()));
                }
                user.setItemsHash(itemHash);
                //添加到缓存
                addToHash(user);
            }
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
        Info info = IconMenuApi.register(get(700, rewardsUser.getName()).getText(), 36, false, instance);
        infHash.put(rewardsUser.getType(), info);
        for (Map.Entry<Integer, ItemStack> entry: rewardsUser.getItemsHash().entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < 36) {
                info.setItem(entry.getKey(), entry.getValue());
            }
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

    private void loadConfig() {
        tipRewards = config.getBoolean("tip");

        //物品显示
        int id, smallId;
        String name;
        List<String> lore;
        ItemMeta im;

        prePos = config.getInt("showRewards.pre.pos");
        String[] temp = config.getString("showRewards.pre.item").split(":");
        if (temp.length == 2) {
            id = Integer.parseInt(temp[0]);
            smallId = Integer.parseInt(temp[1]);
        }else {
            id = Integer.parseInt(temp[0]);
            smallId = 0;
        }
        name = CoreApi.convert(config.getString("showRewards.pre.name"));
        lore = config.getStringList("showRewards.pre.lore");
        for (int i=0;i<lore.size();i++) lore.set(i, CoreApi.convert(lore.get(i)));
        pre = new ItemStack(id, 1, (short)smallId);
        im = CoreApi.EmptyIm.clone();
        im.setDisplayName(name);
        im.setLore(lore);
        pre.setItemMeta(im);

        getPos = config.getInt("showRewards.get.pos");
        temp = config.getString("showRewards.get.item").split(":");
        if (temp.length == 2) {
            id = Integer.parseInt(temp[0]);
            smallId = Integer.parseInt(temp[1]);
        }else {
            id = Integer.parseInt(temp[0]);
            smallId = 0;
        }
        name = CoreApi.convert(config.getString("showRewards.get.name"));
        lore = config.getStringList("showRewards.get.lore");
        for (int i=0;i<lore.size();i++) lore.set(i, CoreApi.convert(lore.get(i)));
        get = new ItemStack(id, 1, (short)smallId);
        im = CoreApi.EmptyIm.clone();
        im.setDisplayName(name);
        im.setLore(lore);
        get.setItemMeta(im);

        nextPos = config.getInt("showRewards.next.pos");
        temp = config.getString("showRewards.next.item").split(":");
        if (temp.length == 2) {
            id = Integer.parseInt(temp[0]);
            smallId = Integer.parseInt(temp[1]);
        }else {
            id = Integer.parseInt(temp[0]);
            smallId = 0;
        }
        name = CoreApi.convert(config.getString("showRewards.next.name"));
        lore = config.getStringList("showRewards.next.lore");
        for (int i=0;i<lore.size();i++) lore.set(i, CoreApi.convert(lore.get(i)));
        next = new ItemStack(id, 1, (short)smallId);
        im = CoreApi.EmptyIm.clone();
        im.setDisplayName(name);
        im.setLore(lore);
        next.setItemMeta(im);

        delPos = config.getInt("showRewards.del.pos");
        temp = config.getString("showRewards.del.item").split(":");
        if (temp.length == 2) {
            id = Integer.parseInt(temp[0]);
            smallId = Integer.parseInt(temp[1]);
        }else {
            id = Integer.parseInt(temp[0]);
            smallId = 0;
        }
        name = CoreApi.convert(config.getString("showRewards.del.name"));
        lore = config.getStringList("showRewards.del.lore");
        for (int i=0;i<lore.size();i++) lore.set(i, CoreApi.convert(lore.get(i)));
        del = new ItemStack(id, 1, (short)smallId);
        im = CoreApi.EmptyIm.clone();
        im.setDisplayName(name);
        im.setLore(lore);
        del.setItemMeta(im);

        infoPos = config.getInt("showRewards.info.pos");
        infoItem = config.getInt("showRewards.info.item");
        infoItemSmallId = config.getInt("showRewards.info.smallId");
        infoOwner = CoreApi.convert(config.getString("showRewards.info.owner"));
        infoName = CoreApi.convert(config.getString("showRewards.info.name"));
        infoGold = CoreApi.convert(config.getString("showRewards.info.gold"));
        infoExp = CoreApi.convert(config.getString("showRewards.info.exp"));
        infoLevel = CoreApi.convert(config.getString("showRewards.info.level"));
        infoTip = CoreApi.convert(config.getString("showRewards.info.tip"));

        //读取奖励信息
        String path = config.getString("path");
        File file = new File(path);
        file.getParentFile().mkdirs();
        YamlConfiguration saveConfig = CoreApi.loadConfigByUTF8(file);
        if (saveConfig == null) {
            ConfigApi.log(RewardsPlugin.pn, "path is error");
            return;
        }
        reloadRewards(RewardsPlugin.pn, saveConfig);
        //重新读取提示
        TransactionApi.reloadTips(RewardsPlugin.pn);
    }

    private FancyMessage get(String player, int id, Object... args) {
        return langConfig.getLang().get(player, id, args);
    }
}
