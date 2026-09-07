import axios from 'axios';

const API_BASE_URL = '/api';

export const authApi = {
  login(username, password) {
    return axios.post(`${API_BASE_URL}/user/login`, { username, password });
  }
};

export const templateApi = {
  getAllTemplates() {
    return axios.get(`${API_BASE_URL}/template/list`);
  },
  
  getTemplateById(id) {
    return axios.get(`${API_BASE_URL}/template/${id}`);
  },
  
  createTemplate(name, createdBy, description = '', templateType = 1) {
    return axios.post(`${API_BASE_URL}/template/create`, { name, createdBy, description, templateType });
  },
  
  updateTemplate(id, name, updatedBy) {
    return axios.put(`${API_BASE_URL}/template/${id}`, { name, updatedBy });
  },
  
  updateTemplateProperties(id, description, enabled, templateType, taskConfig, factorTemplateId, updatedBy) {
    return axios.put(`${API_BASE_URL}/template/${id}/properties`, { description, enabled, templateType, taskConfig, factorTemplateId, updatedBy });
  },
  
  copyTemplate(sourceId, newName, createdBy) {
    return axios.post(`${API_BASE_URL}/template/copy/${sourceId}`, { newName, createdBy });
  },

  deleteTemplate(id) {
    return axios.delete(`${API_BASE_URL}/template/${id}`);
  },

  /**
   * 校验核算模版（仅核算模版有效）
   * 执行因子模版设置、采集点存在且启用、小类覆盖、单独因子一致性、空核算节点检查，
   * 结果保存到模版并返回 { checkResult, checkTime, checkMessage(富文本HTML), items }
   * @param {number|string} id 核算模版ID
   */
  validateTemplate(id) {
    return axios.post(`${API_BASE_URL}/template/${id}/validate`);
  },

  /**
   * 查看核算模版最近一次校验结果（不重新校验）
   * @param {number|string} id 核算模版ID
   * @returns {Promise<Object>} { checkResult, checkTime, checkMessage }
   */
  getValidationResult(id) {
    return axios.get(`${API_BASE_URL}/template/${id}/validation-result`);
  }
};

export const nodeApi = {
  getTree(templateId = null) {
    const url = templateId ? `${API_BASE_URL}/nodes/tree?templateId=${templateId}` : `${API_BASE_URL}/nodes/tree`;
    return axios.get(url);
  },
  
  getNodeById(id) {
    return axios.get(`${API_BASE_URL}/nodes/${id}`);
  },
  
  createNode(data) {
    return axios.post(`${API_BASE_URL}/nodes`, data);
  },

  /**
   * 挂载节点模版
   * 将指定节点模版（templateType=1）所包含的全部子节点复制到当前模版树中，
   * 挂载到目标父节点（typeId=1/2 的"排放核算点"）之下。
   * @param {number} parentId     目标父节点ID（挂载位置）
   * @param {number} templateId   来源节点模版ID
   * @param {number} createdBy     创建人ID
   * @returns {Promise} axios 请求 Promise，成功返回挂载后的目标父节点DTO
   */
  mountNodeTemplate(parentId, templateId, createdBy) {
    return axios.post(`${API_BASE_URL}/nodes/${parentId}/mount-template`, { templateId, createdBy });
  },
  
  updateNode(id, data) {
    return axios.put(`${API_BASE_URL}/nodes/${id}`, data);
  },
  
  deleteNode(id) {
    return axios.delete(`${API_BASE_URL}/nodes/${id}`);
  },
  
  moveNode(id, direction) {
    return axios.post(`${API_BASE_URL}/nodes/${id}/move?direction=${direction}`);
  },
  
  getNodeConfig(id) {
    return axios.get(`${API_BASE_URL}/nodes/${id}/config`);
  },
  
  updateNodeConfig(id, config) {
    return axios.put(`${API_BASE_URL}/nodes/${id}/config`, config);
  },
  
  getOptions() {
    return axios.get(`${API_BASE_URL}/nodes/options`);
  }
};

/**
 * 碳排放核算能耗缺省单位 API
 * 后端接口路径：/api/calc-unit-defaults
 *
 * 行数据固定（数据库初始化的 22 条排放数据小类），前端仅允许
 * 修改每行的核算单位、报告单位、换算系数、备注，不允许新增/删除行。
 */
