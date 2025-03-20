package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目区域对象 project_region
 * 
 * @author ruoyi
 * @date 2025-03-20
 */
public class ProjectRegion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 区域ID */
    private Long id;

    /** 区域名称 */
    @Excel(name = "区域名称")
    private String regionName;

    /** 区域描述 */
    @Excel(name = "区域描述")
    private String description;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setRegionName(String regionName) 
    {
        this.regionName = regionName;
    }

    public String getRegionName() 
    {
        return regionName;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("regionName", getRegionName())
            .append("description", getDescription())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .toString();
    }
}
