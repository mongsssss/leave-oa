package com.ruoyi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.ApprovalRecord;
import com.ruoyi.system.service.IApprovalRecordService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 审批记录Controller
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
@RestController
@RequestMapping("/leave/record")
public class ApprovalRecordController extends BaseController
{
    @Autowired
    private IApprovalRecordService approvalRecordService;

    /**
     * 查询审批记录列表 -- 审批流水（只有总监和管理员可查）
     */
    @PreAuthorize("@ss.hasPermi('leave:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApprovalRecord approvalRecord)
    {
        startPage();
        List<ApprovalRecord> list = approvalRecordService.selectApprovalRecordList(approvalRecord);
        return getDataTable(list);
    }

    /**
     * 导出审批记录列表
     */
    @PreAuthorize("@ss.hasPermi('leave:record:export')")
    @Log(title = "审批记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ApprovalRecord approvalRecord)
    {
        List<ApprovalRecord> list = approvalRecordService.selectApprovalRecordList(approvalRecord);
        ExcelUtil<ApprovalRecord> util = new ExcelUtil<ApprovalRecord>(ApprovalRecord.class);
        util.exportExcel(response, list, "审批记录数据");
    }

    /**
     * 获取审批记录详细信息-------------------------------多余，目前用不上-----------
     */
//    @PreAuthorize("@ss.hasPermi('leave:record:query')")
//    @GetMapping(value = "/{id}")
//    public AjaxResult getInfo(@PathVariable("id") Long id)
//    {
//        return success(approvalRecordService.selectApprovalRecordById(id));
//    }

    /**
     * 新增审批记录------------------------------------多余，目前用不上--------------
     */
//    @PreAuthorize("@ss.hasPermi('leave:record:add')")
//    @Log(title = "审批记录", businessType = BusinessType.INSERT)
//    @PostMapping
//    public AjaxResult add(@RequestBody ApprovalRecord approvalRecord)
//    {
//        return toAjax(approvalRecordService.insertApprovalRecord(approvalRecord));
//    }

    /**
     * 修改审批记录---------------------------------------多余，目前用不上--------------
     */
//    @PreAuthorize("@ss.hasPermi('leave:record:edit')")
//    @Log(title = "审批记录", businessType = BusinessType.UPDATE)
//    @PutMapping
//    public AjaxResult edit(@RequestBody ApprovalRecord approvalRecord)
//    {
//        return toAjax(approvalRecordService.updateApprovalRecord(approvalRecord));
//    }

    /**
     * 删除审批记录---------------------------------------多余，目前用不上--------------
     */
//    @PreAuthorize("@ss.hasPermi('leave:record:remove')")
//    @Log(title = "审批记录", businessType = BusinessType.DELETE)
//	@DeleteMapping("/{ids}")
//    public AjaxResult remove(@PathVariable Long[] ids)
//    {
//        return toAjax(approvalRecordService.deleteApprovalRecordByIds(ids));
//    }

    /**
     * 查询我已审批的请假记录列表  -- 我已审批 （只包含通过和驳回两个行为）
     */
    @PreAuthorize("@ss.hasPermi('leave:record:myList')")
    @GetMapping("/myList")
    public TableDataInfo myList(ApprovalRecord approvalRecord){
        startPage();
        List<ApprovalRecord> list = approvalRecordService.selectMyApprovalRecordList(approvalRecord);
        return getDataTable(list);
    }
}