export const calcUnitDefaultApi = {
  /**
   * 查询全部缺省单位设置（按 subcategoryCode 升序）
   * @returns {Promise<Array>} 22 条缺省单位记录
   */
  listAll() {
    return axios.get(`${API_BASE_URL}/calc-unit-defaults`);
  },

  /**
   * 按排放数据小类编码查询单条设置
   * @param {string} subcategoryCode - 小类编码，如 'FF_D'
   * @returns {Promise<Object>} 该小类的缺省单位记录
   */
  getByCode(subcategoryCode) {
    return axios.get(`${API_BASE_URL}/calc-unit-defaults/${subcategoryCode}`);
  },

  /**
   * 更新单条缺省单位设置（按 subcategoryCode 定位）
   * 换算系数不再存储，由 emission_unit_conversion 表动态查询
   * @param {string} subcategoryCode        - 排放数据小类编码（必填）
   * @param {Object} payload
   * @param {string} payload.calculationUnit - 核算单位编码（必填，必须为 emission_unit_standard.unit_code）
   * @param {string} payload.reportUnit      - 报告单位编码（必填，必须为 emission_unit_standard.unit_code）
   * @param {string} [payload.remark]         - 备注说明（可选）
   * @param {number} [payload.updatedBy]      - 更新人ID（可选）
   * @returns {Promise<Object>} { success, message, data }
   */
  update(subcategoryCode, payload) {
    return axios.put(`${API_BASE_URL}/calc-unit-defaults/${subcategoryCode}`, payload);
  }
};

/**
 * 计量单位统一 API
 * 后端接口路径：/api/units
 *
 * 提供标准单位查询、转换系数查询、单位换算等接口。
 * 前端所有"计量单位"下拉框都从此 API 拉取选项，禁止人工录入。
 */
export const unitApi = {
  /**
   * 查询全部启用的标准单位（按 sort_order 升序）
   * @returns {Promise<Array>} 标准单位列表
   */
  listAllStandards() {
    return axios.get(`${API_BASE_URL}/units/standards`);
  },

  /**
   * 按单位大类查询标准单位
   * @param {string} category - ELECTRIC/HEAT/LIQUID_FUEL/GAS_FUEL/SOLID_FUEL/AREA
   * @returns {Promise<Array>}
   */
  listByCategory(category) {
    return axios.get(`${API_BASE_URL}/units/standards/category/${category}`);
  },

  /**
   * 按小类编码查询其全部转换关系
   * @param {string} subcategoryCode - 排放数据小类编码，如 'FF_D'
   * @returns {Promise<Array>} 转换关系列表
   */
  listConversions(subcategoryCode) {
    return axios.get(`${API_BASE_URL}/units/conversions/${subcategoryCode}`);
  },

  /**
   * 按小类+源单位查询可转换的目标单位列表（下拉联动用）
   * @param {string} subcategoryCode
   * @param {string} fromUnitCode
   * @returns {Promise<Array>}
   */
  listConversionsFrom(subcategoryCode, fromUnitCode) {
    return axios.get(`${API_BASE_URL}/units/conversions/${subcategoryCode}/from/${fromUnitCode}`);
  },

  /**
   * 单位换算
   * @param {string} subcategoryCode
   * @param {string} fromUnitCode
   * @param {string} toUnitCode
   * @param {number} value
   * @returns {Promise<Object>} { subcategoryCode, fromUnitCode, toUnitCode, inputValue, outputValue }
   */
  convert(subcategoryCode, fromUnitCode, toUnitCode, value) {
    return axios.get(`${API_BASE_URL}/units/convert`, {
      params: { subcategoryCode, fromUnitCode, toUnitCode, value }
    });
  },

  /**
   * 校验单位编码是否合法
   * @param {string} unitCode
   * @returns {Promise<Object>} { unitCode, valid }
   */
  validate(unitCode) {
    return axios.get(`${API_BASE_URL}/units/validate`, { params: { unitCode } });
  }
};

/**
 * 温室气体质量单位 API
 * 后端接口路径：/api/ghg-units
 *
 * 提供碳排放因子的分子单位（kgCO2、tCO2、kgCH4、tCH4、kgN2O、tN2O）查询，
 * 数据由数据库初始化固定，前端因子单位分子下拉从本 API 拉取。
 */
export const ghgApi = {
  /**
   * 查询全部启用的温室气体质量单位（按 sort_order 升序）
   * @returns {Promise<Array>} 温室气体质量单位列表
   */
  listAll() {
    return axios.get(`${API_BASE_URL}/ghg-units`);
  },

  /**
   * 按 ghg_code 查询单条
   * @param {string} code - 温室气体质量单位编码，如 'kgCO2'、'tCO2'
   * @returns {Promise<Object>} 该单位记录
   */
  getByCode(code) {
    return axios.get(`${API_BASE_URL}/ghg-units/${code}`);
  }
};

