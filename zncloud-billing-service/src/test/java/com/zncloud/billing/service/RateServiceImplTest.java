package com.zncloud.billing.service;

import com.zncloud.billing.event.RateChangeEventPublisher;
import com.zncloud.billing.model.dto.BatchRateRequest;
import com.zncloud.billing.model.dto.BillingRateRequest;
import com.zncloud.billing.model.dto.BillingRateResponse;
import com.zncloud.billing.model.entity.BillingRate;
import com.zncloud.billing.model.entity.RateChangeLog;
import com.zncloud.billing.model.enums.BillingRateStatus;
import com.zncloud.billing.model.enums.ConfigLevel;
import com.zncloud.billing.repository.BillingRateMapper;
import com.zncloud.billing.repository.RateChangeLogMapper;
import com.zncloud.billing.service.impl.RateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateServiceImplTest {

    @Mock
    private BillingRateMapper billingRateMapper;

    @Mock
    private RateChangeLogMapper rateChangeLogMapper;

    @Mock
    private RateChangeEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<BillingRate> rateCaptor;

    @Captor
    private ArgumentCaptor<RateChangeLog> logCaptor;

    private RateServiceImpl rateService;

    @BeforeEach
    void setUp() {
        rateService = new RateServiceImpl(billingRateMapper, rateChangeLogMapper, eventPublisher);
    }

    // ==================== 创建费率测试 ====================

    @Test
    void createRate_shouldSucceed() {
        // given
        BillingRateRequest request = new BillingRateRequest();
        request.setCafeId("cafe-001");
        request.setConfigLevel(ConfigLevel.MAINSTREAM);
        request.setPricePerHour(new BigDecimal("5.00"));

        when(billingRateMapper.selectOne(any())).thenReturn(null);
        when(billingRateMapper.insert(any())).thenAnswer(inv -> {
            BillingRate r = inv.getArgument(0);
            r.setId(1L);
            r.setCreatedAt(LocalDateTime.now());
            return 1;
        });

        // when
        BillingRateResponse response = rateService.createRate(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCafeId()).isEqualTo("cafe-001");
        assertThat(response.getConfigLevel()).isEqualTo(ConfigLevel.MAINSTREAM);
        assertThat(response.getPricePerHour()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(response.getStatus()).isEqualTo(BillingRateStatus.ACTIVE);

        verify(eventPublisher).publish(any());
        verify(rateChangeLogMapper).insert(any());
    }

    @Test
    void createRate_withEmptyCafeId_shouldSetToEmpty() {
        // given
        BillingRateRequest request = new BillingRateRequest();
        request.setCafeId(null);  // null cafeId becomes global
        request.setConfigLevel(ConfigLevel.ENTRY);
        request.setPricePerHour(new BigDecimal("3.00"));

        when(billingRateMapper.selectOne(any())).thenReturn(null);
        when(billingRateMapper.insert(any())).thenAnswer(inv -> {
            BillingRate r = inv.getArgument(0);
            r.setId(2L);
            return 1;
        });

        // when
        BillingRateResponse response = rateService.createRate(request);

        // then
        assertThat(response.getCafeId()).isEqualTo("");  // global config
    }

    @Test
    void createRate_withNullConfigLevel_shouldThrow() {
        BillingRateRequest request = new BillingRateRequest();
        request.setCafeId("cafe-001");
        request.setConfigLevel(null);
        request.setPricePerHour(new BigDecimal("5.00"));

        assertThatThrownBy(() -> rateService.createRate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("配置等级不能为空");
    }

    @Test
    void createRate_withZeroPrice_shouldThrow() {
        BillingRateRequest request = new BillingRateRequest();
        request.setCafeId("cafe-001");
        request.setConfigLevel(ConfigLevel.MAINSTREAM);
        request.setPricePerHour(BigDecimal.ZERO);

        assertThatThrownBy(() -> rateService.createRate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("价格必须大于0");
    }

    @Test
    void createRate_withExistingActiveRate_shouldMarkOldAsHistory() {
        // given
        BillingRate existing = new BillingRate();
        existing.setId(10L);
        existing.setCafeId("cafe-001");
        existing.setConfigLevel(ConfigLevel.MAINSTREAM);
        existing.setPricePerHour(new BigDecimal("4.00"));
        existing.setStatus(BillingRateStatus.ACTIVE);
        existing.setEffectiveAt(LocalDateTime.now().minusDays(1));

        BillingRateRequest request = new BillingRateRequest();
        request.setCafeId("cafe-001");
        request.setConfigLevel(ConfigLevel.MAINSTREAM);
        request.setPricePerHour(new BigDecimal("6.00"));

        when(billingRateMapper.selectOne(any())).thenReturn(existing);
        when(billingRateMapper.insert(any())).thenAnswer(inv -> {
            BillingRate r = inv.getArgument(0);
            r.setId(11L);
            return 1;
        });

        // when
        BillingRateResponse response = rateService.createRate(request);

        // then
        assertThat(existing.getStatus()).isEqualTo(BillingRateStatus.HISTORY);
        verify(billingRateMapper).updateById(existing);
        assertThat(response.getPricePerHour()).isEqualByComparingTo(new BigDecimal("6.00"));
    }

    // ==================== 批量创建费率测试 ====================

    @Test
    void batchCreateRates_shouldCreateForMultipleCafes() {
        // given
        BatchRateRequest request = new BatchRateRequest();
        request.setCafeIds(Arrays.asList("cafe-001", "cafe-002", "cafe-003"));
        request.setConfigLevel(ConfigLevel.HIGH_PERFORMANCE);
        request.setPricePerHour(new BigDecimal("8.00"));
        request.setOperatorId("op-001");
        request.setRemark("批量调价");

        when(billingRateMapper.selectOne(any())).thenReturn(null);
        when(billingRateMapper.insert(any())).thenAnswer(inv -> {
            BillingRate r = inv.getArgument(0);
            r.setId((long) (Math.random() * 1000));
            return 1;
        });

        // when
        List<BillingRateResponse> results = rateService.batchCreateRates(request);

        // then
        assertThat(results).hasSize(3);
        results.forEach(r -> {
            assertThat(r.getConfigLevel()).isEqualTo(ConfigLevel.HIGH_PERFORMANCE);
            assertThat(r.getPricePerHour()).isEqualByComparingTo(new BigDecimal("8.00"));
        });

        verify(billingRateMapper, times(3)).insert(any());
        verify(rateChangeLogMapper, times(3)).insert(any());
        verify(eventPublisher).publish(any());
    }

    @Test
    void batchCreateRates_withEmptyCafeIds_shouldThrow() {
        BatchRateRequest request = new BatchRateRequest();
        request.setCafeIds(null);
        request.setConfigLevel(ConfigLevel.ENTRY);
        request.setPricePerHour(new BigDecimal("3.00"));

        assertThatThrownBy(() -> rateService.batchCreateRates(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("网吧ID列表不能为空");
    }

    @Test
    void batchCreateRates_exceedingLimit_shouldThrow() {
        BatchRateRequest request = new BatchRateRequest();
        request.setCafeIds(java.util.stream.Stream.generate(() -> "cafe-x").limit(501).toList());
        request.setConfigLevel(ConfigLevel.ENTRY);
        request.setPricePerHour(new BigDecimal("3.00"));

        assertThatThrownBy(() -> rateService.batchCreateRates(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最多支持500个网吧");
    }

    // ==================== 查询费率测试 ====================

    @Test
    void getRateByCafeAndLevel_withCafeConfig_shouldReturnCafeRate() {
        // given
        BillingRate cafeRate = new BillingRate();
        cafeRate.setId(1L);
        cafeRate.setCafeId("cafe-001");
        cafeRate.setConfigLevel(ConfigLevel.MAINSTREAM);
        cafeRate.setPricePerHour(new BigDecimal("6.00"));
        cafeRate.setStatus(BillingRateStatus.ACTIVE);
        cafeRate.setEffectiveAt(LocalDateTime.now().minusHours(1));

        when(billingRateMapper.selectOne(any())).thenReturn(cafeRate);

        // when
        BillingRateResponse response = rateService.getRateByCafeAndLevel("cafe-001", ConfigLevel.MAINSTREAM);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPricePerHour()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(response.getCafeId()).isEqualTo("cafe-001");
    }

    @Test
    void getRateByCafeAndLevel_withoutCafeConfig_shouldFallbackToGlobal() {
        // given — first call returns null (no cafe config), second returns global
        when(billingRateMapper.selectOne(any()))
                .thenReturn(null)  // cafe-level: no result
                .thenReturn(buildGlobalRate(ConfigLevel.MAINSTREAM, new BigDecimal("5.00")));

        // when
        BillingRateResponse response = rateService.getRateByCafeAndLevel("cafe-001", ConfigLevel.MAINSTREAM);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPricePerHour()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void getRateByCafeAndLevel_noRate_shouldThrow() {
        when(billingRateMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> rateService.getRateByCafeAndLevel("cafe-001", ConfigLevel.MAINSTREAM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未找到有效的费率配置");
    }

    @Test
    void getRatesByCafe_shouldReturnActiveRates() {
        // given
        List<BillingRate> rates = Arrays.asList(
                buildRate("cafe-001", ConfigLevel.ENTRY, new BigDecimal("3.00")),
                buildRate("cafe-001", ConfigLevel.MAINSTREAM, new BigDecimal("5.00")),
                buildRate("cafe-001", ConfigLevel.HIGH_PERFORMANCE, new BigDecimal("8.00"))
        );

        when(billingRateMapper.selectList(any())).thenReturn(rates);

        // when
        List<BillingRateResponse> results = rateService.getRatesByCafe("cafe-001");

        // then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getConfigLevel()).isEqualTo(ConfigLevel.ENTRY);
    }

    @Test
    void getActiveRates_shouldReturnDeduplicatedLatestRates() {
        // given — two rates for same cafe+level but different effective times
        BillingRate older = buildRate("cafe-001", ConfigLevel.MAINSTREAM, new BigDecimal("4.00"));
        older.setEffectiveAt(LocalDateTime.now().minusDays(7));
        BillingRate newer = buildRate("cafe-001", ConfigLevel.MAINSTREAM, new BigDecimal("6.00"));
        newer.setEffectiveAt(LocalDateTime.now().minusDays(1));

        when(billingRateMapper.selectList(any())).thenReturn(Arrays.asList(older, newer));

        // when
        List<BillingRateResponse> results = rateService.getActiveRates();

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPricePerHour()).isEqualByComparingTo(new BigDecimal("6.00"));
    }

    // ==================== 费用计算测试 ====================

    @Test
    void calculateCost_shouldReturnCorrectAmount() {
        // given — 2 hours at 5 CNY/hour = 10 CNY
        BillingRate rate = buildRate("cafe-001", ConfigLevel.MAINSTREAM, new BigDecimal("5.00"));
        when(billingRateMapper.selectOne(any())).thenReturn(rate);

        // when
        BigDecimal cost = rateService.calculateCost("cafe-001", ConfigLevel.MAINSTREAM, 7200);

        // then
        assertThat(cost).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    // ==================== 辅助方法 ====================

    private BillingRate buildRate(String cafeId, ConfigLevel level, BigDecimal price) {
        BillingRate r = new BillingRate();
        r.setId((long) (Math.random() * 1000));
        r.setCafeId(cafeId);
        r.setConfigLevel(level);
        r.setPricePerHour(price);
        r.setStatus(BillingRateStatus.ACTIVE);
        r.setEffectiveAt(LocalDateTime.now());
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    private BillingRate buildGlobalRate(ConfigLevel level, BigDecimal price) {
        return buildRate("", level, price);
    }
}
