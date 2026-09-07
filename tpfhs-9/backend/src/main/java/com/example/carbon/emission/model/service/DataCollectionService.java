package com.example.carbon.emission.model.service;

/**
 * 数据采集服务接口
 *
 * 负责碳排放数据采集的后台执行。采集采用两阶段模式：
 * 1. 每日 0 点 0 分 0 秒扫描三张采集点表（emission_meter_info、
 *    emission_fossil_fuel_meter_info、emission_purchased_heat_meter_info），
 *    根据 billing_cycle_unit、billing_cycle_start_date、billing_cycle_length
 *    判断是否到达需要采集数据的时间，到达则把采集点信息插入采集记录表
 *    (emission_collection_record，collection_status=0)。
 *    所有周期统一按每年的 1 月 1 日开始计算。
 * 2. 由专门的线程遍历 emission_collection_record 表中 collection_status=0
 *    的记录，依次执行采集任务；采集成功后回填采集点的 last_collection_time
 *    （对应采集记录 collection_time 字段的值）。
 */
public interface DataCollectionService {

    /**
     * 扫描采集点并生成待采集记录
     *
     * 遍历三张采集点表中 status=1（启用）的记录，根据
     * billing_cycle_unit、billing_cycle_start_date、billing_cycle_length
     * 及采集点 last_collection_time 判断是否到达采集时间。
     * 到达则插入一条 collection_status=0 的采集记录（若该采集点
     * 已存在未处理的待采集记录则跳过，避免重复插入）。
     */
    void scanAndScheduleCollection();

    /**
     * 处理待采集记录
     *
     * 遍历 emission_collection_record 表中 collection_status=0 的记录，
     * 依次执行采集任务（读取计量表数值），成功后更新记录的
     * collection_status=1、reading_value、collection_time、last_fetch_time，
     * 并回填对应采集点表的 last_collection_time。
     */
    void processPendingCollectionRecords();
}