/**
 * 碳排放因子单位 API
 * 后端接口路径：/api/factor-units
 *
 * 提供按小类查询可选因子单位（分子+分母组合），供前端因子单位下拉使用。
 * 每行一个合法的因子单位组合，按排放数据小类限定。
 */
export const factorUnitApi = {
  /**
   * 查询全部启用的因子单位（按 sort_order 升序）
   * @returns {Promise<Array>} 因子单位列表
   */
  listAll() {
    return axios.get(`${API_BASE_URL}/factor-units`);
  },

  /**
   * 按排放数据小类编码查询启用单位（按 sort_order 升序）
   * @param {string} code - 排放数据小类编码，如 'PE_PF'、'FF_D'
   * @returns {Promise<Array>} 该小类的因子单位列表
   */
  getBySubcategory(code) {
    return axios.get(`${API_BASE_URL}/factor-units/by-subcategory/${code}`);
  },

  /**
   * 按多个小类编码批量查询启用单位（前端一次拉取多小类下拉用）
   * @param {string[]} codes - 排放数据小类编码数组，如 ['PE_PF', 'FF_D']
   * @returns {Promise<Array>} 命中小类的因子单位列表
   */
  getBySubcategories(codes) {
    return axios.get(`${API_BASE_URL}/factor-units/by-subcategories`, {
      params: { codes: codes.join(',') }
    });
  }
};

/**
 * 核算节点汇总数据 API
 * 后端：/api/calc-summary/...
 */
export const calcSummaryApi = {
  /**
   * 按核算任务 ID 查询节点汇总
   * @param {number|string} templateId - 核算任务 ID（calculation_template.id）
   * @param {Object} [params] 可选过滤
   * @param {number} [params.nodeId] 节点快照 ID
   * @param {string} [params.subcategoryCode] 排放小类编码如 PE_PF
   * @param {string} [params.l1] 一级场景
   * @param {string} [params.l2] 二级场景
   * @param {string} [params.l3] 三级场景
   * @returns {Promise<Array>} 汇总列表
   */
  byTemplate(templateId, params = {}) {
    return axios.get(`${API_BASE_URL}/calc-summary/by-template/${templateId}`, { params });
  },

  /**
   * 按组织节点 + 周期范围查询
   * @param {Object} params
   * @param {number} params.sourceNodeId 源节点 ID
   * @param {string} params.cycleStartDate 起始日 yyyy-MM-dd
   * @param {string} params.cycleEndDate   截止日 yyyy-MM-dd
   * @returns {Promise<Array>} 匹配周期内所有成功核算的汇总
   */
  byNodeAndCycle(params) {
    return axios.get(`${API_BASE_URL}/calc-summary/by-node-and-cycle`, { params });
  }
};

/**
 * 系统缺省碳排放因子设置 API
 * 后端：/api/default-factors/...
 * 针对不同能耗小类设置系统默认使用的碳排放因子（从各因子库选择）
 */
export const defaultFactorApi = {
  /**
   * 查询全部缺省因子（启用，按小类编码升序）
   * @returns {Promise<Array>} 缺省因子记录列表
   */
  listAll() {
    return axios.get(`${API_BASE_URL}/default-factors`);
  },

  /**
   * 按小类编码查询单条
   * @param {string} subcategoryCode 排放小类编码，如 PE_PF
   * @returns {Promise<Object>} 记录
   */
  getBySubcategory(subcategoryCode) {
    return axios.get(`${API_BASE_URL}/default-factors/${subcategoryCode}`);
  },

  /**
   * 新增一条缺省因子（添加能耗小类并选择因子）
   * @param {Object} data { subcategoryCode, subcategoryName, factorSource, factorId, factorName, factorValue, factorUnit, factorDescription, remark, createdBy }
   * @returns {Promise<Object>} 后端返回 { success, message, data }
   */
  create(data) {
    return axios.post(`${API_BASE_URL}/default-factors`, data);
  },

  /**
   * 更新（更换因子/修改备注）
   * @param {number|string} id 记录主键
   * @param {Object} data 同 create（updatedBy）
   * @returns {Promise<Object>}
   */
  update(id, data) {
    return axios.put(`${API_BASE_URL}/default-factors/${id}`, data);
  },

  /**
   * 删除一条缺省因子
   * @param {number|string} id 记录主键
   * @returns {Promise<Object>}
   */
  remove(id) {
    return axios.delete(`${API_BASE_URL}/default-factors/${id}`);
  },

  /**
   * 按模版ID查询缺省因子
   * @param {number|string} templateId 模版ID
   * @returns {Promise<Array>} 该模版下的缺省因子列表
   */
  listByTemplate(templateId) {
    return axios.get(`${API_BASE_URL}/default-factors/by-template/${templateId}`);
  },

  /**
   * 按模版ID删除全部缺省因子（删模版时级联清理）
   * @param {number|string} templateId 模版ID
   * @returns {Promise<Object>}
   */
  removeByTemplate(templateId) {
    return axios.delete(`${API_BASE_URL}/default-factors/by-template/${templateId}`);
  }
};

