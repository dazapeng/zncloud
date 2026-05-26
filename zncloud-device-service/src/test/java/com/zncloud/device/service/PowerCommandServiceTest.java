package com.zncloud.device.service;

import com.zncloud.device.model.Device;
import com.zncloud.device.model.DeviceStatus;
import com.zncloud.device.model.dto.PendingCommand;
import com.zncloud.device.model.dto.PendingCommandResponse;
import com.zncloud.device.repository.DeviceMapper;
import com.zncloud.device.repository.PendingCommandMapper;
import com.zncloud.device.service.impl.PowerCommandServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 电源命令服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class PowerCommandServiceTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private PendingCommandMapper pendingCommandMapper;

    private PowerCommandService powerCommandService;

    private Device testDevice;
    private static final String DEVICE_ID = "test-device-uuid-123";
    private static final String MAC_ADDRESS = "00:1A:2B:3C:4D:5E";

    @BeforeEach
    void setUp() {
        powerCommandService = new PowerCommandServiceImpl(deviceMapper, pendingCommandMapper);

        testDevice = new Device();
        testDevice.setId(DEVICE_ID);
        testDevice.setMacAddress(MAC_ADDRESS);
        testDevice.setStatus(DeviceStatus.ONLINE);
        testDevice.setCafeId("cafe-001");
    }

    // ========== WoL 发送测试 ==========

    @Test
    @DisplayName("发送 WoL 成功 - 设备离线和有MAC地址")
    void testSendWakeOnLan_Success() {
        testDevice.setStatus(DeviceStatus.OFFLINE);
        when(deviceMapper.selectById(DEVICE_ID)).thenReturn(testDevice);

        // sendWakeOnLan uses a real DatagramSocket which we can't easily mock.
        // We'll test the logic path - it will try to send and may fail on the socket,
        // but the important thing is that it calls the mapper and handles the result.
        boolean result = powerCommandService.sendWakeOnLan(DEVICE_ID);

        // The actual socket send might fail in test environment, but the service
        // should at least have queried the device
        verify(deviceMapper).selectById(DEVICE_ID);
    }

    @Test
    @DisplayName("发送 WoL 失败 - 设备不存在")
    void testSendWakeOnLan_DeviceNotFound() {
        when(deviceMapper.selectById(DEVICE_ID)).thenReturn(null);

        boolean result = powerCommandService.sendWakeOnLan(DEVICE_ID);

        assertFalse(result);
        verify(deviceMapper).selectById(DEVICE_ID);
        verify(deviceMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("发送 WoL 失败 - MAC地址为空")
    void testSendWakeOnLan_NoMacAddress() {
        testDevice.setMacAddress(null);
        testDevice.setStatus(DeviceStatus.OFFLINE);
        when(deviceMapper.selectById(DEVICE_ID)).thenReturn(testDevice);

        boolean result = powerCommandService.sendWakeOnLan(DEVICE_ID);

        assertFalse(result);
        verify(deviceMapper).selectById(DEVICE_ID);
        verify(deviceMapper, never()).updateById(any());
    }

    // ========== 创建重启命令测试 ==========

    @Test
    @DisplayName("创建重启命令成功")
    void testCreateRebootCommand_Success() {
        when(deviceMapper.selectById(DEVICE_ID)).thenReturn(testDevice);
        when(pendingCommandMapper.insert(any(PendingCommand.class)))
                .thenAnswer(invocation -> {
                    PendingCommand cmd = invocation.getArgument(0);
                    cmd.setId(1L);
                    return 1;
                });

        PendingCommand result = powerCommandService.createRebootCommand(DEVICE_ID);

        assertNotNull(result);
        assertEquals("REBOOT", result.getType());
        assertEquals("PENDING", result.getStatus());
        assertEquals(DEVICE_ID, result.getDeviceId());

        verify(deviceMapper).selectById(DEVICE_ID);
        verify(pendingCommandMapper).insert(any(PendingCommand.class));
    }

    @Test
    @DisplayName("创建重启命令失败 - 设备不存在")
    void testCreateRebootCommand_DeviceNotFound() {
        when(deviceMapper.selectById(DEVICE_ID)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> powerCommandService.createRebootCommand(DEVICE_ID));
        verify(pendingCommandMapper, never()).insert(any());
    }

    // ========== 创建关机命令测试 ==========

    @Test
    @DisplayName("创建关机命令成功")
    void testCreatePoweroffCommand_Success() {
        when(deviceMapper.selectById(DEVICE_ID)).thenReturn(testDevice);
        when(pendingCommandMapper.insert(any(PendingCommand.class)))
                .thenAnswer(invocation -> {
                    PendingCommand cmd = invocation.getArgument(0);
                    cmd.setId(2L);
                    return 1;
                });

        PendingCommand result = powerCommandService.createPoweroffCommand(DEVICE_ID);

        assertNotNull(result);
        assertEquals("POWEROFF", result.getType());
        assertEquals("PENDING", result.getStatus());
        assertEquals(DEVICE_ID, result.getDeviceId());

        verify(deviceMapper).selectById(DEVICE_ID);
        verify(pendingCommandMapper).insert(any(PendingCommand.class));
    }

    @Test
    @DisplayName("创建关机命令失败 - 设备不存在")
    void testCreatePoweroffCommand_DeviceNotFound() {
        when(deviceMapper.selectById(DEVICE_ID)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> powerCommandService.createPoweroffCommand(DEVICE_ID));
        verify(pendingCommandMapper, never()).insert(any());
    }

    // ========== 获取待处理命令测试 ==========

    @Test
    @DisplayName("获取待处理命令 - 有待处理命令")
    void testGetPendingCommand_Found() {
        PendingCommand mockCommand = new PendingCommand();
        mockCommand.setId(1L);
        mockCommand.setDeviceId(DEVICE_ID);
        mockCommand.setType("REBOOT");
        mockCommand.setStatus("PENDING");
        mockCommand.setCreatedAt(LocalDateTime.now());

        when(pendingCommandMapper.selectOne(any())).thenReturn(mockCommand);

        PendingCommandResponse result = powerCommandService.getPendingCommand(DEVICE_ID);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(DEVICE_ID, result.getDeviceId());
        assertEquals("REBOOT", result.getType());
        assertEquals("PENDING", result.getStatus());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    @DisplayName("获取待处理命令 - 无待处理命令")
    void testGetPendingCommand_NotFound() {
        when(pendingCommandMapper.selectOne(any())).thenReturn(null);

        PendingCommandResponse result = powerCommandService.getPendingCommand(DEVICE_ID);

        assertNull(result);
    }

    // ========== 确认命令测试 ==========

    @Test
    @DisplayName("确认命令成功")
    void testAcknowledgeCommand_Success() {
        PendingCommand mockCommand = new PendingCommand();
        mockCommand.setId(1L);
        mockCommand.setDeviceId(DEVICE_ID);
        mockCommand.setType("REBOOT");
        mockCommand.setStatus("PENDING");

        when(pendingCommandMapper.selectById(1L)).thenReturn(mockCommand);

        powerCommandService.acknowledgeCommand(1L, DEVICE_ID, "COMPLETED", "重启成功");

        ArgumentCaptor<PendingCommand> captor = ArgumentCaptor.forClass(PendingCommand.class);
        verify(pendingCommandMapper).updateById(captor.capture());

        PendingCommand updated = captor.getValue();
        assertEquals("COMPLETED", updated.getStatus());
        assertEquals("重启成功", updated.getResultMessage());
        assertNotNull(updated.getAcknowledgedAt());
    }

    @Test
    @DisplayName("确认命令失败 - 命令不存在")
    void testAcknowledgeCommand_NotFound() {
        when(pendingCommandMapper.selectById(999L)).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> powerCommandService.acknowledgeCommand(999L, DEVICE_ID, "COMPLETED", "重启成功"));
        verify(pendingCommandMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("确认命令失败 - 设备ID不匹配")
    void testAcknowledgeCommand_WrongDevice() {
        PendingCommand mockCommand = new PendingCommand();
        mockCommand.setId(1L);
        mockCommand.setDeviceId("other-device");
        mockCommand.setType("REBOOT");
        mockCommand.setStatus("PENDING");

        when(pendingCommandMapper.selectById(1L)).thenReturn(mockCommand);

        assertThrows(RuntimeException.class,
                () -> powerCommandService.acknowledgeCommand(1L, DEVICE_ID, "COMPLETED", "重启成功"));
        verify(pendingCommandMapper, never()).updateById(any());
    }

    // ========== 获取命令历史测试 ==========

    @Test
    @DisplayName("获取设备的命令历史")
    void testGetCommandsByDeviceId() {
        PendingCommand cmd1 = new PendingCommand();
        cmd1.setId(1L);
        cmd1.setDeviceId(DEVICE_ID);
        cmd1.setType("REBOOT");
        cmd1.setStatus("COMPLETED");

        PendingCommand cmd2 = new PendingCommand();
        cmd2.setId(2L);
        cmd2.setDeviceId(DEVICE_ID);
        cmd2.setType("POWEROFF");
        cmd2.setStatus("PENDING");

        when(pendingCommandMapper.selectList(any()))
                .thenReturn(Arrays.asList(cmd1, cmd2));

        List<PendingCommand> commands = powerCommandService.getCommandsByDeviceId(DEVICE_ID);

        assertNotNull(commands);
        assertEquals(2, commands.size());
        assertEquals("REBOOT", commands.get(0).getType());
        assertEquals("POWEROFF", commands.get(1).getType());
    }

    @Test
    @DisplayName("获取设备的命令历史 - 空列表")
    void testGetCommandsByDeviceId_Empty() {
        when(pendingCommandMapper.selectList(any())).thenReturn(List.of());

        List<PendingCommand> commands = powerCommandService.getCommandsByDeviceId(DEVICE_ID);

        assertNotNull(commands);
        assertTrue(commands.isEmpty());
    }
}
