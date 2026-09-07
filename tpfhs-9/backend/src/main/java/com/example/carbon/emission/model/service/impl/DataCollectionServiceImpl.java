package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.CollectionRecord;
import com.example.carbon.emission.model.entity.FossilFuelMeterInfo;
import com.example.carbon.emission.model.entity.MeterInfo;
import com.example.carbon.emission.model.entity.PurchasedHeatMeterInfo;
import com.example.carbon.emission.model.entity.SystemConfig;
import com.example.carbon.emission.model.repository.CollectionRecordRepository;
import com.example.carbon.emission.model.repository.FossilFuelMeterInfoRepository;
import com.example.carbon.emission.model.repository.MeterInfoRepository;
import com.example.carbon.emission.model.repository.PurchasedHeatMeterInfoRepository;
import com.example.carbon.emission.model.repository.SystemConfigRepository;
import com.example.carbon.emission.model.service.DataCollectionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据采集服务实现
 *
 * 采集采用两阶段模式（详见 {@link DataCollectionService}）：
 * <ol>
 *   <li>扫描阶段：每日 0 点遍历三张采集点表，依据计费周期判断是否到达
 *       采集时间，到达则插入 collection_status=0 的待采集记录。</li>
 *   <li>执行阶段：专门线程（固定延迟调度）遍历待采集记录，执行采集
 *       （当前为模拟），成功后回填采集点的 last_collection_time。</li>
 * </ol>
 *
 * 执行阶段处理的记录范围：collection_status=0（未读取）或
 * collection_status=2（读取失败后重新读取）。
 * 对于 collection_status=2 的记录，当当前日期距离本计费周期截止日期
 * 超过 {@code Days_abandon_read_operation}（系统配置，单位：天）后，
 * 将该记录状态修改为 3（多次读取失败后取消），系统不再重复读取。
 *
 * 周期判断规则（所有周期统一按当年 1 月 1 日起算）：
 * <ul>
 *   <li>last_collection_time 为空（从未采集过）：当年首个采集日 =
 *       当年 1 月 1 日 + (billing_cycle_start_date - 1) 天；
 *       若当前时间不早于该日则需采集。</li>
 *   <li>last_collection_time 不为空：下次采集时间 =
 *       last_collection_time + 计费周期；若当前时间不早于该时间则需采集。</li>
 *   <li>周期长度：unit=1 周 → length×7 天；unit=2 月 → length 月；
 *       unit=3 季度 → length×3 月；unit=4 年 → length 年。</li>
 * </ul>
 */
@Service
public class DataCollectionServiceImpl implements DataCollectionService {

    private static final Logger logger = LoggerFactory.getLogger(DataCollectionServiceImpl.class);

    /** 采集点类型：1-电力表 */
    private static final int TYPE_METER = 1;
    /** 采集点类型：2-化石燃料 */
    private static final int TYPE_FOSSIL_FUEL = 2;
    /** 采集点类型：3-外购热能 */
    private static final int TYPE_PURCHASED_HEAT = 3;
    /** 采集点启用状态 */
    private static final int STATUS_ENABLED = 1;
    /** 采集记录状态：未读取 */
    private static final int STATUS_PENDING = 0;
    /** 采集记录状态：读取成功 */
    private static final int STATUS_SUCCESS = 1;
    /** 采集记录状态：读取失败 */
    private static final int STATUS_FAILED = 2;
    /** 采集记录状态：多次读取失败后取消 */
    private static final int STATUS_ABANDONED = 3;

    /** 系统配置项：采集任务循环执行时间间隔（分钟） */
    private static final String CFG_CYCLE_INTERVAL = "Collection_task_cycle_interval";
    /** 系统配置项：读取失败后取消重复读取的天数（天） */
    private static final String CFG_DAYS_ABANDON = "Days_abandon_read_operation";
    /** 采集循环间隔默认值（分钟，配置缺失或非法时使用） */
    private static final int DEFAULT_CYCLE_INTERVAL_MIN = 5;
    /** 放弃重读默认天数（配置缺失或非法时使用） */
    private static final int DEFAULT_ABANDON_DAYS = 5;

    @Autowired
    private MeterInfoRepository meterInfoRepository;

    @Autowired
    private FossilFuelMeterInfoRepository fossilFuelMeterInfoRepository;

    @Autowired
    private PurchasedHeatMeterInfoRepository purchasedHeatMeterInfoRepository;

    @Autowired
    private CollectionRecordRepository collectionRecordRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** 每条待采集记录在独立事务中处理，避免单条失败回滚整批 */
    private TransactionTemplate transactionTemplate;

