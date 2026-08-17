-- GoView permissions for the carbon emission entry page.
-- Run this after the base RuoYi SQL when these permissions need to be assigned by role.

insert into sys_menu values('2000', '碳排放平台权限', '0', '5', 'carbon-permissions', null, '', '', 1, 0, 'M', '1', '0', '', 'dashboard', 'admin', sysdate(), '', null, '碳排放平台按钮与GoView权限');

insert into sys_menu values('2010', '项目管理', '2000', '1', 'goview-project', null, '', '', 1, 0, 'M', '1', '0', 'goview:project:*', '#', 'admin', sysdate(), '', null, 'GoView项目管理');
insert into sys_menu values('2011', '项目查看', '2010', '1', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:project:view', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2012', '项目创建', '2010', '2', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:project:create', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2013', '项目编辑', '2010', '3', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:project:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2014', '项目删除', '2010', '4', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:project:delete', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2015', '项目发布', '2010', '5', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:project:publish', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2016', '项目复制', '2010', '6', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:project:copy', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu values('2020', '模板管理', '2000', '2', 'goview-template', null, '', '', 1, 0, 'M', '1', '0', 'goview:template:*', '#', 'admin', sysdate(), '', null, 'GoView模板管理');
insert into sys_menu values('2021', '查看我的模板', '2020', '1', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:template:view', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2022', '查看模板市场', '2020', '2', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:template:market', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2023', '模板创建', '2020', '3', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:template:create', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2024', '模板使用', '2020', '4', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:template:use', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2025', '模板删除', '2020', '5', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:template:delete', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu values('2030', '图表编辑', '2000', '3', 'goview-chart', null, '', '', 1, 0, 'M', '1', '0', 'goview:chart:*', '#', 'admin', sysdate(), '', null, 'GoView图表编辑');
insert into sys_menu values('2031', '编辑画布', '2030', '1', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:chart:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2032', '图表预览', '2030', '2', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:chart:preview', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2033', '图表保存', '2030', '3', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:chart:save', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2034', '组件管理', '2030', '4', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:chart:component:*', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2035', '数据配置', '2030', '5', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:chart:data:config', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu values('2040', '数据池管理', '2000', '4', 'goview-data', null, '', '', 1, 0, 'M', '1', '0', 'goview:data:*', '#', 'admin', sysdate(), '', null, 'GoView数据池管理');
insert into sys_menu values('2041', '数据池查看', '2040', '1', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:data:view', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2042', '数据池创建', '2040', '2', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:data:create', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2043', '数据池编辑', '2040', '3', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:data:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2044', '数据池删除', '2040', '4', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:data:delete', '#', 'admin', sysdate(), '', null, '');

insert into sys_menu values('2050', 'GoView系统设置', '2000', '5', 'goview-system', null, '', '', 1, 0, 'M', '1', '0', 'goview:system:*', '#', 'admin', sysdate(), '', null, 'GoView系统设置');
insert into sys_menu values('2051', '主题切换', '2050', '1', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:system:theme', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2052', '语言切换', '2050', '2', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:system:lang', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2053', '系统配置', '2050', '3', '', '', '', '', 1, 0, 'F', '1', '0', 'goview:system:setting', '#', 'admin', sysdate(), '', null, '');
