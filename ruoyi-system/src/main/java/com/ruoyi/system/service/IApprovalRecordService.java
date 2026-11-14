package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.ApprovalRecord;

/**
 * 审批记录Service接口
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
public interface IApprovalRecordService 
{
    /**
     * 查询审批记录---------------------------用不上----------
     * 
     * @param id 审批记录主键
     * @return 审批记录
     */
//    public ApprovalRecord selectApprovalRecordById(Long id);

    /**
     * 查询审批记录列表
     * 
     * @param approvalRecord 审批记录
     * @return 审批记录集合
     */
    public List<ApprovalRecord> selectApprovalRecordList(ApprovalRecord approvalRecord);

    /**
     * 新增审批记录-----------------------------------用不到
     * 
     * @param approvalRecord 审批记录
     * @return 结果
     */
//    public int insertApprovalRecord(ApprovalRecord approvalRecord);

    /**
     * 修改审批记录-----------------------------------用不到
     * 
     * @param approvalRecord 审批记录
     * @return 结果
     */
//    public int updateApprovalRecord(ApprovalRecord approvalRecord);

    /**
     * 批量删除审批记录-----------------------------------用不到
     * 
     * @param ids 需要删除的审批记录主键集合
     * @return 结果
     */
//    public int deleteApprovalRecordByIds(Long[] ids);

    /**
     * 删除审批记录信息-----------------------------------用不到
     * 
     * @param id 审批记录主键
     * @return 结果
     */
//    public int deleteApprovalRecordById(Long id);

    /**
     * 查询我的审批记录列表
     *
     * @param approvalRecord 审批记录
     * @return 审批记录集合
     */
    public List<ApprovalRecord> selectMyApprovalRecordList(ApprovalRecord approvalRecord);
}
