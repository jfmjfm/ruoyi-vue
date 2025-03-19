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
import com.ruoyi.project.domain.TestStudent;
import com.ruoyi.project.service.ITestStudentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 学生信息Controller
 * 
 * @author ruoyi
 * @date 2025-03-17
 */
@RestController
@RequestMapping("/project/student")
public class TestStudentController extends BaseController
{
    @Autowired
    private ITestStudentService testStudentService;

    /**
     * 查询学生信息列表
     */
    @PreAuthorize("@ss.hasPermi('project:student:list')")
    @GetMapping("/list")
    public TableDataInfo list(TestStudent testStudent)
    {
        startPage();
        List<TestStudent> list = testStudentService.selectTestStudentList(testStudent);
        return getDataTable(list);
    }

    /**
     * 导出学生信息列表
     */
    @PreAuthorize("@ss.hasPermi('project:student:export')")
    @Log(title = "学生信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TestStudent testStudent)
    {
        List<TestStudent> list = testStudentService.selectTestStudentList(testStudent);
        ExcelUtil<TestStudent> util = new ExcelUtil<TestStudent>(TestStudent.class);
        util.exportExcel(response, list, "学生信息数据");
    }

    /**
     * 获取学生信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('project:student:query')")
    @GetMapping(value = "/{studentId}")
    public AjaxResult getInfo(@PathVariable("studentId") Long studentId)
    {
        return success(testStudentService.selectTestStudentByStudentId(studentId));
    }

    /**
     * 新增学生信息
     */
    @PreAuthorize("@ss.hasPermi('project:student:add')")
    @Log(title = "学生信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TestStudent testStudent)
    {
        return toAjax(testStudentService.insertTestStudent(testStudent));
    }

    /**
     * 修改学生信息
     */
    @PreAuthorize("@ss.hasPermi('project:student:edit')")
    @Log(title = "学生信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TestStudent testStudent)
    {
        return toAjax(testStudentService.updateTestStudent(testStudent));
    }

    /**
     * 删除学生信息
     */
    @PreAuthorize("@ss.hasPermi('project:student:remove')")
    @Log(title = "学生信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{studentIds}")
    public AjaxResult remove(@PathVariable Long[] studentIds)
    {
        return toAjax(testStudentService.deleteTestStudentByStudentIds(studentIds));
    }
}
