package com.fyxridd.lib.rewards.mapper;

import com.fyxridd.lib.rewards.model.RewardsUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RewardsUserMapper {
    /**
     * 检测是否存在
     */
    boolean exist(@Param("name") String name, @Param("type") String type);

    List<RewardsUser> selectAll(@Param("name") String name);

    void insert(@Param("user") RewardsUser user);

    void update(@Param("user") RewardsUser user);

    void delete(@Param("user") RewardsUser user);
}
