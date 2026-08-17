package com.example.carbon.emission.model.scheduler;

import com.example.carbon.emission.model.entity.EmissionNode;
import com.example.carbon.emission.model.entity.EmissionNodeConfig;
import com.example.carbon.emission.model.repository.EmissionNodeConfigRepository;
import com.example.carbon.emission.model.repository.EmissionNodeRepository;
import com.example.carbon.emission.model.service.DataCollectionService;
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
 * 数据采集任务调度器
 * 
 * 负责定时检查并执行数据采集任务。
 * 每分钟检查一次所有数据采集节点（typeId=3），根据配置的执行时间和周期判断是否执行任务。
 */
@Component
public class DataCollectionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataCollectionScheduler.class);

    @Autowired
    private EmissionNodeRepository nodeRepository;

    @Autowired
    private EmissionNodeConfigRepository configRepository;

    @Autowired
    private DataCollectionService dataCollectionService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 定时检查并执行数据采集任务
     * 
     * 使用 cron 表达式每分钟执行一次。
     * 遍历所有数据采集节点（typeId=3），判断是否满足执行条件，满足条件则执行数据采集。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkAndExecuteDataCollection() {
        // 记录检查时间，所有节点使用同一基准时间判断
        LocalDateTime checkTime = LocalDateTime.now();
        logger.trace("=== DataCollectionScheduler 开始检查定时任务 ===");
        
        // 查询所有数据采集节点（typeId=3）
        List<EmissionNode> dataCollectionNodes = nodeRepository.findByTypeId(3);
        logger.debug("找到数据采集节点数量: {}", dataCollectionNodes.size());

        // 遍历节点，检查是否需要执行
        for (EmissionNode node : dataCollectionNodes) {
            try {
                if (shouldExecuteCollection(node, checkTime)) {
                    logger.debug("+++检测到需要执行数据采集，节点ID: {}, 名称: {}", node.getId(), node.getName());
                    dataCollectionService.executeDataCollection(node.getId());
                } else {
                    logger.debug("节点不满足执行条件，节点ID: {}, 名称: {}", node.getId(), node.getName());
                }
            } catch (Exception e) {
                logger.error("---执行数据采集任务失败，节点ID: {}", node.getId(), e);
            }
        }
        logger.debug("=== DataCollectionScheduler 检查完成 ===");
    }

    /**
     * 判断节点是否应该执行数据采集
     * 
     * 判断逻辑：
     * 1. 检查节点配置是否存在
     * 2. 检查数据来源是否为手工录入（手工录入不自动执行）
     * 3. 检查任务配置是否存在
     * 4. 检查执行时间和周期是否匹配
     * 
     * @param node 节点实体
     * @param checkTime 检查时间
     * @return 是否应该执行
     */
    private boolean shouldExecuteCollection(EmissionNode node, LocalDateTime checkTime) {
        EmissionNodeConfig config = configRepository.findByNodeId(node.getId()).orElse(null);
        if (config == null) {
            logger.debug("节点配置为空，节点ID: {}", node.getId());
            return false;
        }

        String dataSource = config.getDataSource();
        if ("手工录入".equals(dataSource)) {
            logger.debug("数据来源为手工录入，跳过，节点ID: {}", node.getId());
            return false;
        }

        String taskConfigJson = config.getTaskConfig();
        if (taskConfigJson == null || taskConfigJson.isEmpty()) {
            logger.debug("任务配置为空，节点ID: {}", node.getId());
            return false;
        }

        try {
            JsonNode taskConfig = objectMapper.readTree(taskConfigJson);
            return checkExecutionTime(taskConfig, checkTime);
        } catch (Exception e) {
            logger.error("解析节点任务配置失败，节点ID: {}", node.getId(), e);
            return false;
        }
    }

    /**
     * 检查执行时间是否匹配
     * 
     * @param taskConfig 任务配置
     * @param checkTime 检查时间
     * @return 是否匹配
     */
    private boolean checkExecutionTime(JsonNode taskConfig, LocalDateTime checkTime) {
        String startDateStr = getTextOrNull(taskConfig, "startDate");
        String endDateStr = getTextOrNull(taskConfig, "endDate");
        String executionTimeStr = getTextOrNull(taskConfig, "executionTime");

        logger.debug("检查执行时间: 检查时间={}, startDate={}, endDate={}, executionTime={}", 
            checkTime, startDateStr, endDateStr, executionTimeStr);

        // 检查起始日期
        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                LocalDate startDate = parseDateTime(startDateStr).toLocalDate();
                if (checkTime.toLocalDate().isBefore(startDate)) {
                    logger.debug("当前日期在起始日期之前，跳过");
                    return false;
                }
            } catch (Exception e) {
                logger.debug("解析起始日期失败: {}", startDateStr);
            }
        }

        // 检查截止日期
        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                LocalDate endDate = parseDateTime(endDateStr).toLocalDate();
                if (checkTime.toLocalDate().isAfter(endDate)) {
                    logger.debug("当前日期在截止日期之后，跳过");
                    return false;
                }
            } catch (Exception e) {
                logger.debug("解析截止日期失败: {}", endDateStr);
            }
        }

        // 检查执行时间
        if (executionTimeStr == null || executionTimeStr.isEmpty()) {
            logger.debug("执行时间为空，跳过");
            return false;
        }

        try {
            LocalTime executionTime = LocalTime.parse(executionTimeStr);
            // 截断到分钟级别比较
            LocalTime checkTimeTruncated = checkTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
            LocalTime executionTimeTruncated = executionTime.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
            logger.debug("比较时间: 检查时间={}, 执行时间={}, 是否相等={}", checkTimeTruncated, executionTimeTruncated, checkTimeTruncated.equals(executionTimeTruncated));
            if (!checkTimeTruncated.equals(executionTimeTruncated)) {
                return false;
            }
        } catch (Exception e) {
            logger.debug("解析执行时间失败: {}", executionTimeStr);
            return false;
        }

        // 检查执行周期
        String cycleType = taskConfig.has("cycleType") ? taskConfig.get("cycleType").asText() : "DAILY";
        boolean cycleResult = checkCycle(cycleType, taskConfig, checkTime);
        logger.debug("周期检查结果: cycleType={}, result={}", cycleType, cycleResult);
        return cycleResult;
    }

    /**
     * 检查执行周期是否匹配
     * 
     * @param cycleType 周期类型
     * @param taskConfig 任务配置
     * @param now 当前时间
     * @return 是否匹配
     */
    private boolean checkCycle(String cycleType, JsonNode taskConfig, LocalDateTime now) {
        int interval = taskConfig.has("interval") ? taskConfig.get("interval").asInt(1) : 1;

        switch (cycleType) {
            case "DAILY":
                return true;
            case "WEEKLY":
                JsonNode weekDays = taskConfig.has("weekDays") ? taskConfig.get("weekDays") : null;
                if (weekDays != null && weekDays.isArray()) {
                    int todayOfWeek = now.getDayOfWeek().getValue();
                    for (JsonNode day : weekDays) {
                        if (day.asText().equals(String.valueOf(todayOfWeek))) {
                            return checkWeekInterval(taskConfig, now, interval);
                        }
                    }
                }
                return false;
            case "MONTHLY":
                String monthDaysStr = taskConfig.has("monthDays") ? taskConfig.get("monthDays").asText() : null;
                if (monthDaysStr != null && !monthDaysStr.isEmpty()) {
                    int todayOfMonth = now.getDayOfMonth();
                    String[] days = monthDaysStr.split(",");
                    for (String day : days) {
                        try {
                            if (Integer.parseInt(day.trim()) == todayOfMonth) {
                                return checkMonthInterval(taskConfig, now, interval);
                            }
                        } catch (NumberFormatException e) {
                            logger.debug("解析月执行日期失败: {}", day);
                        }
                    }
                }
                return false;
            case "QUARTERLY":
                String quarterMonthDaysStr = taskConfig.has("monthDays") ? taskConfig.get("monthDays").asText() : null;
                if (quarterMonthDaysStr != null && !quarterMonthDaysStr.isEmpty()) {
                    int todayOfMonth = now.getDayOfMonth();
                    int monthOfQuarter = (now.getMonthValue() - 1) % 3;
                    if (monthOfQuarter == 0) {
                        String[] days = quarterMonthDaysStr.split(",");
                        for (String day : days) {
                            try {
                                if (Integer.parseInt(day.trim()) == todayOfMonth) {
                                    return checkQuarterInterval(taskConfig, now, interval);
                                }
                            } catch (NumberFormatException e) {
                                logger.debug("解析季执行日期失败: {}", day);
                            }
                        }
                    }
                }
                return false;
            case "YEARLY":
                String yearMonthsStr = taskConfig.has("yearMonths") ? taskConfig.get("yearMonths").asText() : null;
                if (yearMonthsStr != null && !yearMonthsStr.isEmpty()) {
                    int todayMonth = now.getMonthValue();
                    int todayDay = now.getDayOfMonth();
                    String[] monthDayPairs = yearMonthsStr.split(",");
                    for (String pair : monthDayPairs) {
                        try {
                            String[] parts = pair.trim().split("-");
                            if (parts.length == 2) {
                                int month = Integer.parseInt(parts[0].trim());
                                int day = Integer.parseInt(parts[1].trim());
                                if (month == todayMonth && day == todayDay) {
                                    return checkYearInterval(taskConfig, now, interval);
                                }
                            }
                        } catch (NumberFormatException e) {
                            logger.debug("解析年执行日期失败: {}", pair);
                        }
                    }
                }
                return false;
            default:
                return false;
        }
    }

    private boolean checkWeekInterval(JsonNode taskConfig, LocalDateTime now, int interval) {
        return true;
    }

    private boolean checkMonthInterval(JsonNode taskConfig, LocalDateTime now, int interval) {
        String startDateStr = taskConfig.has("startDate") ? taskConfig.get("startDate").asText() : null;
        if (startDateStr == null || startDateStr.isEmpty()) {
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

    private boolean checkQuarterInterval(JsonNode taskConfig, LocalDateTime now, int interval) {
        String startDateStr = taskConfig.has("startDate") ? taskConfig.get("startDate").asText() : null;
        if (startDateStr == null || startDateStr.isEmpty()) {
            return true;
        }
        try {
            LocalDate startDate = parseDateTime(startDateStr).toLocalDate();
            long quartersSinceStart = java.time.temporal.ChronoUnit.MONTHS.between(startDate, now.toLocalDate()) / 3;
            return quartersSinceStart % interval == 0;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean checkYearInterval(JsonNode taskConfig, LocalDateTime now, int interval) {
        String startDateStr = taskConfig.has("startDate") ? taskConfig.get("startDate").asText() : null;
        if (startDateStr == null || startDateStr.isEmpty()) {
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
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime 对象
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            throw new IllegalArgumentException("日期时间字符串为空");
        }

        String normalizedStr = dateTimeStr.trim();

        if (normalizedStr.endsWith("Z")) {
            try {
                return java.time.Instant.parse(normalizedStr)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            } catch (Exception e) {
            }
        }

        if (normalizedStr.contains("T")) {
            String[] parts = normalizedStr.split("T");
            if (parts.length == 2) {
                String timePart = parts[1];
                if (timePart.split(":").length == 2) {
                    normalizedStr = normalizedStr + ":00";
                }
            }
        }

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
