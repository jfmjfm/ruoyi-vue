package com.ruoyi.project.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.ProjectRegionServiceMapper;
import com.ruoyi.project.domain.ProjectRegionService;
import com.ruoyi.project.service.IProjectRegionServiceService;

/**
 * 项目区域-服务类型关联Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-03-20
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
    @Override
    public int insertProjectRegionService(ProjectRegionService projectRegionService)
    {
        return projectRegionServiceMapper.insertProjectRegionService(projectRegionService);
    }

    /**
     * 修改项目区域-服务类型关联
     * 
     * @param projectRegionService 项目区域-服务类型关联
     * @return 结果
     */
    @Override
    public int updateProjectRegionService(ProjectRegionService projectRegionService)
    {
        return projectRegionServiceMapper.updateProjectRegionService(projectRegionService);
    }

    /**
     * 批量删除项目区域-服务类型关联
     * 
     * @param ids 需要删除的项目区域-服务类型关联主键
     * @return 结果
     */
    @Override
    public int deleteProjectRegionServiceByIds(Long[] ids)
    {
        return projectRegionServiceMapper.deleteProjectRegionServiceByIds(ids);
    }

    /**
     * 删除项目区域-服务类型关联信息
     * 
     * @param id 项目区域-服务类型关联主键
     * @return 结果
     */
    @Override
    public int deleteProjectRegionServiceById(Long id)
    {
        return projectRegionServiceMapper.deleteProjectRegionServiceById(id);
    }
}
