package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 请假申请对象 leave_request
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
public class LeaveRequest extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 请假单ID */
    private Long id;

    /** 申请人的用户ID（关联sys_user） */
    private Long userId;

    /** 申请人所在部门ID（关联sys_dept，为了经理查数据权限） */
    private Long deptId;

    /** 请假类型（事假/病假/年假等字典值） */
    @Excel(name = "请假类型", dictType = "leave_request_type")
    private String leaveType;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    /** 请假时长（天），保留一位小数（半天算0.5） */
    @Excel(name = "请假时长(天)")
    private BigDecimal duration;

    /** 请假事由 */
    @Excel(name = "请假事由")
    private String reason;

    /** 附件OSS链接（病假条照片等） */
    @Excel(name = "附件")
    private String attachmentUrl;

    /** 状态：PENDING-待经理审批，MANAGER_APPROVED-待总监审批，APPROVED-已通过，REJECTED-已驳回，CANCELLED-已撤回 */
    @Excel(name = "审批状态", dictType = "leave_request_status")
    private String status;

    /** 开始时间上午/下午 */
    @Excel(name = "开始半天",readConverterExp = "AM=上午,PM=下午")
    private String startHalf;

    @Excel(name = "结束半天",readConverterExp = "AM=上午,PM=下午")
    private String endHalf;

    /** 申请人姓名，join sys_user.nick_name 得到 */
    @Excel(name = "申请人")
    private String userName;

    /** 部门名称，join sys_dept.dept_name 得到 */
    @Excel(name = "所属部门")
    private String deptName;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    public void setLeaveType(String leaveType) 
    {
        this.leaveType = leaveType;
    }

    public String getLeaveType() 
    {
        return leaveType;
    }

    public void setStartTime(Date startTime) 
    {
        this.startTime = startTime;
    }

    public Date getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(Date endTime) 
    {
        this.endTime = endTime;
    }

    public Date getEndTime() 
    {
        return endTime;
    }

    public void setDuration(BigDecimal duration) 
    {
        this.duration = duration;
    }

    public BigDecimal getDuration() 
    {
        return duration;
    }

    public void setReason(String reason) 
    {
        this.reason = reason;
    }

    public String getReason() 
    {
        return reason;
    }

    public void setAttachmentUrl(String attachmentUrl) 
    {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentUrl() 
    {
        return attachmentUrl;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStartHalf(String startHalf)
    {
        this.startHalf = startHalf;
    }

    public String getStartHalf()
    {
        return startHalf;
    }

    public void setEndHalf(String endHalf)
    {
        this.endHalf = endHalf;
    }

    public String getEndHalf()
    {
        return endHalf;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("deptId", getDeptId())
            .append("leaveType", getLeaveType())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("duration", getDuration())
            .append("reason", getReason())
            .append("attachmentUrl", getAttachmentUrl())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
