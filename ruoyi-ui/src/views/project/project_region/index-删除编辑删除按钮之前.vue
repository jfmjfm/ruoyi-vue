<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="区域名称" prop="regionName">
        <el-input
          v-model="queryParams.regionName"
          placeholder="请输入区域名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['project:project_region:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['project:project_region:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['project:project_region:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['project:project_region:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="project_regionList" @selection-change="handleSelectionChange" class="project-table">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="区域名称" prop="regionName" width="200" align="center" />
      <el-table-column label="服务类型" min-width="500">
        <template slot-scope="scope">
          <div class="service-button-container">
            <el-button
              v-for="(item, index) in scope.row.description.split('、')"
              :key="index"
              size="mini"
              type="primary"
              plain
              class="service-tag-in-list"
            >{{ item }}</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['project:project_region:view']"
          >查看</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['project:project_region:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['project:project_region:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
      class="project-pagination"
    />

    <!-- 添加或修改项目区域对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="区域名称" prop="regionName">
          <el-input v-model="form.regionName" placeholder="请输入区域名称" />
        </el-form-item>
        <el-form-item label="服务类型">
          <div class="service-type-container">
            <div v-for="item in serviceTypeOptions" :key="item.dictValue" class="service-type-item">
              <el-checkbox 
                v-model="selectedServiceTypes[item.dictValue]" 
                @change="handleServiceTypeChange"
              ></el-checkbox>
              <el-button 
                size="small" 
                :type="selectedServiceTypes[item.dictValue] ? 'primary' : ''" 
                :plain="!selectedServiceTypes[item.dictValue]"
                @click="toggleServiceType(item.dictValue)"
                class="service-type-button"
              >{{ item.dictLabel }}</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="已选服务">
          <div class="selected-services">
            <el-tag
              v-for="(service, index) in selectedServices"
              :key="index"
              type="info"
              effect="plain"
              class="selected-service-tag"
            >
              {{ service }}
            </el-tag>
            <div v-if="selectedServices.length === 0" class="no-service-selected">请选择服务类型</div>
          </div>
          <div v-if="serviceValidationError" class="error-message">请至少选择一个服务类型</div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProject_region, getProject_region, delProject_region, addProject_region, updateProject_region } from "@/api/project/project_region";

/**
 * 项目区域管理
 * 
 * 支持项目区域-服务类型-应用案例的多层级关联关系：
 * 1. 项目区域(project_region)与服务类型(project_region_service)是一对多关系
 * 2. 服务类型(project_region_service)与应用案例(project_service_case)是一对多关系
 * 
 * 数据处理流程：
 * - 前端提交表单时，生成projectRegionServiceList数组，包含serviceType值
 * - 新增时不传regionId，由后端自动关联新增的project_region记录ID
 * - 修改时传递regionId，后端应先删除原关联记录，再插入新记录
 * - 保留description字段以兼容现有逻辑
 * 
 * 后端处理建议：
 * 1. 接收projectRegionServiceList数组
 * 2. 新增时，先保存project_region，再用新ID关联创建project_region_service记录
 * 3. 修改时，先根据regionId删除原有project_region_service记录，再创建新记录
 */

