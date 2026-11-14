<!--
  请假单详情弹窗（只读）：单据信息 + 审批轨迹时间线。

  被两个表格共用，所以抽成组件而不是在任一家里复制一份：
    LeaveTable.vue  → 我的请假 / 待我审批 / 请假台账 三处的【查看】
    RecordTable.vue → 我已审批 / 审批流水 的【查看】（打开该行流水指向的请假单）
  详情 UI 将来必然还会改（加字段、改排版），两处副本迟早不同步——
  半天字段这次就是每个显示位置都要补一遍，复制一份等于给未来埋雷。

  用法：父组件 <leave-detail-dialog ref="detailDialog" /> + this.$refs.detailDialog.open(id)。
  数据加载也在这里面完成：父组件只负责给一个请假单 id。
  权限判断完全交给后端 selectLeaveRequestById（谁能看：本人 / admin / 现任审批人 / 经手过的流水人），
  被拒时 request.js 会弹后端那句"您无权查看该请假申请"，弹窗自动关闭——
  前端不复刻权限规则，理由和列表页"不拦越权点击"是同一个口径。
-->
<template>
  <el-dialog title="请假申请详情" :visible.sync="visible" width="760px" append-to-body>
    <el-form label-width="100px" size="small" v-loading="loading">
      <el-row>
        <el-col :span="12">
          <el-form-item label="请假单ID"><el-input :value="detail.id || ''" disabled /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="申请人"><el-input :value="detail.userName" disabled /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属部门"><el-input :value="detail.deptName" disabled /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="请假类型"><dict-tag :options="dict.type.leave_request_type" :value="detail.leaveType" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="开始时间"><el-input :value="parseTime(detail.startTime, '{y}-{m}-{d}') + halfLabel(detail.startHalf)" disabled /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束时间"><el-input :value="parseTime(detail.endTime, '{y}-{m}-{d}') + halfLabel(detail.endHalf)" disabled /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="请假时长"><el-input :value="detail.duration != null ? detail.duration : ''" disabled /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="当前状态"><dict-tag :options="dict.type.leave_request_status" :value="detail.status" /></el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="请假事由"><el-input :value="detail.reason" type="textarea" :rows="2" disabled /></el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="附件">
            <template v-for="(key, i) in $attachment.list(detail.attachmentUrl)">
              <el-image
                v-if="$attachment.isImage(key)"
                :key="'img' + i"
                :src="$attachment.url(key)"
                :preview-src-list="$attachment.urlList(detail.attachmentUrl)"
                style="width: 100px; height: 75px; margin-right: 10px"
                fit="cover"
              />
              <el-link
                v-else
                :key="'link' + i"
                type="primary"
                :href="$attachment.url(key)"
                target="_blank"
                style="margin-right: 14px"
              >附件{{ i + 1 }}</el-link>
            </template>
            <span v-if="!$attachment.count(detail.attachmentUrl)">无</span>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="left">审批轨迹</el-divider>

    <el-timeline>
      <!-- 第一条：提交。提交动作没有写进 approval_record（那张表只记状态变更），
           所以用请假单自己的 createTime + userName 拼出来 -->
      <el-timeline-item
        :timestamp="parseTime(detail.createTime, '{y}-{m}-{d} {h}:{i}')"
        placement="top"
        color="#409EFF"
      >
        <b>{{ detail.userName }}</b> 提交请假申请
      </el-timeline-item>
      <el-timeline-item
        v-for="rec in (detail.approvalRecordsList || [])"
        :key="rec.id"
        :timestamp="parseTime(rec.createTime, '{y}-{m}-{d} {h}:{i}')"
        placement="top"
        :color="actionColor(rec.action)"
      >
        <b>{{ rec.approverName || ('用户' + rec.approverId) }}</b> {{ actionLabel(rec.action) }}
        <span v-if="rec.comment" style="color: #909399">「{{ rec.comment }}」</span>
      </el-timeline-item>
    </el-timeline>

    <el-alert
      v-if="!loading && (!detail.approvalRecordsList || detail.approvalRecordsList.length === 0)"
      title="该申请暂无审批动作"
      type="info"
      :closable="false"
      show-icon
    />

    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">关 闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getLeaveRequest } from "@/api/leave/leaveRequest"

export default {
  name: "LeaveDetailDialog",
  dicts: ['leave_request_status', 'leave_request_type'],
  data() {
    return {
      // 弹窗显隐
      visible: false,
      // 详情接口加载中（表格区域转圈）
      loading: false,
      // 后端返回的请假单（含 approvalRecordsList 审批流水数组）
      detail: {}
    }
  },
  methods: {
    /** 父组件唯一入口：给请假单 id，自己拉数据自己开弹窗 */
    open(id) {
      this.detail = {}
      this.loading = true
      this.visible = true
      getLeaveRequest(id).then(response => {
        this.detail = response.data || {}
        this.loading = false
      }).catch(() => {
        // 后端拒绝（无权限 / 单不存在）时 request.js 已弹错，
        // 这里关掉弹窗，不给用户留一个半空数据的壳
        this.visible = false
        this.loading = false
      })
    },
    /** 半天的中文后缀，拼在日期后面显示（带前导空格），空值兜底显示纯日期 */
    halfLabel(half) {
      const map = { AM: ' 上午', PM: ' 下午' }
      return map[half] || ''
    },
    /** 动作编码转中文，用于时间线 */
    actionLabel(action) {
      const map = { APPROVE: "审批通过", REJECT: "驳回", CANCEL: "撤回" }
      return map[action] || action
    },
    /** 时间线圆点颜色，成功绿、驳回红、撤回橙 */
    actionColor(action) {
      const map = { APPROVE: "#67C23A", REJECT: "#F56C6C", CANCEL: "#E6A23C" }
      return map[action] || "#409EFF"
    }
  }
}
</script>
