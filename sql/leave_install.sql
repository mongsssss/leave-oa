-- =============================================================
-- 员工请假审批系统 · 业务库安装脚本
-- 生成方式：mysqldump 导出数据 + 手写建表 DDL
-- -------------------------------------------------------------
-- 用法（顺序不能乱）：
--   1) 先跑若依官方脚本  ry_20260417.sql   （建 sys_* 基础表 + 系统菜单 + admin）
--   2) 再跑定时任务脚本  quartz.sql
--   3) 最后跑本脚本      leave_install.sql （建业务表 + 导入请假业务数据）
-- 执行：mysql -u<user> -p <库名> < leave_install.sql
-- 说明：本脚本可重复执行（业务表先 DROP 再建，数据表先删同主键区间再插）
-- =============================================================

SET NAMES utf8mb4;

-- ---------- 业务表结构（手写 DDL，含修正后的字段注释） ----------
DROP TABLE IF EXISTS `leave_request`;
CREATE TABLE `leave_request` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '请假单ID',
  `user_id`        bigint       NOT NULL COMMENT '申请人的用户ID（关联sys_user）',
  `dept_id`        bigint       NOT NULL COMMENT '申请人所在部门ID（关联sys_dept，用于经理数据权限）',
  `leave_type`     varchar(20)  NOT NULL COMMENT '请假类型（对应字典 leave_request_type）',
  `start_time`     datetime     NOT NULL COMMENT '开始时间',
  `end_time`       datetime     NOT NULL COMMENT '结束时间',
  `duration`       decimal(4,1) NOT NULL COMMENT '请假时长（天），半天算0.5',
  `reason`         varchar(500) NOT NULL COMMENT '请假事由',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件OSS链接（病假条照片等）',
  `status`         varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待经理审批，MANAGER_APPROVED-待总监审批，APPROVED-已通过，REJECTED-已驳回，CANCELLED-已撤回',
  `start_half`     varchar(4)   NOT NULL DEFAULT 'AM' COMMENT '开始那天是上午(AM)还是下午(PM)',
  `end_half`       varchar(4)   NOT NULL DEFAULT 'PM' COMMENT '结束那天是上午(AM)还是下午(PM)',
  `create_time`    datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `update_time`    datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_dept_status` (`dept_id`,`status`),
  KEY `idx_leave_user_status` (`user_id`,`status`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';

DROP TABLE IF EXISTS `approval_record`;
CREATE TABLE `approval_record` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `leave_id`    bigint       NOT NULL COMMENT '关联的请假单ID',
  `approver_id` bigint       NOT NULL COMMENT '审批人用户ID',
  `action`      varchar(20)  NOT NULL COMMENT '动作（APPROVE-通过 / REJECT-驳回 / CANCEL-撤回）',
  `from_status` varchar(20)  NOT NULL COMMENT '操作前的状态',
  `to_status`   varchar(20)  NOT NULL COMMENT '操作后的状态',
  `comment`     varchar(255) DEFAULT NULL COMMENT '审批意见（比如驳回原因）',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  PRIMARY KEY (`id`),
  KEY `idx_leave_id` (`leave_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流转记录表';

