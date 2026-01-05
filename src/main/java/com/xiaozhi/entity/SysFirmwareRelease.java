package com.xiaozhi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 固件发布表（用于 OTA 返回不同机型/版本的固件信息）。
 *
 * 说明：
 * - chipModelName 对应 SysDevice.chipModelName（如 esp32 / esp32s3）。
 * - type 对应 SysDevice.type（板型/机型，可为空表示该 chip 通用固件）。
 * - versionCode 用于可靠的版本比较（避免字符串比较出错）。
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({ "startTime", "endTime", "start", "limit" })
@Schema(description = "固件发布信息")
public class SysFirmwareRelease extends Base<SysFirmwareRelease> {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "芯片型号（如 esp32/esp32s3）")
    private String chipModelName;

    @Schema(description = "板型/机型（对应 sys_device.type，可为空）")
    private String type;

    @Schema(description = "语义化版本号（如 1.0.2）")
    private String version;

    @Schema(description = "整型版本号，用于比较（如 10002）")
    private Integer versionCode;

    @Schema(description = "下载路径（以 / 开头），如 /firmware/esp32/xiaozhi_1.0.2.bin")
    private String downloadPath;

    @Schema(description = "固件 SHA256（可选）")
    private String sha256;

    @Schema(description = "是否最新：1=是，0=否")
    private Integer isLatest;

    @Schema(description = "状态：1=启用，0=停用")
    private Integer status;

    @Schema(description = "最低支持版本号（可选），低于则建议/强制升级")
    private Integer minSupportedCode;

//    @Schema(description = "创建时间")
//    private Data createTime;
//
//    @Schema(description = "更新时间")
//    private Data updateTime;
}
