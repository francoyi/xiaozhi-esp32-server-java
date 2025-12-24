package com.xiaozhi.service.impl;

import com.github.pagehelper.PageHelper;
import com.xiaozhi.common.cache.CacheHelper;
import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.dao.RoleMapper;
import com.xiaozhi.entity.SysRole;
import com.xiaozhi.service.SysRoleService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

/**
 * 角色操作
 *
 * @author Joey
 *
 */

@Service
public class SysRoleServiceImpl extends BaseServiceImpl implements SysRoleService {
    private final static String CACHE_NAME = "XiaoZhi:SysRole";

    @Resource
    private RoleMapper roleMapper;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Resource
    private CacheHelper cacheHelper;

    /**
     * 添加角色
     *
     * @param role
     * @return
     */
    @Override
    @Transactional
    public int add(SysRole role) {
        // 默认未发布
        if (role != null && role.getPublished() == null) {
            role.setPublished(0);
        }

        // 如果当前配置被设置为默认，则将同类型同用户的其他配置设置为非默认
        if (role.getIsDefault() != null && role.getIsDefault().equals("1")) {
            roleMapper.resetDefault(role);
        }
        // 添加角色
        return roleMapper.add(role);
    }

    /**
     * 查询角色信息
     * 指定分页信息
     * @param role
     * @param pageFilter
     * @return
     */
    @Override
    public List<SysRole> query(SysRole role, PageFilter pageFilter) {
        if(pageFilter != null){
            PageHelper.startPage(pageFilter.getStart(), pageFilter.getLimit());
        }
        return roleMapper.query(role);
    }

    /**
     * 更新角色信息
     *
     * @param role
     * @return
     */
    @Override
    @Transactional
    public int update(SysRole role) {
        // 如果当前配置被设置为默认，则将同类型同用户的其他配置设置为非默认
        if (role.getIsDefault() != null && role.getIsDefault().equals("1")) {
            roleMapper.resetDefault(role);
        }
        
        int result = roleMapper.update(role);
        
        // 如果更新成功且roleId不为空，直接将更新后的完整对象加载到缓存中
        if (result > 0 && role.getRoleId() != null && cacheManager != null) {
            // 直接从数据库查询最新数据
            SysRole updatedRole = roleMapper.selectRoleById(role.getRoleId());
            // 手动更新缓存
            if (updatedRole != null) {
                Cache cache = cacheManager.getCache(CACHE_NAME);
                if (cache != null) {
                    cache.put(updatedRole.getRoleId(), updatedRole);
                }
            }
        }

        return result;
    }

    /**
     * 删除角色
     *
     * @param roleId
     * @return
     */
    @Override
    @Transactional
    public int deleteById(Integer roleId) {
        int result = roleMapper.deleteById(roleId);

        // 如果删除成功，清除缓存
        if (result > 0 && cacheManager != null) {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                cache.evict(roleId);
            }
        }

        return result;
    }

    @Override
    public SysRole selectRoleById(Integer roleId) {
        // 使用分布式锁防止缓存击穿(特别是默认角色的高并发访问)
        return cacheHelper.getWithLock(
            "role:" + roleId,
            // 从缓存获取
            () -> {
                if (cacheManager != null) {
                    Cache cache = cacheManager.getCache(CACHE_NAME);
                    if (cache != null) {
                        Cache.ValueWrapper wrapper = cache.get(roleId);
                        if (wrapper != null) {
                            return (SysRole) wrapper.get();
                        }
                    }
                }
                return null;
            },
            // 从数据库获取
            () -> {
                SysRole role = roleMapper.selectRoleById(roleId);

                // 手动写入缓存
                if (role != null && cacheManager != null) {
                    Cache cache = cacheManager.getCache(CACHE_NAME);
                    if (cache != null) {
                        cache.put(roleId, role);
                    }
                }

                return role;
            }
        );
    }

    /**
     * 查询当前用户已发布(端侧引用)的角色（最多2个）
     */
    @Override
    public List<SysRole> listPublishedRolesByUserId(Integer userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<SysRole> roles = roleMapper.listPublishedRolesByUserId(userId);
        return roles == null ? new ArrayList<>() : roles;
    }

    /**
     * 覆盖式设置发布角色：先清空该用户所有角色 published，再设置选中的为 1。
     * 业务规则：最多2个。
     */
    @Override
    @Transactional
    public void setPublishedRoleIds(Integer userId, List<Integer> roleIds) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        if (roleIds != null && roleIds.size() > 2) {
            throw new IllegalArgumentException("最多只能发布2个角色");
        }

        // 1) 先清空
        roleMapper.clearPublishedByUserId(userId);

        // 2) 再设置
        if (roleIds != null && !roleIds.isEmpty()) {
            roleMapper.setPublishedByRoleIds(userId, roleIds);

            // 可选：更新这些 roleId 的缓存（若开启缓存）
            if (cacheManager != null) {
                Cache cache = cacheManager.getCache(CACHE_NAME);
                if (cache != null) {
                    for (Integer rid : roleIds) {
                        if (rid == null) continue;
                        SysRole latest = roleMapper.selectRoleById(rid);
                        if (latest != null) {
                            cache.put(rid, latest);
                        }
                    }
                }
            }
        }
    }
}