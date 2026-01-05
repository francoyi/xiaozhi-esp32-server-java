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
   * 获取某个用户所有已发布(published=1)的角色，用于设备端同步。
   */
  List<SysRole> listPublishedByUserId(Integer userId);

  /**
   * 覆盖式设置用户的已发布角色（最多2个；传空列表表示全部取消发布）。
   *
   * @param userId  当前用户ID
   * @param roleIds 要发布的角色ID列表
   * @return 更新后已发布的角色列表
   */
  List<SysRole> setPublishedRoles(Integer userId, List<Integer> roleIds);

}