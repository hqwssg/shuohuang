package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.DefaultFactor;
import com.example.carbon.emission.model.repository.DefaultFactorRepository;
import com.example.carbon.emission.model.service.DefaultFactorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DefaultFactorServiceImpl implements DefaultFactorService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFactorServiceImpl.class);

    @Autowired
    private DefaultFactorRepository defaultFactorRepository;

    /**
     * 查询全部启用的缺省因子（status=1，按 subcategoryCode 升序）
     *
     * @return 启用状态缺省因子列表；无数据返回空列表
     */
    @Override
    public List<DefaultFactor> findAll() {
        return defaultFactorRepository.findByStatusOrderBySubcategoryCodeAsc(1);
    }

    /**
     * 按模版ID查询缺省因子（按 subcategoryCode 升序）
     *
     * @param templateId 模版ID；为 null/0 时表示查询系统缺省因子
     * @return 该模版下的缺省因子列表；无数据返回空列表
     */
    @Override
    public List<DefaultFactor> findByTemplateId(Long templateId) {
        return defaultFactorRepository.findByTemplateIdOrderBySubcategoryCodeAsc(templateId);
    }

    /**
     * 按排放数据小类编码查询单条记录
     *
     * @param subcategoryCode 排放数据小类编码
     * @return 实体；找不到返回 null
     */
    @Override
    public DefaultFactor findBySubcategoryCode(String subcategoryCode) {
        Optional<DefaultFactor> opt = defaultFactorRepository.findBySubcategoryCode(subcategoryCode);
        return opt.orElse(null);
    }

    /**
     * 新增缺省因子
     * <p>
     * 流程：subcategoryCode trim -> 非空校验 -> (templateId, subcategoryCode) 联合唯一校验
     * -> factorSource trim -> 保存。任一校验失败返回 null。
     *
     * @param req 待新增实体（templateId+subcategoryCode 必填且联合唯一）
     * @return 新增后的实体；联合唯一冲突或编码为空返回 null
     */
    @Override
    @Transactional
    public DefaultFactor create(DefaultFactor req) {
        String code = req.getSubcategoryCode();
        if (code != null) {
            code = code.trim();
            req.setSubcategoryCode(code);
        }
        if (code == null || code.isBlank()) {
            log.warn("新增缺省因子失败：排放数据小类编码为空");
            return null;
        }
        Long templateId = req.getTemplateId();
        if (defaultFactorRepository.existsByTemplateIdAndSubcategoryCode(templateId, code)) {
            log.warn("新增缺省因子失败：templateId={}, subcategoryCode={} 已存在缺省因子", templateId, code);
            return null;
        }
        if (req.getFactorSource() != null) {
            req.setFactorSource(req.getFactorSource().trim());
        }
        DefaultFactor saved = defaultFactorRepository.save(req);
        log.info("系统缺省碳排放因子已新增：id={}，templateId={}，subcategoryCode={}，factorSource={}，factorId={}",
                saved.getId(), saved.getTemplateId(), saved.getSubcategoryCode(), saved.getFactorSource(), saved.getFactorId());
        return saved;
    }

    /**
     * 更新缺省因子的因子选择/备注等信息
     * <p>
     * 可更新字段：factorSource（trim 后）、factorId、factorName、factorValue、
     * factorUnit、factorDescription、remark、updatedBy、status。仅对非空字段赋值。
     *
     * @param id  主键ID
     * @param req 待更新字段
     * @return 更新后的实体；找不到记录返回 null
     */
    @Override
    @Transactional
    public DefaultFactor update(Long id, DefaultFactor req) {
        Optional<DefaultFactor> opt = defaultFactorRepository.findById(id);
        if (opt.isEmpty()) {
            log.warn("更新缺省因子失败：记录不存在，id={}", id);
            return null;
        }
        DefaultFactor entity = opt.get();
        if (req.getFactorSource() != null && !req.getFactorSource().isBlank()) {
            entity.setFactorSource(req.getFactorSource().trim());
        }
        entity.setFactorId(req.getFactorId());
        entity.setFactorName(req.getFactorName());
        entity.setFactorValue(req.getFactorValue());
        entity.setFactorUnit(req.getFactorUnit());
        entity.setFactorDescription(req.getFactorDescription());
        entity.setRemark(req.getRemark());
        if (req.getUpdatedBy() != null) {
            entity.setUpdatedBy(req.getUpdatedBy());
        }
        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }
        DefaultFactor saved = defaultFactorRepository.save(entity);
        log.info("系统缺省碳排放因子已更新：id={}，templateId={}，subcategoryCode={}，factorSource={}，factorId={}",
                saved.getId(), saved.getTemplateId(), saved.getSubcategoryCode(), saved.getFactorSource(), saved.getFactorId());
        return saved;
    }

    /**
     * 删除缺省因子
     *
     * @param id 主键ID
     * @return 删除成功返回 true；记录不存在返回 false
     */
    @Override
    @Transactional
    public boolean delete(Long id) {
        if (!defaultFactorRepository.existsById(id)) {
            log.warn("删除缺省因子失败：记录不存在，id={}", id);
            return false;
        }
        defaultFactorRepository.deleteById(id);
        log.info("系统缺省碳排放因子已删除：id={}", id);
        return true;
    }

    /**
     * 按模版ID删除全部缺省因子（删模版时级联清理）
     *
     * @param templateId 模版ID
     */
    @Override
    @Transactional
    public void deleteByTemplateId(Long templateId) {
        defaultFactorRepository.deleteByTemplateId(templateId);
        log.info("已按模版ID删除全部缺省因子：templateId={}", templateId);
    }
}
