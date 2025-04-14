package com.ruoyi.project.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.project.domain.ProjectServiceCase;
import com.ruoyi.project.mapper.ProjectRegionServiceMapper;
import com.ruoyi.project.domain.ProjectRegionService;
import com.ruoyi.project.service.IProjectRegionServiceService;

/**
 * 项目区域-服务类型关联Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-04-13
 */
@Service
public class ProjectRegionServiceServiceImpl implements IProjectRegionServiceService 
{
    @Autowired
    private ProjectRegionServiceMapper projectRegionServiceMapper;

    /**
     * 查询项目区域-服务类型关联
     * 
     * @param id 项目区域-服务类型关联主键
     * @return 项目区域-服务类型关联
     */
    @Override
    public ProjectRegionService selectProjectRegionServiceById(Long id)
    {
        return projectRegionServiceMapper.selectProjectRegionServiceById(id);
    }

    /**
     * 查询项目区域-服务类型关联列表
     * 
     * @param projectRegionService 项目区域-服务类型关联
     * @return 项目区域-服务类型关联
     */
    @Override
    public List<ProjectRegionService> selectProjectRegionServiceList(ProjectRegionService projectRegionService)
    {
        return projectRegionServiceMapper.selectProjectRegionServiceList(projectRegionService);
    }

    /**
     * 新增项目区域-服务类型关联
     * 
     * @param projectRegionService 项目区域-服务类型关联
     * @return 结果
     */
    @Transactional
    @Override
    public int insertProjectRegionService(ProjectRegionService projectRegionService)
    {
        int rows = projectRegionServiceMapper.insertProjectRegionService(projectRegionService);
        insertProjectServiceCase(projectRegionService);
        return rows;
    }

    /**
     * 修改项目区域-服务类型关联
     * 
     * @param projectRegionService 项目区域-服务类型关联
     * @return 结果
     */
    @Transactional
    @Override
    public int updateProjectRegionService(ProjectRegionService projectRegionService)
    {
        projectRegionServiceMapper.deleteProjectServiceCaseByRegionServiceId(projectRegionService.getId());
        insertProjectServiceCase(projectRegionService);
        return projectRegionServiceMapper.updateProjectRegionService(projectRegionService);
    }

    /**
     * 批量删除项目区域-服务类型关联
     * 
     * @param ids 需要删除的项目区域-服务类型关联主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteProjectRegionServiceByIds(Long[] ids)
    {
        projectRegionServiceMapper.deleteProjectServiceCaseByRegionServiceIds(ids);
        return projectRegionServiceMapper.deleteProjectRegionServiceByIds(ids);
    }

    /**
     * 删除项目区域-服务类型关联信息
     * 
     * @param id 项目区域-服务类型关联主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteProjectRegionServiceById(Long id)
    {
        projectRegionServiceMapper.deleteProjectServiceCaseByRegionServiceId(id);
        return projectRegionServiceMapper.deleteProjectRegionServiceById(id);
    }

    /**
     * 新增服务案例信息
     * 
     * @param projectRegionService 项目区域-服务类型关联对象
     */
    public void insertProjectServiceCase(ProjectRegionService projectRegionService)
    {
        List<ProjectServiceCase> projectServiceCaseList = projectRegionService.getProjectServiceCaseList();
        Long id = projectRegionService.getId();
        if (StringUtils.isNotNull(projectServiceCaseList))
        {
            List<ProjectServiceCase> list = new ArrayList<ProjectServiceCase>();
            for (ProjectServiceCase projectServiceCase : projectServiceCaseList)
            {
                projectServiceCase.setRegionServiceId(id);
                list.add(projectServiceCase);
            }
            if (list.size() > 0)
            {
                projectRegionServiceMapper.batchProjectServiceCase(list);
            }
        }
    }
}
