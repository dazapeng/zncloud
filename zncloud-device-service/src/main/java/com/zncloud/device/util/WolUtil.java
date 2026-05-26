package com.zncloud.device.util;

import lombok.extern.slf4j.Slf4j;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Wake-on-LAN 工具类
 * 用于发送网络唤醒魔术包
 */
@Slf4j
public class WolUtil {

    /** WoL 默认端口 */
    public static final int DEFAULT_PORT = 9;
    /** 备用端口 */
    public static final int ALTERNATE_PORT = 7;
    /** 广播地址 */
    public static final String BROADCAST_ADDRESS = "255.255.255.255";
    /** MAC 地址字节数 */
    private static final int MAC_ADDRESS_LENGTH = 6;
    /** 魔术包前缀：6字节 0xFF */
    private static final int PREFIX_LENGTH = 6;
    /** MAC 地址重复次数 */
    private static final int MAC_REPETITIONS = 16;

    /**
     * 发送 WoL 魔术包
     *
     * @param macAddress MAC 地址（支持格式：00-1A-2B-3C-4D-5E, 00:1A:2B:3C:4D:5E, 001A2B3C4D5E）
     * @return 发送结果
     */
    public static WolResult sendMagicPacket(String macAddress) {
        return sendMagicPacket(macAddress, BROADCAST_ADDRESS, DEFAULT_PORT);
    }

    /**
     * 发送 WoL 魔术包到指定广播地址和端口
     *
     * @param macAddress MAC 地址
     * @param broadcastAddress 广播地址
     * @param port 端口
     * @return 发送结果
     */
    public static WolResult sendMagicPacket(String macAddress, String broadcastAddress, int port) {
        try {
            byte[] macBytes = parseMacAddress(macAddress);
            byte[] magicPacket = buildMagicPacket(macBytes);

            InetAddress address = InetAddress.getByName(broadcastAddress);
            DatagramPacket packet = new DatagramPacket(magicPacket, magicPacket.length, address, port);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.send(packet);
            }

            // 也尝试发送到备用端口
            if (port != ALTERNATE_PORT) {
                try {
                    InetAddress altAddress = InetAddress.getByName(broadcastAddress);
                    DatagramPacket altPacket = new DatagramPacket(magicPacket, magicPacket.length, altAddress, ALTERNATE_PORT);
                    try (DatagramSocket altSocket = new DatagramSocket()) {
                        altSocket.setBroadcast(true);
                        altSocket.send(altPacket);
                    }
                } catch (Exception e) {
                    log.debug("发送 WoL 到备用端口 {} 失败: {}", ALTERNATE_PORT, e.getMessage());
                }
            }

            log.info("WoL 魔术包发送成功, MAC: {}, 广播地址: {}, 端口: {}", macAddress, broadcastAddress, port);
            return WolResult.success();
        } catch (IllegalArgumentException e) {
            log.warn("MAC 地址格式无效: {} - {}", macAddress, e.getMessage());
            return WolResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("发送 WoL 魔术包失败, MAC: {}, 广播地址: {}, 端口: {}",
                    macAddress, broadcastAddress, port, e);
            return WolResult.failure("发送 WoL 魔术包失败: " + e.getMessage());
        }
    }

    /**
     * 解析 MAC 地址字符串为字节数组
     * 支持格式：
     * - 00-1A-2B-3C-4D-5E
     * - 00:1A:2B:3C:4D:5E
     * - 001A2B3C4D5E
     *
     * @param macAddress MAC 地址字符串
     * @return 6字节的 MAC 地址
     * @throws IllegalArgumentException 如果 MAC 地址格式无效
     */
    public static byte[] parseMacAddress(String macAddress) {
        if (macAddress == null || macAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("MAC 地址不能为空");
        }

        String cleaned = macAddress.trim();

        // 移除分隔符
        if (cleaned.contains("-") || cleaned.contains(":")) {
            cleaned = cleaned.replaceAll("[-:]", "");
        }

        // 去除可能的空白字符
        cleaned = cleaned.replaceAll("\\s+", "");

        if (cleaned.length() != MAC_ADDRESS_LENGTH * 2) {
            throw new IllegalArgumentException(
                    String.format("MAC 地址长度无效: %s (期望 %d 个十六进制字符, 实际 %d 个)",
                            macAddress, MAC_ADDRESS_LENGTH * 2, cleaned.length()));
        }

        byte[] macBytes = new byte[MAC_ADDRESS_LENGTH];
        for (int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
            int hexIndex = i * 2;
            try {
                macBytes[i] = (byte) Integer.parseInt(cleaned.substring(hexIndex, hexIndex + 2), 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format("MAC 地址包含无效的十六进制字符: %s ('%s' 位置 %d-%d)",
                                macAddress, cleaned.substring(hexIndex, hexIndex + 2), hexIndex, hexIndex + 2));
            }
        }

        return macBytes;
    }

    /**
     * 构建 WoL 魔术包
     * 格式：6字节 0xFF + 16次重复的 MAC 地址
     *
     * @param macBytes MAC 地址字节数组（6字节）
     * @return 魔术包字节数组
     */
    public static byte[] buildMagicPacket(byte[] macBytes) {
        if (macBytes == null || macBytes.length != MAC_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("MAC 地址字节数组长度必须为 " + MAC_ADDRESS_LENGTH);
        }

        byte[] packet = new byte[PREFIX_LENGTH + MAC_ADDRESS_LENGTH * MAC_REPETITIONS];

        // 前缀：6字节 0xFF
        for (int i = 0; i < PREFIX_LENGTH; i++) {
            packet[i] = (byte) 0xFF;
        }

        // 16次重复的 MAC 地址
        for (int i = 0; i < MAC_REPETITIONS; i++) {
            System.arraycopy(macBytes, 0, packet, PREFIX_LENGTH + i * MAC_ADDRESS_LENGTH, MAC_ADDRESS_LENGTH);
        }

        return packet;
    }

    /**
     * WoL 发送结果
     */
    public static class WolResult {
        private final boolean success;
        private final String message;

        private WolResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static WolResult success() {
            return new WolResult(true, null);
        }

        public static WolResult failure(String message) {
            return new WolResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
