package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Date;
import java.util.Objects;


import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.ApprovalRecord;
import com.ruoyi.system.domain.vo.LeaveRequestVo;
import com.ruoyi.system.enums.ApprovalAction;
import com.ruoyi.system.enums.HalfDay;
import com.ruoyi.system.enums.LeaveStatus;
import com.ruoyi.system.mapper.ApprovalRecordMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.LeaveRequestMapper;
import com.ruoyi.system.domain.LeaveRequest;
import com.ruoyi.system.service.ILeaveRequestService;
import org.springframework.transaction.annotation.Transactional;



// TODO 判断用户新增的假条请假时间是否和之前重叠
/**
 * 请假申请Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
@Service
public class LeaveRequestServiceImpl implements ILeaveRequestService 
{
    @Autowired
    private LeaveRequestMapper leaveRequestMapper;
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;


    /**
     * 查询请假申请详细信息
     * 
     * @param id 请假申请主键
     * @return 请假申请
     */
    @Override
    public LeaveRequest selectLeaveRequestById(Long id)
    {
        LeaveRequestVo leaveRequest = leaveRequestMapper.selectLeaveRequestById(id);
        if(leaveRequest == null){
            throw new ServiceException("不存在该请假申请");
        }
        if(SecurityUtils.getUserId().equals(leaveRequest.getUserId()) || SecurityUtils.isAdmin()){
            return leaveRequest;
        }else if(SecurityUtils.hasRole("manager") &&
                leaveRequest.getStatus().equals(LeaveStatus.PENDING.getCode()) &&
                Objects.equals(SecurityUtils.getDeptId(), leaveRequest.getDeptId())){
            return leaveRequest;
        }else if(SecurityUtils.hasRole("director") && leaveRequest.getStatus().equals(LeaveStatus.MANAGER_APPROVED.getCode())){
                return leaveRequest;
        } else if (approvalRecordMapper.countByLeaveAndApprover(id, SecurityUtils.getUserId()) > 0) {
            return leaveRequest; // 判断当前用户是否是该单的审批人
        } else{
        throw new ServiceException("您无权查看该请假申请");
        }
    }

    /**
     * 查询请假申请列表  -- 请假台账
     * 
     * @param leaveRequest 请假申请
     * @return 请假申请
     */
    @Override
    @DataScope(deptAlias = "l", userAlias = "l")
    public List<LeaveRequest> selectLeaveRequestList(LeaveRequest leaveRequest)
    {
        return leaveRequestMapper.selectLeaveRequestList(leaveRequest);
    }

    /**
     * 新增请假申请
     * 
     * @param leaveRequest 请假申请
     * @return 结果
     */
    // TODO 存在重复提交问题（并发穿透） -- 需要使用分布式锁来解决
    @Override
    public int insertLeaveRequest(LeaveRequest leaveRequest)
    {
        // 第一道：admin 是运维身份，名下不该有请假单
        assertNotAdmin();

        // 判断当前用户是否是请假申请的属于者
        if (leaveRequest.getUserId() != null && !SecurityUtils.getUserId().equals(leaveRequest.getUserId())) {
                throw new ServiceException("无权限申请请假");
        }

        //通过角色判断是否可以申请请假（SecurityUtils.hasRole("<role_key>")来判断当前用户是否所属该角色）
        if(SecurityUtils.hasRole("director")){ //如果角色属于总监，就不可通过该方式请假
            throw new ServiceException("不可通过该方式请假");
        }

        // 根据角色设置请假状态
        if(SecurityUtils.hasRole("manager")){
            leaveRequest.setStatus(LeaveStatus.MANAGER_APPROVED.getCode());
        } else if (SecurityUtils.hasRole("employee")) {
            leaveRequest.setStatus(LeaveStatus.PENDING.getCode());
        }else{
            throw new ServiceException("无权限申请请假");
        }

        if(leaveRequest.getStartTime() == null || leaveRequest.getEndTime() == null){
            throw new ServiceException("开始时间和结束时间不能为空");
        }
        if(leaveRequest.getEndTime().before(leaveRequest.getStartTime())){
            throw new ServiceException("结束时间不能早于开始时间");
        }
        if(leaveRequest.getStartHalf() == null || leaveRequest.getEndHalf() == null){
            throw new ServiceException("开始时间和结束时间的上下午不能为空");
        }

        // 先把上下午的字符串转换成枚举类型
        HalfDay startHalf = HalfDay.ofCode(leaveRequest.getStartHalf());
        HalfDay endHalf = HalfDay.ofCode(leaveRequest.getEndHalf());

        // 设置请假单其他值
        leaveRequest.setUserId(SecurityUtils.getUserId());
        leaveRequest.setDeptId(SecurityUtils.getDeptId());
        // 通过calcDuration方法得到,并传入请假日期和上下午
        leaveRequest.setDuration(calcDuration(leaveRequest.getStartTime(), leaveRequest.getEndTime(), startHalf, endHalf));
        leaveRequest.setCreateTime(DateUtils.getNowDate());
        leaveRequest.setUpdateTime(DateUtils.getNowDate());

        // 判交 -- 判断该时间段内是否已有请假申请 -- 必须在设置userId和请假时间之后，否则会查询不到
        if(leaveRequestMapper.countOverlapLeave(leaveRequest) > 0){
            throw new ServiceException("该时间段已有请假申请，请勿重复提交");
        }

        return leaveRequestMapper.insertLeaveRequest(leaveRequest);
    }

    /**
     * 批量删除请假申请---------------------------------不需要--------
     * 
     * @param ids 需要删除的请假申请主键
     * @return 结果
     */
