<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="区域名称" prop="regionName">
        <el-input
          v-model="queryParams.regionName"
          placeholder="请输入区域名称"
          clearable
          size="small"
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
          v-hasPermi="['project:region:add']"
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
          v-hasPermi="['project:region:edit']"
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
          v-hasPermi="['project:region:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="refreshServiceTypes"
          v-hasPermi="['project:region:refresh']"
        >刷新服务类型</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="regionList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="区域名称" align="center" prop="regionName" width="180" />
      <el-table-column 
        label="服务类型" 
        align="left" 
        prop="description" 
        min-width="400" 
        :show-overflow-tooltip="true"
        class-name="description-cell" 
      />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['project:region:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['project:region:remove']"
          >删除</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-map-location"
            @click="handleGIS(scope.row)"
          >GIS定位</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改项目区域对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="区域名称" prop="regionName">
          <el-input v-model="form.regionName" placeholder="请输入区域名称" />
        </el-form-item>
        <el-form-item label="服务类型" prop="serviceTypes">
          <el-checkbox-group v-model="form.serviceTypes" @change="updateDescription">
            <el-checkbox
              v-for="dict in serviceTypeOptions"
              :key="dict.dictValue"
              :label="dict.dictValue"
            >{{ dict.dictLabel }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="已选服务" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            placeholder="请输入服务类型" 
            readonly 
          />
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
import { listProject_region, getProject_region, addProject_region, updateProject_region, delProject_region } from "@/api/project/project_region";

export default {
  name: "Region",
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
      regionList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 服务类型字典
      serviceTypeOptions: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        regionName: null
      },
      // 表单参数
      form: {
        id: null,
        regionName: null,
        description: null,
        serviceTypes: []
      },
      // 表单校验
      rules: {
        regionName: [
          { required: true, message: "区域名称不能为空", trigger: "blur" }
        ],
        serviceTypes: [
          { required: true, message: "请至少选择一项服务类型", trigger: "change" }
        ]
      },
      // 新增的监听器
      isUpdatingDescription: false
    };
  },
  created() {
    this.getList();
    this.loadServiceTypes();
  },
  methods: {
    /** 查询项目区域列表 */
    getList() {
      this.loading = true;
      listProject_region(this.queryParams).then(response => {
        if (!response) {
          console.error("API响应为空");
          this.loading = false;
          return;
        }
        
        console.log("API响应:", response); // 查看具体响应内容
        
        if (response.code === 200) { // 假设成功状态码为200
          this.regionList = response.rows || [];
          this.total = response.total || 0;
        } else {
          console.error("API响应错误:", response.msg);
          this.$modal.msgError(response.msg || "获取数据失败");
          this.regionList = [];
          this.total = 0;
        }
        this.loading = false;
      }).catch(error => {
        console.error("获取项目区域列表失败:", error);
        this.$modal.msgError("获取项目区域列表失败: " + (error.message || error));
        this.regionList = [];
        this.total = 0;
        this.loading = false;
      });
    },
    // 获取服务类型标签
    getServiceTypeLabel(value) {
      if (this.serviceTypeOptions.length === 0) {
        return value;
      }
      const dict = this.serviceTypeOptions.find(item => item.dictValue === value);
      return dict ? dict.dictLabel : value;
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
        serviceTypes: []
      };
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
      this.ids = selection.map(item => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加项目区域";
      
      // 明确初始化 baseDescription
      this.form.description = "";
      this.form.baseDescription = "";
      this.form.serviceTypes = []; // 确保服务类型是空数组
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids;
      getProject_region(id).then(response => {
        if (response && response.data) {
          this.form = response.data;
          
          // 检查服务类型是否已初始化
          if (!this.form.serviceTypes || !Array.isArray(this.form.serviceTypes) || this.form.serviceTypes.length === 0) {
            // 如果服务类型未初始化，则根据描述内容设置
            this.form.serviceTypes = this.parseServiceTypesFromDescription(this.form.description);
            console.log("根据描述解析的服务类型:", this.form.serviceTypes);
          } else if (typeof this.form.serviceTypes === 'string') {
            // 如果是字符串，转为数组
            this.form.serviceTypes = this.form.serviceTypes.split(',');
          }
          
          // 确保数组元素为字符串
          this.form.serviceTypes = this.form.serviceTypes.map(item => String(item));
          
          this.open = true;
          this.title = "修改项目区域";
        }
      });
    },
    /**
     * 从描述文本中解析服务类型
     * @param {string} description - 描述文本
     * @return {Array} 解析出的服务类型ID数组
     */
    parseServiceTypesFromDescription(description) {
      if (!description || !this.serviceTypeOptions || this.serviceTypeOptions.length === 0) {
        return [];
      }
      
      // 创建一个包含所有服务类型标签的数组
      const allLabels = this.serviceTypeOptions.map(opt => opt.dictLabel);
      
      // 创建一个映射：标签 -> ID
      const labelToValue = {};
      this.serviceTypeOptions.forEach(opt => {
        labelToValue[opt.dictLabel] = opt.dictValue;
      });
      
      // 从描述中提取可能的服务类型部分
      const serviceTypePart = description.includes("\n\n") 
        ? description.split("\n\n")[1] 
        : description;
      
      // 查找匹配的服务类型标签
      const result = [];
      
      // 按优先级尝试不同的分隔符
      const separators = ["、", "，", ",", " ", "；", ";"];
      
      for (const separator of separators) {
        if (serviceTypePart.includes(separator)) {
          const parts = serviceTypePart.split(separator);
          parts.forEach(part => {
            const trimmed = part.trim();
            if (allLabels.includes(trimmed) && labelToValue[trimmed]) {
              result.push(labelToValue[trimmed]);
            }
          });
          
          // 如果找到了匹配项，就停止查找
          if (result.length > 0) {
            break;
          }
        }
      }
      
      // 如果上面的分隔符都不匹配，则尝试完整匹配每个标签
      if (result.length === 0) {
        allLabels.forEach(label => {
          if (serviceTypePart.includes(label)) {
            result.push(labelToValue[label]);
          }
        });
      }
      
      console.log("从描述中解析的服务类型:", result);
      return result;
    },
    /** 更新区域描述 */
    updateDescription() {
      this.isUpdatingDescription = true; // 标记正在由方法更新
      
      // 获取选中的服务类型标签
      const selectedLabels = [];
      if (this.form.serviceTypes && this.form.serviceTypes.length > 0) {
        this.form.serviceTypes.forEach(type => {
          const option = this.serviceTypeOptions.find(opt => String(opt.dictValue) === String(type));
          if (option) {
            selectedLabels.push(option.dictLabel);
          }
        });
      }
      
      // 直接设置描述为选中的服务类型列表
      if (selectedLabels.length > 0) {
        this.form.description = selectedLabels.join("、");
      } else {
        this.form.description = "";
      }
      
      console.log("更新后的描述:", this.form.description);
      
      // 最后重置标记
      this.$nextTick(() => {
        this.isUpdatingDescription = false;
      });
    },
    /** 提交按钮 */
    submitForm() {
      // 先检查服务类型是否选择
      if (!this.form.serviceTypes || this.form.serviceTypes.length === 0) {
        this.$modal.msgError("请至少选择一项服务类型");
        return;
      }
      
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 提交前更新描述
          this.updateDescription();
          
          // 移除临时字段，不提交到后端
          const submitForm = {...this.form};
          delete submitForm.baseDescription;
          
          if (this.form.id != null) {
            updateProject_region(submitForm).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            }).catch(error => {
              this.$modal.msgError("修改失败: " + (error.message || "未知错误"));
            });
          } else {
            addProject_region(submitForm).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            }).catch(error => {
              this.$modal.msgError("新增失败: " + (error.message || "未知错误"));
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除项目区域编号为"' + ids + '"的数据项？').then(() => {
        return delProject_region(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** GIS定位按钮操作 */
    handleGIS(row) {
      const regionId = row.id;
      const regionName = row.regionName;
      const serviceTypes = row.serviceTypes ? row.serviceTypes.join(',') : '';
      
      // 使用路由跳转，并传递参数
      this.$router.push({
        path: '/project/prjGIS',
        query: {
          regionId: regionId,
          regionName: regionName,
          serviceTypes: serviceTypes
        }
      });
    },
    // 添加一个专用方法加载服务类型
    loadServiceTypes() {
      console.log("开始加载服务类型字典数据...");
      
      // 清除字典缓存，确保每次都从服务器获取最新数据
      this.$store.dispatch('dict/removeDict', 'sys_service_type');
      
      this.getDicts("sys_service_type").then(response => {
        console.log("字典响应原始数据:", response);
        if (response && response.data) {
          this.serviceTypeOptions = response.data;
          console.log("加载到的服务类型选项数量:", this.serviceTypeOptions.length);
          
          // 如果当前正在编辑，尝试更新服务类型选中状态
          if (this.open && this.form.description) {
            // 只有在没有初始化服务类型的情况下才进行解析
            if (!this.form.serviceTypes || this.form.serviceTypes.length === 0) {
              const parsedTypes = this.parseServiceTypesFromDescription(this.form.description);
              if (parsedTypes.length > 0) {
                this.form.serviceTypes = parsedTypes;
                console.log("服务类型加载后，从描述更新选中状态:", this.form.serviceTypes);
              }
            }
          }
          
          if (this.serviceTypeOptions.length < 8) {
            console.warn(`警告：只加载了 ${this.serviceTypeOptions.length} 个服务类型选项，期望的是 8 个，请检查数据库配置`);
          }
        } else {
          console.error("加载服务类型选项失败:", response);
          this.$modal.msgError("获取服务类型选项失败，请刷新重试");
        }
      }).catch(error => {
        console.error("获取字典数据出错:", error);
        this.$modal.msgError("获取服务类型失败: " + (error.message || error));
      });
    },
    // 修改刷新按钮的处理方法
    refreshServiceTypes() {
      this.$modal.msgInfo("正在刷新服务类型数据...");
      this.loadServiceTypes();
    }
  }
};
</script>

<style scoped>
.el-table .cell {
  line-height: 1.5;
  padding: 8px 12px;
}

.el-table .description-cell {
  white-space: pre-line;
  word-break: break-word;
}
</style> 