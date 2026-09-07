package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模版校验结果 VO
 * <p>
 * 返回校验结论（checkResult）、校验时间、富文本详情（checkMessage）
 * 以及结构化的明细项列表（items），供前端弹窗展示。
 */
@Data
public class TemplateValidationResultVO {

    /**
     * 校验结果：0-未检查，1-完全正确，2-正确（存在提示信息），
     * 3-存在告警，4-存在错误
     */
    private Integer checkResult;

    /** 校验时间 */
    private LocalDateTime checkTime;

    /** 校验结果详情（富文本HTML，错误/告警/提示以不同颜色字体显示） */
    private String checkMessage;

    /** 结构化明细项列表 */
    private List<Item> items = new ArrayList<>();

    /**
     * 单条校验明细
     */
    @Data
    public static class Item {
        /**
         * 级别：ERROR-错误（红）、WARNING-告警（橙）、INFO-提示（蓝）
         */
        private String level;
        /** 明细内容 */
        private String message;

        public Item() {
        }

        public Item(String level, String message) {
            this.level = level;
            this.message = message;
        }
    }
}
