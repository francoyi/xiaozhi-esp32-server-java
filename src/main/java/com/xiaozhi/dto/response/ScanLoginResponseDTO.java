package com.xiaozhi.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 扫码登录结果
 */
@Data
@Schema(description = "扫码登录结果")
public class ScanLoginResponseDTO {

    @Schema(description = "该设备是否已绑定到某个账户")
    private boolean bound;

    @Schema(description = "二维码中的code")
    private String code;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "已绑定时返回的登录信息")
    private LoginResponseDTO login;

    public static ScanLoginResponseDTO unbound(String code, String deviceId) {
        ScanLoginResponseDTO dto = new ScanLoginResponseDTO();
        dto.bound = false;
        dto.code = code;
        dto.deviceId = deviceId;
        return dto;
    }

    public static ScanLoginResponseDTO bound(String code, String deviceId, LoginResponseDTO login) {
        ScanLoginResponseDTO dto = new ScanLoginResponseDTO();
        dto.bound = true;
        dto.code = code;
        dto.deviceId = deviceId;
        dto.login = login;
        return dto;
    }
}
