package com.zncloud.common.util.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * HMAC-SHA256 工具类
 * 用于 AK/SK 签名验证和 Webhook 签名
 */
public class HmacSha256Util {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final int KEY_SECRET_BYTE_LENGTH = 32;

    /**
     * 计算 HMAC-SHA256 签名，返回 Base64 编码字符串
     */
    public static String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 signing failed", e);
        }
    }

    /**
     * 验证 HMAC-SHA256 签名
     */
    public static boolean verify(String data, String secret, String expectedSignature) {
        String actualSignature = sign(data, secret);
        return MessageDigest.isEqual(
                actualSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成随机 AccessKey Secret (Base64 编码，32字节)
     */
    public static String generateKeySecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[KEY_SECRET_BYTE_LENGTH];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 生成随机 AccessKey ID (32位十六进制字符串)
     */
    public static String generateKeyId() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return "AK" + HexFormat.of().formatHex(bytes).toUpperCase();
    }

    /**
     * 生成随机 Webhook Secret (32字节 Base64)
     */
    public static String generateWebhookSecret() {
        return generateKeySecret();
    }
}
