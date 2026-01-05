package com.xiaozhi.utils;

/**
 * 将语义化版本号转换为可比较的整型版本号。
 *
 * 规则（可按需调整）：
 * - 支持："1.2.3" / "v1.2.3" / "1.2" / "1"
 * - versionCode = major * 10000 + minor * 100 + patch
 *   例：1.0.2 -> 10002，1.10.0 -> 11000
 */
public final class VersionCodeUtil {

    private VersionCodeUtil() {
    }

    public static int toVersionCode(String version) {
        if (version == null) {
            return 0;
        }
        String v = version.trim();
        if (v.isEmpty()) {
            return 0;
        }

        // 去掉常见前缀：v/V
        if (v.startsWith("v") || v.startsWith("V")) {
            v = v.substring(1);
        }

        // 去掉可能的后缀：1.2.3-beta -> 1.2.3
        int dash = v.indexOf('-');
        if (dash > 0) {
            v = v.substring(0, dash);
        }

        String[] parts = v.split("\\.");
        int major = parsePart(parts, 0);
        int minor = parsePart(parts, 1);
        int patch = parsePart(parts, 2);

        // 防御：避免溢出
        major = clamp(major, 0, 999);
        minor = clamp(minor, 0, 99);
        patch = clamp(patch, 0, 99);

        return major * 10000 + minor * 100 + patch;
    }

    private static int parsePart(String[] parts, int idx) {
        if (parts == null || idx >= parts.length) {
            return 0;
        }
        String p = parts[idx];
        if (p == null) {
            return 0;
        }
        p = p.trim();
        if (p.isEmpty()) {
            return 0;
        }
        // 只保留数字前缀
        int end = 0;
        while (end < p.length() && Character.isDigit(p.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(p.substring(0, end));
        } catch (Exception ignore) {
            return 0;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
