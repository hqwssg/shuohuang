package com.example.carbon.emission.model.scheduler;

import com.example.carbon.emission.model.audit.RuoYiAuditClient;
import com.example.carbon.emission.model.entity.SystemConfig;
import com.example.carbon.emission.model.entity.Template;
import com.example.carbon.emission.model.repository.SystemConfigRepository;
import com.example.carbon.emission.model.repository.TemplateRepository;
import com.example.carbon.emission.model.service.DataCollectionService;
import com.example.carbon.emission.model.service.impl.AsyncTaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 碳排放核算任务调度器
 *
 * 负责两类调度：
 * <ol>
 *   <li>碳排放核算任务：每分钟检查一次所有启用的模板，根据配置的执行时间和
 *       周期判断是否执行任务，满足条件则提交到异步线程池执行。</li>
 *   <li>采集计划扫描任务：每天 0 点 0 分 0 秒执行一次，遍历三张采集点表，
 *       到达采集时间则插入待采集记录。</li>
 *   <li>待采集记录处理任务：固定延迟调度。从上一次任务结束时间算起，
 *       间隔 {@code Collection_task_cycle_interval}（系统配置，单位：分钟）
 *       后再次执行，遍历待采集记录并依次执行读取。采用自重调度（每轮结束后
 *       重新读取配置）实现固定延迟，支持运行时调整间隔。</li>
 * </ol>
 *
 * 仅在 job Profile 下激活（--spring.profiles.active=job），
 * 用于 Web/Job 分离部署：Web 服务器不加载本类，避免定时任务重复执行。
 */
