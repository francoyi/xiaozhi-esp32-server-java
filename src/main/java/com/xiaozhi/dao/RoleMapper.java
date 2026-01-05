package com.xiaozhi.dao;

import java.util.List;

import com.xiaozhi.entity.SysRole;
import org.apache.ibatis.annotations.Param;

/**
 * 角色管理 数据层
 *
 * @author Joey
 *
 */
public interface RoleMapper {
  List<SysRole> query(SysRole role);

  /**
   * 查询某个用户已发布(published=1)的角色列表，供设备端同步。
   */
  List<SysRole> selectPublishedByUserId(@Param("userId") Integer userId);

  /**
   * 清空某个用户的已发布标记（published=0）。
   */
  int resetPublishedByUserId(@Param("userId") Integer userId);

  /**
   * 将指定 roleIds 设置为已发布（published=1）。
   */
  int setPublishedByRoleIds(@Param("userId") Integer userId, @Param("roleIds") List<Integer> roleIds);

  /**
   * 校验 roleIds 是否全部属于当前用户且可用(state=1)。
   */
  int countOwnedActiveRoles(@Param("userId") Integer userId, @Param("roleIds") List<Integer> roleIds);

  int update(SysRole role);

  int resetDefault(SysRole role);

  int add(SysRole role);

  SysRole selectRoleById(Integer roleId);

  int deleteById(@Param("roleId") Integer roleId);
}