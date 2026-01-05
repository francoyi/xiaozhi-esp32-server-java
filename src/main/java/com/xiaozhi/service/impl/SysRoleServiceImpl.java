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
import java.util.Objects;
import java.util.stream.Collectors;

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
     * 设备端同步：返回用户所有已发布(published=1)的角色
     */
    @Override
    public List<SysRole> listPublishedByUserId(Integer userId) {
        if (userId == null) {
            return List.of();
        }
        return roleMapper.selectPublishedByUserId(userId);
    }

    /**
     * 覆盖式设置“已发布到端侧”的角色（最多2个；传空列表表示全部取消发布）。
     */
    @Override
    public List<SysRole> setPublishedRoles(Integer userId, List<Integer> roleIds) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }

        // 允许传 null/空 -> 取消全部发布
        final List<Integer> normalized = (roleIds == null ? List.<Integer>of() : roleIds)
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (normalized.size() > 2) {
            throw new IllegalArgumentException("最多只能发布2个角色");
        }

        // 先清空该用户全部 published
        roleMapper.resetPublishedByUserId(userId);

        // 再设置新的 published
        if (!normalized.isEmpty()) {
            int owned = roleMapper.countOwnedActiveRoles(userId, normalized);
            if (owned != normalized.size()) {
                throw new IllegalArgumentException("发布失败：存在不属于当前用户或已失效的角色");
            }
            roleMapper.setPublishedByRoleIds(userId, normalized);
        }

        return roleMapper.selectPublishedByUserId(userId);
    }
}