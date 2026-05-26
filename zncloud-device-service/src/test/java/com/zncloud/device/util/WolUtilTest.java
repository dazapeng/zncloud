package com.zncloud.device.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WoL 工具类单元测试
 */
class WolUtilTest {

    // ========== MAC 地址解析测试 ==========

    @Test
    @DisplayName("解析连字符格式 MAC 地址")
    void testParseMacAddress_HyphenFormat() {
        byte[] result = WolUtil.parseMacAddress("00-1A-2B-3C-4D-5E");
        assertNotNull(result);
        assertEquals(6, result.length);
        assertArrayEquals(new byte[]{(byte) 0x00, (byte) 0x1A, (byte) 0x2B, (byte) 0x3C, (byte) 0x4D, (byte) 0x5E}, result);
    }

    @Test
    @DisplayName("解析冒号格式 MAC 地址")
    void testParseMacAddress_ColonFormat() {
        byte[] result = WolUtil.parseMacAddress("00:1A:2B:3C:4D:5E");
        assertNotNull(result);
        assertEquals(6, result.length);
        assertArrayEquals(new byte[]{(byte) 0x00, (byte) 0x1A, (byte) 0x2B, (byte) 0x3C, (byte) 0x4D, (byte) 0x5E}, result);
    }

    @Test
    @DisplayName("解析纯十六进制格式 MAC 地址")
    void testParseMacAddress_RawHexFormat() {
        byte[] result = WolUtil.parseMacAddress("001A2B3C4D5E");
        assertNotNull(result);
        assertEquals(6, result.length);
        assertArrayEquals(new byte[]{(byte) 0x00, (byte) 0x1A, (byte) 0x2B, (byte) 0x3C, (byte) 0x4D, (byte) 0x5E}, result);
    }

