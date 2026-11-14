package com.ruoyi.system.mapper;

import java.util.Date;
import java.util.List;
import com.ruoyi.system.domain.LeaveRequest;
import com.ruoyi.system.domain.vo.LeaveRequestVo;
import org.apache.ibatis.annotations.Param;

/**
 * 请假申请Mapper接口
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
public interface LeaveRequestMapper 
{
    /**
     * 查询请假申请详细信息
     * 
     * @param id 请假申请主键
     * @return 请假申请
     */
    public LeaveRequestVo selectLeaveRequestById(Long id);

    /**
     * 查询请假申请列表  -- 请假台账
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
     * 修改请假申请
     * 
     * @param leaveRequest 请假申请
     * @return 结果
     */
    public int updateLeaveRequest(LeaveRequest leaveRequest);

    /**
     * 删除请假申请
     * 
     * @param id 请假申请主键
     * @return 结果
     */
    public int deleteLeaveRequestById(Long id);

    /**
     * 批量删除请假申请
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLeaveRequestByIds(Long[] ids);

    /**
     * 根据id和oldstatus修改status，保证了一个人查看操作时，如果被另外一个人修改了，那么这个操作就不成功
     * @param id
     * @param status
     * @param oldStatus
     * @param nowDate
     * @return
     */
    int updateStatusById(@Param("id") Long id, @Param("status")String status,
                         @Param("oldStatus") String oldStatus,@Param("nowDate") Date nowDate);

    /**
     * 查询我的请假申请列表  -- 我的请假（员工、经理）
     * @param leaveRequest
     * @return
     */
    List<LeaveRequest> selectMyLeaveRequestList(LeaveRequest leaveRequest);

    /**
     * 查询待审批的请假申请列表  -- 代办审批
     * @param leaveRequest
     * @return
     */
    List<LeaveRequest> selectPendingApprovalList(LeaveRequest leaveRequest);

    /**
     * 查询请假重叠的个数
     * @param leaveRequest
     * @return
     */
    int countOverlapLeave(LeaveRequest leaveRequest);

}
