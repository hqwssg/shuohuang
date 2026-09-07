package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.entity.DefaultFactor;

import java.util.List;

/**
 * 系统缺省碳排放因子设置 Service
 * <p>
 * 针对不同排放数据小类（emission_subcategory）维护系统默认使用的碳排放因子，
 * 因子从对应因子库（电力/化石燃料/热力/固废焚烧/废水处理）中选择，
 * 所选因子的名称/值/单位/说明以快照形式冗余存储。
 */
public interface DefaultFactorService {

    /**
     * 查询全部启用的缺省因子（status=1，按 subcategoryCode 升序）
     *
     * @return 启用状态缺省因子列表；无数据返回空列表
     */
    List<DefaultFactor> findAll();

    /**
     * 按模版ID查询缺省因子（按 subcategoryCode 升序）
     * templateId 为 null 时查系统缺省
     *
     * @param templateId 模版ID；为 null/0 时表示查询系统缺省因子
     * @return 该模版下的缺省因子列表；无数据返回空列表
     */
    List<DefaultFactor> findByTemplateId(Long templateId);

    /**
     * 按排放数据小类编码查询单条记录
     *
     * @param subcategoryCode 排放数据小类编码
     * @return 实体；找不到返回 null
     */
    DefaultFactor findBySubcategoryCode(String subcategoryCode);

    /**
     * 新增缺省因子
     *
     * @param req 待新增实体（templateId+subcategoryCode 必填且联合唯一）
     * @return 新增后的实体；若 templateId+subcategoryCode 已存在返回 null（由 Controller 转 400）
     */
    DefaultFactor create(DefaultFactor req);

    /**
     * 更新缺省因子的因子选择/备注等信息
     *
     * @param id  主键ID
     * @param req 待更新字段
     * @return 更新后的实体；找不到记录返回 null
     */
    DefaultFactor update(Long id, DefaultFactor req);

    /**
     * 删除缺省因子
     *
     * @param id 主键ID
     * @return 删除成功返回 true；记录不存在返回 false
     */
    boolean delete(Long id);

    /**
     * 按模版ID删除全部缺省因子（删模版时级联清理）
     *
     * @param templateId 模版ID
     */
    void deleteByTemplateId(Long templateId);
}
