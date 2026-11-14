package com.ruoyi.system.service.impl;

import java.util.List;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.ApprovalRecordMapper;
import com.ruoyi.system.domain.ApprovalRecord;
import com.ruoyi.system.service.IApprovalRecordService;

/**
 * 审批记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
@Service
public class ApprovalRecordServiceImpl implements IApprovalRecordService 
{
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    /**
     * 查询审批记录详细信息----------------------用不上-----------------
     * 
     * @param id 审批记录主键
     * @return 审批记录
     */
//    @Override
//    public ApprovalRecord selectApprovalRecordById(Long id)
//    {
//        return approvalRecordMapper.selectApprovalRecordById(id);
//    }

    /**
     * 查询审批记录列表  -- 审批流水（只有总监和管理员可查）
     * 
     * @param approvalRecord 审批记录
     * @return 审批记录
     */
    @Override
    public List<ApprovalRecord> selectApprovalRecordList(ApprovalRecord approvalRecord)
    {
        // 移除approvalOnly参数，因为这个参数是用于“我已审批”这个接口的
        approvalRecord.getParams().remove("approvalOnly");
        if(SecurityUtils.hasRole("director") || SecurityUtils.hasRole("admin")){
            return approvalRecordMapper.selectApprovalRecordList(approvalRecord);
        }else {
            throw new ServiceException("无权限访问");
        }
    }

    /**
     * 新增审批记录--------------------------用不上-----------
     * 
     * @param approvalRecord 审批记录
     * @return 结果
     */
//    @Override
//    public int insertApprovalRecord(ApprovalRecord approvalRecord)
//    {
//        approvalRecord.setCreateTime(DateUtils.getNowDate());
//        return approvalRecordMapper.insertApprovalRecord(approvalRecord);
//    }

    /**
     * 修改审批记录----------------------------------用不上---------------
     * 
     * @param approvalRecord 审批记录
     * @return 结果
     */
//    @Override
//    public int updateApprovalRecord(ApprovalRecord approvalRecord)
//    {
//        return approvalRecordMapper.updateApprovalRecord(approvalRecord);
//    }

    /**
     * 批量删除审批记录
     * 
     * @param ids 需要删除的审批记录主键 ---------------------------用不上--------
     * @return 结果
     */
//    @Override
//    public int deleteApprovalRecordByIds(Long[] ids)
//    {
//        return approvalRecordMapper.deleteApprovalRecordByIds(ids);
//    }

    /**
     * 删除审批记录信息------------------------------- 用不上---------
     * 
     * @param id 审批记录主键
     * @return 结果
     */
//    @Override
//    public int deleteApprovalRecordById(Long id)
//    {
//        return approvalRecordMapper.deleteApprovalRecordById(id);
//    }

    /**
     * 查询我的审批记录列表  -- 我已审批
     *
     * @param approvalRecord 审批记录
     * @return 审批记录
     */
    @Override
    public List<ApprovalRecord> selectMyApprovalRecordList(ApprovalRecord approvalRecord)
    {
        approvalRecord.setApproverId(SecurityUtils.getUserId());
        // 只有“我已审批”这个接口有approvalOnly这个属性，且值为true。为的是跟“审批流水”这个接口区分开
        approvalRecord.getParams().put("approvalOnly",true);
        if(SecurityUtils.hasRole("director") || SecurityUtils.hasRole("admin")){
            return approvalRecordMapper.selectApprovalRecordList(approvalRecord);
        }else if(SecurityUtils.hasRole("manager")){
            return approvalRecordMapper.selectApprovalRecordList(approvalRecord);
        }else {
            throw new ServiceException("无权限访问");
        }
    }
}
