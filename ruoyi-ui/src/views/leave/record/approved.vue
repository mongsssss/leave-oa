<!--
  菜单「我已审批」的入口，view-mode="my" → 调 /leave/record/myList。

  后端 ApprovalRecordServiceImpl#selectMyApprovalRecordList 做两件事：
    1) 强制 approverId = 当前登录用户（前端传什么都不算）
    2) 给 params 打 approvalOnly 标记，SQL 里过滤成 action in ('APPROVE','REJECT')

  所以这一页看不到"撤回"是设计如此：撤回那一刻的操作人是申请人自己，
  它属于「我的请假」视角，那边还能看到请假日期、事由、附件。

  为什么一个菜单要单独一个文件：若依的 sys_menu.component 必须指向具体 vue 文件；
  而且几个菜单若指向同一组件，Vue Router 会判定"组件没变"而复用实例，切菜单不重新拉数据。
  逻辑仍然只有 RecordTable.vue 一份。
-->
<template>
  <record-table view-mode="my" />
</template>

<script>
import RecordTable from './RecordTable'

export default {
  name: "MyApprovedRecord",
  components: { RecordTable }
}
</script>
