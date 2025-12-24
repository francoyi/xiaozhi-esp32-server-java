package com.xiaozhi.service;

import com.xiaozhi.common.web.PageFilter;
import com.xiaozhi.entity.SysRole;

import java.util.List;

/**
 * 角色查询/更新
 * 
 * @author Joey
 * 
 */
public interface SysRoleService {

  /**
   * 添加角色
   * 
   * @param role
   * @return
   */
  int add(SysRole role);

  /**
   * 查询角色信息
   * 指定分页信息
   * @param role
   * @param pageFilter
   * @return
   */
  List<SysRole> query(SysRole role, PageFilter pageFilter);

  /**
   * 更新角色信息
   *
   * @param role
   * @return
   */
  int update(SysRole role);

  /**
   * 删除角色
   *
   * @param roleId
   * @return
   */
  int deleteById(Integer roleId);

  SysRole selectRoleById(Integer roleId);

  /**
   * 查询当前用户已发布(端侧可拉取)的角色列表（最多2个）
   */
  List<SysRole> listPublishedRolesByUserId(Integer userId);

  /**
   * 覆盖式设置当前用户已发布的角色ID集合（最多2个）
   * <p>
   * 规则：先将该用户所有角色 published 置 0，再将 roleIds 对应的角色置 1。
   */
  void setPublishedRoleIds(Integer userId, List<Integer> roleIds);

}