    /** 防止处理任务重叠执行的标志（固定延迟调度保证不重叠，此处作为防御性兜底） */
    private final AtomicBoolean processing = new AtomicBoolean(false);

    @PostConstruct
    private void init() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 扫描采集点并生成待采集记录
     */
    @Override
    public void scanAndScheduleCollection() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("========== 采集计划扫描开始，扫描时间: {} ==========", now);

        int meterCount = scanMeterInfos(now);
        int fossilCount = scanFossilFuelMeterInfos(now);
        int heatCount = scanPurchasedHeatMeterInfos(now);
        int total = meterCount + fossilCount + heatCount;

        logger.info("========== 采集计划扫描结束，电表新增 {} 条，化石燃料新增 {} 条，外购热能新增 {} 条，合计 {} 条 ==========",
                meterCount, fossilCount, heatCount, total);
    }

    /**
     * 扫描电表采集点
     */
    private int scanMeterInfos(LocalDateTime now) {
        List<MeterInfo> meters = meterInfoRepository.findByStatus(STATUS_ENABLED);
        int count = 0;
        for (MeterInfo meter : meters) {
            try {
                if (!needToCollect(now, meter.getLastCollectionTime(),
                        meter.getBillingCycleUnit(), meter.getBillingCycleStartDate(), meter.getBillingCycleLength())) {
                    continue;
                }
                if (hasPendingRecord(TYPE_METER, meter.getId())) {
                    logger.debug("电表[{}:{}] 已有未处理的待采集记录，跳过", meter.getId(), meter.getName());
                    continue;
                }
                insertCollectionRecord(TYPE_METER, meter.getId(), meter.getName(), meter.getEmissionSubcategory(),
                        meter.getMeasurementUnit(), meter.getMeterReadingMethod(), meter.getIsCumulative(),
                        now, meter.getBillingCycleUnit(), meter.getBillingCycleStartDate(), meter.getBillingCycleLength());
                count++;
            } catch (Exception e) {
                logger.error("扫描电表采集点失败，电表ID: {}, 名称: {}", meter.getId(), meter.getName(), e);
            }
        }
        return count;
    }

    /**
     * 扫描化石燃料采集点
     */
    private int scanFossilFuelMeterInfos(LocalDateTime now) {
        List<FossilFuelMeterInfo> meters = fossilFuelMeterInfoRepository.findByStatus(STATUS_ENABLED);
        int count = 0;
        for (FossilFuelMeterInfo meter : meters) {
            try {
                if (!needToCollect(now, meter.getLastCollectionTime(),
                        meter.getBillingCycleUnit(), meter.getBillingCycleStartDate(), meter.getBillingCycleLength())) {
                    continue;
                }
                if (hasPendingRecord(TYPE_FOSSIL_FUEL, meter.getId())) {
                    logger.debug("化石燃料采集点[{}:{}] 已有未处理的待采集记录，跳过", meter.getId(), meter.getName());
                    continue;
                }
                // FossilFuelMeterInfo.fuelType 对应 emission_subcategory 列
                insertCollectionRecord(TYPE_FOSSIL_FUEL, meter.getId(), meter.getName(), meter.getFuelType(),
                        meter.getMeasurementUnit(), meter.getMeterReadingMethod(), meter.getIsCumulative(),
                        now, meter.getBillingCycleUnit(), meter.getBillingCycleStartDate(), meter.getBillingCycleLength());
                count++;
            } catch (Exception e) {
                logger.error("扫描化石燃料采集点失败，ID: {}, 名称: {}", meter.getId(), meter.getName(), e);
            }
        }
        return count;
    }

    /**
     * 扫描外购热能采集点
     */
    private int scanPurchasedHeatMeterInfos(LocalDateTime now) {
        List<PurchasedHeatMeterInfo> meters = purchasedHeatMeterInfoRepository.findByStatus(STATUS_ENABLED);
        int count = 0;
        for (PurchasedHeatMeterInfo meter : meters) {
            try {
                if (!needToCollect(now, meter.getLastCollectionTime(),
                        meter.getBillingCycleUnit(), meter.getBillingCycleStartDate(), meter.getBillingCycleLength())) {
                    continue;
                }
                if (hasPendingRecord(TYPE_PURCHASED_HEAT, meter.getId())) {
                    logger.debug("外购热能采集点[{}:{}] 已有未处理的待采集记录，跳过", meter.getId(), meter.getName());
                    continue;
                }
                // PurchasedHeatMeterInfo.heatType 对应 emission_subcategory 列
                insertCollectionRecord(TYPE_PURCHASED_HEAT, meter.getId(), meter.getName(), meter.getHeatType(),
                        meter.getMeasurementUnit(), meter.getMeterReadingMethod(), meter.getIsCumulative(),
                        now, meter.getBillingCycleUnit(), meter.getBillingCycleStartDate(), meter.getBillingCycleLength());
                count++;
            } catch (Exception e) {
                logger.error("扫描外购热能采集点失败，ID: {}, 名称: {}", meter.getId(), meter.getName(), e);
            }
        }
        return count;
    }

    /**
     * 判断采集点是否到达需要采集数据的时间
     *
     * @param now                当前时间
     * @param lastCollectionTime 上次采集时间（为 null 表示从未采集）
     * @param unit               计费周期单位：1-周，2-月，3-季度，4-年
     * @param startDate          计费周期起始日期偏移量（1=第1天）
     * @param length             计费周期长度
     * @return true 表示需要采集
     */
    private boolean needToCollect(LocalDateTime now, LocalDateTime lastCollectionTime,
                                  Integer unit, Integer startDate, Integer length) {
        Period period = computePeriod(unit, length);
        if (lastCollectionTime == null) {
            // 从未采集过：当年首个采集日 = 当年1月1日 + (startDate - 1) 天
            LocalDate today = now.toLocalDate();
            int offset = (startDate == null || startDate < 1) ? 0 : startDate - 1;
            LocalDate firstCollectionDay = LocalDate.of(today.getYear(), 1, 1).plusDays(offset);
            return !today.isBefore(firstCollectionDay);
        }
        // 已采集过：下次采集时间 = 上次采集时间 + 周期
        LocalDateTime nextTime = lastCollectionTime.plus(period);
        return !now.isBefore(nextTime);
    }

    /**
     * 根据计费周期单位和长度计算周期 Period
     *
     * @param unit   周期单位：1-周，2-月，3-季度，4-年
     * @param length 周期长度（<=0 时按 1 处理）
     * @return Period 对象
     */
    private Period computePeriod(Integer unit, Integer length) {
        int u = (unit == null) ? 2 : unit;
        int l = (length == null || length <= 0) ? 1 : length;
        return switch (u) {
            case 1 -> Period.ofDays(l * 7);     // 周
            case 2 -> Period.ofMonths(l);         // 月
            case 3 -> Period.ofMonths(l * 3);     // 季度
            case 4 -> Period.ofYears(l);          // 年
            default -> Period.ofMonths(l);
        };
    }

    /**
     * 判断采集点是否已存在未处理或读取失败的待采集记录（status=0 或 2），避免重复插入
     * 存在未读取或读取失败（仍待重读）记录时，本次扫描跳过该采集点
     */
    private boolean hasPendingRecord(int pointType, Long pointId) {
        List<CollectionRecord> pending = collectionRecordRepository
                .findByCollectionPointTypeAndCollectionPointIdAndCollectionStatus(pointType, pointId, STATUS_PENDING);
        if (pending != null && !pending.isEmpty()) {
            return true;
        }
        List<CollectionRecord> failed = collectionRecordRepository
                .findByCollectionPointTypeAndCollectionPointIdAndCollectionStatus(pointType, pointId, STATUS_FAILED);
        return failed != null && !failed.isEmpty();
    }

    /**
     * 构造并插入一条待采集记录
     *
     * 计费周期起止日期基于当年 1 月 1 日 + (startDate - 1) 作为本周期起点，
     * 长度按周期单位计算，截止日期 = 起点加周期再减一天。
     */
    private void insertCollectionRecord(int pointType, Long pointId, String pointName, String subcategory,
                                        String measurementUnit, Integer meterReadingMethod, Integer isCumulative,
                                        LocalDateTime now, Integer unit, Integer startDate, Integer length) {
        Period period = computePeriod(unit, length);
        LocalDate today = now.toLocalDate();
        int offset = (startDate == null || startDate < 1) ? 0 : startDate - 1;
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1).plusDays(offset);

        // 找到包含 today 的当前计费周期 [cycleStart, cycleEnd]
        LocalDate cycleStart = yearStart;
        LocalDate cycleEnd = cycleStart.plus(period).minusDays(1);
        // 若 today 早于首个周期起点，本周期取首个周期（此时 needToCollect 应已返回 false，正常不会进入这里）
        while (cycleEnd.isBefore(today)) {
            cycleStart = cycleStart.plus(period);
            cycleEnd = cycleStart.plus(period).minusDays(1);
        }

        CollectionRecord record = new CollectionRecord();
        record.setCollectionPointType(pointType);
        record.setCollectionPointId(pointId);
        record.setCollectionStatus(STATUS_PENDING);
        record.setCollectionPointName(pointName);
        record.setEmissionSubcategory(subcategory);
        record.setMeasurementUnit(measurementUnit);
        record.setMeterReadingMethod(meterReadingMethod);
        record.setIsCumulative(isCumulative);
        record.setBillingCycleStartDate(cycleStart);
        record.setBillingCycleEndDate(cycleEnd);
        collectionRecordRepository.save(record);

        logger.info("新增待采集记录：类型={}, 采集点ID={}, 名称={}, 计费周期={} ~ {}",
                pointType, pointId, pointName, cycleStart, cycleEnd);
    }

    /**
     * 处理待采集记录
     *
     * 遍历 collection_status=0（未读取）或 collection_status=2（读取失败需重读）的记录：
     * <ul>
     *   <li>对于 status=2 的记录，先判断是否超过放弃重读天数：若当前日期距离
     *       本计费周期截止日期超过 {@code Days_abandon_read_operation}（系统配置，单位：天），
     *       则将状态修改为 3（多次读取失败后取消），不再重复读取。</li>
     *   <li>其余记录执行采集：读取成功置 status=1 并回填采集点 last_collection_time；
     *       读取失败置 status=2，等待下一轮重读。</li>
     * </ul>
     */
    @Override
    public void processPendingCollectionRecords() {
        // 固定延迟调度保证不重叠，此处作为防御性兜底
        if (!processing.compareAndSet(false, true)) {
            logger.debug("上一次采集处理仍在进行中，跳过本次触发");
            return;
        }
        try {
            int abandonDays = readAbandonDays();
            List<CollectionRecord> pending = collectionRecordRepository
                    .findByCollectionStatusIn(List.of(STATUS_PENDING, STATUS_FAILED));
            logger.info("========== 处理待采集记录开始，待处理 {} 条（放弃重读天数: {}） ==========",
                    pending.size(), abandonDays);

            int success = 0;
            int fail = 0;
            int abandoned = 0;
            for (CollectionRecord record : pending) {
                try {
                    // 失败记录超期则放弃，不再重复读取
                    if (record.getCollectionStatus() != null && record.getCollectionStatus() == STATUS_FAILED
                            && isAbandoned(record, abandonDays)) {
                        markAbandoned(record);
                        abandoned++;
                        continue;
                    }
                    boolean ok = processSingleRecord(record);
                    if (ok) {
                        success++;
                    } else {
                        fail++;
                    }
                } catch (Exception e) {
                    logger.error("处理采集记录失败，记录ID: {}, 采集点类型: {}, 采集点ID: {}",
                            record.getId(), record.getCollectionPointType(), record.getCollectionPointId(), e);
                    fail++;
                }
            }
            logger.info("========== 处理待采集记录结束，成功 {} 条，失败 {} 条，放弃 {} 条 ==========",
                    success, fail, abandoned);
        } finally {
            processing.set(false);
        }
    }

    /**
     * 判断失败记录是否已超过放弃重读天数
     *
     * 当前日期距离本计费周期截止日期（billing_cycle_end_date）超过 abandonDays 天则放弃。
     * 若 billing_cycle_end_date 为空，则按记录创建时间 created_at 计算。
     *
     * @param record      采集记录
     * @param abandonDays 放弃重读天数（来自系统配置 Days_abandon_read_operation）
     * @return true 表示应放弃该记录
     */
    private boolean isAbandoned(CollectionRecord record, int abandonDays) {
        LocalDate today = LocalDate.now();
        LocalDate baseline = record.getBillingCycleEndDate();
        if (baseline == null) {
            baseline = (record.getCreatedAt() != null) ? record.getCreatedAt().toLocalDate() : today;
        }
        long days = ChronoUnit.DAYS.between(baseline, today);
        return days > abandonDays;
    }

    /**
     * 将采集记录标记为"多次读取失败后取消"（status=3），在独立事务中执行
     */
    private void markAbandoned(CollectionRecord record) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                record.setCollectionStatus(STATUS_ABANDONED);
                record.setLastFetchTime(LocalDateTime.now());
                collectionRecordRepository.save(record);
            });
            logger.info("采集记录已放弃重读（status=3），记录ID: {}, 采集点类型: {}, 采集点ID: {}",
                    record.getId(), record.getCollectionPointType(), record.getCollectionPointId());
        } catch (Exception e) {
            logger.error("标记采集记录为放弃状态失败，记录ID: {}", record.getId(), e);
        }
    }

    /**
     * 在独立事务中处理单条采集记录：执行采集，成功则回填采集点 last_collection_time
     *
     * 单条记录独立事务，避免一条失败导致整批回滚。
     * 成功与失败分别在不同事务中更新状态。
     *
     * @param record 采集记录
     * @return true 读取成功，false 读取失败
     */
    private boolean processSingleRecord(CollectionRecord record) {
        try {
            // 读取 + 成功状态更新在独立事务中
            transactionTemplate.executeWithoutResult(status -> {
                BigDecimal readingValue = simulateReading(record);
                LocalDateTime now = LocalDateTime.now();

                record.setReadingValue(readingValue);
                record.setCollectionTime(now);
                record.setLastFetchTime(now);
                record.setCollectionStatus(STATUS_SUCCESS);
                collectionRecordRepository.save(record);

                // 回填采集点表的上次采集时间（对应 collection_time）
                updateMeterLastCollectionTime(record.getCollectionPointType(), record.getCollectionPointId(), now);
            });
            return true;
        } catch (Exception e) {
            logger.error("采集读取失败，记录ID: {}, 采集点类型: {}, 采集点ID: {}，置为 status=2 待重读",
                    record.getId(), record.getCollectionPointType(), record.getCollectionPointId(), e);
            // 失败状态更新在独立事务中，避免读取事务回滚导致状态无法落库
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    record.setCollectionStatus(STATUS_FAILED);
                    record.setLastFetchTime(LocalDateTime.now());
                    collectionRecordRepository.save(record);
                });
            } catch (Exception ex) {
                logger.error("写入采集失败状态异常，记录ID: {}", record.getId(), ex);
            }
            return false;
        }
    }

    /**
     * 读取采集任务循环执行时间间隔（分钟）
     * 来自系统配置 Collection_task_cycle_interval，缺失或非法时返回默认值 5
     *
     * @return 循环间隔（分钟）
     */
    public int readCycleIntervalMinutes() {
        return readIntConfig(CFG_CYCLE_INTERVAL, DEFAULT_CYCLE_INTERVAL_MIN);
    }

    /**
     * 读取放弃重读天数
     * 来自系统配置 Days_abandon_read_operation，缺失或非法时返回默认值 5
     *
     * @return 放弃重读天数
     */
    private int readAbandonDays() {
        return readIntConfig(CFG_DAYS_ABANDON, DEFAULT_ABANDON_DAYS);
    }

    /**
     * 从系统配置表读取整数型配置值
     *
     * @param key          配置键
     * @param defaultValue 缺失或解析失败时的默认值
     * @return 配置值
     */
    private int readIntConfig(String key, int defaultValue) {
        try {
            return systemConfigRepository.findByConfigKey(key)
                    .map(SystemConfig::getConfigValue)
                    .map(Integer::parseInt)
                    .orElse(defaultValue);
        } catch (Exception e) {
            logger.warn("读取系统配置 {} 失败，使用默认值 {}", key, defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * 模拟采集：读取计量表数值
     *
     * 当前为占位实现，返回 BigDecimal.ZERO 并记录日志。
     * 实际采集需对接外部抄表接口（auto_meter_reading_config 中配置）。
     *
     * @param record 采集记录
     * @return 读取到的数值
     */
    private BigDecimal simulateReading(CollectionRecord record) {
        logger.info("【模拟采集】采集点类型: {}, 采集点ID: {}, 名称: {}, 单位: {}, 累加量: {}",
                record.getCollectionPointType(), record.getCollectionPointId(),
                record.getCollectionPointName(), record.getMeasurementUnit(), record.getIsCumulative());
        return BigDecimal.ZERO;
    }

    /**
     * 回填采集点表的 last_collection_time
     *
     * @param pointType 采集点类型
     * @param pointId   采集点ID
     * @param time      上次采集时间（对应采集记录 collection_time）
     */
    private void updateMeterLastCollectionTime(int pointType, Long pointId, LocalDateTime time) {
        switch (pointType) {
            case TYPE_METER -> meterInfoRepository.updateLastCollectionTime(pointId, time);
            case TYPE_FOSSIL_FUEL -> fossilFuelMeterInfoRepository.updateLastCollectionTime(pointId, time);
            case TYPE_PURCHASED_HEAT -> purchasedHeatMeterInfoRepository.updateLastCollectionTime(pointId, time);
            default -> logger.warn("未知采集点类型: {}, 跳过回填 last_collection_time", pointType);
        }
    }
}
