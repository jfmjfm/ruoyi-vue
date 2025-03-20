package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目区域-服务类型关联对象 project_region_service
 * 
 * @author ruoyi
 * @date 2025-03-20
 */
public class ProjectRegionService extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** 区域ID */
    @Excel(name = "区域ID")
    private Long regionId;

    /** 服务类型 */
    @Excel(name = "服务类型")
    private String serviceType;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setRegionId(Long regionId) 
    {
        this.regionId = regionId;
    }

    public Long getRegionId() 
    {
        return regionId;
    }

    public void setServiceType(String serviceType) 
    {
        this.serviceType = serviceType;
    }

    public String getServiceType() 
    {
        return serviceType;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("regionId", getRegionId())
            .append("serviceType", getServiceType())
            .toString();
    }
}
