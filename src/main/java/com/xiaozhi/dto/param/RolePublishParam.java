package com.xiaozhi.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 设置已发布角色（端侧可拉取）
 */
@Data
@Schema(description = "发布角色参数")
public class RolePublishParam {

    @NotNull
    @Schema(description = "要发布的角色ID列表（最多2个）", example = "[1,2]")
    private List<Integer> roleIds;
}
