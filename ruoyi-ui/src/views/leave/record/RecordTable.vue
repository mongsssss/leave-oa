<!--
  审批流水表的公共实现，被两个菜单共用：
    views/leave/record/approved.vue  → view-mode="my"   我已审批
    views/leave/record/flow.vue      → view-mode="all"  审批流水

  两个视图看的是同一张 approval_record，差别只在查询口径：
    /leave/record/list     全公司所有状态变更，含撤回
    /leave/record/myList   后端 ApprovalRecordServiceImpl 强制 approverId = 当前用户，
                           并给 SQL 打上 approvalOnly 标记，过滤成 action in ('APPROVE','REJECT')

  「我已审批」里看不到撤回是设计如此，不是丢了：撤回那一刻的操作人就是申请人自己，
  它属于「我的请假」视角，去那边看还能看到请假日期、事由和附件。
-->
<template>
  <div class="app-container">
    <el-alert
      :title="pageHint"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <!-- 只放后端 selectApprovalRecordList 真正支持的筛选条件。
           原先这里让手填「关联的请假单ID」「审批人用户ID」，
           一是没人知道自己要填哪个数字，二是「我已审批」下后端会强制覆盖 approverId，填了也没用 -->
      <el-form-item label="动作" prop="action">
        <!-- 用 visibleActionOptions 而不是 actionOptions：
             「我已审批」后端已经过滤掉 CANCEL，此时让人选"撤回"只会得到一张空表，
             而且他分不清是"我没撤回过"还是"筛选坏了" -->
        <el-select v-model="queryParams.action" placeholder="请选择动作" clearable>
          <el-option
            v-for="item in visibleActionOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审批意见" prop="comment">
        <el-input
          v-model="queryParams.comment"
          placeholder="请输入审批意见"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <!-- 这里没有新增/修改/删除按钮，而且后端那四个接口也已经注释掉了
           （ApprovalRecordController 的 getInfo / add / edit / remove）。
           理由是一句话：approval_record 的每一行都必须是 approve / reject / cancel
           三个业务动作在事务里留下的痕迹，手工补一条、改一条、删一条，
           等于让这张表失去"事后对账"的价值。 -->
      <el-col :span="1.5">
        <!-- 后端只有 /leave/record/export 一个导出接口，它走的是不带 approvalOnly 的那条 SQL，
             导出来永远是全公司流水。所以在「我已审批」里点导出，拿到的会和屏幕上不是一张表，
             干脆只在「审批流水」显示这个按钮。
             要真做出「导出我批过的」，得给后端加一个 /record/exportMy 复用 myList 那个 Service 方法 -->
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['leave:record:export']"
          v-if="viewMode === 'all'"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="recordList">
      <el-table-column label="记录ID" align="center" prop="id" width="80" />
      <el-table-column label="请假单ID" align="center" prop="leaveId" width="90" />
      <el-table-column label="操作人" align="center" prop="approverName" width="110">
        <template slot-scope="scope">
          <!-- 这一列刻意不叫「审批人」：数据库列名是 approver_id，但 LeaveRequestServiceImpl#insertRecord
               统一用 SecurityUtils.getUserId() 填充，撤回（CANCEL）那一刻的当前用户是申请人本人，
               所以它的真实语义是"谁做了这个动作"，叫审批人会在撤回那行骗人。
               approverName 是后端 join sys_user.nick_name 得到的；
               万一这个用户被删了 join 不上，兜底显示「用户+ID」而不是留白 -->
          <span>{{ scope.row.approverName || ('用户' + scope.row.approverId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="动作" align="center" prop="action" width="100">
        <template slot-scope="scope">
          <el-tag :type="actionTagType(scope.row.action)" size="small">{{ actionLabel(scope.row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态变化" align="center" width="260">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.leave_request_status" :value="scope.row.fromStatus" />
          <i class="el-icon-right" style="margin: 0 6px; color: #909399"></i>
          <dict-tag :options="dict.type.leave_request_status" :value="scope.row.toStatus" />
        </template>
      </el-table-column>
      <el-table-column label="审批意见" align="center" prop="comment" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          <span>{{ scope.row.comment || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="90" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <!-- 查看该流水指向的请假单详情。和 LeaveTable 同一条规则：
               前端不做权限显隐，能不能打开由后端 selectLeaveRequestById 说了算——
               「我已审批」的单子必然在流水里有我，正好命中它的"经手人放行"分支；
               点了不该点的，弹"您无权查看该请假申请"，比按钮消失更好排查 -->
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
          >查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 请假单详情弹窗：与「我的请假」等三个视图共用同一个组件 -->
    <leave-detail-dialog ref="detailDialog" />
  </div>
</template>

<script>
import { listRecord, listMyRecord } from "@/api/leave/record"
import LeaveDetailDialog from "@/views/leave/components/LeaveDetailDialog"

export default {
  name: "RecordTable",
  components: { LeaveDetailDialog },
  dicts: ['leave_request_status'],
  props: {
    // 由薄壳菜单传进来：my=我已审批（走 /myList）  all=审批流水（走 /list）
    viewMode: {
      type: String,
      required: true,
      validator: value => ['my', 'all'].indexOf(value) !== -1
    }
  },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 审批流水表格数据
      recordList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        action: null,
        comment: null,
      },
      // 动作下拉：后端 ApprovalAction 枚举一共就三个值，直接写死，不必配字典
      actionOptions: [
        { value: "APPROVE", label: "审批通过" },
        { value: "REJECT", label: "驳回" },
        { value: "CANCEL", label: "撤回" }
      ]
    }
  },
  computed: {
    pageHint() {
      const hints = {
        my: "这里只列出你本人做过的审批动作（通过 / 驳回）。撤回不算审批——那是申请人自己的动作，去「我的请假」看。",
        all: "审批流水：全公司所有状态变更，一条记录代表有人对某张单子做了一次动作，含撤回。这张表只能由系统写入。"
      }
      return hints[this.viewMode] || ""
    },
    // 「我已审批」那条 SQL 被后端打了 approvalOnly 标记，只会返回 APPROVE / REJECT，
    // 所以这个视图下把"撤回"选项拿掉，免得让人误以为筛选坏了
    visibleActionOptions() {
      if (this.viewMode === 'my') {
        return this.actionOptions.filter(item => item.value !== 'CANCEL')
      }
      return this.actionOptions
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询列表：按视图走不同接口，数据范围由后端各自决定 */
    getList() {
      this.loading = true
      const api = this.viewMode === 'my' ? listMyRecord : listRecord
      api(this.queryParams).then(response => {
        this.recordList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        // 被后端拒绝时也要关掉 loading，否则表格一直转圈
        this.recordList = []
        this.total = 0
        this.loading = false
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    /** 查看流水指向的请假单：把流水行的 leaveId 交给公共详情弹窗，
        它自己调 /leave/leaveRequest/{id}，权限被后端拒绝时弹窗不显示、只弹错误提示 */
    handleView(row) {
      this.$refs.detailDialog.open(row.leaveId)
    },
    /** 动作编码转中文 */
    actionLabel(action) {
      const map = { APPROVE: "审批通过", REJECT: "驳回", CANCEL: "撤回" }
      return map[action] || action
    },
    /** 动作对应的 el-tag 颜色 */
    actionTagType(action) {
      const map = { APPROVE: "success", REJECT: "danger", CANCEL: "warning" }
      return map[action] || "info"
    },
    /** 导出按钮操作：只在「审批流水」出现，理由见模板里那段注释 */
    handleExport() {
      this.download('leave/record/export', {
        ...this.queryParams
      }, `record_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
