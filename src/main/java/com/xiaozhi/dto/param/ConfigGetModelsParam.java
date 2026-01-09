package com.xiaozhi.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 获取模型列表请求参数
 *
 * @author Joey
 */
@Data
@Schema(description = "获取模型列表请求参数")
public class ConfigGetModelsParam {

    @Schema(description = "服务提供商的API地址", example = "https://dashscope.aliyuncs.com/compatible-mode/v1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "API地址不能为空")
    private String apiUrl;

    @Schema(description = "服务提供商分配的ApiKey", example = "sk-xxxxx", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ApiKey不能为空")
    private String apiKey;
}
