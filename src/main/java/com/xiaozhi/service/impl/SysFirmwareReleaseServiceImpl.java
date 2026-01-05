package com.xiaozhi.service.impl;

import com.xiaozhi.dao.FirmwareReleaseMapper;
import com.xiaozhi.entity.SysFirmwareRelease;
import com.xiaozhi.service.SysFirmwareReleaseService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysFirmwareReleaseServiceImpl implements SysFirmwareReleaseService {

    @Resource
    private FirmwareReleaseMapper firmwareReleaseMapper;

    @Override
    public List<SysFirmwareRelease> query(SysFirmwareRelease release) {
        return firmwareReleaseMapper.query(release);
    }

    @Override
    public SysFirmwareRelease getLatest(String chipModelName, String type) {
        if (chipModelName == null || chipModelName.isBlank()) {
            return null;
        }
        return firmwareReleaseMapper.selectLatest(chipModelName.trim().toLowerCase(),
                type == null ? null : type.trim());
    }
}
