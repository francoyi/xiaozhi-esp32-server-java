package com.xiaozhi.dto.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 扫码绑定设备请求参数
 */
@Data
@Schema(description = "扫码绑定设备请求参数")
public class DeviceScanBindParam {

    @Schema(description = "设备验证码", example = "ABCD1234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "设备验证码不能为空")
    private String code;
}