-- ==== 1. dict seed ====
delete from sys_dict_data where dict_type in ('leave_request_type','leave_request_status');
delete from sys_dict_type where dict_type in ('leave_request_type','leave_request_status');
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (100,'请假类型','leave_request_type','0','admin','2026-10-30 18:28:23','',NULL,NULL);
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (101,'请假状态','leave_request_status','0','admin','2026-10-30 18:36:52','',NULL,NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (100,1,'事假','personal','leave_request_type',NULL,'primary','N','0','admin','2026-10-30 18:40:04','',NULL,NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (101,2,'病假','sick','leave_request_type',NULL,'warning','N','0','admin','2026-10-30 18:40:25','admin','2026-10-30 19:23:53',NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (102,3,'年假','annual','leave_request_type',NULL,'success','N','0','admin','2026-10-30 18:41:00','',NULL,NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (103,1,'待经理审批','PENDING','leave_request_status',NULL,'info','N','0','admin','2026-10-30 19:14:53','',NULL,NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (104,2,'待总监审核','MANAGER_APPROVED','leave_request_status',NULL,'warning','N','0','admin','2026-10-30 19:16:39','',NULL,NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (105,3,'已通过','APPROVED','leave_request_status',NULL,'success','N','0','admin','2026-10-30 19:17:14','',NULL,NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (106,4,'已驳回','REJECTED','leave_request_status',NULL,'danger','N','0','admin','2026-10-30 19:17:44','',NULL,NULL);
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (107,5,'已撤回','CANCELLED','leave_request_status',NULL,'default','N','0','admin','2026-10-30 19:18:12','',NULL,NULL);

-- ==== 2. demo depts ====
delete from sys_dept where dept_id >= 200;
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (200,100,'0,100','技术部',1,'王啵啵','18823453456','1728398765@qq.com','0','0','admin','2026-10-30 17:10:57','',NULL);
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (201,100,'0,100','人事部',4,'尚啵啵','14278784646','1234229898@qq.com','0','0','admin','2026-10-30 17:12:13','',NULL);

-- ==== 3. menus & buttons ====
delete from sys_menu where menu_id between 2000 and 2099;
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2001,'请假单详情',2021,3,'#','',NULL,'',1,0,'F','0','0','leave:leaveRequest:query','#','admin','2026-10-30 15:58:03','',NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2002,'申请请假',2021,1,'#','',NULL,'',1,0,'F','0','0','leave:leaveRequest:add','#','admin','2026-10-30 15:58:03','admin','2026-11-05 08:50:42','');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2005,'台账导出',2023,2,'#','',NULL,'',1,0,'F','0','0','leave:leaveRequest:export','#','admin','2026-10-30 15:58:03','',NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2011,'流水导出',2025,1,'#','',NULL,'',1,0,'F','0','0','leave:record:export','#','admin','2026-10-30 15:58:55','',NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2012,'请假撤回',2021,2,'',NULL,NULL,'',1,0,'F','0','0','leave:leaveRequest:cancel','#','admin','2026-11-02 21:26:33','',NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2013,'审批通过',2022,1,'',NULL,NULL,'',1,0,'F','0','0','leave:leaveRequest:approve','checkbox','admin','2026-11-05 08:53:03','admin','2026-11-08 12:32:51','');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2014,'审批驳回',2022,2,'',NULL,NULL,'',1,0,'F','0','0','leave:leaveRequest:reject','#','admin','2026-11-05 19:50:49','admin','2026-11-08 12:33:06','');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2020,'请假管理',0,4,'leave',NULL,'','',1,0,'M','0','0','','form','admin','2026-11-10 15:24:20','',NULL,'请假业务目录');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2021,'我的请假',2020,1,'my','leave/leaveRequest/my','','',1,0,'C','0','0','leave:leaveRequest:myList','#','admin','2026-11-10 15:24:20','',NULL,'只查当前登录用户自己提交的单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2022,'待我审批',2020,2,'pending','leave/leaveRequest/pending','','',1,0,'C','0','0','leave:leaveRequest:pendingApproval','#','admin','2026-11-10 15:24:20','',NULL,'轮到我处理的一级/二级审批单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2023,'请假台账',2020,3,'ledger','leave/leaveRequest/ledger','','',1,0,'C','0','0','leave:leaveRequest:list','#','admin','2026-11-10 15:24:20','',NULL,'数据范围由 sys_role.data_scope 决定');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2024,'我已审批',2020,4,'approved','leave/record/approved','','',1,0,'C','0','0','leave:record:myList','#','admin','2026-11-10 15:24:20','',NULL,'只含 APPROVE / REJECT，不含撤回');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2025,'审批流水',2020,5,'flow','leave/record/flow','','',1,0,'C','0','0','leave:record:list','#','admin','2026-11-10 15:24:20','',NULL,'全公司状态变更，含撤回');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2026,'请假单详情',2023,1,'#','','','',1,0,'F','0','0','leave:leaveRequest:query','#','admin','2026-11-10 15:24:20','',NULL,'与 2001 同一个权限标识，挂在台账下给总监用');

-- ==== 4. roles & menu grants ====
delete from sys_role_menu where role_id in (100,101,102);
delete from sys_role where role_id in (100,101,102);
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (100,'部门经理','manager',3,'3',1,1,'0','0','admin','2026-10-30 17:15:02','admin','2026-11-08 12:31:37',NULL);
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (101,'普通员工','employee',4,'5',1,1,'0','0','admin','2026-10-30 17:15:27','admin','2026-11-08 12:34:18',NULL);
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (102,'总监','director',5,'1',1,1,'0','0','admin','2026-10-30 20:18:49','admin','2026-11-08 12:33:52',NULL);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2012);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2014);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2023);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (100,2026);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (101,2001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (101,2002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (101,2012);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (101,2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (101,2021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2011);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2014);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2023);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (102,2026);

-- ==== 5. demo users ====
delete from sys_user_role where user_id >= 100;
delete from sys_user where user_id >= 100;
INSERT INTO `sys_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `pwd_update_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (100,200,'zhangsan','张三','00','1324345566@qq.com','14378785757','0','','$2a$10$e5cC.s5.qKGlgkc/MoiNO.lhlJSE.R0itK2c94wRVJzxl2Ye97j1K','0','0','127.0.0.1','2026-11-12 18:54:34',NULL,'admin','2026-10-30 17:17:22','',NULL,NULL);
INSERT INTO `sys_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `pwd_update_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (101,200,'lisi','李四','00','','','0','','$2a$10$XWXE7vpVS7RT9OdyMIeGte4mDNO3DdW6fJ6QzwKHE57nDl80mv2/C','0','0','127.0.0.1','2026-11-12 18:55:06',NULL,'admin','2026-10-30 17:17:50','',NULL,NULL);
INSERT INTO `sys_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `pwd_update_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (102,201,'wangwu','王五','00','','','0','','$2a$10$5iHOS1NH/DjaXmIcmVvhxelKbew6XBaE0upQ2NGoZhhHDi9SiAuyi','0','0','127.0.0.1','2026-11-10 15:54:11',NULL,'admin','2026-11-04 20:57:43','',NULL,NULL);
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (100,101);
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (101,100);
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (102,102);

-- ==== 6. demo leave data ====
delete from approval_record;
delete from leave_request;
INSERT INTO `leave_request` (`id`, `user_id`, `dept_id`, `leave_type`, `start_time`, `end_time`, `duration`, `reason`, `attachment_url`, `status`, `create_time`, `update_time`, `start_half`, `end_half`) VALUES (3,100,200,'personal','2026-11-06 00:00:00','2026-11-08 00:00:00',1.0,'家中有事，处理完即销假',NULL,'CANCELLED','2026-11-05 08:34:53','2026-11-12 22:09:17','AM','PM');
INSERT INTO `leave_request` (`id`, `user_id`, `dept_id`, `leave_type`, `start_time`, `end_time`, `duration`, `reason`, `attachment_url`, `status`, `create_time`, `update_time`, `start_half`, `end_half`) VALUES (4,100,200,'personal','2026-11-12 00:00:00','2026-11-21 00:00:00',7.0,'老家建房需长期跟进，附证明材料待补',NULL,'REJECTED','2026-11-05 19:54:37','2026-11-12 22:09:17','AM','PM');
INSERT INTO `leave_request` (`id`, `user_id`, `dept_id`, `leave_type`, `start_time`, `end_time`, `duration`, `reason`, `attachment_url`, `status`, `create_time`, `update_time`, `start_half`, `end_half`) VALUES (6,100,200,'sick','2026-11-13 00:00:00','2026-11-14 00:00:00',1.0,'连续低烧，需就医观察两天','leave/2026/11/06/f541fc14-810d-4000-8541-d4dce49024ef.jpg','PENDING','2026-11-06 14:10:32','2026-11-12 22:09:17','AM','PM');
INSERT INTO `leave_request` (`id`, `user_id`, `dept_id`, `leave_type`, `start_time`, `end_time`, `duration`, `reason`, `attachment_url`, `status`, `create_time`, `update_time`, `start_half`, `end_half`) VALUES (7,100,200,'sick','2026-11-08 00:00:00','2026-11-09 00:00:00',1.0,'复查肠胃，病假条见附件','leave/2026/11/06/b91c5fd117054d3d9921d717e687970c.jpg','PENDING','2026-11-06 14:32:10','2026-11-12 22:09:17','AM','PM');
INSERT INTO `leave_request` (`id`, `user_id`, `dept_id`, `leave_type`, `start_time`, `end_time`, `duration`, `reason`, `attachment_url`, `status`, `create_time`, `update_time`, `start_half`, `end_half`) VALUES (8,100,200,'annual','2026-11-09 00:00:00','2026-11-10 00:00:00',2.0,'年假带家人短途出行','leave/2026/11/07/023251f41f494ebcbaf7bf4e60798aa7.jpg','APPROVED','2026-11-07 15:35:07','2026-11-12 22:09:17','AM','PM');
INSERT INTO `leave_request` (`id`, `user_id`, `dept_id`, `leave_type`, `start_time`, `end_time`, `duration`, `reason`, `attachment_url`, `status`, `create_time`, `update_time`, `start_half`, `end_half`) VALUES (9,100,200,'annual','2026-11-26 00:00:00','2026-11-27 00:00:00',2.0,'年假陪同家人复查','leave/2026/11/08/34fb0ae32ed5481dbc7c2b4c0d119db9.jpg','REJECTED','2026-11-08 12:35:41','2026-11-12 22:09:17','AM','PM');
INSERT INTO `leave_request` (`id`, `user_id`, `dept_id`, `leave_type`, `start_time`, `end_time`, `duration`, `reason`, `attachment_url`, `status`, `create_time`, `update_time`, `start_half`, `end_half`) VALUES (11,100,200,'sick','2026-11-11 00:00:00','2026-11-11 00:00:00',1.0,'肠胃炎发作，居家休息一天','leave/2026/11/10/39959afa450e4e69b36185a70d9223a3.jpg','APPROVED','2026-11-10 15:47:22','2026-11-12 22:09:17','AM','PM');
INSERT INTO `leave_request` (`id`, `user_id`, `dept_id`, `leave_type`, `start_time`, `end_time`, `duration`, `reason`, `attachment_url`, `status`, `create_time`, `update_time`, `start_half`, `end_half`) VALUES (12,100,200,'personal','2026-11-12 00:00:00','2026-11-13 00:00:00',1.5,'家中水管爆裂，需上午处理','leave/2026/11/11/6e2b5c2e12d340408f2d778f90ccb39b.jpg,leave/2026/11/11/ebf6240cd49c46e089f89d4cb58570c2.jpg','MANAGER_APPROVED','2026-11-11 17:09:59','2026-11-12 22:09:17','AM','AM');
INSERT INTO `approval_record` (`id`, `leave_id`, `approver_id`, `action`, `from_status`, `to_status`, `comment`, `create_time`) VALUES (2,3,100,'CANCEL','MANAGER_APPROVED','CANCELLED',NULL,'2026-11-05 08:37:21');
INSERT INTO `approval_record` (`id`, `leave_id`, `approver_id`, `action`, `from_status`, `to_status`, `comment`, `create_time`) VALUES (3,4,101,'APPROVE','PENDING','MANAGER_APPROVED','行！','2026-11-05 20:10:03');
INSERT INTO `approval_record` (`id`, `leave_id`, `approver_id`, `action`, `from_status`, `to_status`, `comment`, `create_time`) VALUES (4,4,102,'REJECT','MANAGER_APPROVED','REJECTED','事由不够具体，请补充后重新提交','2026-11-05 20:10:34');
INSERT INTO `approval_record` (`id`, `leave_id`, `approver_id`, `action`, `from_status`, `to_status`, `comment`, `create_time`) VALUES (5,8,102,'APPROVE','MANAGER_APPROVED','APPROVED',NULL,'2026-11-07 15:35:49');
INSERT INTO `approval_record` (`id`, `leave_id`, `approver_id`, `action`, `from_status`, `to_status`, `comment`, `create_time`) VALUES (8,9,102,'REJECT','MANAGER_APPROVED','REJECTED','出行安排与团队排期冲突，驳回','2026-11-10 15:54:35');
INSERT INTO `approval_record` (`id`, `leave_id`, `approver_id`, `action`, `from_status`, `to_status`, `comment`, `create_time`) VALUES (9,8,101,'APPROVE','PENDING','MANAGER_APPROVED','情况属实，同意','2026-11-07 15:35:20');
INSERT INTO `approval_record` (`id`, `leave_id`, `approver_id`, `action`, `from_status`, `to_status`, `comment`, `create_time`) VALUES (10,9,101,'APPROVE','PENDING','MANAGER_APPROVED','同意，注意安排交接','2026-11-08 12:36:00');
INSERT INTO `approval_record` (`id`, `leave_id`, `approver_id`, `action`, `from_status`, `to_status`, `comment`, `create_time`) VALUES (11,12,101,'APPROVE','PENDING','MANAGER_APPROVED','处理半天够了，同意','2026-11-11 17:10:05');
INSERT INTO `approval_record` (`id`, `leave_id`, `approver_id`, `action`, `from_status`, `to_status`, `comment`, `create_time`) VALUES (12,11,101,'APPROVE','PENDING','APPROVED','感冒注意多休息','2026-11-10 15:53:16');

-- install script end
