package com.ruoyi.project.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 服务案例对象 project_service_case
 * 
 * @author ruoyi
 * @date 2025-04-13
 */
public class ProjectServiceCase extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 案例ID */
    private Long id;

    /** 区域服务关联ID */
    @Excel(name = "区域服务关联ID")
    private Long regionServiceId;

    /** 案例名称 */
    @Excel(name = "案例名称")
    private String caseName;

    /**  */
    @Excel(name = "保存路径")
    private String caseDir;

    /** 是否默认案例(0否 1是) */
    @Excel(name = "是否默认案例(0否 1是)")
    private Integer isDefault;

    /** 案例参数 */
    @Excel(name = "案例参数")
    private String validparam;

    /** 案例描述 */
    @Excel(name = "案例描述")
    private String description;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setRegionServiceId(Long regionServiceId) 
    {
        this.regionServiceId = regionServiceId;
    }

    public Long getRegionServiceId() 
    {
        return regionServiceId;
    }

    public void setCaseName(String caseName) 
    {
        this.caseName = caseName;
    }

    public String getCaseName() 
    {
        return caseName;
    }

    public void setCaseDir(String caseDir) 
    {
        this.caseDir = caseDir;
    }

    public String getCaseDir() 
    {
        return caseDir;
    }

    public void setIsDefault(Integer isDefault) 
    {
        this.isDefault = isDefault;
    }

    public Integer getIsDefault() 
    {
        return isDefault;
    }

    public void setValidparam(String validparam) 
    {
        this.validparam = validparam;
    }

    public String getValidparam() 
    {
        return validparam;
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
            .append("regionServiceId", getRegionServiceId())
            .append("caseName", getCaseName())
            .append("caseDir", getCaseDir())
            .append("isDefault", getIsDefault())
            .append("validparam", getValidparam())
            .append("description", getDescription())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("createBy", getCreateBy())
            .append("updateBy", getUpdateBy())
            .toString();
    }
}
