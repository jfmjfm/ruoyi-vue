package com.ruoyi.project.service;

import java.util.List;
import com.ruoyi.project.domain.ProjectRegionService;

/**
 * 项目区域-服务类型关联Service接口
 * 
 * @author ruoyi
 * @date 2025-03-20
 */
public interface IProjectRegionServiceService 
{
    /**
     * 查询项目区域-服务类型关联
     * 
     * @param id 项目区域-服务类型关联主键
     * @return 项目区域-服务类型关联
     */
    public ProjectRegionService selectProjectRegionServiceById(Long id);

    /**
     * 查询项目区域-服务类型关联列表
     * 
     * @param projectRegionService 项目区域-服务类型关联
     * @return 项目区域-服务类型关联集合
     */
    public List<ProjectRegionService> selectProjectRegionServiceList(ProjectRegionService projectRegionService);

    /**
     * 新增项目区域-服务类型关联
     * 
     * @param projectRegionService 项目区域-服务类型关联
     * @return 结果
     */
    public int insertProjectRegionService(ProjectRegionService projectRegionService);

    /**
     * 修改项目区域-服务类型关联
     * 
     * @param projectRegionService 项目区域-服务类型关联
     * @return 结果
     */
    public int updateProjectRegionService(ProjectRegionService projectRegionService);

    /**
     * 批量删除项目区域-服务类型关联
     * 
     * @param ids 需要删除的项目区域-服务类型关联主键集合
     * @return 结果
     */
    public int deleteProjectRegionServiceByIds(Long[] ids);

    /**
     * 删除项目区域-服务类型关联信息
     * 
     * @param id 项目区域-服务类型关联主键
     * @return 结果
     */
    public int deleteProjectRegionServiceById(Long id);
}
