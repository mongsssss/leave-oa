package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.ApprovalRecord;
import org.apache.ibatis.annotations.Param;

/**
 * 审批记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
public interface ApprovalRecordMapper 
{
    /**
     * 查询审批记录
     * 
     * @param id 审批记录主键
     * @return 审批记录
     */
    public ApprovalRecord selectApprovalRecordById(Long id);

    /**
     * 查询审批记录列表  -- 我已审批 / 审批流水
     * 
     * @param approvalRecord 审批记录
     * @return 审批记录集合
     */
    public List<ApprovalRecord> selectApprovalRecordList(ApprovalRecord approvalRecord);

    /**
     * 新增审批记录
     * 
     * @param approvalRecord 审批记录
     * @return 结果
     */
    public int insertApprovalRecord(ApprovalRecord approvalRecord);

    /**
     * 修改审批记录
     * 
     * @param approvalRecord 审批记录
     * @return 结果
     */
    public int updateApprovalRecord(ApprovalRecord approvalRecord);

    /**
     * 删除审批记录
     * 
     * @param id 审批记录主键
     * @return 结果
     */
    public int deleteApprovalRecordById(Long id);

    /**
     * 批量删除审批记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteApprovalRecordByIds(Long[] ids);

    /**
     * 根据请假id和审批人id查询审批记录个数
     * @param leaveId
     * @param approverId
     * @return
     */
    public int countByLeaveAndApprover(@Param("leaveId") Long leaveId, @Param("approverId") Long approverId);
}
