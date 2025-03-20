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
import com.ruoyi.project.domain.ProjectRegion;
import com.ruoyi.project.service.IProjectRegionService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 项目区域Controller
 * 
 * @author ruoyi
 * @date 2025-03-20
 */
@RestController
@RequestMapping("/project/project_region")
public class ProjectRegionController extends BaseController
{
    @Autowired
    private IProjectRegionService projectRegionService;

    /**
     * 查询项目区域列表
     */
    @PreAuthorize("@ss.hasPermi('project:project_region:list')")
    @GetMapping("/list")
    public TableDataInfo list(ProjectRegion projectRegion)
    {
        startPage();
        List<ProjectRegion> list = projectRegionService.selectProjectRegionList(projectRegion);
        return getDataTable(list);
    }

    /**
     * 导出项目区域列表
     */
    @PreAuthorize("@ss.hasPermi('project:project_region:export')")
    @Log(title = "项目区域", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ProjectRegion projectRegion)
    {
        List<ProjectRegion> list = projectRegionService.selectProjectRegionList(projectRegion);
        ExcelUtil<ProjectRegion> util = new ExcelUtil<ProjectRegion>(ProjectRegion.class);
        util.exportExcel(response, list, "项目区域数据");
    }

    /**
     * 获取项目区域详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:project_region:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(projectRegionService.selectProjectRegionById(id));
    }

    /**
     * 新增项目区域
     */
    @PreAuthorize("@ss.hasPermi('project:project_region:add')")
    @Log(title = "项目区域", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProjectRegion projectRegion)
    {
        return toAjax(projectRegionService.insertProjectRegion(projectRegion));
    }

    /**
     * 修改项目区域
     */
    @PreAuthorize("@ss.hasPermi('project:project_region:edit')")
    @Log(title = "项目区域", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProjectRegion projectRegion)
    {
        return toAjax(projectRegionService.updateProjectRegion(projectRegion));
    }

    /**
     * 删除项目区域
     */
    @PreAuthorize("@ss.hasPermi('project:project_region:remove')")
    @Log(title = "项目区域", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(projectRegionService.deleteProjectRegionByIds(ids));
    }
}
