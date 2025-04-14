package com.ruoyi.project.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.project.mapper.ProjectServiceCaseMapper;
import com.ruoyi.project.domain.ProjectServiceCase;
import com.ruoyi.project.service.IProjectServiceCaseService;

/**
 * 服务案例Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-04-13
 */
@Service
public class ProjectServiceCaseServiceImpl implements IProjectServiceCaseService 
{
    @Autowired
    private ProjectServiceCaseMapper projectServiceCaseMapper;

    /**
     * 查询服务案例
     * 
     * @param id 服务案例主键
     * @return 服务案例
     */
    @Override
    public ProjectServiceCase selectProjectServiceCaseById(Long id)
    {
        return projectServiceCaseMapper.selectProjectServiceCaseById(id);
    }

    /**
     * 查询服务案例列表
     * 
     * @param projectServiceCase 服务案例
     * @return 服务案例
     */
    @Override
    public List<ProjectServiceCase> selectProjectServiceCaseList(ProjectServiceCase projectServiceCase)
    {
        return projectServiceCaseMapper.selectProjectServiceCaseList(projectServiceCase);
    }

    /**
     * 新增服务案例
     * 
     * @param projectServiceCase 服务案例
     * @return 结果
     */
    @Override
    public int insertProjectServiceCase(ProjectServiceCase projectServiceCase)
    {
        projectServiceCase.setCreateTime(DateUtils.getNowDate());
        return projectServiceCaseMapper.insertProjectServiceCase(projectServiceCase);
    }

    /**
     * 修改服务案例
     * 
     * @param projectServiceCase 服务案例
     * @return 结果
     */
    @Override
    public int updateProjectServiceCase(ProjectServiceCase projectServiceCase)
    {
        projectServiceCase.setUpdateTime(DateUtils.getNowDate());
        return projectServiceCaseMapper.updateProjectServiceCase(projectServiceCase);
    }

    /**
     * 批量删除服务案例
     * 
     * @param ids 需要删除的服务案例主键
     * @return 结果
     */
    @Override
    public int deleteProjectServiceCaseByIds(Long[] ids)
    {
        return projectServiceCaseMapper.deleteProjectServiceCaseByIds(ids);
    }

    /**
     * 删除服务案例信息
     * 
     * @param id 服务案例主键
     * @return 结果
     */
    @Override
    public int deleteProjectServiceCaseById(Long id)
    {
        return projectServiceCaseMapper.deleteProjectServiceCaseById(id);
    }
}