export default {
  name: "Project_region",
  dicts: ['sys_service_type'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 项目区域表格数据
      project_regionList: [],
      // 服务类型选项
      serviceTypeOptions: [],
      // 选中的服务类型
      selectedServiceTypes: {},
      // 已选服务
      selectedServices: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        regionName: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        regionName: [
          { required: true, message: "区域名称不能为空", trigger: "blur" }
        ]
      },
      serviceValidationError: false,
    };
  },
  created() {
    this.getList();
    // Load dictionary data for service types
    this.getDicts("sys_service_type").then(response => {
      this.serviceTypeOptions = response.data;
      console.log("Service type options loaded:", this.serviceTypeOptions);
    }).catch(error => {
      console.error("Failed to load service type options:", error);
    });
  },
  methods: {
    /** 查询项目区域列表 */
    getList() {
      this.loading = true;
      listProject_region(this.queryParams).then(response => {
        this.project_regionList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        regionName: null,
        description: null,
        createTime: null,
        updateTime: null,
        createBy: null,
        updateBy: null
      };
      this.selectedServiceTypes = {};
      this.selectedServices = [];
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 服务类型选择变化处理 */
    handleServiceTypeChange() {
      const selectedServices = [];
      for (const key in this.selectedServiceTypes) {
        if (this.selectedServiceTypes[key]) {
          const found = this.serviceTypeOptions.find(item => item.dictValue === key);
          if (found) {
            selectedServices.push(found.dictLabel);
          }
        }
      }
      this.selectedServices = selectedServices;
      this.form.description = selectedServices.join("、");
      
      // Clear validation error if services are selected
      if (selectedServices.length > 0) {
        this.serviceValidationError = false;
      }
    },
    /** 点击按钮切换服务类型选择状态 */
    toggleServiceType(dictValue) {
      this.$set(this.selectedServiceTypes, dictValue, !this.selectedServiceTypes[dictValue]);
      this.handleServiceTypeChange();
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加项目区域";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getProject_region(id).then(response => {
        this.form = response.data;
        // 处理服务类型复选框选中状态
        if (this.form.description) {
          const services = this.form.description.split("、");
          this.selectedServices = services;
          services.forEach(service => {
            const found = this.serviceTypeOptions.find(item => item.dictLabel === service);
            if (found) {
              this.$set(this.selectedServiceTypes, found.dictValue, true);
            }
          });
        }
        
        // 如果后端返回了projectRegionServiceList，也使用它来设置选中状态
        if (this.form.projectRegionServiceList && this.form.projectRegionServiceList.length > 0) {
          this.form.projectRegionServiceList.forEach(service => {
            if (service.serviceType) {
              this.$set(this.selectedServiceTypes, service.serviceType, true);
              
              // 确保selectedServices也正确设置
              const found = this.serviceTypeOptions.find(item => item.dictValue === service.serviceType);
              if (found && !this.selectedServices.includes(found.dictLabel)) {
                this.selectedServices.push(found.dictLabel);
              }
            }
          });
          
          // 更新description以保持一致性
          this.form.description = this.selectedServices.join("、");
        }
        
        this.open = true;
        this.title = "修改项目区域";
      });
    },
    /** 查看按钮操作-跳转到GIS页面 */
    handleView(row) {
      this.$router.push({ 
        path: '/project/prjGIS',
        query: { regionId: row.id }
      });
    },
    /** 准备服务类型数据 */
    prepareServiceData() {
      // 构建服务类型关联数据
      const projectRegionServiceList = [];
      const selectedServices = [];

      for (const [key, isSelected] of Object.entries(this.selectedServiceTypes)) {
        if (isSelected) {
          const serviceType = this.serviceTypeOptions.find(item => item.dictValue === key);
          if (serviceType) {
            // 添加到关联列表
            projectRegionServiceList.push({
              serviceType: key,
              regionId: this.form.id // 仅在编辑时包含regionId
            });
            // 添加到描述列表
            selectedServices.push(serviceType.dictLabel);
          }
        }
      }

      return {
        projectRegionServiceList,
        description: selectedServices.join("、")
      };
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (!valid) {
          return;
        }

        // 验证是否选择了服务类型
        if (Object.values(this.selectedServiceTypes).filter(Boolean).length === 0) {
          this.serviceValidationError = true;
          return;
        }
        this.serviceValidationError = false;

        // 准备表单数据
        const { projectRegionServiceList, description } = this.prepareServiceData();
        const formData = {
          ...this.form,
          projectRegionServiceList,
          description
        };

        // 记录调试信息
        console.log("提交的表单数据:", {
          formType: this.form.id ? "更新" : "新增",
          data: formData
        });

        // 提交表单
        const request = this.form.id ? 
          updateProject_region(formData) : 
          addProject_region(formData);

        request
          .then(response => {
            this.$modal.msgSuccess(`${this.form.id ? "修改" : "新增"}成功`);
            this.open = false;
            this.getList();
          })
          .catch(error => {
            console.error(`${this.form.id ? "更新" : "添加"}失败:`, error);
            this.$modal.msgError(`操作失败: ${error.message || "未知错误"}`);
          });
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除项目区域编号为"' + ids + '"的数据项？').then(function() {
        return delProject_region(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('project/project_region/export', {
        ...this.queryParams
      }, `project_region_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

<style scoped>
.service-type-container {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.service-type-item {
  display: flex;
  align-items: center;
  margin-right: 10px;
  margin-bottom: 10px;
}

.service-type-item .el-checkbox {
  margin-right: 5px;
}

.service-type-button {
  font-size: 14px;
}

.selected-services {
  min-height: 80px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 5px;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  background-color: #fff;
}

.selected-service-tag {
  margin-right: 10px;
  margin-bottom: 10px;
  background-color: #f0f9ff;
  border-color: #d9ecff;
  color: #409eff;
  font-size: 14px;
}

.no-service-selected {
  color: #909399;
  padding: 5px;
  font-size: 14px;
}

.mr5 {
  margin-right: 5px;
  margin-bottom: 5px;
}

/* Ensure consistent font sizes */
/deep/ .el-form-item__label,
/deep/ .el-input__inner,
/deep/ .el-textarea__inner {
  font-size: 14px !important;
}

.error-message {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1;
  padding-top: 4px;
  position: absolute;
  top: 100%;
  left: 0;
}

.service-tag-in-list {
  margin-right: 8px;
  margin-bottom: 8px;
  border-radius: 4px;
  padding: 5px 10px;
  font-size: 14px;
  height: 30px;
  line-height: 1;
}

.project-table {
  margin-top: 10px;
}

.project-table /deep/ .el-table__header-wrapper th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 500;
  height: 40px;
}

.project-table /deep/ .el-table__row {
  height: 50px;
}

.service-button-container {
  display: flex;
  flex-wrap: wrap;
  padding: 5px 0;
}

.service-tag-in-list {
  margin-right: 8px;
  margin-bottom: 8px;
  border-radius: 4px;
  padding: 5px 10px;
  font-size: 14px;
  height: 30px;
  line-height: 1;
}

.project-pagination {
  margin-top: 15px;
  text-align: right;
}
</style>
