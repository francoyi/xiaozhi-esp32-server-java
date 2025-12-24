package com.xiaozhi.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 覆盖式设置已发布角色集合（最多2个）
 */
@Data
@Schema(description = "设置已发布角色集合参数")
public class RolePublishSetParam {

    @Schema(description = "要发布的角色ID列表（最终集合，最多2个）", example = "[12,18]")
    private List<Integer> roleIds;
}
