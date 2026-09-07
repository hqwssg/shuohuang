package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.CollectionRecord;
import com.example.carbon.emission.model.entity.FossilFuelMeterInfo;
import com.example.carbon.emission.model.entity.MeterInfo;
import com.example.carbon.emission.model.entity.PurchasedHeatMeterInfo;
import com.example.carbon.emission.model.repository.CollectionRecordRepository;
import com.example.carbon.emission.model.repository.FossilFuelMeterInfoRepository;
import com.example.carbon.emission.model.repository.MeterInfoRepository;
import com.example.carbon.emission.model.repository.PurchasedHeatMeterInfoRepository;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 能耗计量值计算器
 *
 * 实现两类采集点的能耗计量值计算：
 * <ul>
 *   <li>算法A：累加量采集点（isCumulative=1）—— 通过核算周期起止日期附近的两条采集记录差值计算能耗。</li>
 *   <li>算法B：非累加量采集点（isCumulative=0）—— 通过核算周期内各计费周期记录的读数之和计算能耗。</li>
 * </ul>
 *
 * 返回结果包含：能耗计量值、数据状态、采集记录起止日期、数据缺失说明、修正后能耗计量值。
 */
@Component
public class EnergyMeasurementCalculator {

    private static final Logger logger = LoggerFactory.getLogger(EnergyMeasurementCalculator.class);

    private static final int SCALE = 6;

    /** 数据状态：1-完整，2-没有数据，3-数据不完整，4-数据超范围，5-数据不完整且超范围 */
    private static final int STATUS_COMPLETE = 1;
    private static final int STATUS_NO_DATA = 2;
    private static final int STATUS_INCOMPLETE = 3;
    private static final int STATUS_OVER_RANGE = 4;
    private static final int STATUS_INCOMPLETE_AND_OVER_RANGE = 5;

    /** 采集成功状态 */
    private static final int COLLECTION_STATUS_SUCCESS = 1;

    /** 采集点类型：1-电力表（用于虚拟电表/分摊子电表判定） */
    private static final int COLLECTION_TYPE_ELECTRICITY = 1;

    /** 凌晨6点分界：6点以前采集的算前一天的值 */
    private static final int SIX_AM_HOUR = 6;

    @Autowired
    private CollectionRecordRepository collectionRecordRepository;

    @Autowired
    private MeterInfoRepository meterInfoRepository;

    @Autowired
    private FossilFuelMeterInfoRepository fossilFuelMeterInfoRepository;

    @Autowired
    private PurchasedHeatMeterInfoRepository purchasedHeatMeterInfoRepository;

    /**
     * 采集点的启停与创建时间信息
     * <p>
     * 用于累加量算法在"数据不完整"判定时排除以下例外情况：
     * <ul>
     *   <li>起始日期侧：采集点在核算周期起始日之后才创建或启用 → 不视为数据缺失</li>
     *   <li>截止日期侧：采集点在核算周期截止日之前就被停用 → 不视为数据缺失</li>
     * </ul>
     */
    @Data
    private static class CollectionPointInfo {
        /** 创建时间 */
        private LocalDateTime createdAt;
        /** 启用时间（首次启用或最近一次启用） */
        private LocalDateTime enableTime;
        /** 停用时间 */
        private LocalDateTime disableTime;
    }

    /**
     * 查询采集点的创建/启用/停用时间信息
     *
     * @param collectionPointType 采集点类型：1-电力表，2-化石燃料，3-外购热能
     * @param collectionPointId   采集点ID
     * @return 时间信息对象，查询不到时返回 null
     */
    private CollectionPointInfo loadCollectionPointInfo(Integer collectionPointType, Long collectionPointId) {
        if (collectionPointId == null || collectionPointType == null) {
            return null;
        }
        CollectionPointInfo info = new CollectionPointInfo();
        if (collectionPointType == 1) {
            Optional<MeterInfo> opt = meterInfoRepository.findById(collectionPointId);
            if (opt.isEmpty()) return null;
            MeterInfo m = opt.get();
            info.setCreatedAt(m.getCreatedAt());
            info.setEnableTime(m.getEnableTime());
            info.setDisableTime(m.getDisableTime());
        } else if (collectionPointType == 2) {
            Optional<FossilFuelMeterInfo> opt = fossilFuelMeterInfoRepository.findById(collectionPointId);
            if (opt.isEmpty()) return null;
            FossilFuelMeterInfo m = opt.get();
            info.setCreatedAt(m.getCreatedAt());
            info.setEnableTime(m.getEnableTime());
            info.setDisableTime(m.getDisableTime());
        } else if (collectionPointType == 3) {
            Optional<PurchasedHeatMeterInfo> opt = purchasedHeatMeterInfoRepository.findById(collectionPointId);
            if (opt.isEmpty()) return null;
            PurchasedHeatMeterInfo m = opt.get();
            info.setCreatedAt(m.getCreatedAt());
            info.setEnableTime(m.getEnableTime());
            info.setDisableTime(m.getDisableTime());
        } else {
            return null;
        }
        return info;
    }

    /**
     * 能耗计量值计算结果
     */
    @Data
    public static class Result {
        /** 能耗计量值 */
        private BigDecimal energyMeasurementValue;
        /** 数据状态：1-完整，2-没有数据，3-数据不完整，4-数据超范围，5-不完整且超范围 */
        private Integer dataStatus;
        /** 采集记录起始日期 */
        private LocalDate collectionRecordStartDate;
        /** 采集记录截止日期 */
        private LocalDate collectionRecordEndDate;
        /** 数据缺失说明 */
        private String dataMissingDescription;
        /** 修正后能耗计量值 */
        private BigDecimal adjustedEnergyValue;
    }

