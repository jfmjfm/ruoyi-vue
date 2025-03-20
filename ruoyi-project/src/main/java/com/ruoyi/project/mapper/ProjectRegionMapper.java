package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.ProjectRegion;

/**
 * 项目区域Mapper接口
 * 
 * @author ruoyi
 * @date 2025-03-20
 */
public interface ProjectRegionMapper 
{
    /**
     * 查询项目区域
     * 
     * @param id 项目区域主键
     * @return 项目区域
     */
    public ProjectRegion selectProjectRegionById(Long id);

    /**
     * 查询项目区域列表
     * 
     * @param projectRegion 项目区域
     * @return 项目区域集合
     */
    public List<ProjectRegion> selectProjectRegionList(ProjectRegion projectRegion);

    /**
     * 新增项目区域
     * 
     * @param projectRegion 项目区域
     * @return 结果
     */
    public int insertProjectRegion(ProjectRegion projectRegion);

    /**
     * 修改项目区域
     * 
     * @param projectRegion 项目区域
     * @return 结果
     */
    public int updateProjectRegion(ProjectRegion projectRegion);

    /**
     * 删除项目区域
     * 
     * @param id 项目区域主键
     * @return 结果
     */
    public int deleteProjectRegionById(Long id);

    /**
     * 批量删除项目区域
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProjectRegionByIds(Long[] ids);
}
