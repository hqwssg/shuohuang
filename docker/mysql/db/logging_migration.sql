-- RuoYi 日志模块增量迁移脚本
-- 仅创建缺失对象，不删除现有业务表和日志数据。
-- 执行前请确认当前数据库为 carbon_emissions。

USE carbon_emissions;

CREATE TABLE IF NOT EXISTS sys_oper_log (
  oper_id           BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  title             VARCHAR(50) DEFAULT '' COMMENT '模块标题',
  business_type     INT(2) DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  method            VARCHAR(200) DEFAULT '' COMMENT '方法名称',
  request_method    VARCHAR(10) DEFAULT '' COMMENT '请求方式',
  operator_type     INT(1) DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  oper_name         VARCHAR(50) DEFAULT '' COMMENT '操作人员',
  dept_name         VARCHAR(50) DEFAULT '' COMMENT '部门名称',
  oper_url          VARCHAR(255) DEFAULT '' COMMENT '请求URL',
  oper_ip           VARCHAR(128) DEFAULT '' COMMENT '主机地址',
  oper_location     VARCHAR(255) DEFAULT '' COMMENT '操作地点',
  oper_param        VARCHAR(2000) DEFAULT '' COMMENT '请求参数',
  json_result       VARCHAR(2000) DEFAULT '' COMMENT '返回参数',
  status            INT(1) DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  error_msg         VARCHAR(2000) DEFAULT '' COMMENT '错误消息',
  oper_time         DATETIME COMMENT '操作时间',
  cost_time         BIGINT(20) DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (oper_id),
  KEY idx_sys_oper_log_bt (business_type),
  KEY idx_sys_oper_log_s (status),
  KEY idx_sys_oper_log_ot (oper_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';

CREATE TABLE IF NOT EXISTS sys_logininfor (
  info_id        BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  user_name      VARCHAR(50) DEFAULT '' COMMENT '用户账号',
  ipaddr         VARCHAR(128) DEFAULT '' COMMENT '登录IP地址',
  status         CHAR(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  msg            VARCHAR(255) DEFAULT '' COMMENT '提示信息',
  access_time    DATETIME COMMENT '访问时间',
  PRIMARY KEY (info_id),
  KEY idx_sys_logininfor_s (status),
  KEY idx_sys_logininfor_lt (access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统访问记录';

CREATE TABLE IF NOT EXISTS sys_job_log (
  job_log_id       BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  job_name         VARCHAR(64) NOT NULL COMMENT '任务名称',
  job_group        VARCHAR(64) NOT NULL COMMENT '任务组名',
  invoke_target    VARCHAR(500) NOT NULL COMMENT '调用目标字符串',
  job_message      VARCHAR(500) COMMENT '日志信息',
  status           CHAR(1) DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  exception_info   VARCHAR(2000) DEFAULT '' COMMENT '异常信息',
  start_time       DATETIME COMMENT '执行开始时间',
  end_time         DATETIME COMMENT '执行结束时间',
  create_time      DATETIME COMMENT '创建时间',
  PRIMARY KEY (job_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务调度日志表';

-- RuoYi 标准日志菜单。INSERT IGNORE 可重复执行，不会覆盖已有菜单数据。
INSERT IGNORE INTO sys_menu VALUES
('108', '日志管理', '1', '9', 'log', '', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', NOW(), '', NULL, '日志管理菜单'),
('500', '操作日志', '108', '1', 'operlog', 'system/operlog/index', '', '', 1, 0, 'C', '0', '0', 'system:operlog:list', 'form', 'admin', NOW(), '', NULL, '操作日志菜单'),
('501', '登录日志', '108', '2', 'logininfor', 'system/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'system:logininfor:list', 'logininfor', 'admin', NOW(), '', NULL, '登录日志菜单'),
('1039', '操作查询', '500', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:operlog:query', '#', 'admin', NOW(), '', NULL, ''),
('1040', '操作删除', '500', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:operlog:remove', '#', 'admin', NOW(), '', NULL, ''),
('1041', '日志导出', '500', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:operlog:export', '#', 'admin', NOW(), '', NULL, ''),
('1042', '登录查询', '501', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:logininfor:query', '#', 'admin', NOW(), '', NULL, ''),
('1043', '登录删除', '501', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:logininfor:remove', '#', 'admin', NOW(), '', NULL, ''),
('1044', '日志导出', '501', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:logininfor:export', '#', 'admin', NOW(), '', NULL, ''),
('1045', '账户解锁', '501', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:logininfor:unlock', '#', 'admin', NOW(), '', NULL, ''),
('110', '定时任务', '2', '2', 'job', 'monitor/job/index', '', '', 1, 0, 'C', '0', '0', 'monitor:job:list', 'job', 'admin', NOW(), '', NULL, '定时任务菜单'),
('1049', '任务查询', '110', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query', '#', 'admin', NOW(), '', NULL, ''),
('1050', '任务新增', '110', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add', '#', 'admin', NOW(), '', NULL, ''),
('1051', '任务修改', '110', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit', '#', 'admin', NOW(), '', NULL, ''),
('1052', '任务删除', '110', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove', '#', 'admin', NOW(), '', NULL, ''),
('1053', '状态修改', '110', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus', '#', 'admin', NOW(), '', NULL, ''),
('1054', '任务导出', '110', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export', '#', 'admin', NOW(), '', NULL, '');

-- 为标准普通角色补充日志菜单；超级管理员不依赖此关联。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (108, 500, 501, 110, 1039, 1040, 1041, 1042, 1043, 1044, 1045, 1049, 1050, 1051, 1052, 1053, 1054)
WHERE r.role_key = 'common';
