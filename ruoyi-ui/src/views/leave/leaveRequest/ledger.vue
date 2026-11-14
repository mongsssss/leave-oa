<!--
  菜单「请假台账」的入口，view-mode="all" → 调 /leave/leaveRequest/list。

  为什么不叫「全部请假」：因为它不是全部。
  同一个权限标识 leave:leaveRequest:list 三个角色都有，它只管"这个接口谁能调"；
  调通之后能拿到哪几行，由 sys_role.data_scope 决定
  （LeaveRequestServiceImpl#selectLeaveRequestList 上有 @DataScope(deptAlias="l", userAlias="l")，
   切面会在 SQL 末尾拼上 ${params.dataScope}）：
      经理 = 本部门(3)      总监 = 全公司(1)      员工 = 仅本人(5)
  员工拿到的结果和「我的请假」一模一样，所以这个菜单不授权给员工，
  不是靠前端 if 隐藏——原来那种 !$auth.hasRole('employee') 的写法还有个坑：
  plugins/auth.js 对 admin 恒返回 true，会把 admin 自己也一并挡掉。

  这一页是三个角色数据范围不一样的典型例子，简历上讲"权限标识 ≠ 数据权限"就用它。
-->
<template>
  <leave-table view-mode="all" />
</template>

<script>
import LeaveTable from './LeaveTable'

export default {
  name: "LeaveLedger",
  components: { LeaveTable }
}
</script>
