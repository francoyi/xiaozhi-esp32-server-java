package com.xiaozhi.dao;

import com.xiaozhi.entity.SysFirmwareRelease;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 固件发布 数据层
 */
public interface FirmwareReleaseMapper {

    /**
     * 查询固件发布（用于后台管理/调试；当前仅供服务层使用）
     */
    List<SysFirmwareRelease> query(SysFirmwareRelease release);

    /**
     * 获取指定 chip + type 维度的“最新且启用”的固件。
     * - 优先精确匹配 type（板型/机型）
     * - 若找不到精确匹配，再返回 type 为空的通用固件
     */
    SysFirmwareRelease selectLatest(@Param("chipModelName") String chipModelName,
                                    @Param("type") String type);
}
