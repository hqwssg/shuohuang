package com.example.carbon.emission.model.scheduler;

import com.example.carbon.emission.model.entity.Template;
import com.example.carbon.emission.model.repository.TemplateRepository;
import com.example.carbon.emission.model.service.impl.AsyncTaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 碳排放核算任务调度器
 * 
 * 负责定时检查并执行碳排放核算任务。
 * 每分钟检查一次所有启用的模板，根据配置的执行时间和周期判断是否执行任务。
 * 满足条件的任务会提交到异步线程池执行。
 */
@Component
public class EmissionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmissionScheduler.class);

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private AsyncTaskService asyncTaskService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 定时检查并执行碳排放核算任务
     * 
     * 使用 cron 表达式每分钟执行一次。
     * 遍历所有启用的模板，判断是否满足执行条件，满足条件则提交异步任务。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkAndExecuteScheduledTasks() {
        // 记录检查时间，所有任务使用同一基准时间判断，避免任务执行过程中时间变化
        LocalDateTime checkTime = LocalDateTime.now();
        logger.debug("=== EmissionScheduler 开始检查定时任务 ===");
        
        // 查询所有启用的模板
        List<Template> enabledTemplates = templateRepository.findByEnabledTrue();
        logger.debug("找到启用的模板数量: {}", enabledTemplates.size());

        // 遍历模板，检查是否需要执行
        for (Template template : enabledTemplates) {
            try {
                if (shouldExecute(template, checkTime)) {
                    logger.info("****检测到需要执行的任务，模板ID: {}, 名称: {}", template.getId(), template.getName());
                    // 提交异步任务执行
                    asyncTaskService.executeEmissionCalculationAsync(template.getId(), template.getName());
                    logger.info("****已提交异步任务，模板ID: {}, 名称: {}", template.getId(), template.getName());
                } else {
                    logger.debug("模板不满足执行条件，模板ID: {}, 名称: {}", template.getId(), template.getName());
                }
            } catch (Exception e) {
                logger.error("****提交碳排放核算任务失败，模板ID: {}", template.getId(), e);
            }
        }
        logger.debug("=== EmissionScheduler 检查完成 ===");
    }

    /**
     * 判断模板是否应该执行
     * 
     * 判断逻辑：
     * 1. 检查任务配置是否存在
     * 2. 检查当前日期是否在起始日期之后
     * 3. 检查当前日期是否在截止日期之前（如果有）
     * 4. 检查当前时间是否匹配配置的执行时间
     * 5. 检查执行周期是否匹配
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

            logger.debug("检查执行时间: 模板ID={}, 检查时间={}, cycleType={}, executionTime={}, startDate={}, endDate={}", 
                template.getId(), checkTime, cycleType, executionTimeStr, startDateStr, endDateStr);

            // 检查起始日期
            if (startDateStr != null && !startDateStr.isEmpty()) {
                try {
                    LocalDate startDate = parseDateTime(startDateStr).toLocalDate();
                    if (checkTime.toLocalDate().isBefore(startDate)) {
                        logger.debug("当前日期在起始日期之前，跳过，模板ID: {}", template.getId());
                        return false;
                    }
                } catch (Exception e) {
                    logger.debug("解析起始日期失败: {}, 模板ID: {}", startDateStr, template.getId());
                }
            }

            // 检查截止日期
            if (endDateStr != null && !endDateStr.isEmpty()) {
                try {
                    LocalDate endDate = parseDateTime(endDateStr).toLocalDate();
                    if (endDate != null && checkTime.toLocalDate().isAfter(endDate)) {
                        logger.debug("当前日期在截止日期之后，跳过，模板ID: {}", template.getId());
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

            // 检查执行周期
            boolean cycleResult = checkCycle(cycleType, taskConfig, checkTime);
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
