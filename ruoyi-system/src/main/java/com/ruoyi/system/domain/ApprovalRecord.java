package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 审批记录对象 approval_record
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
public class ApprovalRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long id;

    /** 关联的请假单ID */
    @Excel(name = "关联的请假单ID")
    private Long leaveId;

    /** 审批人用户ID */
    private Long approverId;

    /** 动作（APPROVE-通过 / REJECT-驳回 / CANCEL-撤回） */
    @Excel(name = "动作", readConverterExp = "APPROVE=通过,REJECT=驳回,CANCEL=撤回")
    private String action;

    /** 操作前的状态 */
    @Excel(name = "操作前状态", dictType = "leave_request_status")
    private String fromStatus;

    /** 操作后的状态 */
    @Excel(name = "操作后状态", dictType = "leave_request_status")
    private String toStatus;

    /** 审批意见（比如驳回原因） */
    @Excel(name = "审批意见")
    private String comment;

    /** 审批人姓名，join sys_user.nick_name 得到 */
    @Excel(name = "审批人")
    private String approverName;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setLeaveId(Long leaveId) 
    {
        this.leaveId = leaveId;
    }

    public Long getLeaveId() 
    {
        return leaveId;
    }

    public void setApproverId(Long approverId) 
    {
        this.approverId = approverId;
    }

    public Long getApproverId() 
    {
        return approverId;
    }

    public void setAction(String action) 
    {
        this.action = action;
    }

    public String getAction() 
    {
        return action;
    }

    public void setFromStatus(String fromStatus) 
    {
        this.fromStatus = fromStatus;
    }

    public String getFromStatus() 
    {
        return fromStatus;
    }

    public void setToStatus(String toStatus) 
    {
        this.toStatus = toStatus;
    }

    public String getToStatus() 
    {
        return toStatus;
    }

    public void setComment(String comment) 
    {
        this.comment = comment;
    }

    public String getComment() 
    {
        return comment;
    }

    public void setApproverName(String approverName)
    {
        this.approverName = approverName;
    }

    public String getApproverName()
    {
        return approverName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("leaveId", getLeaveId())
            .append("approverId", getApproverId())
            .append("action", getAction())
            .append("fromStatus", getFromStatus())
            .append("toStatus", getToStatus())
            .append("comment", getComment())
            .append("createTime", getCreateTime())
            .toString();
    }
}
