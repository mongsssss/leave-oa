package com.ruoyi.system.domain.vo;


import com.ruoyi.system.domain.ApprovalRecord;
import com.ruoyi.system.domain.LeaveRequest;

import java.util.List;

public class LeaveRequestVo extends LeaveRequest {
    private static final long serialVersionUID = 1L;

    private List<ApprovalRecord> approvalRecordsList;

    public List<ApprovalRecord> getApprovalRecordsList() {
        return approvalRecordsList;
    }

    public void setApprovalRecordsList(List<ApprovalRecord> approvalRecordslist){
        this.approvalRecordsList = approvalRecordslist;
    }
}
