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

  int update(SysRole role);

  int resetDefault(SysRole role);

  int add(SysRole role);

  SysRole selectRoleById(Integer roleId);

  int deleteById(@Param("roleId") Integer roleId);

  /** 清空用户的发布标记 */
  int clearPublishedByUserId(@Param("userId") Integer userId);

  /** 批量设置用户的发布标记 */
  int setPublishedByRoleIds(@Param("userId") Integer userId, @Param("roleIds") List<Integer> roleIds);

  /** 查询用户已发布角色（最多2个） */
  List<SysRole> listPublishedRolesByUserId(@Param("userId") Integer userId);
}