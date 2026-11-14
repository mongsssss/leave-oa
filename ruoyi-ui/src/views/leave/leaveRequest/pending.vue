<!--
  菜单「待我审批」的入口，view-mode="pending" → 调 /leave/leaveRequest/pendingApproval。

  谁能看到什么由 LeaveRequestServiceImpl#selectPendingApprovalList 决定：
    经理 → 本部门的 PENDING（一级审批）
    总监 → 所有 MANAGER_APPROVED（二级审批）
    既是经理又是总监 → both
    admin  → 全部待审批（这条在 Service 里由 SecurityUtils.isAdmin() 单独分流）
  批完一张单子它就从这个列表消失，历史记录去「我已审批」看。

  员工没有 leave:leaveRequest:pendingApproval 权限，sys_menu 里也不会授权给他，
  所以这个菜单对他根本不出现——不需要在页面里再写一遍 if 判断。
-->
<template>
  <leave-table view-mode="pending" />
</template>

<script>
import LeaveTable from './LeaveTable'

export default {
  name: "PendingApproval",
  components: { LeaveTable }
}
</script>
