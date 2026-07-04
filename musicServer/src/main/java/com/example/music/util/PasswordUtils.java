package com.example.music.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

/**
 * 密码工具类
 * 新密码使用 BCrypt 加密；兼容存量 MD5（32位hex）密码，登录时自动迁移到 BCrypt。
 */
public class PasswordUtils {

    private static final BCryptPasswordEncoder BCRYPT_ENCODER = new BCryptPasswordEncoder();

    /**
     * 加密密码（用于注册/新增/重置密码）
     */
    public static String encode(String rawPassword) {
        return BCRYPT_ENCODER.encode(rawPassword);
    }

    /**
     * 校验密码（用于登录/校验旧密码）
     * 兼容旧 MD5 密码：若匹配成功则视为合法
     *
     * @param rawPassword     用户输入的明文密码
     * @param storedPassword  数据库存储的密码（BCrypt 或旧 MD5）
     * @return true 校验通过
     */
    public static boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        // 旧 MD5 密码：32位十六进制
        if (isLegacyMd5(storedPassword)) {
            return DigestUtils.md5DigestAsHex(rawPassword.getBytes()).equals(storedPassword);
        }
        return BCRYPT_ENCODER.matches(rawPassword, storedPassword);
    }

    /**
     * 判断密码是否为旧 MD5 格式（需要迁移）
     */
    public static boolean isLegacyMd5(String storedPassword) {
        return storedPassword != null
                && storedPassword.length() == 32
                && storedPassword.matches("^[0-9a-fA-F]{32}$");
    }

    public static void main(String[] args) {
        System.out.println("admin123=" + encode("admin123"));
        System.out.println("user1234=" + encode("user1234"));
    }
}