//    @Override
//    public int deleteLeaveRequestByIds(Long[] ids)
//    {
//        return leaveRequestMapper.deleteLeaveRequestByIds(ids);
//    }

    /**
     * 删除请假申请信息---------------------------------不需要--------
     * 
     * @param id 请假申请主键
     * @return 结果
     */
//    @Override
//    public int deleteLeaveRequestById(Long id)
//    {
//        return leaveRequestMapper.deleteLeaveRequestById(id);
//    }

    /**
     * 撤回请假申请
     * @param id
     * @return
     */
    @Transactional
    @Override
    public int cancelLeaveRequest(Long id) {
        // 根据前端传入的请假单id校验请假单是否存在
        LeaveRequest leaveRequest = leaveRequestMapper.selectLeaveRequestById(id);
        if(leaveRequest == null){
            throw new ServiceException("不存在该请假申请");
        }

        // 根据请假单id获取请假单属于者，并判断当前用户是否是请假单属于者
        if(!SecurityUtils.getUserId().equals(leaveRequest.getUserId())){ // 要把不会为null的放在equals外面
            throw new ServiceException("无权限撤回该请假申请");
        }

        // 校验请假单状态是否允许撤回 --- 用枚举类里面的canTransitTo方法处理
        LeaveStatus status = LeaveStatus.ofCode(leaveRequest.getStatus());
        if(!status.canTransitTo(LeaveStatus.CANCELLED)){
            throw new ServiceException("当前状态【" + status.getDesc() + "】不允许撤回");
        }

        // 更新请假单数据:其中除了要更改的status和updateTime和条件判断的id，还要传入查找到的oldStatus，用于判断当前状态是否被改变了（充当一个锁，防止同时修改）
        int rows = leaveRequestMapper.updateStatusById(id, LeaveStatus.CANCELLED.getCode(), leaveRequest.getStatus(), DateUtils.getNowDate());
        if(rows == 0) {
            throw new ServiceException("该单据状态已变更，请刷新后重试");
        }
        // 抽出approvalRecord的插入代码，抽到了下面，封装成方法 --- 因为撤回，经理审批通过，总监审批通过，经理拒绝，总监拒绝，都需要插入审批记录
        insertRecord(id, ApprovalAction.CANCEL, leaveRequest.getStatus(), LeaveStatus.CANCELLED.getCode(), null);
        return rows;
    }

    /**
     * 批准请假申请
     * @param id
     * @return
     */
    @Transactional
    @Override
    public int approveLeaveRequest(Long id, String comment) {
        // 放在第一道：下面所有基于 hasRole 的判断对 admin 恒为 true，拦不住它，
        // 必须在进业务逻辑之前先把超管摘出去（理由见 assertNotAdmin）
        assertNotAdmin();

        // 校验请假单是否存在
        LeaveRequest leaveRequest = leaveRequestMapper.selectLeaveRequestById(id);
        if(leaveRequest == null) throw new ServiceException("不存在该请假申请");

        // 把状态转为枚举类对象status，并校验请假单是否为结束状态
        LeaveStatus status = LeaveStatus.ofCode(leaveRequest.getStatus());
        if(status.isFinal()) throw new ServiceException("当前状态不允许审批");

        // // 审批人不能是申请人：角色重叠（经理兼总监）时，这是唯一挡住自批的闸门，不许删
        if(leaveRequest.getUserId().equals(SecurityUtils.getUserId())) throw new ServiceException("不能审批自己的请假申请");

        // 校验当前审批人是否有审批该状态的请假单的权限
        if(!SecurityUtils.hasRole(status.getApproverRole())) throw new ServiceException("无权限审批该状态的请假单");

        // 判断当前是哪个状态，进行相应的处理
        if(status == LeaveStatus.MANAGER_APPROVED){ // 当状态为MANAGER_APPROVED时，只能总监批
            int rows = leaveRequestMapper.updateStatusById(id, LeaveStatus.APPROVED.getCode(), leaveRequest.getStatus(), DateUtils.getNowDate());
            if(rows == 0) throw new ServiceException("该单据状态已变更，请刷新后重试");
            insertRecord(id, ApprovalAction.APPROVE, leaveRequest.getStatus(), LeaveStatus.APPROVED.getCode(), comment);
            return rows;
        } else if (status == LeaveStatus.PENDING) { //当状态为PENDING时
            // 校验当前审批人是否和申请人为同一个部门
            if(!leaveRequest.getDeptId().equals(SecurityUtils.getDeptId())) throw new ServiceException("无权限审批非本部门的请假申请");

            // 调用我们自定义的isLongLeave()方法来判断是长假还是短假
            if(isLongLeave(leaveRequest.getDuration())){ //如果是长假 -- 把状态设置为MANAGER_APPROVED
                int rows = leaveRequestMapper.updateStatusById(id, LeaveStatus.MANAGER_APPROVED.getCode(), leaveRequest.getStatus(),DateUtils.getNowDate());
                if(rows == 0) throw new ServiceException("该单据状态已变更，请刷新后重试");
                insertRecord(id,ApprovalAction.APPROVE,leaveRequest.getStatus(),LeaveStatus.MANAGER_APPROVED.getCode(), comment);
                return rows;
            }else{
                int rows =  leaveRequestMapper.updateStatusById(id, LeaveStatus.APPROVED.getCode(), leaveRequest.getStatus(),DateUtils.getNowDate());
                if(rows == 0) throw new ServiceException("该单据状态已变更，请刷新后重试");
                insertRecord(id,ApprovalAction.APPROVE,leaveRequest.getStatus(),LeaveStatus.APPROVED.getCode(), comment);
                return rows;
            }
        }else{
            throw new ServiceException("当前状态不允许审批");
        }
    }

    /**
     * 驳回请假申请
     * @param id
     * @return
     */
    @Override
    @Transactional
    public int rejectLeaveRequest(Long id, String comment) {
        // 同 approveLeaveRequest：admin 不参与业务动作，必须先摘出去
        assertNotAdmin();

        // 校验请假单是否存在
        LeaveRequest leaveRequest = leaveRequestMapper.selectLeaveRequestById(id);
        if(leaveRequest == null) throw new ServiceException("不存在该请假申请");

        // 把状态转为枚举类对象status，并校验请假单是否为结束状态
        LeaveStatus status = LeaveStatus.ofCode(leaveRequest.getStatus());
        if(status.isFinal()) throw new ServiceException("当前状态不允许审批");

        // 校验有没有填写意见comment -- 驳回必须填写意见
        if(StringUtils.isEmpty(comment)){
            throw new ServiceException("驳回必须填写意见");
        }

        // 校验当前审批人是否和请假申请人为同一个人
        if(leaveRequest.getUserId().equals(SecurityUtils.getUserId())) throw new ServiceException("不能审批自己的请假申请");

        // 校验当前审批人是否有审批该状态的请假单的权限
        if(!SecurityUtils.hasRole(status.getApproverRole())) throw new ServiceException("无权限审批该状态的请假单");

        // 判断当前是哪个状态，进行相应的处理
        if(status == LeaveStatus.MANAGER_APPROVED){ // 当状态为MANAGER_APPROVED时，只能总监批
            int rows = leaveRequestMapper.updateStatusById(id, LeaveStatus.REJECTED.getCode(), leaveRequest.getStatus(), DateUtils.getNowDate());
            if(rows == 0) throw new ServiceException("该单据状态已变更，请刷新后重试");
            insertRecord(id, ApprovalAction.REJECT, leaveRequest.getStatus(), LeaveStatus.REJECTED.getCode(), comment);
            return rows;
        } else if (status == LeaveStatus.PENDING) { //当状态为PENDING时
            // 校验当前审批人是否和申请人为同一个部门
            if(!leaveRequest.getDeptId().equals(SecurityUtils.getDeptId())) throw new ServiceException("无权限审批非本部门的请假申请");
            // 如果是就进行驳回操作
            int rows =  leaveRequestMapper.updateStatusById(id, LeaveStatus.REJECTED.getCode(), leaveRequest.getStatus(),DateUtils.getNowDate());
            if(rows == 0) throw new ServiceException("该单据状态已变更，请刷新后重试");
            insertRecord(id,ApprovalAction.REJECT,leaveRequest.getStatus(),LeaveStatus.REJECTED.getCode(), comment);
            return rows;
        }else{
            throw new ServiceException("当前状态不允许审批");
        }
    }

    /**
     * 查询我的请假申请列表  -- 我的请假（员工、经理）
     * @return
     */
    @Override
    public List<LeaveRequest> selectMyLeaveRequestList(LeaveRequest leaveRequest) {
        //仅需传入当前用户id即可
        leaveRequest.setUserId(SecurityUtils.getUserId());
        return leaveRequestMapper.selectMyLeaveRequestList(leaveRequest);
    }

    /**
     * 查询待审批的请假申请列表  -- 代办审批
     * @param leaveRequest
     * @return
     */
    @Override
    public List<LeaveRequest> selectPendingApprovalList(LeaveRequest leaveRequest) {
        // 在传过来的leaveRequest实体对象里，加入一个当前用户id的参数（getParams()就是获取参数Map的方法，put就是向Map(参数)里添加元素）
        leaveRequest.getParams().put("currentUserId", SecurityUtils.getUserId()); // 加了个key为currentUserId，value为当前用户id的参数

        // 如果是管理员，则查看所有待审批的请假申请
        if(SecurityUtils.isAdmin()) {
            leaveRequest.getParams().put("viewType", "admin"); // 加了个key为viewType（表示查看的角色类型），value为admin的参数
            return leaveRequestMapper.selectPendingApprovalList(leaveRequest);
        }

        // 判断当前用户是属于一级审批人还是二级审批人，或者都不是（即全为false）
        boolean managerView  = SecurityUtils.hasRole(LeaveStatus.PENDING.getApproverRole()); // 获取一级审批人角色，判断当前用户是否是一级审批人
        boolean directorView = SecurityUtils.hasRole(LeaveStatus.MANAGER_APPROVED.getApproverRole()); // 获取二级审批人角色，判断当前用户是否是二级审批人

        // 1.先排除掉两个绝对错误情况----------------------------------------------
        // 1.1如果当前用户既不是一级审批人也不是二级审批人，则抛出异常，提示当前用户无权审批
       if(!managerView && !directorView){
           throw new ServiceException("您无权审批");
       }
       // 1.2如果当前用户是一级审批人（因为规定一级审批人只管自己的部门），但账号未分配部门，则抛出异常，提示当前用户无法查看待审批列表
       if(managerView && SecurityUtils.getDeptId() == null){
           throw new ServiceException("您的账号未分配部门，无法查看待审批列表");
       }

       // 2.根据当前用户是一级审批人还是二级审批人，还是两者都是，来设置viewType参数（viewType表示查看的角色类型）
       String viewType;
       if(managerView && directorView){ // 2.1如果既是一级审批人又是二级审批人，则viewType为both
           viewType = "both";
       }else if(managerView){ // 2.2如果只是是一级审批人，则viewType为manager
           viewType = "manager";
       }else{ //2.3如果只是二级审批人，则viewType为director
           viewType = "director";
       }

       // 3.继续加viewType参数，根据viewType参数，设置查询条件
       leaveRequest.getParams().put("viewType", viewType);
       leaveRequest.setDeptId(SecurityUtils.getDeptId());

        return leaveRequestMapper.selectPendingApprovalList(leaveRequest);
    }

    // 创建并插入审批记录 -- 单抽出来，这样就不用在每个方法都写一遍封装审批记录和插入审批记录的代码了
    private void insertRecord(Long leaveId, ApprovalAction action, String fromStatus, String toStatus, String comment){

        ApprovalRecord approvalRecord = new ApprovalRecord();
        approvalRecord.setLeaveId(leaveId);
        approvalRecord.setApproverId(SecurityUtils.getUserId()); // 可以直接获取，而不需要参数传入
        approvalRecord.setAction(action.getCode());  // 枚举只在 Java 层用，进库前转成字符串
        approvalRecord.setFromStatus(fromStatus);
        approvalRecord.setToStatus(toStatus);
        approvalRecord.setComment(comment); // 如果没有comment就写为null
        approvalRecord.setCreateTime(DateUtils.getNowDate());

        approvalRecordMapper.insertApprovalRecord(approvalRecord); // 可直接获取不需要参数传入
    }

    // 计算请假天数 --- 该方法用于计算请假天数，并通过DayOfWeek判断是否是周末，排除周末
    private BigDecimal calcDuration(Date startTime, Date endTime, HalfDay startHalf, HalfDay endHalf){
        //Date → LocalDate  老式 java.util.Date 没有"日期"概念，只有时间戳，所以转成 LocalDate，只保留年月日，丢掉时分秒。后面按"天"遍历就方便了。
        LocalDate s = startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate e = endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        int halves = 0; // 用halves表示请假的半天数，通过累加半天数

        // 通过一天一天遍历来加半天数，遍历到申请的第一天和最后一天单独算，如果遍历到第一天且第一天下午开始，则halves加1。
        // 如果遍历到最后一天，且是上午结束，则halves加1，其余都加2，为一整天，最后返回halves除以2，得到请假天数

        // 从开始那天，一天一天往后走，直到超过结束那天为止 --- 为了计算请假天数，并通过DayOfWeek判断是否是周末，排除周末
        for(LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)){
            DayOfWeek w = d.getDayOfWeek(); // DayOfWeek是枚举类，表示一周中的某一天（如周天就是SUNDAY）
            // 第一关：如果是周末，直接跳过，不计算，直接遍历下一个（注意一定要先判断是不是周末哦，这样第一天和最后一天的半天数判断才不会出错）
            if(w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY){
                continue;
            }
            //第二关：判断当前遍历的天是不是第一天的下午（isStartAfternoon）或者最后一天的上午
            boolean isStartAfternoon = d.equals(s) && startHalf == HalfDay.PM;
            boolean isEndMorning = d.equals(e) && endHalf == HalfDay.AM;

            halves += (isStartAfternoon || isEndMorning)? 1 : 2;
        }
        //判断当前天数是否为0，如果为0则抛出异常，提示请假天数不能为0
        if(halves == 0) {
            if(s.equals(e)){
                throw new ServiceException("同一天内不能下午开始、上午结束");
            }
            throw new ServiceException("请假区间内没有可请的工作日");
        }

        // 返回半天数除以2
        return BigDecimal.valueOf(halves).divide(BigDecimal.valueOf(2));
    }

    // 判断是否是长假
    private boolean isLongLeave(BigDecimal duration){
        // 如果大于3就是长假
        return duration.compareTo(BigDecimal.valueOf(3)) > 0;
    }

    /**
     * 超级管理员不参与任何业务动作（提交请假 / 审批通过 / 驳回）。
     *
     * 为什么要专门写这一道闸，而不是靠下面那些角色判断：
     *  1) SecurityUtils.hasRole 源码里带 Constants.SUPER_ADMIN 特判，
     *     所以 admin 调 hasRole("manager")、hasRole("director") 全都返回 true，
     *     「必须是某角色才能审批」这类判断对 admin 天然失效；
     *  2) 而 approveLeaveRequest 里 MANAGER_APPROVED 那条分支没有部门校验，
     *     结果是 admin 能越过总监，直接把一张待总监审批的单批成通过；
     *  3) 抛开越权不谈，approval_record 里出现 operator=admin 也解释不通——
     *     真实企业里这意味着一个不在审批链上的人动了单子，那是事故，不是功能。
     *
     * admin 仍然保留全部【查询】能力：审批流水、请假台账、单据详情都看得到，
     * 也就是"能审计，不能下手"。
     *
     * cancelLeaveRequest 没调这个方法，不是遗漏：它开头就用
     * SecurityUtils.getUserId().equals(leaveRequest.getUserId()) 挡住了"撤别人的单"，
     * 而 admin 又请不了假（本方法在 insertLeaveRequest 里同样会拦），
     * 所以 admin 名下永远没有单，这条路根本走不到。
     */
    private void assertNotAdmin()
    {
        if (SecurityUtils.isAdmin())
        {
            throw new ServiceException("超级管理员不参与业务流程，请使用经理或总监账号操作");
        }
    }


}