/**
 * 碳排放核算因子模版 API
 * 后端：/api/factor-templates/...
 * 管理多个碳排放因子设置库（模版），支持 CRUD、启用/停用、共享/私有切换
 */
export const factorTemplateApi = {
  /**
   * 查询全部模版（按创建时间倒序）
   * @returns {Promise<Array>}
   */
  listAll() {
    return axios.get(`${API_BASE_URL}/factor-templates`);
  },

  /**
   * 按ID查询单个模版
   * @param {number|string} id
   * @returns {Promise<Object>}
   */
  getById(id) {
    return axios.get(`${API_BASE_URL}/factor-templates/${id}`);
  },

  /**
   * 新建模版
   * @param {Object} data { templateName, templateDescription, isShared, createdBy }
   * @returns {Promise<Object>} { success, message, data }
   */
  create(data) {
    return axios.post(`${API_BASE_URL}/factor-templates`, data);
  },

  /**
   * 更新模版
   * @param {number|string} id
   * @param {Object} data { templateName, templateDescription, isShared, status, updatedBy }
   * @returns {Promise<Object>}
   */
  update(id, data) {
    return axios.put(`${API_BASE_URL}/factor-templates/${id}`, data);
  },

  /**
   * 删除模版（后端不级联，前端需先调 defaultFactorApi.removeByTemplate）
   * @param {number|string} id
   * @returns {Promise<Object>}
   */
  remove(id) {
    return axios.delete(`${API_BASE_URL}/factor-templates/${id}`);
  },

  /**
   * 切换模版启用/停用状态
   * @param {number|string} id
   * @param {number|string} updatedBy
   * @returns {Promise<Object>}
   */
  toggleStatus(id, updatedBy) {
    return axios.put(`${API_BASE_URL}/factor-templates/${id}/toggle-status`, { updatedBy });
  },

  /**
   * 切换模版共享/私有状态
   * @param {number|string} id
   * @param {number|string} updatedBy
   * @returns {Promise<Object>}
   */
  toggleShared(id, updatedBy) {
    return axios.put(`${API_BASE_URL}/factor-templates/${id}/toggle-shared`, { updatedBy });
  },

  copyTemplate(sourceId, newName, createdBy) {
    return axios.post(`${API_BASE_URL}/factor-templates/copy`, { sourceId, newName, createdBy });
  },

  getReferenceCount(id, userId) {
    const params = userId ? `?userId=${userId}` : '';
    return axios.get(`${API_BASE_URL}/factor-templates/${id}/reference-count${params}`);
  }
};

/**
 * 碳排放核算单位转换系数设置 API
 * 后端：/api/unit-conversions/...
 *
 * 为每个能耗小类维护成对的单位转换系数（如汽油 L↔t，双向显式存储）。
 * 转换公式：目标值 = 源值 × conversionFactor。
 * 源/目标单位必须从标准单位（emission_unit_standard）下拉选择，禁止人工录入。
 */
export const unitConversionApi = {
  /**
   * 查询全部转换系数（按小类编码、源单位编码升序）
   * @returns {Promise<Array>} 转换系数列表
   */
  listAll() {
    return axios.get(`${API_BASE_URL}/unit-conversions`);
  },

  /**
   * 新增一条转换系数
   * @param {Object} data { subcategoryCode, fromUnitCode, toUnitCode, conversionFactor, remark }
   * @returns {Promise<Object>} 后端返回 { success, message, data }
   */
  create(data) {
    return axios.post(`${API_BASE_URL}/unit-conversions`, data);
  },

  /**
   * 更新一条转换系数
   * @param {number|string} id 记录主键
   * @param {Object} data { fromUnitCode, toUnitCode, conversionFactor, remark }
   * @returns {Promise<Object>}
   */
  update(id, data) {
    return axios.put(`${API_BASE_URL}/unit-conversions/${id}`, data);
  },

  /**
   * 删除一条转换系数
   * @param {number|string} id 记录主键
   * @returns {Promise<Object>}
   */
  remove(id) {
    return axios.delete(`${API_BASE_URL}/unit-conversions/${id}`);
  }
};
