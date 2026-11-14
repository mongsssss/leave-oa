import request from '@/utils/request'

// 查询请假申请列表
export function listLeaveRequest(query) {
  return request({
    url: '/leave/leaveRequest/list',
    method: 'get',
    params: query
  })
}

// 查询请假申请详细
export function getLeaveRequest(id) {
  return request({
    url: '/leave/leaveRequest/' + id,
    method: 'get'
  })
}

// 新增请假申请
export function addLeaveRequest(data) {
  return request({
    url: '/leave/leaveRequest',
    method: 'post',
    data: data
  })
}

// 这里没有 delLeaveRequest：后端 LeaveRequestController 的 remove 接口已注释掉。
// 请假单是业务凭证，也是审批流水的父亲——DELETE /leave/leaveRequest/{ids} 只删
// leave_request 而不会去清理 approval_record，删完流水表里就剩一堆指向空单号的幽灵记录，
// 审批流水页会显示"请假单ID=8"但点进去查无此单。不想要一张单子，走【撤回】就好。

// 查询我的请假申请列表（后端强制按当前登录用户过滤，不受数据权限影响）
export function listMyLeaveRequest(query) {
  return request({
    url: '/leave/leaveRequest/my',
    method: 'get',
    params: query
  })
}

// 查询待我审批的请假申请列表（按当前用户角色返回不同的范围）
export function listPendingApproval(query) {
  return request({
    url: '/leave/leaveRequest/pendingApproval',
    method: 'get',
    params: query
  })
}

// 撤回请假申请
export function cancelLeaveRequest(id) {
  return request({
    url: '/leave/leaveRequest/cancel/' + id,
    method: 'put'
  })
}

// 审批通过。comment 是后端用 @RequestParam 接的，必须放 params（拼在URL问号后面），不能放 data
export function approveLeaveRequest(id, comment) {
  return request({
    url: '/leave/leaveRequest/approve/' + id,
    method: 'put',
    params: { comment: comment }
  })
}

// 驳回请假申请。comment 后端强制必填
export function rejectLeaveRequest(id, comment) {
  return request({
    url: '/leave/leaveRequest/reject/' + id,
    method: 'put',
    params: { comment: comment }
  })
}
