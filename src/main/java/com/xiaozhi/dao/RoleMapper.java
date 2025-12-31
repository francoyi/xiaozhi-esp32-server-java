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

  int update(SysRole role);

  int resetDefault(SysRole role);

  int add(SysRole role);

  SysRole selectRoleById(Integer roleId);

  int deleteById(@Param("roleId") Integer roleId);
}