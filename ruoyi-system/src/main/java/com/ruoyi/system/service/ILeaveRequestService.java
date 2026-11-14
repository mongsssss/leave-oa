package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.LeaveRequest;

/**
 * 请假申请Service接口
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
public interface ILeaveRequestService 
{
    /**
     * 查询请假申请详细信息  -- 请假台账
     * 
     * @param id 请假申请主键
     * @return 请假申请
     */
    public LeaveRequest selectLeaveRequestById(Long id);

    /**
     * 查询请假申请列表
     * 
     * @param leaveRequest 请假申请
     * @return 请假申请集合
     */
    public List<LeaveRequest> selectLeaveRequestList(LeaveRequest leaveRequest);

    /**
     * 新增请假申请
     * 
     * @param leaveRequest 请假申请
     * @return 结果
     */
    public int insertLeaveRequest(LeaveRequest leaveRequest);

    /**
     * 批量删除请假申请-----------------------------------------不需要--------
     * 
     * @param ids 需要删除的请假申请主键集合
     * @return 结果
     */
//    public int deleteLeaveRequestByIds(Long[] ids);

    /**
     * 删除请假申请信息----------------------------------------不需要
     * 
     * @param id 请假申请主键
     * @return 结果
     */
//    public int deleteLeaveRequestById(Long id);

    /**
     * 撤回请假申请
     * @param id
     * @return
     */
    int cancelLeaveRequest(Long id);

    /**
     * 批准请假申请
     * @param id
     * @return
     */
    int approveLeaveRequest(Long id, String comment);

    /**
     * 驳回请假申请
     * @param id
     * @param comment
     * @return
     */
    int rejectLeaveRequest(Long id, String comment);

    /**
     * 查询我的请假申请列表  -- 我的请假（员工、经理）
     * @return
     */
    List<LeaveRequest> selectMyLeaveRequestList(LeaveRequest leaveRequest);

    /**
     * 查询待自己审批的请假申请列表  -- 代办审批
     * @param leaveRequest
     * @return
     */
    List<LeaveRequest> selectPendingApprovalList(LeaveRequest leaveRequest);
}
