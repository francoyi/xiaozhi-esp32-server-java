package com.xiaozhi.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 覆盖式设置“已发布到端侧”的角色列表。
 *
 * 约束：最多2个（为空表示全部取消发布）。
 */
public class RolePublishParam {

    @Schema(description = "需要发布的角色ID列表（最多2个；为空表示全部取消发布）")
    @NotNull(message = "roleIds 不能为空")
    @Size(max = 2, message = "最多只能发布2个角色")
    private List<Integer> roleIds;

    public List<Integer> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Integer> roleIds) {
        this.roleIds = roleIds;
    }
}
