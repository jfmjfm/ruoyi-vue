package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.ProjectServiceCase;

/**
 * 服务案例Mapper接口
 * 
 * @author ruoyi
 * @date 2025-04-13
 */
public interface ProjectServiceCaseMapper 
{
    /**
     * 查询服务案例
     * 
     * @param id 服务案例主键
     * @return 服务案例
     */
    public ProjectServiceCase selectProjectServiceCaseById(Long id);

    /**
     * 查询服务案例列表
     * 
     * @param projectServiceCase 服务案例
     * @return 服务案例集合
     */
    public List<ProjectServiceCase> selectProjectServiceCaseList(ProjectServiceCase projectServiceCase);

    /**
     * 新增服务案例
     * 
     * @param projectServiceCase 服务案例
     * @return 结果
     */
    public int insertProjectServiceCase(ProjectServiceCase projectServiceCase);

    /**
     * 修改服务案例
     * 
     * @param projectServiceCase 服务案例
     * @return 结果
     */
    public int updateProjectServiceCase(ProjectServiceCase projectServiceCase);

    /**
     * 删除服务案例
     * 
     * @param id 服务案例主键
     * @return 结果
     */
    public int deleteProjectServiceCaseById(Long id);

    /**
     * 批量删除服务案例
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProjectServiceCaseByIds(Long[] ids);
}
