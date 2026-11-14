package com.ruoyi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.LeaveRequest;
import com.ruoyi.system.service.ILeaveRequestService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

import static com.ruoyi.common.utils.PageUtils.startPage;

/**
 * 请假申请Controller
 * 
 * @author ruoyi
 * @date 2026-10-30
 */
@RestController
@RequestMapping("/leave/leaveRequest")
public class LeaveRequestController extends BaseController
{
    @Autowired
    private ILeaveRequestService leaveRequestService;

    /**
     * 查询请假申请列表  -- 请假台账 （三个角色的数据权限不同，数据权限管的事该接口被调用后能拿到几行，
     *                            由 @DataScope 注解在 SQL 后面拼 ${params.dataScope} 实现。）
     */
    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:list')") // 三个角色的权限标识一样（权限标识只管这个接口谁能调用）
    @GetMapping("/list")
    public TableDataInfo list(LeaveRequest leaveRequest)
    {
        startPage();
        List<LeaveRequest> list = leaveRequestService.selectLeaveRequestList(leaveRequest);
        return getDataTable(list);
    }

    /**
     * 导出请假申请列表
     */
    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:export')")
    @Log(title = "请假申请", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LeaveRequest leaveRequest)
    {
        List<LeaveRequest> list = leaveRequestService.selectLeaveRequestList(leaveRequest);
        ExcelUtil<LeaveRequest> util = new ExcelUtil<LeaveRequest>(LeaveRequest.class);
        util.exportExcel(response, list, "请假申请数据");
    }

    /**
     * 获取请假申请详细信息
     */
    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(leaveRequestService.selectLeaveRequestById(id));
    }

    /**
     * 新增请假申请
     */
    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:add')")
    @Log(title = "请假申请", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LeaveRequest leaveRequest)
    {
        return toAjax(leaveRequestService.insertLeaveRequest(leaveRequest));
    }

    /**
     * 删除请假申请-----------------------------不需要-----------------
     */
//    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:remove')")
//    @Log(title = "请假申请", businessType = BusinessType.DELETE)
//	@DeleteMapping("/{ids}")
//    public AjaxResult remove(@PathVariable Long[] ids)
//    {
//        return toAjax(leaveRequestService.deleteLeaveRequestByIds(ids));
//    }

    /**
     * 撤回请假申请
     */
    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:cancel')")
    @Log(title = "请假申请",businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{id}")
    public AjaxResult cancel(@PathVariable Long id){
        return toAjax(leaveRequestService.cancelLeaveRequest(id));
    }

    /**
     * 批准请假申请
     * @param id
     * @return
     */
    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:approve')")
    @Log(title = "请假申请", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{id}")
    public AjaxResult approve(@PathVariable Long id, @RequestParam(required = false) String comment){
        return toAjax(leaveRequestService.approveLeaveRequest(id,comment));
    }

    /**
     * 驳回请假申请
     * @param id
     * @return
     */
    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:reject')")
    @Log(title = "请假申请", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{id}")
    public AjaxResult reject(@PathVariable Long id, @RequestParam(required = false) String comment){
        return toAjax(leaveRequestService.rejectLeaveRequest(id,comment));
    }

    /**
     * 分页查询自己的请假申请  -- 我的请假（员工、经理）
     * @param leaveRequest
     * @return
     */
    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:myList')")
    @GetMapping("/my")
    public TableDataInfo myList(LeaveRequest leaveRequest){
        startPage();
        List<LeaveRequest> list = leaveRequestService.selectMyLeaveRequestList(leaveRequest);
        return getDataTable(list);
    }

    /**
     * 分页查询待自己审批的请假申请  -- 代办审批
     */
    @PreAuthorize("@ss.hasPermi('leave:leaveRequest:pendingApproval')")
    @GetMapping("/pendingApproval")
    public TableDataInfo pendingApproval(LeaveRequest leaveRequest){
        startPage();
        List<LeaveRequest> list = leaveRequestService.selectPendingApprovalList(leaveRequest);
        return getDataTable(list);
    }

}
