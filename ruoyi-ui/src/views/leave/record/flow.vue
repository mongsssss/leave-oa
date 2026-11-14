<!--
  菜单「审批流水」的入口，view-mode="all" → 调 /leave/record/list。

  全公司所有状态变更，含撤回，是事后稽查用的视图。
  权限两道：
    菜单/接口权限 leave:record:list（sys_role_menu 决定菜单出不出现在侧边栏）
    Service 里还有 hasRole("director") || hasRole("admin") 兜底
  经理没有这个菜单，所以他进来只看到「我已审批」——这不是页面坏了，是他不该看到别的部门的流水。

  这张表在前端没有任何写入口，后端 ApprovalRecordController 的
  getInfo / add / edit / remove 也已注释掉：每一行都必须是业务动作在事务里留下的痕迹。
-->
<template>
  <record-table view-mode="all" />
</template>

<script>
import RecordTable from './RecordTable'

export default {
  name: "ApprovalFlow",
  components: { RecordTable }
}
</script>
