package com.ruoyi.project.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
import com.ruoyi.project.domain.ProjectServiceCase;
import com.ruoyi.project.service.IProjectServiceCaseService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 服务案例Controller
 * 
 * @author ruoyi
 * @date 2025-04-13
 */
@RestController
@RequestMapping("/project/project_service_case")
public class ProjectServiceCaseController extends BaseController
{
    @Autowired
    private IProjectServiceCaseService projectServiceCaseService;

    /**
     * 查询服务案例列表
     */
    @PreAuthorize("@ss.hasPermi('project:project_service_case:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProjectServiceCase projectServiceCase)
    {
        startPage();
        List<ProjectServiceCase> list = projectServiceCaseService.selectProjectServiceCaseList(projectServiceCase);
        return getDataTable(list);
    }

    /**
     * 导出服务案例列表
     */
    @PreAuthorize("@ss.hasPermi('project:project_service_case:export')")
    @Log(title = "服务案例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProjectServiceCase projectServiceCase)
    {
        List<ProjectServiceCase> list = projectServiceCaseService.selectProjectServiceCaseList(projectServiceCase);
        ExcelUtil<ProjectServiceCase> util = new ExcelUtil<ProjectServiceCase>(ProjectServiceCase.class);
        util.exportExcel(response, list, "服务案例数据");
    }

    /**
     * 获取服务案例详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:project_service_case:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(projectServiceCaseService.selectProjectServiceCaseById(id));
    }

    /**
     * 新增服务案例
     */
    @PreAuthorize("@ss.hasPermi('project:project_service_case:add')")
    @Log(title = "服务案例", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProjectServiceCase projectServiceCase)
    {
        return toAjax(projectServiceCaseService.insertProjectServiceCase(projectServiceCase));
    }

    /**
     * 修改服务案例
     */
    @PreAuthorize("@ss.hasPermi('project:project_service_case:edit')")
    @Log(title = "服务案例", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProjectServiceCase projectServiceCase)
    {
        return toAjax(projectServiceCaseService.updateProjectServiceCase(projectServiceCase));
    }

    /**
     * 删除服务案例
     */
    @PreAuthorize("@ss.hasPermi('project:project_service_case:remove')")
    @Log(title = "服务案例", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(projectServiceCaseService.deleteProjectServiceCaseByIds(ids));
    }
}
