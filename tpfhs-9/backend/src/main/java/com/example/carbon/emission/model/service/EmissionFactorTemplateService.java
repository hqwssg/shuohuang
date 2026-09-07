package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.entity.EmissionFactorTemplate;

import java.util.List;

/**
 * 排放因子模版 Service
 * <p>
 * 维护排放因子模版的增删改查、状态切换、引用计数及模版拷贝等业务逻辑。
 */
public interface EmissionFactorTemplateService {

    /**
     * 查询全部排放因子模版（按创建时间倒序）
     *
     * @return 模版列表；无数据返回空列表
     */
    List<EmissionFactorTemplate> findAll();

    /**
     * 按主键ID查询单条排放因子模版
     *
     * @param id 模版主键ID
     * @return 模版实体；不存在返回 null
     */
    EmissionFactorTemplate findById(Long id);

    /**
     * 新增排放因子模版
     * <p>
     * 模版名称自动 trim 并校验唯一性，重复时返回 null。
     *
     * @param req 待新增实体（templateName 必填且唯一）
     * @return 新增后的实体；名称重复返回 null（由 Controller 转 400）
     */
    EmissionFactorTemplate create(EmissionFactorTemplate req);

    /**
     * 按主键ID更新排放因子模版
     * <p>
     * 仅更新非空字段：名称、描述、是否共享、状态、更新人。
     *
     * @param id  模版主键ID
     * @param req 待更新字段
     * @return 更新后的实体；记录不存在返回 null
     */
    EmissionFactorTemplate update(Long id, EmissionFactorTemplate req);

    /**
     * 按主键ID删除排放因子模版
     *
     * @param id 模版主键ID
     * @return 删除成功返回 true；记录不存在返回 false
     */
    boolean delete(Long id);

    /**
     * 切换模版启停状态（1<->0）
     *
     * @param id        模版主键ID
     * @param updatedBy 更新人ID
     * @return 切换成功返回 true；记录不存在返回 false
     */
    boolean toggleStatus(Long id, Long updatedBy);

    /**
     * 切换模版共享状态（1<->0）
     *
     * @param id        模版主键ID
     * @param updatedBy 更新人ID
     * @return 切换成功返回 true；记录不存在返回 false
     */
    boolean toggleShared(Long id, Long updatedBy);

    /**
     * 统计模版被核算模版引用的次数
     * <p>
     * userId 不为空时仅统计他人引用（用于编辑锁定判断），自己引用不锁定；
     * userId 为 null 时统计全部引用。
     *
     * @param factorTemplateId 因子模版主键ID
     * @param excludeUserId    需排除的用户ID（即当前用户）；为 null 时统计全部引用
     * @return 引用计数；factorTemplateId 为 null 时返回 0
     */
    long countReferences(Long factorTemplateId, Long excludeUserId);

    /**
     * 拷贝源模版生成新模版（含其下全部缺省因子）
     * <p>
     * 拷贝源模版的描述、共享标识，并复制全部 DefaultFactor 到新模版下；
     * 新名称重复或源不存在时返回 null。
     *
     * @param sourceId  源模版主键ID
     * @param newName   新模版名称（不能重复）
     * @param createdBy 创建人ID
     * @return 新模版实体；源不存在或名称重复返回 null
     */
    EmissionFactorTemplate copyTemplate(Long sourceId, String newName, Long createdBy);
}