    /**
     * 计算能耗计量值
     *
     * @param collectionPointType 采集点类型：1-电力表，2-化石燃料，3-外购热能
     * @param collectionPointId   采集点ID
     * @param isCumulative        是否累加量：1-是，0-否
     * @param cycleStartDate      核算周期起始日期
     * @param cycleEndDate        核算周期截止日期
     * @return 计算结果
     */
    public Result calculate(Integer collectionPointType, Long collectionPointId,
                            Integer isCumulative, LocalDate cycleStartDate, LocalDate cycleEndDate) {
        if (cycleStartDate == null || cycleEndDate == null) {
            Result r = new Result();
            r.setEnergyMeasurementValue(BigDecimal.ZERO);
            r.setDataStatus(STATUS_NO_DATA);
            r.setDataMissingDescription("核算周期日期为空");
            r.setAdjustedEnergyValue(BigDecimal.ZERO);
            return r;
        }

        // 电力表的虚拟电表/分摊子电表特殊处理：
        // 虚拟电表用电量 = 父电表用电量 − 其余子电表用电量之和
        // 分摊子电表用电量 = 父电表用电量 × allocation_ratio
        if (collectionPointType != null && collectionPointType == COLLECTION_TYPE_ELECTRICITY
                && collectionPointId != null) {
            Result special = calculateSpecialElectricMeter(
                    collectionPointId, cycleStartDate, cycleEndDate, new HashSet<>());
            if (special != null) {
                return special;
            }
        }

        if (isCumulative != null && isCumulative == 1) {
            return calculateCumulative(collectionPointType, collectionPointId, cycleStartDate, cycleEndDate);
        } else {
            return calculateNonCumulative(collectionPointType, collectionPointId, cycleStartDate, cycleEndDate);
        }
    }

    // ========================= 虚拟电表 / 分摊子电表 =========================

    /**
     * 电力表用电量及数据状态持有者（用于虚拟/分摊推导）
     */
    @Data
    private static class MeterUsage {
        /** 用电量（数据状态为 NO_DATA 时为 null） */
        private BigDecimal value;
        /** 数据状态 */
        private int dataStatus;
        /** 缺失说明 */
        private String description;
    }

    /**
     * 电力表特殊计量判定入口。
     * <p>
     * 仅当该电表为虚拟电表（is_virtual_meter=1）或分摊子电表（is_allocation_child=1）时
     * 走推导逻辑，否则返回 null，由调用方走正常累加/非累加算法。
     *
     * @param meterId       电表ID
     * @param cycleStartDate 核算周期起始日期
     * @param cycleEndDate   核算周期截止日期
     * @param visited        当前计算链路上已访问的电表ID（防循环引用）
     * @return 推导结果；普通电表返回 null
     */
    private Result calculateSpecialElectricMeter(Long meterId, LocalDate cycleStartDate,
                                                  LocalDate cycleEndDate, Set<Long> visited) {
        if (meterId == null || visited.contains(meterId)) {
            return null;
        }
        Optional<MeterInfo> opt = meterInfoRepository.findById(meterId);
        if (opt.isEmpty()) {
            return null;
        }
        MeterInfo meter = opt.get();
        boolean isVirtual = Integer.valueOf(1).equals(meter.getIsVirtualMeter());
        boolean isAllocation = Integer.valueOf(1).equals(meter.getIsAllocationChild());
        if (!isVirtual && !isAllocation) {
            return null; // 普通电表，走正常流程
        }

        // 加入访问集合，递归计算父电表
        visited.add(meterId);
        try {
            MeterUsage parentUsage = calculateElectricMeterUsage(
                    meter.getParentMeterId(), cycleStartDate, cycleEndDate, visited);
            if (parentUsage == null || parentUsage.getValue() == null) {
                return buildDerivedResult(null, STATUS_NO_DATA,
                        "父电表(id=" + meter.getParentMeterId() + ")用电量无法计算");
            }

            BigDecimal parentValue = parentUsage.getValue();

            // 分摊子电表：用电量 = 父电表用电量 × 分摊比例
            if (isAllocation) {
                BigDecimal ratio = meter.getAllocationRatio() != null
                        ? meter.getAllocationRatio() : BigDecimal.ONE;
                BigDecimal usage = parentValue.multiply(ratio).setScale(SCALE, RoundingMode.HALF_UP);
                String desc = parentUsage.getDataStatus() == STATUS_COMPLETE ? null
                        : ("父电表数据" + statusName(parentUsage.getDataStatus()));
                return buildDerivedResult(usage, parentUsage.getDataStatus(), desc);
            }

            // 虚拟电表：用电量 = 父电表用电量 − 其余子电表用电量之和
            List<MeterInfo> siblings = meterInfoRepository.findByParentMeterId(meter.getParentMeterId());
            BigDecimal siblingsSum = BigDecimal.ZERO;
            int worstStatus = parentUsage.getDataStatus();
            boolean hasMissingSibling = false;
            StringBuilder missingDesc = new StringBuilder();
            for (MeterInfo sib : siblings) {
                if (meterId.equals(sib.getId())) {
                    continue; // 排除虚拟电表自身
                }
                MeterUsage sibUsage = calculateElectricMeterUsage(
                        sib.getId(), cycleStartDate, cycleEndDate, visited);
                if (sibUsage == null || sibUsage.getValue() == null) {
                    hasMissingSibling = true;
                    if (missingDesc.length() > 0) {
                        missingDesc.append(";");
                    }
                    missingDesc.append("子电表(id=").append(sib.getId()).append(")无数据");
                    continue;
                }
                siblingsSum = siblingsSum.add(sibUsage.getValue());
                worstStatus = combineWorstStatus(worstStatus, sibUsage.getDataStatus());
            }

            BigDecimal usage = parentValue.subtract(siblingsSum);
            if (usage.compareTo(BigDecimal.ZERO) < 0) {
                usage = BigDecimal.ZERO;
            }
            usage = usage.setScale(SCALE, RoundingMode.HALF_UP);

            int status = (worstStatus == STATUS_COMPLETE && !hasMissingSibling)
                    ? STATUS_COMPLETE : STATUS_INCOMPLETE;
            String desc = null;
            if (status != STATUS_COMPLETE) {
                desc = "虚拟电表推导" + (hasMissingSibling ? "(部分子电表无数据)" : "");
                if (parentUsage.getDataStatus() != STATUS_COMPLETE) {
                    desc = desc + ";父电表数据" + statusName(parentUsage.getDataStatus());
                }
                if (missingDesc.length() > 0) {
                    desc = desc + ";" + missingDesc;
                }
            }
            return buildDerivedResult(usage, status, desc);
        } finally {
            visited.remove(meterId);
        }
    }

