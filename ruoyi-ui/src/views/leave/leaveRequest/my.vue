<!--
  菜单「我的请假」的入口。

  这个文件只干一件事：把 view-mode="my" 传给 LeaveTable，让它去调 /leave/leaveRequest/my。
  LeaveRequestServiceImpl#selectMyLeaveRequestList 里强制 userId = 当前登录用户，
  所以这一页永远只看得到自己的单子，跟角色的 data_scope 无关。

  为什么一个菜单要单独一个文件：
  若依的 sys_menu.component 必须指向一个具体 vue 文件；而且如果几个菜单指向同一个组件，
  Vue Router 会认为"组件没变"而复用同一个实例，切菜单不会重新拉数据。
  拆成薄壳之后各自 name 不同、实例独立，逻辑仍然只有 LeaveTable.vue 一份。
-->
<template>
  <leave-table view-mode="my" />
</template>

<script>
import LeaveTable from './LeaveTable'

export default {
  name: "MyLeave",
  components: { LeaveTable }
}
</script>
