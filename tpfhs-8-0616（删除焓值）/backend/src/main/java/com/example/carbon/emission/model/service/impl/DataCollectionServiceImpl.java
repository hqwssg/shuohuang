package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.EmissionNode;
import com.example.carbon.emission.model.entity.EmissionNodeConfig;
import com.example.carbon.emission.model.repository.EmissionNodeConfigRepository;
import com.example.carbon.emission.model.repository.EmissionNodeRepository;
import com.example.carbon.emission.model.service.DataCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DataCollectionServiceImpl implements DataCollectionService {

    private static final Logger logger = LoggerFactory.getLogger(DataCollectionServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private EmissionNodeRepository nodeRepository;

    @Autowired
    private EmissionNodeConfigRepository configRepository;

    @Override
    public void executeDataCollection(Long nodeId) {
        long startTime = System.currentTimeMillis();
        String startTimeStr = LocalDateTime.now().format(FORMATTER);
        
        logger.info("========== 数据采集任务开始 ==========");
        logger.info("任务开始时间: {}", startTimeStr);
        logger.info("待采集节点ID: {}", nodeId);
        
        try {
            EmissionNode node = nodeRepository.findById(nodeId).orElse(null);
            if (node == null) {
                logger.warn("[WARN] 数据采集节点不存在，节点ID: {}", nodeId);
                logger.info("========== 数据采集任务结束 (节点不存在) ==========\n");
                return;
            }

            logger.info("节点名称: {}", node.getName());
            logger.info("节点类型ID: {} ({})", node.getTypeId(), getTypeName(node.getTypeId()));
            logger.info("所属模版ID: {}", node.getTemplateId());
            logger.info("父节点ID: {}", node.getParentId());

            EmissionNodeConfig config = configRepository.findByNodeId(nodeId).orElse(null);
            if (config != null) {
                logger.info("数据来源: {}", config.getDataSource());
                logger.info("统计口径: {}", config.getStatisticalCaliber());
                logger.info("排放类别: {} - {}", config.getEmissionCategory(), config.getEmissionSubcategory());
            }

            logger.info("--------------------------");

            String result = "SUCCESS";
            try {
                switch (node.getTypeId()) {
                    case 3:
                        collectDataFromDataCollectionPoint(node);
                        break;
                    case 4:
                        collectDataFromTransportNode(node);
                        break;
                    default:
                        logger.info("节点类型 {} 不支持自动数据采集，跳过", node.getTypeId());
                        result = "SKIPPED";
                }
            } catch (Exception e) {
                logger.error("[ERROR] 数据采集执行异常", e);
                result = "FAILED";
            }

            long duration = System.currentTimeMillis() - startTime;
            String endTimeStr = LocalDateTime.now().format(FORMATTER);

            logger.info("--------------------------");
            logger.info("任务执行结果: {}", result);
            logger.info("任务结束时间: {}", endTimeStr);
            logger.info("任务耗时: {} ms", duration);
            logger.info("========== 数据采集任务结束 ==========\n");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[ERROR] 数据采集任务整体异常，节点ID: {}, 耗时: {} ms", nodeId, duration, e);
            logger.info("========== 数据采集任务结束 (异常) ==========\n");
        }
    }

    private void collectDataFromDataCollectionPoint(EmissionNode node) {
        logger.info("[数据采集点] 开始采集数据");
        logger.info("[数据采集点] 节点名称: {}", node.getName());
        logger.info("[数据采集点] 模拟执行数据采集操作...");
        logger.info("[数据采集点] 采集完成");
    }

    private void collectDataFromTransportNode(EmissionNode node) {
        logger.info("[运输节点] 开始采集数据");
        logger.info("[运输节点] 节点名称: {}", node.getName());
        logger.info("[运输节点] 机车类型: {}", node.getLocomotiveType());
        logger.info("[运输节点] 模拟执行运输数据采集操作...");
        logger.info("[运输节点] 采集完成");
    }

    private String getTypeName(Integer typeId) {
        return switch (typeId) {
            case 1 -> "根节点";
            case 2 -> "核算子节点";
            case 3 -> "排放数据采集点";
            case 4 -> "运输生产碳排放核算节点";
            default -> "未知类型";
        };
    }
}