    /**
     * 计算电力表用电量（统一入口，递归处理虚拟/分摊电表）。
     * <p>
     * 普通电表走正常累加/非累加算法；虚拟/分摊电表按推导规则计算。
     * 数据状态为 NO_DATA 时返回的 value 为 null（不计入求和）。
     *
     * @param meterId 电表ID
     * @param visited 当前计算链路上已访问的电表ID（防循环引用）
     * @return 用电量持有者；电表不存在或循环引用返回 null
     */
    private MeterUsage calculateElectricMeterUsage(Long meterId, LocalDate cycleStartDate,
                                                    LocalDate cycleEndDate, Set<Long> visited) {
        MeterUsage usage = new MeterUsage();
        if (meterId == null || meterId == 0L || visited.contains(meterId)) {
            return null;
        }
        Optional<MeterInfo> opt = meterInfoRepository.findById(meterId);
        if (opt.isEmpty()) {
            return null;
        }
        MeterInfo meter = opt.get();
        boolean isVirtual = Integer.valueOf(1).equals(meter.getIsVirtualMeter());
        boolean isAllocation = Integer.valueOf(1).equals(meter.getIsAllocationChild());

        if (isVirtual || isAllocation) {
            visited.add(meterId);
            try {
                MeterUsage parentUsage = calculateElectricMeterUsage(
                        meter.getParentMeterId(), cycleStartDate, cycleEndDate, visited);
                if (parentUsage == null || parentUsage.getValue() == null) {
                    usage.setDataStatus(STATUS_NO_DATA);
                    usage.setDescription("父电表(id=" + meter.getParentMeterId() + ")用电量无法计算");
                    return usage;
                }
                BigDecimal parentValue = parentUsage.getValue();
                if (isAllocation) {
                    BigDecimal ratio = meter.getAllocationRatio() != null
                            ? meter.getAllocationRatio() : BigDecimal.ONE;
                    usage.setValue(parentValue.multiply(ratio).setScale(SCALE, RoundingMode.HALF_UP));
                    usage.setDataStatus(parentUsage.getDataStatus());
                    usage.setDescription(parentUsage.getDescription());
                    return usage;
                }
                // 虚拟电表
                List<MeterInfo> siblings = meterInfoRepository.findByParentMeterId(meter.getParentMeterId());
                BigDecimal siblingsSum = BigDecimal.ZERO;
                int worstStatus = parentUsage.getDataStatus();
                boolean hasMissingSibling = false;
                for (MeterInfo sib : siblings) {
                    if (meterId.equals(sib.getId())) {
                        continue;
                    }
                    MeterUsage sibUsage = calculateElectricMeterUsage(
                            sib.getId(), cycleStartDate, cycleEndDate, visited);
                    if (sibUsage == null || sibUsage.getValue() == null) {
                        hasMissingSibling = true;
                        continue;
                    }
                    siblingsSum = siblingsSum.add(sibUsage.getValue());
                    worstStatus = combineWorstStatus(worstStatus, sibUsage.getDataStatus());
                }
                BigDecimal value = parentValue.subtract(siblingsSum);
                if (value.compareTo(BigDecimal.ZERO) < 0) {
                    value = BigDecimal.ZERO;
                }
                usage.setValue(value.setScale(SCALE, RoundingMode.HALF_UP));
                usage.setDataStatus((worstStatus == STATUS_COMPLETE && !hasMissingSibling)
                        ? STATUS_COMPLETE : STATUS_INCOMPLETE);
                return usage;
            } finally {
                visited.remove(meterId);
            }
        }

        // 普通电表：正常累加/非累加算法
        Result r;
        if (meter.getIsCumulative() != null && meter.getIsCumulative() == 1) {
            r = calculateCumulative(COLLECTION_TYPE_ELECTRICITY, meterId, cycleStartDate, cycleEndDate);
        } else {
            r = calculateNonCumulative(COLLECTION_TYPE_ELECTRICITY, meterId, cycleStartDate, cycleEndDate);
        }
        if (r.getDataStatus() == STATUS_NO_DATA) {
            usage.setDataStatus(STATUS_NO_DATA);
            usage.setDescription(r.getDataMissingDescription());
            return usage; // value 保持 null
        }
        usage.setValue(r.getEnergyMeasurementValue());
        usage.setDataStatus(r.getDataStatus());
        usage.setDescription(r.getDataMissingDescription());
        return usage;
    }

    /**
     * 构造虚拟/分摊电表的推导结果。
     * <p>
     * 虚拟/分摊电表无自身采集记录，其用电量为推导值，故 energyMeasurementValue
     * 与 adjustedEnergyValue 取相同值；collectionRecord 起止日期置空。
     *
     * @param usage     推导用电量（NO_DATA 时为 null）
     * @param status    数据状态
     * @param desc      缺失说明
     */
    private Result buildDerivedResult(BigDecimal usage, int status, String desc) {
        Result r = new Result();
        BigDecimal v = usage != null ? usage : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        r.setEnergyMeasurementValue(v);
        r.setAdjustedEnergyValue(v);
        r.setDataStatus(status);
        r.setDataMissingDescription(desc);
        return r;
    }

    /**
     * 取两个数据状态中较差的一个（COMPLETE < 其他 < NO_DATA）
     */
    private int combineWorstStatus(int a, int b) {
        if (a == STATUS_NO_DATA || b == STATUS_NO_DATA) {
            return STATUS_NO_DATA;
        }
        if (a == STATUS_COMPLETE) {
            return b;
        }
        if (b == STATUS_COMPLETE) {
            return a;
        }
        return a; // 两者均非 COMPLETE/NO_DATA，任取一个
    }

    /**
     * 数据状态中文名（用于缺失说明）
     */
    private String statusName(int status) {
        switch (status) {
            case STATUS_COMPLETE: return "完整";
            case STATUS_NO_DATA: return "无数据";
            case STATUS_INCOMPLETE: return "不完整";
            case STATUS_OVER_RANGE: return "超范围";
            case STATUS_INCOMPLETE_AND_OVER_RANGE: return "不完整且超范围";
            default: return "异常";
        }
    }

    // ========================= 算法A：累加量采集点 =========================

