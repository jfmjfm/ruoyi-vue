package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.ProjectRegionMapper;
import com.ruoyi.project.domain.ProjectRegion;
import com.ruoyi.project.service.IProjectRegionService;

/**
 * 项目区域Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-03-20
 */
@Service
public class ProjectRegionServiceImpl implements IProjectRegionService 
{
    @Autowired
    private ProjectRegionMapper projectRegionMapper;

    /**
     * 查询项目区域
     * 
     * @param id 项目区域主键
     * @return 项目区域
     */
    @Override
    public ProjectRegion selectProjectRegionById(Long id)
    {
        return projectRegionMapper.selectProjectRegionById(id);
    }

    /**
     * 查询项目区域列表
     * 
     * @param projectRegion 项目区域
     * @return 项目区域
     */
    @Override
    public List<ProjectRegion> selectProjectRegionList(ProjectRegion projectRegion)
    {
        return projectRegionMapper.selectProjectRegionList(projectRegion);
    }

    /**
     * 新增项目区域
     * 
     * @param projectRegion 项目区域
     * @return 结果
     */
    @Override
    public int insertProjectRegion(ProjectRegion projectRegion)
    {
        projectRegion.setCreateTime(DateUtils.getNowDate());
        return projectRegionMapper.insertProjectRegion(projectRegion);
    }

    /**
     * 修改项目区域
     * 
     * @param projectRegion 项目区域
     * @return 结果
     */
    @Override
    public int updateProjectRegion(ProjectRegion projectRegion)
    {
        projectRegion.setUpdateTime(DateUtils.getNowDate());
        return projectRegionMapper.updateProjectRegion(projectRegion);
    }

    /**
     * 批量删除项目区域
     * 
     * @param ids 需要删除的项目区域主键
     * @return 结果
     */
    @Override
    public int deleteProjectRegionByIds(Long[] ids)
    {
        return projectRegionMapper.deleteProjectRegionByIds(ids);
    }

    /**
     * 删除项目区域信息
     * 
     * @param id 项目区域主键
     * @return 结果
     */
    @Override
    public int deleteProjectRegionById(Long id)
    {
        return projectRegionMapper.deleteProjectRegionById(id);
    }
}
