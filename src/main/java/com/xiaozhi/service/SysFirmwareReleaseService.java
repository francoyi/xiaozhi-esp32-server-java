package com.xiaozhi.service;

import com.xiaozhi.entity.SysFirmwareRelease;

import java.util.List;

/**
 * 固件发布服务
 */
public interface SysFirmwareReleaseService {

    List<SysFirmwareRelease> query(SysFirmwareRelease release);

    /**
     * 获取最新启用的固件发布：
     * - 优先 chip+type 精确匹配
     * - 其次 chip 通用(type 为空)
     */
    SysFirmwareRelease getLatest(String chipModelName, String type);
}