    /**
     * 算法A：累加量采集点能耗计量值计算
     *
     * 1. 只查 collectionStatus=1（成功）的记录
     * 2. 找起始记录（距核算周期起始日最近）和截止记录（距核算周期截止日最近，凌晨6点前算前一天）
     * 3. 判断数据状态（完整/不完整/超范围/不完整且超范围）
     * 4. 能耗计量值 = 截止值 - 起始值
     * 5. 修正值 = 原值 ± 超限/缺失天数 × 日均能耗
     */
    private Result calculateCumulative(Integer collectionPointType, Long collectionPointId,
                                       LocalDate cycleStartDate, LocalDate cycleEndDate) {
        Result result = new Result();

        // 加载采集点的创建/启用/停用时间，用于"数据不完整"判定的例外处理
        CollectionPointInfo pointInfo = loadCollectionPointInfo(collectionPointType, collectionPointId);

        List<CollectionRecord> successRecords = querySuccessRecords(collectionPointType, collectionPointId);
        if (successRecords.isEmpty()) {
            result.setEnergyMeasurementValue(BigDecimal.ZERO);
            result.setDataStatus(STATUS_NO_DATA);
            result.setDataMissingDescription("无采集数据");
            result.setAdjustedEnergyValue(BigDecimal.ZERO);
            return result;
        }

        successRecords.sort(Comparator.comparing(CollectionRecord::getCollectionTime));

        // 起始记录：collectionTime.toLocalDate() 距 cycleStartDate 最近，差值相等选起始日之后的
        CollectionRecord startRecord = findClosestRecord(successRecords, cycleStartDate,
                r -> r.getCollectionTime().toLocalDate(), true);
        LocalDate startEffectiveDate = startRecord.getCollectionTime().toLocalDate();

        // 截止记录：effective date（含6点规则）距 cycleEndDate 最近，差值相等选截止日之前的
        CollectionRecord endRecord = findClosestRecord(successRecords, cycleEndDate,
                r -> computeEndEffectiveDate(r.getCollectionTime()), false);
        LocalDate endEffectiveDate = computeEndEffectiveDate(endRecord.getCollectionTime());

        // 数据状态判断（含采集点启停时间的例外判定）
        int startStatus = determineStartStatus(startEffectiveDate, cycleStartDate, pointInfo);
        int endStatus = determineEndStatus(endEffectiveDate, cycleEndDate, pointInfo);
        int dataStatus = combineStatus(startStatus, endStatus);

        // 能耗计量值 = 截止值 - 起始值
        BigDecimal energyValue = endRecord.getReadingValue()
                .subtract(startRecord.getReadingValue())
                .setScale(SCALE, RoundingMode.HALF_UP);
        if (energyValue.compareTo(BigDecimal.ZERO) < 0) {
            energyValue = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }

        result.setEnergyMeasurementValue(energyValue);
        result.setDataStatus(dataStatus);
        result.setCollectionRecordStartDate(startRecord.getCollectionTime().toLocalDate());
        result.setCollectionRecordEndDate(endRecord.getCollectionTime().toLocalDate());
        result.setDataMissingDescription(buildCumulativeDescription(
                startStatus, endStatus, startEffectiveDate, endEffectiveDate,
                cycleStartDate, cycleEndDate));

        // 修正值
        result.setAdjustedEnergyValue(computeCumulativeAdjustedValue(
                energyValue, startStatus, endStatus, startEffectiveDate, endEffectiveDate,
                cycleStartDate, cycleEndDate, successRecords));

        return result;
    }

    /**
     * 计算截止记录的有效日期：凌晨6点前采集的算前一天
     */
    private LocalDate computeEndEffectiveDate(LocalDateTime collectionTime) {
        LocalDate date = collectionTime.toLocalDate();
        if (collectionTime.getHour() < SIX_AM_HOUR) {
            return date.minusDays(1);
        }
        return date;
    }

    /**
     * 在记录列表中查找距目标日期最近的记录
     *
     * @param records        记录列表
     * @param targetDate     目标日期
     * @param dateExtractor  从记录中提取比较日期的函数
     * @param preferAfter    差值相等时是否优先选择目标日期之后的记录（起始记录=true，截止记录=false）
     */
    private CollectionRecord findClosestRecord(List<CollectionRecord> records, LocalDate targetDate,
                                               Function<CollectionRecord, LocalDate> dateExtractor,
                                               boolean preferAfter) {
        CollectionRecord closest = null;
        long minDiff = Long.MAX_VALUE;
        for (CollectionRecord record : records) {
            LocalDate recordDate = dateExtractor.apply(record);
            long diff = Math.abs(ChronoUnit.DAYS.between(targetDate, recordDate));

            boolean shouldReplace;
            if (diff < minDiff) {
                shouldReplace = true;
            } else if (diff == minDiff && closest != null) {
                LocalDate closestDate = dateExtractor.apply(closest);
                if (preferAfter) {
                    shouldReplace = recordDate.isAfter(targetDate) && !closestDate.isAfter(targetDate);
                } else {
                    shouldReplace = recordDate.isBefore(targetDate) && !closestDate.isBefore(targetDate);
                }
            } else {
                shouldReplace = false;
            }

            if (shouldReplace) {
                minDiff = diff;
                closest = record;
            }
        }
        return closest;
    }

    /**
     * 判断起始记录状态：0-匹配，3-不完整（在起始日之后），4-超范围（在起始日之前）
     * <p>
     * 例外：当 startEffectiveDate 在 cycleStartDate 之后（原本应为"不完整"），
     * 但采集点在 cycleStartDate 之后才创建或启用（即采集点在该日期前根本不存在，
     * 并非数据缺失），此时不算数据不完整，返回 0（匹配），后续修正逻辑也不补加缺失值。
     * 判定条件：采集点的 createdAt 或 enableTime 任一非空且其日期部分在 cycleStartDate 之后。
     * </p>
     *
     * @param startEffectiveDate 起始记录的有效日期
     * @param cycleStartDate     核算周期起始日期
     * @param pointInfo          采集点时间信息（可空）
     */
    private int determineStartStatus(LocalDate startEffectiveDate, LocalDate cycleStartDate,
                                     CollectionPointInfo pointInfo) {
        if (startEffectiveDate.equals(cycleStartDate)) {
            return 0;
        }
        if (startEffectiveDate.isBefore(cycleStartDate)) {
            return STATUS_OVER_RANGE;
        }
        // 例外判定：采集点在核算周期起始日之后才创建或启用 → 视为匹配，不视为数据不完整
        if (pointInfo != null) {
            if (pointInfo.getCreatedAt() != null
                    && pointInfo.getCreatedAt().toLocalDate().isAfter(cycleStartDate)) {
                return 0;
            }
            if (pointInfo.getEnableTime() != null
                    && pointInfo.getEnableTime().toLocalDate().isAfter(cycleStartDate)) {
                return 0;
            }
        }
        return STATUS_INCOMPLETE;
    }

