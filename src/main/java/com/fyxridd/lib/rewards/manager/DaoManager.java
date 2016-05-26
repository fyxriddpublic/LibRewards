package com.fyxridd.lib.rewards.manager;

import com.fyxridd.lib.core.api.SqlApi;
import com.fyxridd.lib.rewards.RewardsPlugin;
import com.fyxridd.lib.rewards.mapper.RewardsUserMapper;
import com.fyxridd.lib.rewards.model.RewardsUser;
import org.apache.ibatis.session.SqlSession;

import java.io.File;
import java.util.*;

public class DaoManager {
    public DaoManager() {
        SqlApi.registerMapperXml(new File(RewardsPlugin.instance.dataPath, "RewardsUserMapper.xml"));
    }

    public List<RewardsUser> getRewardsUsers(String name) {
        if (name == null) return new ArrayList<>();

        SqlSession session = SqlApi.getSqlSessionFactory().openSession();
        try {
            RewardsUserMapper mapper = session.getMapper(RewardsUserMapper.class);
            return mapper.selectAll(name);
        } finally {
            session.close();
        }
    }

    public void deletes(Collection<RewardsUser> c) {
        if (c == null || c.isEmpty()) return;

        SqlSession session = SqlApi.getSqlSessionFactory().openSession();
        try {
            RewardsUserMapper mapper = session.getMapper(RewardsUserMapper.class);
            for (RewardsUser user:c) mapper.delete(user);
        } finally {
            session.commit();
            session.close();
        }
    }

    public void saveOrUpdates(Collection<RewardsUser> c) {
        if (c == null || c.isEmpty()) return;

        SqlSession session = SqlApi.getSqlSessionFactory().openSession();
        try {
            RewardsUserMapper mapper = session.getMapper(RewardsUserMapper.class);
            for (RewardsUser user:c) {
                if (!mapper.exist(user.getName(), user.getType())) mapper.insert(user);
                else mapper.update(user);
            }
        } finally {
            session.commit();
            session.close();
        }
    }
}
