<template>
  <div class="app-container">
    <!-- 本组件是「我的请假 / 待我审批 / 请假台账」三个菜单的唯一实现。
         三个菜单各有一个薄壳 vue 文件（my.vue / pending.vue / ledger.vue），
         只负责把 view-mode 传进来——若依的菜单 component 必须指向一个具体文件，
         而且三个菜单如果用同一个组件，Vue Router 会因为组件相同复用实例、切菜单不重新拉数据，
         所以拆成三个薄壳，逻辑仍集中在这一份里。

         viewMode 是 props 不是 data：由菜单决定，页面内部不再自己判断"我该显示哪个页签"。
         原来那套 el-tabs + $auth.hasPermi 显隐判断已经整个删掉了——
         能不能看某个视图，交给菜单权限（sys_role_menu）决定，这才是若依本来的机制。 -->
    <el-alert
      :title="pageHint"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <!-- 只放后端 selectLeaveRequestList / selectMyLeaveRequestList / selectPendingApprovalList
           三条 SQL 都认的字段。原先还留了 userId、deptId 输入框（让手填用户ID、部门ID），
           一是没人知道自己要填哪个数字，二是这三个视图的数据范围本来就由后端锁死 -->
      <el-form-item label="请假类型" prop="leaveType">
        <el-select v-model="queryParams.leaveType" placeholder="请选择请假类型" clearable>
          <el-option
            v-for="dict in dict.type.leave_request_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker clearable
                        v-model="queryParams.startTime"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择开始时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker clearable
                        v-model="queryParams.endTime"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择结束时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="请假时长" prop="duration">
        <el-input
          v-model="queryParams.duration"
          placeholder="请输入请假时长"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="审批状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择审批状态" clearable>
          <el-option
            v-for="dict in dict.type.leave_request_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <!-- 提交请假只在「我的请假」里给。
             待我审批、请假台账是别人的单子，在那里点"新增"会做出一个
             "我在看张三的台账，提交出来却是我自己请假"的动作，容易误解 -->
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['leave:leaveRequest:add']"
          v-if="viewMode === 'my'"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <!-- 这里没有删除按钮：后端 LeaveRequestController 的 remove 接口已注释掉。
             请假单是业务凭证，而且 DELETE 只删 leave_request、不会清 approval_record，
             删完审批流水里就剩一堆指向空单号的幽灵记录。不要的单子走【撤回】。
             连带地把表格的多选列（type="selection"）、ids/single/multiple 和
             handleSelectionChange 一起删了——它们只为批量删除存在，留着是死代码 -->
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['leave:leaveRequest:export']"
          v-if="viewMode === 'all'"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="leaveRequestList">
      <el-table-column label="请假单ID" align="center" prop="id" width="80" />
      <!-- 申请人和部门在「我的请假」里永远是自己，看起来冗余，
           但保留一列比给三个视图写两套列定义划算，而且能立刻发现"数据越权"这类问题 -->
      <el-table-column label="申请人" align="center" prop="userName" />
      <el-table-column label="所属部门" align="center" prop="deptName" />
      <el-table-column label="请假类型" align="center" prop="leaveType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.leave_request_type" :value="scope.row.leaveType"/>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="150">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}{{ halfLabel(scope.row.startHalf) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="150">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}{{ halfLabel(scope.row.endHalf) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="请假时长" align="center" prop="duration" />
      <el-table-column label="请假事由" align="center" prop="reason" />
      <el-table-column label="附件" align="center" prop="attachmentUrl" width="90">
        <template slot-scope="scope">
          <el-image
            v-if="$attachment.isFirstImage(scope.row.attachmentUrl)"
            :src="$attachment.firstUrl(scope.row.attachmentUrl)"
            :preview-src-list="$attachment.urlList(scope.row.attachmentUrl)"
            style="width: 40px; height: 40px"
            fit="cover"
          />
          <el-link
            v-else-if="scope.row.attachmentUrl"
            type="primary"
            :href="$attachment.firstUrl(scope.row.attachmentUrl)"
            target="_blank"
          >{{ $attachment.count(scope.row.attachmentUrl) > 1 ? '附件(' + $attachment.count(scope.row.attachmentUrl) + ')' : '下载' }}</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="审批状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.leave_request_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <!-- 查看：三个视图都能点。能不能真打开由后端 selectLeaveRequestById 里的权限判断决定，
               前端不拦，这样越权时会明确收到"您无权查看该请假申请"，比静默隐藏好排查。
               注意这条规则对"我审批过的单子"还没放行——见后端的待补分支 -->
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
          >查看</el-button>
          <!-- 审批：只在"待我审批"出现，且只有待审批/待总监审批的单子可点 -->
          <el-button
            size="mini"
            type="text"
            icon="el-icon-s-check"
            @click="handleApproval(scope.row)"
            v-hasPermi="['leave:leaveRequest:approve']"
            v-if="viewMode === 'pending' && (scope.row.status === 'PENDING' || scope.row.status === 'MANAGER_APPROVED')"
          >审批</el-button>
          <!-- 撤回：自己的单（"我的请假"）且状态还未终结 -->
          <el-button
            size="mini"
            type="text"
            icon="el-icon-refresh-left"
            @click="handleCancel(scope.row)"
            v-hasPermi="['leave:leaveRequest:cancel']"
            v-if="viewMode === 'my' && (scope.row.status === 'PENDING' || scope.row.status === 'MANAGER_APPROVED')"
          >撤回</el-button>
          <!-- 重新提交：只存在于自己的"已撤回/已驳回"单子上，点击回显后走新增接口 -->
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit-outline"
            @click="handleResubmit(scope.row)"
            v-hasPermi="['leave:leaveRequest:add']"
            v-if="viewMode === 'my' && (scope.row.status === 'CANCELLED' || scope.row.status === 'REJECTED')"
          >重新提交</el-button>
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

    <!-- 添加或重新提交请假申请对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="申 请 人">
              <el-input :value="$store.getters.nickName || $store.getters.name" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请假类型" prop="leaveType">
              <el-select v-model="form.leaveType" placeholder="请选择请假类型">
                <el-option
                  v-for="dict in dict.type.leave_request_type"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="14">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker clearable
                              style="width: 100%"
                              v-model="form.startTime"
                              type="date"
                              value-format="yyyy-MM-dd"
                              placeholder="请选择开始时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <!-- 半天下拉不给"空"这个选项可选（required 规则挡着），
               值域是业务定死的 AM/PM 两个值，不是运营可配的东西，所以不走 sys_dict 硬编码在 data 里 -->
          <el-col :span="10">
            <el-form-item label-width="0" prop="startHalf">
              <el-select v-model="form.startHalf" style="width: 100%">
                <el-option v-for="item in halfOptions" :key="item.value" :label="item.label" :value="item.value"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="14">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker clearable
                              style="width: 100%"
                              v-model="form.endTime"
                              type="date"
                              value-format="yyyy-MM-dd"
                              placeholder="请选择结束时间">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label-width="0" prop="endHalf">
              <el-select v-model="form.endHalf" style="width: 100%">
                <el-option v-for="item in halfOptions" :key="item.value" :label="item.label" :value="item.value"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请假事由" prop="reason">
              <el-input v-model="form.reason" type="textarea" placeholder="请输入内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="附件" prop="attachmentUrl">
              <file-upload
                v-model="form.attachmentUrl"
                :file-type="['jpg', 'jpeg', 'png', 'pdf', 'doc', 'docx']"
                :file-size="10"
                :limit="3"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 审批对话框：只读展示单据内容 + 填审批意见，通过和驳回是两个按钮 -->
    <el-dialog title="请假审批" :visible.sync="approvalOpen" width="600px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="请假单ID">
          <el-input :value="approvalRow.id" disabled />
        </el-form-item>
        <el-form-item label="请假类型">
          <dict-tag :options="dict.type.leave_request_type" :value="approvalRow.leaveType" />
        </el-form-item>
        <el-form-item label="请假起止">
          <el-input :value="((parseTime(approvalRow.startTime, '{y}-{m}-{d}') || '') + halfLabel(approvalRow.startHalf)) + ' 至 ' + ((parseTime(approvalRow.endTime, '{y}-{m}-{d}') || '') + halfLabel(approvalRow.endHalf))" disabled />
        </el-form-item>
        <el-form-item label="请假时长">
          <el-input :value="approvalRow.duration" disabled />
        </el-form-item>
        <el-form-item label="请假事由">
          <el-input :value="approvalRow.reason" type="textarea" :rows="2" disabled />
        </el-form-item>
        <el-form-item label="附件">
          <template v-for="(key, i) in $attachment.list(approvalRow.attachmentUrl)">
            <el-image
              v-if="$attachment.isImage(key)"
              :key="'img' + i"
              :src="$attachment.url(key)"
              :preview-src-list="$attachment.urlList(approvalRow.attachmentUrl)"
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
          <span v-if="!$attachment.count(approvalRow.attachmentUrl)">无</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <dict-tag :options="dict.type.leave_request_status" :value="approvalRow.status" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approvalForm.comment" type="textarea" :rows="3"
                    placeholder="通过可不填；驳回必填（后端强制）" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="success" @click="submitApprove" v-hasPermi="['leave:leaveRequest:approve']">通 过</el-button>
        <el-button type="danger" @click="submitReject" v-hasPermi="['leave:leaveRequest:reject']">驳 回</el-button>
        <el-button @click="approvalOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 详情弹窗抽成了公共组件（components/LeaveDetailDialog），
         和「我已审批 / 审批流水」表格共用一份，这里只挂一个壳、通过 ref 调 open(id) -->
    <leave-detail-dialog ref="detailDialog" />
  </div>
</template>

<script>
import {
  listLeaveRequest, listMyLeaveRequest, listPendingApproval, getLeaveRequest,
  addLeaveRequest, cancelLeaveRequest, approveLeaveRequest, rejectLeaveRequest
} from "@/api/leave/leaveRequest"
import LeaveDetailDialog from "@/views/leave/components/LeaveDetailDialog"

export default {
  name: "LeaveTable",
  components: { LeaveDetailDialog },
  dicts: ['leave_request_status', 'leave_request_type'],
  props: {
    // 由薄壳菜单传进来，决定这一页调哪个接口、显示哪些行内按钮
    viewMode: {
      type: String,
      required: true,
      validator: value => ['my', 'pending', 'all'].indexOf(value) !== -1
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
      // 请假申请表格数据
      leaveRequestList: [],
      // 弹出层标题
      title: "",
      // 是否显示新增对话框
      open: false,
      // 是否是"重新提交"模式（复用新增对话框，只是标题不同、提交前多了回显）
      resubmitMode: false,
      // 审批弹窗是否显示
      approvalOpen: false,
      // 当前待审批的请假单（只给弹窗做只读展示，不参与提交）
      approvalRow: {},
      // 审批表单（只提交这两个值）
      approvalForm: { id: null, comment: null },
      // 查询参数：只留页面上真有输入控件的字段，
      // 后端接口对 null 字段不会加条件，所以不用为了"补齐实体字段"而留一堆空值
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        leaveType: null,
        startTime: null,
        endTime: null,
        duration: null,
        status: null,
      },
      // 表单参数
      form: {},
      // 半天下拉选项：值必须和后端 HalfDay 枚举的 code 一字不差（AM / PM）
      halfOptions: [
        { value: 'AM', label: '上午' },
        { value: 'PM', label: '下午' }
      ],
      // 表单校验（只校验用户真正要填的字段；userId/deptId/duration/status 由后端生成，不让用户填）
      rules: {
        leaveType: [
          { required: true, message: "请假类型不能为空", trigger: "change" }
        ],
        startTime: [
          { required: true, message: "开始时间不能为空", trigger: "change" }
        ],
        endTime: [
          { required: true, message: "结束时间不能为空", trigger: "change" },
          {
            validator: (rule, value, callback) => {
              if (value && this.form.startTime && value < this.form.startTime) {
                callback(new Error("结束时间不能早于开始时间"))
              } else {
                callback()
              }
            },
            trigger: "change"
          }
        ],
        // 半天只做必填。"同一天下午开始上午结束"这类组合校验不写在前端——
        // 判定口径（周末剔除、半天份数）全在后端 calcDuration，前端复刻一份只会两边打架
        startHalf: [
          { required: true, message: "开始时间是上午还是下午必选", trigger: "change" }
        ],
        endHalf: [
          { required: true, message: "结束时间是上午还是下午必选", trigger: "change" }
        ],
        reason: [
          { required: true, message: "请假事由不能为空", trigger: "blur" }
        ],
      }
    }
  },
  computed: {
    // 每个菜单顶部一句话说明自己装的是什么。
    // 这行提示的价值不在于好看：五个菜单分开以后，
    // "台账里为什么没有别的部门的人"这类问题会直接写在脸上，不用翻文档也不用问管理员
    pageHint() {
      const hints = {
        my: "这里只列出你自己提交的请假申请。想重新发起一张已撤回或被驳回的单子，点该行的【重新提交】。",
        pending: "这里只列出当前轮到你处理的单子：经理看到本部门待审批的，总监看到待总监审批的。批完就从这里消失，去「我已审批」查记录。",
        all: "请假台账：经理看到本部门全部、总监看到全公司全部，范围由角色的数据权限（data_scope）决定，不是这里能改的。"
      }
      return hints[this.viewMode] || ""
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询列表：三个视图走三个接口，各自的数据范围完全由后端决定 */
    getList() {
      this.loading = true
      // 用映射表分派，避免 viewMode === 'a' ? apiA : viewMode === 'b' ? apiB : apiC 这种嵌套三元
      const apiMap = {
        my: listMyLeaveRequest,
        pending: listPendingApproval,
        all: listLeaveRequest
      }
      const api = apiMap[this.viewMode] || listLeaveRequest
      api(this.queryParams).then(response => {
        this.leaveRequestList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        // 后端可能因为角色不匹配拒绝（比如给员工误开了"待我审批"菜单），
        // 必须把 loading 关掉，否则表格一直转圈
        this.leaveRequestList = []
        this.total = 0
        this.loading = false
      })
    },
    /** 查看详情：弹窗组件自己调详情接口、自己判断开关（见 LeaveDetailDialog 的 open）
        失败时（后端判定"您无权查看该请假申请"）request.js 会弹错误提示，弹窗自动关闭 */
    handleView(row) {
      this.$refs.detailDialog.open(row.id)
    },
    /** 半天的中文后缀，拼在日期后面显示（带前导空格）。
        返回空串兜底：万一库里有半天空值的脏数据，显示成纯日期而不是 undefined */
    halfLabel(half) {
      const map = { AM: ' 上午', PM: ' 下午' }
      return map[half] || ''
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置（只重置用户能填的字段，其余由后端生成）
    reset() {
      this.form = {
        leaveType: null,
        startTime: null,
        // 半天默认 AM→PM（一整天）：绝大多数请假是整天，给个默认少两次点击
        startHalf: 'AM',
        endTime: null,
        endHalf: 'PM',
        reason: null,
        attachmentUrl: null
      }
      this.resetForm("form")
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
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.resubmitMode = false
      this.open = true
      this.title = "添加请假申请"
    },
    /**
     * 重新提交：只出现在自己的"已撤回/已驳回"单子上。
     * 用详情接口把原单内容回显出来，改完点确定走的还是【新增接口】——
     * 后端 insertLeaveRequest 会强制用当前登录用户覆盖 userId/deptId，
     * 所以不用担心前端把 userId 改成别人的单子上去。
     * 原单保持 CANCELLED/REJECTED 不动，作为历史记录留着。
     */
    handleResubmit(row) {
      this.reset()
      getLeaveRequest(row.id).then(response => {
        const src = response.data || {}
        this.form = {
          leaveType: src.leaveType,
          // 后端返回的是完整时间，date-picker 的 value-format 只要 yyyy-MM-dd
          startTime: this.parseTime(src.startTime, '{y}-{m}-{d}'),
          endTime: this.parseTime(src.endTime, '{y}-{m}-{d}'),
          // 半天字段必须跟着回显：漏了这两行，一张"周五下午"的半天的单
          // 重新提交出去会变成"周五全天"——不报错，静默把假期放大一倍
          startHalf: src.startHalf || 'AM',
          endHalf: src.endHalf || 'PM',
          reason: src.reason,
          attachmentUrl: src.attachmentUrl
        }
        this.resubmitMode = true
        this.title = "重新提交请假申请（原单号 " + row.id + "）"
        this.open = true
      })
    },
    /** 提交按钮（只有新增一条路径：后端 edit 接口已删，请假单不能改，只能撤回后重新提交） */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          addLeaveRequest(this.form).then(response => {
            this.$modal.msgSuccess(this.resubmitMode ? "重新提交成功" : "提交成功")
            this.open = false
            this.resubmitMode = false
            this.getList()
          })
        }
      })
    },
    /** 撤回按钮操作 */
    handleCancel(row) {
      this.$modal.confirm('是否确认撤回请假单编号为"' + row.id + '"的申请？').then(function() {
        return cancelLeaveRequest(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("撤回成功")
      }).catch(() => {})
    },
    /** 审批按钮操作：打开弹窗，把这一行数据拿去做只读展示 */
    handleApproval(row) {
      this.approvalRow = row
      this.approvalForm = { id: row.id, comment: null }
      this.approvalOpen = true
    },
    /** 审批通过（意见可为空） */
    submitApprove() {
      approveLeaveRequest(this.approvalForm.id, this.approvalForm.comment).then(() => {
        this.approvalOpen = false
        this.getList()
        this.$modal.msgSuccess("审批通过")
      })
      // 失败时（不是本部门/状态已变更/超管被拦）request.js 会弹错误提示并 reject，
      // 上面的 then 不执行 → 弹窗保持打开，用户可以改意见后重试
    },
    /** 驳回（意见后端强制必填，前端先挡一次，避免白跑一趟接口） */
    submitReject() {
      if (!this.approvalForm.comment || !this.approvalForm.comment.trim()) {
        this.$modal.msgError("驳回必须填写审批意见")
        return
      }
      rejectLeaveRequest(this.approvalForm.id, this.approvalForm.comment).then(() => {
        this.approvalOpen = false
        this.getList()
        this.$modal.msgSuccess("已驳回")
      })
    },
    /** 导出按钮操作：走 /export，它固定用带 @DataScope 的那条查询，所以只在台账视图给按钮 */
    handleExport() {
      this.download('leave/leaveRequest/export', {
        ...this.queryParams
      }, `leaveRequest_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