    /**
     * 判断截止记录状态：0-匹配，3-不完整（在截止日之前），4-超范围（在截止日之后）
     * <p>
     * 例外：当 endEffectiveDate 在 cycleEndDate 之前（原本应为"不完整"），
     * 但采集点在 cycleEndDate 之前就被停用（即采集点在该日期前已不再生效，
     * 并非数据缺失），此时不算数据不完整，返回 0（匹配），后续修正逻辑也不补加缺失值。
     * 判定条件：采集点的 disableTime 非空且其日期部分在 cycleEndDate 之前。
     * </p>
     *
     * @param endEffectiveDate 截止记录的有效日期
     * @param cycleEndDate    核算周期截止日期
     * @param pointInfo       采集点时间信息（可空）
     */
    private int determineEndStatus(LocalDate endEffectiveDate, LocalDate cycleEndDate,
                                   CollectionPointInfo pointInfo) {
        if (endEffectiveDate.equals(cycleEndDate)) {
            return 0;
        }
        if (endEffectiveDate.isAfter(cycleEndDate)) {
            return STATUS_OVER_RANGE;
        }
        // 例外判定：采集点在核算周期截止日之前就被停用 → 视为匹配，不视为数据不完整
        if (pointInfo != null
                && pointInfo.getDisableTime() != null
                && pointInfo.getDisableTime().toLocalDate().isBefore(cycleEndDate)) {
            return 0;
        }
        return STATUS_INCOMPLETE;
    }

    /**
     * 合并起始和截止状态为最终数据状态
     */
    private int combineStatus(int startStatus, int endStatus) {
        if (startStatus == 0 && endStatus == 0) {
            return STATUS_COMPLETE;
        }
        if (startStatus == 0) {
            return endStatus;
        }
        if (endStatus == 0) {
            return startStatus;
        }
        if ((startStatus == STATUS_OVER_RANGE && endStatus == STATUS_INCOMPLETE)
                || (startStatus == STATUS_INCOMPLETE && endStatus == STATUS_OVER_RANGE)) {
            return STATUS_INCOMPLETE_AND_OVER_RANGE;
        }
        if (startStatus == STATUS_OVER_RANGE && endStatus == STATUS_OVER_RANGE) {
            return STATUS_OVER_RANGE;
        }
        return STATUS_INCOMPLETE;
    }

