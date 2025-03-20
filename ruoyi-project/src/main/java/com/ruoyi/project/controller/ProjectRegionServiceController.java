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
import com.ruoyi.project.domain.ProjectRegionService;
import com.ruoyi.project.service.IProjectRegionServiceService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目区域-服务类型关联Controller
 * 
 * @author ruoyi
 * @date 2025-03-20
 */
@RestController
@RequestMapping("/project/project_region_service")
public class ProjectRegionServiceController extends BaseController
{
    @Autowired
    private IProjectRegionServiceService projectRegionServiceService;

    /**
     * 查询项目区域-服务类型关联列表
     */
    @PreAuthorize("@ss.hasPermi('project:project_region_service:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProjectRegionService projectRegionService)
    {
        startPage();
        List<ProjectRegionService> list = projectRegionServiceService.selectProjectRegionServiceList(projectRegionService);
        return getDataTable(list);
    }

    /**
     * 导出项目区域-服务类型关联列表
     */
    @PreAuthorize("@ss.hasPermi('project:project_region_service:export')")
    @Log(title = "项目区域-服务类型关联", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProjectRegionService projectRegionService)
    {
        List<ProjectRegionService> list = projectRegionServiceService.selectProjectRegionServiceList(projectRegionService);
        ExcelUtil<ProjectRegionService> util = new ExcelUtil<ProjectRegionService>(ProjectRegionService.class);
        util.exportExcel(response, list, "项目区域-服务类型关联数据");
    }

    /**
     * 获取项目区域-服务类型关联详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:project_region_service:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(projectRegionServiceService.selectProjectRegionServiceById(id));
    }

    /**
     * 新增项目区域-服务类型关联
     */
    @PreAuthorize("@ss.hasPermi('project:project_region_service:add')")
    @Log(title = "项目区域-服务类型关联", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProjectRegionService projectRegionService)
    {
        return toAjax(projectRegionServiceService.insertProjectRegionService(projectRegionService));
    }

    /**
     * 修改项目区域-服务类型关联
     */
    @PreAuthorize("@ss.hasPermi('project:project_region_service:edit')")
    @Log(title = "项目区域-服务类型关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProjectRegionService projectRegionService)
    {
        return toAjax(projectRegionServiceService.updateProjectRegionService(projectRegionService));
    }

    /**
     * 删除项目区域-服务类型关联
     */
    @PreAuthorize("@ss.hasPermi('project:project_region_service:remove')")
    @Log(title = "项目区域-服务类型关联", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(projectRegionServiceService.deleteProjectRegionServiceByIds(ids));
    }
}