    @Test
    @DisplayName("解析全零 MAC 地址")
    void testParseMacAddress_AllZero() {
        byte[] result = WolUtil.parseMacAddress("00:00:00:00:00:00");
        assertNotNull(result);
        assertEquals(6, result.length);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0}, result);
    }

    @Test
    @DisplayName("解析全 FF MAC 地址")
    void testParseMacAddress_AllFF() {
        byte[] result = WolUtil.parseMacAddress("FF-FF-FF-FF-FF-FF");
        assertNotNull(result);
        assertEquals(6, result.length);
        for (byte b : result) {
            assertEquals((byte) 0xFF, b);
        }
    }

    @Test
    @DisplayName("解析混合大小写 MAC 地址")
    void testParseMacAddress_MixedCase() {
        byte[] result = WolUtil.parseMacAddress("Aa:bB:Cc:Dd:Ee:Ff");
        assertNotNull(result);
        assertEquals(6, result.length);
        assertEquals((byte) 0xAA, result[0]);
        assertEquals((byte) 0xBB, result[1]);
        assertEquals((byte) 0xCC, result[2]);
        assertEquals((byte) 0xDD, result[3]);
        assertEquals((byte) 0xEE, result[4]);
        assertEquals((byte) 0xFF, result[5]);
    }

    // ========== 无效 MAC 地址测试 ==========

    @Test
    @DisplayName("null MAC 地址抛出异常")
    void testParseMacAddress_Null() {
        assertThrows(IllegalArgumentException.class, () -> WolUtil.parseMacAddress(null));
    }

    @Test
    @DisplayName("空 MAC 地址抛出异常")
    void testParseMacAddress_Empty() {
        assertThrows(IllegalArgumentException.class, () -> WolUtil.parseMacAddress(""));
        assertThrows(IllegalArgumentException.class, () -> WolUtil.parseMacAddress("   "));
    }

    @Test
    @DisplayName("过短的 MAC 地址抛出异常")
    void testParseMacAddress_TooShort() {
        assertThrows(IllegalArgumentException.class, () -> WolUtil.parseMacAddress("00-1A-2B-3C"));
    }

    @Test
    @DisplayName("过长的 MAC 地址抛出异常")
    void testParseMacAddress_TooLong() {
        assertThrows(IllegalArgumentException.class, () -> WolUtil.parseMacAddress("00:1A:2B:3C:4D:5E:6F"));
    }

    @Test
    @DisplayName("包含无效字符的 MAC 地址抛出异常")
    void testParseMacAddress_InvalidHex() {
        assertThrows(IllegalArgumentException.class, () -> WolUtil.parseMacAddress("00-1A-2B-3C-4D-XY"));
    }

    @ParameterizedTest
    @CsvSource({
            "'00-1A-2B-3C-4D'",
            "'00:1A:2B:3C:4D'",
            "'001A2B3C4D'",
    })
    @DisplayName("长度不足的 MAC 地址抛出异常")
    void testParseMacAddress_Incomplete(String invalidMac) {
        assertThrows(IllegalArgumentException.class, () -> WolUtil.parseMacAddress(invalidMac));
    }

    // ========== 魔术包构建测试 ==========

    @Test
    @DisplayName("构建标准 WoL 魔术包")
    void testBuildMagicPacket() {
        byte[] macBytes = new byte[]{(byte) 0x00, (byte) 0x1A, (byte) 0x2B, (byte) 0x3C, (byte) 0x4D, (byte) 0x5E};
        byte[] packet = WolUtil.buildMagicPacket(macBytes);

        // 总长度：6字节前缀 + 16 * 6字节MAC = 102字节
        assertEquals(102, packet.length);

        // 前6字节应为 0xFF
        for (int i = 0; i < 6; i++) {
            assertEquals((byte) 0xFF, packet[i]);
        }

        // 后续应为16次重复的MAC地址
        for (int i = 0; i < 16; i++) {
            int offset = 6 + i * 6;
            assertEquals((byte) 0x00, packet[offset]);
            assertEquals((byte) 0x1A, packet[offset + 1]);
            assertEquals((byte) 0x2B, packet[offset + 2]);
            assertEquals((byte) 0x3C, packet[offset + 3]);
            assertEquals((byte) 0x4D, packet[offset + 4]);
            assertEquals((byte) 0x5E, packet[offset + 5]);
        }
    }

    @Test
    @DisplayName("构建 WoL 魔术包 - 全FF MAC 地址")
    void testBuildMagicPacket_AllFF() {
        byte[] macBytes = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        byte[] packet = WolUtil.buildMagicPacket(macBytes);

        assertEquals(102, packet.length);

        // 整个包应该全是 0xFF
        for (byte b : packet) {
            assertEquals((byte) 0xFF, b);
        }
    }

    @Test
    @DisplayName("构建 WoL 魔术包 - 全零 MAC 地址")
    void testBuildMagicPacket_AllZero() {
        byte[] macBytes = new byte[]{0, 0, 0, 0, 0, 0};
        byte[] packet = WolUtil.buildMagicPacket(macBytes);

        assertEquals(102, packet.length);

        // 前6字节为 0xFF
        for (int i = 0; i < 6; i++) {
            assertEquals((byte) 0xFF, packet[i]);
        }

        // 后续96字节应为 0x00
        for (int i = 6; i < 102; i++) {
            assertEquals(0, packet[i]);
        }
    }

    @Test
    @DisplayName("null MAC 字节数组抛出异常")
    void testBuildMagicPacket_Null() {
        assertThrows(IllegalArgumentException.class, () -> WolUtil.buildMagicPacket(null));
    }

    @Test
    @DisplayName("长度错误的 MAC 字节数组抛出异常")
    void testBuildMagicPacket_WrongLength() {
        byte[] wrongMac = new byte[]{0x00, 0x1A, 0x2B};
        assertThrows(IllegalArgumentException.class, () -> WolUtil.buildMagicPacket(wrongMac));
    }

    // ========== WoLResult 测试 ==========

    @Test
    @DisplayName("WoLResult.success() 创建成功结果")
    void testWolResult_Success() {
        WolUtil.WolResult result = WolUtil.WolResult.success();
        assertTrue(result.isSuccess());
        assertNull(result.getMessage());
    }

    @Test
    @DisplayName("WoLResult.failure() 创建失败结果")
    void testWolResult_Failure() {
        WolUtil.WolResult result = WolUtil.WolResult.failure("网络错误");
        assertFalse(result.isSuccess());
        assertEquals("网络错误", result.getMessage());
    }

    // ========== 集成场景测试 ==========

    @Test
    @DisplayName("解析+构建完整链路测试")
    void testParseAndBuild_CompleteFlow() {
        // 多种输入格式都应该得到同样的魔术包
        byte[] packet1 = WolUtil.buildMagicPacket(WolUtil.parseMacAddress("00-1A-2B-3C-4D-5E"));
        byte[] packet2 = WolUtil.buildMagicPacket(WolUtil.parseMacAddress("00:1A:2B:3C:4D:5E"));
        byte[] packet3 = WolUtil.buildMagicPacket(WolUtil.parseMacAddress("001A2B3C4D5E"));

        assertArrayEquals(packet1, packet2);
        assertArrayEquals(packet2, packet3);
        assertEquals(102, packet1.length);
    }
}