    /**
     * 构建累加量算法的数据缺失说明
     * <p>
     * 根据 startStatus/endStatus 决定是否输出说明：
     * <ul>
     *   <li>状态为 0（匹配或例外）：不输出说明</li>
     *   <li>状态为 STATUS_OVER_RANGE：输出"超限"，附实际日期</li>
     *   <li>状态为 STATUS_INCOMPLETE：输出"无数据"</li>
     * </ul>
     * 例外情况下状态为 0，自然不输出说明，符合"不应被视为数据不完整"的语义。
     */
    private String buildCumulativeDescription(int startStatus, int endStatus,
                                              LocalDate startEffectiveDate, LocalDate endEffectiveDate,
                                              LocalDate cycleStartDate, LocalDate cycleEndDate) {
        StringBuilder sb = new StringBuilder();
        if (startStatus == STATUS_OVER_RANGE) {
            sb.append("起始日期超限(实际:").append(startEffectiveDate).append(")");
        } else if (startStatus == STATUS_INCOMPLETE) {
            sb.append("起始日期无数据");
        }
        if (endStatus == STATUS_OVER_RANGE) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append("截止日期数据超限(实际:").append(endEffectiveDate).append(")");
        } else if (endStatus == STATUS_INCOMPLETE) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append("截止日期无数据");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 计算累加量采集点的修正能耗值
     *
     * 超范围部分（起始日前 / 截止日后）的能耗应扣减；
     * 缺失部分（起始日后 / 截止日前）的能耗应补加。
     * 日均能耗从全部成功记录的整体区间估算。
     * <p>
     * 基于 startStatus/endStatus 判断补加/扣减，而非日期差：
     * <ul>
     *   <li>状态为 0（匹配或例外）：不做调整</li>
     *   <li>状态为 STATUS_OVER_RANGE：按超限天数扣减</li>
     *   <li>状态为 STATUS_INCOMPLETE：按缺失天数补加</li>
     * </ul>
     * 例外情况下状态为 0，自然不补加缺失值，符合"后续也不需要对缺失的数据进行插值补充"的语义。
     */
    private BigDecimal computeCumulativeAdjustedValue(BigDecimal energyValue,
                                                      int startStatus, int endStatus,
                                                      LocalDate startEffectiveDate, LocalDate endEffectiveDate,
                                                      LocalDate cycleStartDate, LocalDate cycleEndDate,
                                                      List<CollectionRecord> successRecords) {
        if (startStatus == 0 && endStatus == 0) {
            return energyValue;
        }

        BigDecimal avgDaily = computeCumulativeAvgDaily(successRecords);
        BigDecimal adjustment = BigDecimal.ZERO;

        // 起始超范围（起始日 < 核算起始日）→ 扣减
        if (startStatus == STATUS_OVER_RANGE) {
            long overRangeDays = ChronoUnit.DAYS.between(startEffectiveDate, cycleStartDate);
            adjustment = adjustment.subtract(avgDaily.multiply(BigDecimal.valueOf(overRangeDays)));
        }
        // 起始缺失（起始日 > 核算起始日）→ 补加
        if (startStatus == STATUS_INCOMPLETE) {
            long missingDays = ChronoUnit.DAYS.between(cycleStartDate, startEffectiveDate);
            adjustment = adjustment.add(avgDaily.multiply(BigDecimal.valueOf(missingDays)));
        }
        // 截止超范围（截止日 > 核算截止日）→ 扣减
        if (endStatus == STATUS_OVER_RANGE) {
            long overRangeDays = ChronoUnit.DAYS.between(cycleEndDate, endEffectiveDate);
            adjustment = adjustment.subtract(avgDaily.multiply(BigDecimal.valueOf(overRangeDays)));
        }
        // 截止缺失（截止日 < 核算截止日）→ 补加
        if (endStatus == STATUS_INCOMPLETE) {
            long missingDays = ChronoUnit.DAYS.between(endEffectiveDate, cycleEndDate);
            adjustment = adjustment.add(avgDaily.multiply(BigDecimal.valueOf(missingDays)));
        }

        return energyValue.add(adjustment).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算累加量采集点的日均能耗（基于全部成功记录的首末差值 / 天数）
     */
    private BigDecimal computeCumulativeAvgDaily(List<CollectionRecord> records) {
        if (records.size() < 2) {
            return BigDecimal.ZERO;
        }
        CollectionRecord first = records.get(0);
        CollectionRecord last = records.get(records.size() - 1);
        long totalDays = ChronoUnit.DAYS.between(
                first.getCollectionTime().toLocalDate(),
                last.getCollectionTime().toLocalDate());
        if (totalDays <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalConsumption = last.getReadingValue().subtract(first.getReadingValue());
        return totalConsumption.divide(BigDecimal.valueOf(totalDays), SCALE, RoundingMode.HALF_UP);
    }

    // ========================= 算法B：非累加量采集点 =========================

    /**
     * 算法B：非累加量采集点能耗计量值计算
     *
     * 1. 查询所有 collectionStatus=1 的记录
     * 2. 找 billingCycleStartDate 最接近核算起始日的记录 → collectionRecordStartDate
     * 3. 找 billingCycleEndDate 最接近核算截止日的记录 → collectionRecordEndDate
     * 4. 筛选 billingCycleStartDate ≥ 核算起始日 且 billingCycleEndDate ≤ 核算截止日 的记录
     * 5. 判断首尾衔接：完全衔接且起止匹配 → 完整；无记录 → 无数据；其他 → 不完整
     * 6. 能耗计量值 = 有效记录 readingValue 之和
     * 7. 修正值：超范围扣减、缺失补加、重叠扣减
     */
    private Result calculateNonCumulative(Integer collectionPointType, Long collectionPointId,
                                          LocalDate cycleStartDate, LocalDate cycleEndDate) {
        Result result = new Result();

        // 加载采集点的创建/启用/停用时间，用于"数据不完整"判定的例外处理
        CollectionPointInfo pointInfo = loadCollectionPointInfo(collectionPointType, collectionPointId);

        List<CollectionRecord> successRecords = querySuccessRecords(collectionPointType, collectionPointId);
        // 过滤掉 billingCycle 日期为空的记录
        List<CollectionRecord> validRecords = successRecords.stream()
                .filter(r -> r.getBillingCycleStartDate() != null && r.getBillingCycleEndDate() != null)
                .collect(Collectors.toList());

        if (validRecords.isEmpty()) {
            result.setEnergyMeasurementValue(BigDecimal.ZERO);
            result.setDataStatus(STATUS_NO_DATA);
            result.setDataMissingDescription("无采集数据");
            result.setAdjustedEnergyValue(BigDecimal.ZERO);
            return result;
        }

        // 找 billingCycleStartDate 最接近 cycleStartDate 的记录
        CollectionRecord startRecord = validRecords.stream()
                .min(Comparator.comparing(r ->
                        Math.abs(ChronoUnit.DAYS.between(cycleStartDate, r.getBillingCycleStartDate()))))
                .orElse(null);
        LocalDate collectionRecordStartDate = startRecord != null
                ? startRecord.getBillingCycleStartDate() : null;

        // 找 billingCycleEndDate 最接近 cycleEndDate 的记录
        CollectionRecord endRecord = validRecords.stream()
                .min(Comparator.comparing(r ->
                        Math.abs(ChronoUnit.DAYS.between(cycleEndDate, r.getBillingCycleEndDate()))))
                .orElse(null);
        LocalDate collectionRecordEndDate = endRecord != null
                ? endRecord.getBillingCycleEndDate() : null;

        // 筛选核算周期内的记录
        List<CollectionRecord> inCycleRecords = validRecords.stream()
                .filter(r -> !r.getBillingCycleStartDate().isBefore(cycleStartDate)
                        && !r.getBillingCycleEndDate().isAfter(cycleEndDate))
                .sorted(Comparator.comparing(CollectionRecord::getBillingCycleStartDate))
                .collect(Collectors.toList());

        // 判断数据状态
        int dataStatus;
        String description;

        if (inCycleRecords.isEmpty()) {
            dataStatus = STATUS_NO_DATA;
            description = "核算周期内无采集数据";
        } else {
            ConnectivityResult connectivity = checkConnectivity(inCycleRecords);
            boolean startsMatch = collectionRecordStartDate != null
                    && collectionRecordStartDate.equals(cycleStartDate);
            boolean endsMatch = collectionRecordEndDate != null
                    && collectionRecordEndDate.equals(cycleEndDate);

            // 例外①：前部缺失（collectionRecordStartDate 在 cycleStartDate 之后），
            // 但采集点在 cycleStartDate 之后才创建或启用 → 不视为不完整，startsMatch 视为匹配
            boolean startExempt = false;
            if (!startsMatch && collectionRecordStartDate != null
                    && collectionRecordStartDate.isAfter(cycleStartDate) && pointInfo != null) {
                if (pointInfo.getCreatedAt() != null
                        && pointInfo.getCreatedAt().toLocalDate().isAfter(cycleStartDate)) {
                    startExempt = true;
                }
                if (!startExempt && pointInfo.getEnableTime() != null
                        && pointInfo.getEnableTime().toLocalDate().isAfter(cycleStartDate)) {
                    startExempt = true;
                }
            }
            boolean effectiveStartsMatch = startsMatch || startExempt;

            // 例外②：尾部缺失（collectionRecordEndDate 在 cycleEndDate 之前），
            // 但采集点在 cycleEndDate 之前就被停用 → 不视为不完整，endsMatch 视为匹配
            boolean endExempt = false;
            if (!endsMatch && collectionRecordEndDate != null
                    && collectionRecordEndDate.isBefore(cycleEndDate) && pointInfo != null
                    && pointInfo.getDisableTime() != null
                    && pointInfo.getDisableTime().toLocalDate().isBefore(cycleEndDate)) {
                endExempt = true;
            }
            boolean effectiveEndsMatch = endsMatch || endExempt;

            // 例外③：中间空缺段若属于采集点"停用后再启用"区间 → 视为正常空缺，不需补加
            List<LocalDate[]> effectiveGaps = new ArrayList<>();
            for (LocalDate[] gap : connectivity.gaps) {
                if (!isGapInDisabledPeriod(gap[0], gap[1], pointInfo)) {
                    effectiveGaps.add(gap);
                }
            }

            // 状态判定：使用过滤后的空缺段和例外的首尾匹配
            if (effectiveGaps.isEmpty() && connectivity.overlaps.isEmpty()
                    && effectiveStartsMatch && effectiveEndsMatch) {
                dataStatus = STATUS_COMPLETE;
                description = null;
            } else {
                dataStatus = STATUS_INCOMPLETE;
                StringBuilder sb = new StringBuilder();
                // 输出非例外的空缺段
                for (LocalDate[] gap : effectiveGaps) {
                    if (sb.length() > 0) {
                        sb.append(";");
                    }
                    sb.append("存在空缺(")
                            .append(gap[0])
                            .append("至")
                            .append(gap[1])
                            .append(")");
                }
                // 输出重叠段
                for (LocalDate[] overlap : connectivity.overlaps) {
                    if (sb.length() > 0) {
                        sb.append(";");
                    }
                    sb.append("存在重叠(")
                            .append(overlap[0])
                            .append("至")
                            .append(overlap[1])
                            .append(")");
                }
                // 起始/截止不匹配（仅当未适用例外时输出）
                if (!effectiveStartsMatch) {
                    if (sb.length() > 0) {
                        sb.append(";");
                    }
                    sb.append("起始日期不匹配(实际:").append(collectionRecordStartDate).append(")");
                }
                if (!effectiveEndsMatch) {
                    if (sb.length() > 0) {
                        sb.append(";");
                    }
                    sb.append("截止日期不匹配(实际:").append(collectionRecordEndDate).append(")");
                }
                description = sb.toString();
            }
        }

        // 能耗计量值 = 有效记录 readingValue 之和
        BigDecimal energyValue = inCycleRecords.stream()
                .map(CollectionRecord::getReadingValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        result.setEnergyMeasurementValue(energyValue);
        result.setDataStatus(dataStatus);
        result.setCollectionRecordStartDate(collectionRecordStartDate);
        result.setCollectionRecordEndDate(collectionRecordEndDate);
        result.setDataMissingDescription(description);

        // 修正值：将 effectiveGaps 和例外标记传入，跳过属于例外的空缺补加
        result.setAdjustedEnergyValue(computeNonCumulativeAdjustedValue(
                energyValue, dataStatus, inCycleRecords, validRecords,
                cycleStartDate, cycleEndDate, collectionRecordStartDate, collectionRecordEndDate,
                pointInfo));

        return result;
    }

    /**
     * 检查核算周期内记录的首尾衔接性（无空缺、无重叠）
     * 同时收集所有空缺段和重叠段，供例外判定与修正值计算使用
     */
    private ConnectivityResult checkConnectivity(List<CollectionRecord> records) {
        ConnectivityResult result = new ConnectivityResult();
        result.fullyConnected = true;
        result.description = null;

        StringBuilder desc = new StringBuilder();
        for (int i = 1; i < records.size(); i++) {
            CollectionRecord prev = records.get(i - 1);
            CollectionRecord curr = records.get(i);
            // 上一条 billingCycleEndDate 与下一条 billingCycleStartDate 之间的天数差
            // 差=1 表示衔接（前一天结束，次日开始）；差>1 表示空缺；差<1 表示重叠
            long gap = ChronoUnit.DAYS.between(prev.getBillingCycleEndDate(), curr.getBillingCycleStartDate());
            if (gap == 1) {
                // 正常衔接
            } else if (gap > 1) {
                result.fullyConnected = false;
                LocalDate gapStart = prev.getBillingCycleEndDate().plusDays(1);
                LocalDate gapEnd = curr.getBillingCycleStartDate().minusDays(1);
                result.gaps.add(new LocalDate[]{gapStart, gapEnd});
                if (desc.length() > 0) {
                    desc.append(";");
                }
                desc.append("存在空缺(")
                        .append(gapStart)
                        .append("至")
                        .append(gapEnd)
                        .append(")");
            } else {
                result.fullyConnected = false;
                LocalDate overlapStart = curr.getBillingCycleStartDate();
                LocalDate overlapEnd = prev.getBillingCycleEndDate();
                result.overlaps.add(new LocalDate[]{overlapStart, overlapEnd});
                if (desc.length() > 0) {
                    desc.append(";");
                }
                desc.append("存在重叠(")
                        .append(overlapStart)
                        .append("至")
                        .append(overlapEnd)
                        .append(")");
            }
        }

        if (desc.length() > 0) {
            result.description = desc.toString();
        }
        return result;
    }

    /**
     * 判断中间空缺段是否属于采集点"停用后再启用"的区间（属于正常空缺，不需插值补充）
     * <p>
     * 触发条件：采集点 enableTime 在 disableTime 之后（即停用后再启用），
     * 且空缺段 [gapStart, gapEnd] 完全包含在 [disableTime, enableTime] 内。
     *
     * @param gapStart  空缺段起始日期（含）
     * @param gapEnd    空缺段截止日期（含）
     * @param pointInfo 采集点时间信息（可空）
     */
    private boolean isGapInDisabledPeriod(LocalDate gapStart, LocalDate gapEnd,
                                          CollectionPointInfo pointInfo) {
        if (pointInfo == null
                || pointInfo.getDisableTime() == null
                || pointInfo.getEnableTime() == null) {
            return false;
        }
        LocalDate disableDate = pointInfo.getDisableTime().toLocalDate();
        LocalDate enableDate = pointInfo.getEnableTime().toLocalDate();
        // 仅当 enableTime 在 disableTime 之后时才视为"停用后再启用"
        if (!enableDate.isAfter(disableDate)) {
            return false;
        }
        // 空缺段完全包含在 [disableDate, enableDate] 内
        return !gapStart.isBefore(disableDate) && !gapEnd.isAfter(enableDate);
    }

    /**
     * 计算非累加量采集点的修正能耗值
     *
     * 超范围（起始日之前 / 截止日之后）→ 按日均扣减；
     * 空缺 → 按日均补加；
     * 重叠 → 按日均扣减。
     * <p>
     * 例外处理：跳过属于例外的中间空缺段（采集点"停用后再启用"区间内的空缺），
     * 不对这些空缺进行补加。
     */
    private BigDecimal computeNonCumulativeAdjustedValue(BigDecimal energyValue, int dataStatus,
                                                         List<CollectionRecord> inCycleRecords,
                                                         List<CollectionRecord> allValidRecords,
                                                         LocalDate cycleStartDate, LocalDate cycleEndDate,
                                                         LocalDate collectionRecordStartDate,
                                                         LocalDate collectionRecordEndDate,
                                                         CollectionPointInfo pointInfo) {
        if (dataStatus == STATUS_COMPLETE) {
            return energyValue;
        }
        if (dataStatus == STATUS_NO_DATA) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal avgDaily = computeNonCumulativeAvgDaily(inCycleRecords, allValidRecords);
        BigDecimal adjustment = BigDecimal.ZERO;

        // 起始超范围：collectionRecordStartDate 在 cycleStartDate 之前 → 扣减
        if (collectionRecordStartDate != null && collectionRecordStartDate.isBefore(cycleStartDate)) {
            long overRangeDays = ChronoUnit.DAYS.between(collectionRecordStartDate, cycleStartDate);
            adjustment = adjustment.subtract(avgDaily.multiply(BigDecimal.valueOf(overRangeDays)));
        }
        // 截止超范围：collectionRecordEndDate 在 cycleEndDate 之后 → 扣减
        if (collectionRecordEndDate != null && collectionRecordEndDate.isAfter(cycleEndDate)) {
            long overRangeDays = ChronoUnit.DAYS.between(cycleEndDate, collectionRecordEndDate);
            adjustment = adjustment.subtract(avgDaily.multiply(BigDecimal.valueOf(overRangeDays)));
        }
        // 检查空缺和重叠
        for (int i = 1; i < inCycleRecords.size(); i++) {
            CollectionRecord prev = inCycleRecords.get(i - 1);
            CollectionRecord curr = inCycleRecords.get(i);
            long gap = ChronoUnit.DAYS.between(prev.getBillingCycleEndDate(), curr.getBillingCycleStartDate());
            if (gap > 1) {
                // 空缺：检查是否属于采集点"停用后再启用"区间内的例外空缺
                LocalDate gapStart = prev.getBillingCycleEndDate().plusDays(1);
                LocalDate gapEnd = curr.getBillingCycleStartDate().minusDays(1);
                if (isGapInDisabledPeriod(gapStart, gapEnd, pointInfo)) {
                    // 例外空缺：不补加
                    continue;
                }
                long missingDays = gap - 1;
                adjustment = adjustment.add(avgDaily.multiply(BigDecimal.valueOf(missingDays)));
            } else if (gap < 1) {
                // 重叠 → 扣减
                long overlapDays = 1 - gap;
                adjustment = adjustment.subtract(avgDaily.multiply(BigDecimal.valueOf(overlapDays)));
            }
        }

        return energyValue.add(adjustment).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算非累加量采集点的日均能耗
     * 优先使用核算周期内记录的日均，不足时回退到全部有效记录的日均
     */
    private BigDecimal computeNonCumulativeAvgDaily(List<CollectionRecord> inCycleRecords,
                                                     List<CollectionRecord> allValidRecords) {
        if (!inCycleRecords.isEmpty()) {
            BigDecimal totalValue = inCycleRecords.stream()
                    .map(CollectionRecord::getReadingValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long totalDays = inCycleRecords.stream()
                    .mapToLong(r -> ChronoUnit.DAYS.between(r.getBillingCycleStartDate(), r.getBillingCycleEndDate()) + 1)
                    .sum();
            if (totalDays > 0) {
                return totalValue.divide(BigDecimal.valueOf(totalDays), SCALE, RoundingMode.HALF_UP);
            }
        }
        if (!allValidRecords.isEmpty()) {
            BigDecimal totalValue = allValidRecords.stream()
                    .map(CollectionRecord::getReadingValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long totalDays = allValidRecords.stream()
                    .mapToLong(r -> ChronoUnit.DAYS.between(r.getBillingCycleStartDate(), r.getBillingCycleEndDate()) + 1)
                    .sum();
            if (totalDays > 0) {
                return totalValue.divide(BigDecimal.valueOf(totalDays), SCALE, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }

    // ========================= 公共辅助方法 =========================

    /**
     * 查询采集点的所有成功采集记录
     */
    private List<CollectionRecord> querySuccessRecords(Integer collectionPointType, Long collectionPointId) {
        List<CollectionRecord> records = collectionRecordRepository
                .findByCollectionPointTypeAndCollectionPointIdAndCollectionStatus(
                        collectionPointType, collectionPointId, COLLECTION_STATUS_SUCCESS);
        if (records == null) {
            return new ArrayList<>();
        }
        // 过滤掉 readingValue 或 collectionTime 为空的记录
        return records.stream()
                .filter(r -> r.getReadingValue() != null && r.getCollectionTime() != null)
                .collect(Collectors.toList());
    }

    /**
     * 连接性检查结果
     */
    private static class ConnectivityResult {
        boolean fullyConnected;
        String description;
        /** 空缺段列表：每项 [gapStart, gapEnd]（含首尾，已扣除衔接的1天偏移） */
        List<LocalDate[]> gaps = new ArrayList<>();
        /** 重叠段列表：每项 [overlapStart, overlapEnd]（含首尾） */
        List<LocalDate[]> overlaps = new ArrayList<>();
    }
}