@Component
@Profile("job")
public class EmissionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmissionScheduler.class);

    /** 系统配置项：采集任务循环执行时间间隔（分钟） */
    private static final String CFG_COLLECTION_CYCLE_INTERVAL = "Collection_task_cycle_interval";
    /** 采集循环间隔默认值（分钟，配置缺失或非法时使用） */
    private static final int DEFAULT_COLLECTION_CYCLE_INTERVAL_MIN = 5;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private AsyncTaskService asyncTaskService;

    @Autowired
    private DataCollectionService dataCollectionService;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RuoYiAuditClient auditClient;

    /** 待采集记录处理的固定延迟调度执行器（单线程守护线程，保证顺序执行不重叠） */
    private ScheduledExecutorService collectionProcessingExecutor;

    /**
     * 启动待采集记录处理的自重调度：立即触发首轮，之后每轮结束按配置间隔调度下一轮。
     */
    @PostConstruct
    public void initCollectionProcessing() {
        collectionProcessingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "data-collection-processing");
            t.setDaemon(true);
            return t;
        });
        // 首轮立即执行（延迟 0 秒）
        collectionProcessingExecutor.schedule(this::runCollectionProcessingCycle, 0, TimeUnit.SECONDS);
        logger.info("待采集记录处理固定延迟调度已启动，首轮间隔 {} 分钟", readCollectionIntervalMinutes());
    }

    /**
     * 销毁时关闭待采集记录处理执行器
     */
    @PreDestroy
    public void shutdownCollectionProcessing() {
        if (collectionProcessingExecutor != null) {
            collectionProcessingExecutor.shutdownNow();
            logger.info("待采集记录处理调度执行器已关闭");
        }
    }

    /**
     * 待采集记录处理的一轮执行
     *
     * 同步执行 {@link DataCollectionService#processPendingCollectionRecords()}，
     * 无论正常结束还是异常，都在结束后重新读取 {@code Collection_task_cycle_interval}
     * 配置并按该间隔（分钟）调度下一轮，实现"从上一次任务结束时间算起间隔指定分钟"
     * 的固定延迟语义。配置可在运行时调整，下一轮调度读取最新值。
     */
    private void runCollectionProcessingCycle() {
        long startedAt = System.currentTimeMillis();
        Exception failure = null;
        try {
            dataCollectionService.processPendingCollectionRecords();
        } catch (Exception e) {
            failure = e;
            logger.error("待采集记录处理任务异常", e);
        } finally {
            int intervalMin = readCollectionIntervalMinutes();
            recordJob("碳核算-待采集记录处理", "EmissionScheduler.runCollectionProcessingCycle",
                    failure == null ? "待采集记录处理完成；下一轮将在 " + intervalMin + " 分钟后执行" : "待采集记录处理失败",
                    failure, startedAt);
            logger.debug("下一轮待采集记录处理将在 {} 分钟后执行", intervalMin);
            collectionProcessingExecutor.schedule(this::runCollectionProcessingCycle, intervalMin, TimeUnit.MINUTES);
        }
    }

    /**
     * 读取采集任务循环执行时间间隔（分钟）
     * 来自系统配置 Collection_task_cycle_interval，缺失或非法时返回默认值 5
     */
    private int readCollectionIntervalMinutes() {
        try {
            return systemConfigRepository.findByConfigKey(CFG_COLLECTION_CYCLE_INTERVAL)
                    .map(SystemConfig::getConfigValue)
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_COLLECTION_CYCLE_INTERVAL_MIN);
        } catch (Exception e) {
            logger.warn("读取系统配置 {} 失败，使用默认值 {}",
                    CFG_COLLECTION_CYCLE_INTERVAL, DEFAULT_COLLECTION_CYCLE_INTERVAL_MIN, e);
            return DEFAULT_COLLECTION_CYCLE_INTERVAL_MIN;
        }
    }

    /**
     * 定时检查并执行碳排放核算任务
     *
     * 使用 cron 表达式每分钟执行一次。
     * 仅遍历启用的核算模版（template_type=2，enabled=true），
     * 判断是否满足执行条件，满足条件则提交异步任务。
     * 节点模版（template_type=1）不参与碳排放核算，跳过。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkAndExecuteScheduledTasks() {
        long startedAt = System.currentTimeMillis();
        int submitted = 0;
        int failed = 0;
        // 记录检查时间，所有任务使用同一基准时间判断，避免任务执行过程中时间变化
        LocalDateTime checkTime = LocalDateTime.now();
        logger.debug("=== EmissionScheduler 开始检查定时任务 ===");

        // 仅查询启用状态下的核算模版（templateType=2）
        List<Template> enabledTemplates = templateRepository.findByEnabledTrueAndTemplateType(2);
        logger.debug("找到启用的核算模版数量: {}", enabledTemplates.size());

        // 遍历模版，检查是否需要执行
        for (Template template : enabledTemplates) {
            try {
                if (shouldExecute(template, checkTime)) {
                    logger.info("****检测到需要执行的任务，模板ID: {}, 名称: {}", template.getId(), template.getName());
                    // 提交异步任务执行
                    asyncTaskService.executeEmissionCalculationAsync(template.getId(), template.getName());
                    submitted++;
                    logger.info("****已提交异步任务，模板ID: {}, 名称: {}", template.getId(), template.getName());
                } else {
                    logger.debug("模板不满足执行条件，模板ID: {}, 名称: {}", template.getId(), template.getName());
                }
            } catch (Exception e) {
                failed++;
                logger.error("****提交碳排放核算任务失败，模板ID: {}", template.getId(), e);
            }
        }
        recordJob("碳核算-调度检查", "EmissionScheduler.checkAndExecuteScheduledTasks",
                "检查启用核算模板 " + enabledTemplates.size() + " 个，提交 " + submitted + " 个，失败 " + failed + " 个",
                failed == 0 ? null : new IllegalStateException("有 " + failed + " 个模板提交失败"), startedAt);
        logger.debug("=== EmissionScheduler 检查完成 ===");
    }

    /**
     * 采集计划扫描任务
     *
     * 每天 0 点 0 分 0 秒执行一次，遍历三张采集点表
     * (emission_meter_info、emission_fossil_fuel_meter_info、
     * emission_purchased_heat_meter_info)，根据 billing_cycle_unit、
     * billing_cycle_start_date、billing_cycle_length 及采集点
     * last_collection_time 判断是否到达需要采集数据的时间，
     * 到达则把采集点信息插入采集记录表 (emission_collection_record，
     * collection_status=0)。所有周期统一按当年 1 月 1 日起算。
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void scanCollectionDataTask() {
        long startedAt = System.currentTimeMillis();
        Exception failure = null;
        logger.info("=== 采集计划扫描任务启动 ===");
        try {
            dataCollectionService.scanAndScheduleCollection();
        } catch (Exception e) {
            failure = e;
            logger.error("采集计划扫描任务异常", e);
        } finally {
            recordJob("碳核算-采集计划扫描", "EmissionScheduler.scanCollectionDataTask",
                    failure == null ? "电表、化石燃料和外购热能采集计划扫描完成" : "采集计划扫描失败",
                    failure, startedAt);
        }
        logger.info("=== 采集计划扫描任务结束 ===");
    }

    private void recordJob(String name, String target, String message, Exception failure, long startedAt) {
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("jobName", name);
        job.put("jobGroup", "DEFAULT");
        job.put("invokeTarget", target);
        job.put("jobMessage", message);
        job.put("status", failure == null ? "0" : "1");
        job.put("exceptionInfo", failure == null ? "" : failure.toString());
        job.put("startTime", startedAt);
        job.put("endTime", System.currentTimeMillis());
        auditClient.sendJob(job);
    }

    /**
     * 判断模板是否应该执行
     *
     * 判断逻辑：
     * 1. 检查任务配置是否存在
     * 2. 提取"延迟核算日期"（delayDays，统计周期结束后宽限期天数），计算调整后的判断时间
     *    adjustedTime = checkTime - delayDays，用于判断统计周期（年/月/日/星期）及起止日期是否匹配
     * 3. 检查统计周期起始日期（基于 adjustedTime）
     * 4. 检查统计周期截止日期（基于 adjustedTime，如有）
     * 5. 检查实际触发时间（checkTime 的时分）是否匹配配置的执行时间
     * 6. 检查执行周期是否匹配（基于 adjustedTime）
     *
     * 延迟核算日期语义：实际触发日期 = 配置的执行日期 + delayDays 天。
     * 例如 MONTHLY + monthDays=1 + delayDays=3，表示统计周期为每月1号，
     * 实际在每月4号的执行时间触发核算。delayDays 默认 0（兼容旧数据，立即执行）。
     *
     * @param template 模板实体
     * @param checkTime 检查时间（用于所有判断的基准时间）
     * @return 是否应该执行
     */
    private boolean shouldExecute(Template template, LocalDateTime checkTime) {
        String taskConfigJson = template.getTaskConfig();
        if (taskConfigJson == null || taskConfigJson.isEmpty()) {
            logger.debug("任务配置为空，模板ID: {}", template.getId());
            return false;
        }

        try {
            JsonNode taskConfig = objectMapper.readTree(taskConfigJson);

            // 提取配置参数
            String cycleType = getTextOrNull(taskConfig, "cycleType");
            String executionTimeStr = getTextOrNull(taskConfig, "executionTime");
            String startDateStr = getTextOrNull(taskConfig, "startDate");
            String endDateStr = getTextOrNull(taskConfig, "endDate");

            // 延迟核算日期（天）：统计周期结束后宽限期，实际触发日期 = 配置的执行日期 + delayDays
            // 后端默认 0（兼容旧数据，立即执行）；前端编辑模版时缺省 3 天
            int delayDays = taskConfig.has("delayDays") ? taskConfig.get("delayDays").asInt(0) : 0;
            if (delayDays < 0) {
                delayDays = 0;
            }
            // adjustedTime 用于判断统计周期（年/月/日/星期）及起止日期是否匹配
            LocalDateTime adjustedTime = delayDays > 0 ? checkTime.minusDays(delayDays) : checkTime;

            logger.debug("检查执行时间: 模板ID={}, 检查时间={}, delayDays={}, adjustedTime={}, cycleType={}, executionTime={}, startDate={}, endDate={}",
                template.getId(), checkTime, delayDays, adjustedTime, cycleType, executionTimeStr, startDateStr, endDateStr);

            // 检查起始日期（针对统计周期，基于 adjustedTime）
            if (startDateStr != null && !startDateStr.isEmpty()) {
                try {
                    LocalDate startDate = parseDateTime(startDateStr).toLocalDate();
                    if (adjustedTime.toLocalDate().isBefore(startDate)) {
                        logger.debug("统计周期起始日期之前，跳过，模板ID: {}", template.getId());
                        return false;
                    }
                } catch (Exception e) {
                    logger.debug("解析起始日期失败: {}, 模板ID: {}", startDateStr, template.getId());
                }
            }

            // 检查截止日期（针对统计周期，基于 adjustedTime）
            if (endDateStr != null && !endDateStr.isEmpty()) {
                try {
                    LocalDate endDate = parseDateTime(endDateStr).toLocalDate();
                    if (endDate != null && adjustedTime.toLocalDate().isAfter(endDate)) {
                        logger.debug("统计周期截止日期之后，跳过，模板ID: {}", template.getId());
                        return false;
                    }
                } catch (Exception e) {
                    logger.debug("解析截止日期失败: {}, 模板ID: {}", endDateStr, template.getId());
                }
            }

            // 检查执行时间
            if (executionTimeStr == null || executionTimeStr.isEmpty()) {
                logger.debug("执行时间为空，跳过，模板ID: {}", template.getId());
                return false;
            }

            try {
                LocalTime executionTime = LocalTime.parse(executionTimeStr);
                // 截断到分钟级别比较，避免秒级差异导致匹配失败
                LocalTime checkTimeTruncated = checkTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
                LocalTime executionTimeTruncated = executionTime.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
                logger.debug("比较时间: 检查时间={}, 执行时间={}, 是否相等={}, 模板ID={}", 
                    checkTimeTruncated, executionTimeTruncated, checkTimeTruncated.equals(executionTimeTruncated), template.getId());
                if (!checkTimeTruncated.equals(executionTimeTruncated)) {
                    return false;
                }
            } catch (Exception e) {
                logger.debug("解析执行时间失败: {}, 模板ID: {}", executionTimeStr, template.getId());
                return false;
            }

            // 检查执行周期（基于 adjustedTime 判断统计周期是否匹配，体现"延迟核算日期"）
            boolean cycleResult = checkCycle(cycleType, taskConfig, adjustedTime);
            logger.debug("周期检查结果: cycleType={}, result={}, 模板ID={}", cycleType, cycleResult, template.getId());
            return cycleResult;

        } catch (Exception e) {
            logger.error("解析任务配置失败，模版ID: {}", template.getId(), e);
            return false;
        }
    }

    /**
     * 检查执行周期是否匹配
     * 
     * 支持的周期类型：
     * - DAILY: 每日执行
     * - WEEKLY: 每周执行（可指定星期几）
     * - MONTHLY: 每月执行（可指定日期）
     * - QUARTERLY: 每季度执行（每季度第一个月的指定日期）
     * - YEARLY: 每年执行（可指定月份和日期）
     * 
     * @param cycleType 周期类型
     * @param taskConfig 任务配置
     * @param now 当前时间
     * @return 是否匹配周期
     */
    private boolean checkCycle(String cycleType, JsonNode taskConfig, LocalDateTime now) {
        if (cycleType == null) {
            return false;
        }

        int interval = taskConfig.has("interval") ? taskConfig.get("interval").asInt(1) : 1;

        switch (cycleType) {
            case "DAILY":
                return true;
            case "WEEKLY":
                JsonNode weekdays = taskConfig.has("weekdays") ? taskConfig.get("weekdays") : null;
                if (weekdays != null && weekdays.isArray()) {
                    int todayOfWeek = now.getDayOfWeek().getValue();
                    for (JsonNode day : weekdays) {
                        if (day.asInt() == todayOfWeek) {
                            return checkWeekInterval(taskConfig, now, interval);
                        }
                    }
                }
                return false;
            case "MONTHLY":
                JsonNode monthDays = taskConfig.has("monthDays") ? taskConfig.get("monthDays") : null;
                if (monthDays != null && monthDays.isArray()) {
                    int todayOfMonth = now.getDayOfMonth();
                    for (JsonNode day : monthDays) {
                        if (day.asInt() == todayOfMonth) {
                            return checkMonthInterval(taskConfig, now, interval);
                        }
                    }
                }
                return false;
            case "QUARTERLY":
                int dayOfMonth = now.getDayOfMonth();
                int monthOfQuarter = (now.getMonthValue() - 1) % 3;
                if (monthOfQuarter == 0) {
                    JsonNode quarterDays = taskConfig.has("quarterDays") ? taskConfig.get("quarterDays") : null;
                    if (quarterDays != null && quarterDays.isArray()) {
                        for (JsonNode day : quarterDays) {
                            if (day.asInt() == dayOfMonth) {
                                return checkQuarterInterval(taskConfig, now, interval);
                            }
                        }
                    }
                }
                return false;
            case "YEARLY":
                int month = now.getMonthValue();
                int day = now.getDayOfMonth();
                JsonNode yearMonths = taskConfig.has("yearMonths") ? taskConfig.get("yearMonths") : null;
                JsonNode yearDays = taskConfig.has("yearDays") ? taskConfig.get("yearDays") : null;
                if (yearMonths != null && yearMonths.isArray() && yearDays != null && yearDays.isArray()) {
                    for (int i = 0; i < yearMonths.size(); i++) {
                        if (yearMonths.get(i).asInt() == month &&
                            i < yearDays.size() && yearDays.get(i).asInt() == day) {
                            return checkYearInterval(taskConfig, now, interval);
                        }
                    }
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * 检查周间隔
     * 
     * @param taskConfig 任务配置
     * @param now 当前时间
     * @param interval 间隔周数
     * @return 是否满足间隔条件
     */
    private boolean checkWeekInterval(JsonNode taskConfig, LocalDateTime now, int interval) {
        return true;
    }

    /**
     * 检查月间隔
     * 
     * @param taskConfig 任务配置
     * @param now 当前时间
     * @param interval 间隔月数
     * @return 是否满足间隔条件
     */
    private boolean checkMonthInterval(JsonNode taskConfig, LocalDateTime now, int interval) {
        String startDateStr = taskConfig.has("startDate") ? taskConfig.get("startDate").asText() : null;
        if (startDateStr == null) {
            return true;
        }
        try {
            LocalDate startDate = parseDateTime(startDateStr).toLocalDate();
            long monthsSinceStart = java.time.temporal.ChronoUnit.MONTHS.between(startDate, now.toLocalDate());
            return monthsSinceStart % interval == 0;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 检查季度间隔
     * 
     * @param taskConfig 任务配置
     * @param now 当前时间
     * @param interval 间隔季度数
     * @return 是否满足间隔条件
     */
    private boolean checkQuarterInterval(JsonNode taskConfig, LocalDateTime now, int interval) {
        String startDateStr = taskConfig.has("startDate") ? taskConfig.get("startDate").asText() : null;
        if (startDateStr == null) {
            return true;
        }
        try {
            LocalDate startDate = parseDateTime(startDateStr).toLocalDate();
            long quartersSinceStart = (java.time.temporal.ChronoUnit.MONTHS.between(startDate, now.toLocalDate())) / 3;
            return quartersSinceStart % interval == 0;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 检查年间隔
     * 
     * @param taskConfig 任务配置
     * @param now 当前时间
     * @param interval 间隔年数
     * @return 是否满足间隔条件
     */
    private boolean checkYearInterval(JsonNode taskConfig, LocalDateTime now, int interval) {
        String startDateStr = taskConfig.has("startDate") ? taskConfig.get("startDate").asText() : null;
        if (startDateStr == null) {
            return true;
        }
        try {
            LocalDate startDate = parseDateTime(startDateStr).toLocalDate();
            long yearsSinceStart = java.time.temporal.ChronoUnit.YEARS.between(startDate, now.toLocalDate());
            return yearsSinceStart % interval == 0;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 安全获取 JSON 字段值
     * 
     * 处理字段为 null 或空字符串的情况，返回 null 而非字符串 "null"。
     * 
     * @param node JSON节点
     * @param fieldName 字段名
     * @return 字段值，null 表示不存在或为空
     */
    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        if ("null".equals(value) || value.isEmpty()) {
            return null;
        }
        return value;
    }

    /**
     * 解析日期时间字符串
     * 
     * 支持多种日期时间格式：
     * - ISO 8601 格式（带 Z 后缀的 UTC 时间）
     * - yyyy-MM-dd'T'HH:mm:ss
     * - yyyy-MM-dd'T'HH:mm
     * - yyyy-MM-dd HH:mm:ss
     * - yyyy-MM-dd HH:mm
     * - yyyy-MM-dd（日期格式，转换为当天开始时间）
     * 
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime 对象
     * @throws IllegalArgumentException 无法解析时抛出异常
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            throw new IllegalArgumentException("日期时间字符串为空");
        }

        String normalizedStr = dateTimeStr.trim();

        // 处理带 Z 后缀的 UTC 时间
        if (normalizedStr.endsWith("Z")) {
            try {
                return java.time.Instant.parse(normalizedStr)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            } catch (Exception e) {
            }
        }

        // 补充秒数（如果只有时分）
        if (normalizedStr.contains("T")) {
            String[] parts = normalizedStr.split("T");
            if (parts.length == 2) {
                String timePart = parts[1];
                if (timePart.split(":").length == 2) {
                    normalizedStr = normalizedStr + ":00";
                }
            }
        }

        // 尝试日期时间格式
        DateTimeFormatter[] dateTimeFormatters = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        };

        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                return LocalDateTime.parse(normalizedStr, formatter);
            } catch (Exception e) {
                continue;
            }
        }

        // 尝试日期格式（转换为当天开始时间）
        DateTimeFormatter[] dateFormatters = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                LocalDate date = LocalDate.parse(normalizedStr, formatter);
                return date.atStartOfDay();
            } catch (Exception e) {
                continue;
            }
        }

        throw new IllegalArgumentException("无法解析日期时间: " + dateTimeStr);
    }
}
