import request from '@/utils/request'

// 审批流水：全公司所有状态变更，含撤回。
// 需要 leave:record:list，后端 Service 里还有 hasRole("director"|"admin") 兜底
export function listRecord(query) {
  return request({
    url: '/leave/record/list',
    method: 'get',
    params: query
  })
}

// 我已审批：后端强制 approverId = 当前登录用户，并给 SQL 打 approvalOnly 标记，
// 所以只会返回 APPROVE / REJECT 两种动作——撤回不算审批，那是申请人自己的动作
export function listMyRecord(query) {
  return request({
    url: '/leave/record/myList',
    method: 'get',
    params: query
  })
}

// 这个文件只剩两个查询函数 + 导出，其余生成器产出的接口全部删除，
// 因为后端 ApprovalRecordController 里那四个也已经注释掉了：
//   getRecord    → GET    /leave/record/{id}   （leave:record:query）
//   addRecord    → POST   /leave/record        （leave:record:add）
//   updateRecord → PUT    /leave/record        （leave:record:edit）
//   delRecord    → DELETE /leave/record/{ids}  （leave:record:remove）
//
// 理由是同一句话：approval_record 的每一行都必须是 approve / reject / cancel
// 三个业务动作在事务里留下的痕迹（见 LeaveRequestServiceImpl#insertRecord）。
// 手工补一条、改一条、删一条，等于让"事后对账"失去依据。
// 想看一张单的完整流转轨迹，去请假申请详情的时间线（那里是 join 出来的），
// 不需要单独按流水ID查一条记录，所以 query 也一并删了。
