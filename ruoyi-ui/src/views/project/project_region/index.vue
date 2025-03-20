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
      <div v-loading="formLoading" element-loading-text="正在处理...">
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
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { 
  listProject_region, 
  getProject_region, 
  addProject_region, 
  updateProject_region, 
  delProject_region,
  batchAddRegionServices,
  updateRegionWithServices
} from "@/api/project/project_region";

import {
  listProject_region_service,
  getProject_region_service,
  addProject_region_service,
  delProject_region_service
} from "@/api/project/project_region_service";

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
      isUpdatingDescription: false,
      submitting: false, // 防止重复提交
      searchTimer: null, // 搜索防抖定时器
      submitTimer: null, // 提交防抖定时器
      submitLoading: false, // 提交按钮加载状态
      formLoading: false,
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
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
      }
      this.searchTimer = setTimeout(() => {
        this.queryParams.pageNum = 1;
        this.getList();
      }, 300); // 300ms防抖
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
      if (this.submitTimer) {
        clearTimeout(this.submitTimer);
      }
      this.submitTimer = setTimeout(() => {
        this._submitForm();
      }, 100);
    },
    /**
     * 实际的提交表单方法
     */
    _submitForm() {
      // 表单验证
      this.$refs["form"].validate(valid => {
        if (!valid) return;
        
        if (this.submitting) {
          return; // 防止重复提交
        }
        
        // 设置提交状态和按钮加载
        this.submitting = true;
        this.submitLoading = true;
        
        // 提交前更新描述
        this.updateDescription();
        
        // 处理提交逻辑
        if (this.form.id != null) {
          // 修改项目区域
          console.log(`[${new Date().toISOString()}] 开始更新项目区域:`, this.form.id);
          
          updateProject_region(this.form)
            .then(response => {
              console.log(`[${new Date().toISOString()}] 更新基本信息完成:`, response);
              
              if (response && response.code === 200) {
                // 更新加载提示文本
                this.submitLoading = false;
                
                // 处理服务类型关联
                return this.handleServiceTypeAssociations(this.form.id, this.form.serviceTypes)
                  .then(result => {
                    console.log(`[${new Date().toISOString()}] 服务类型关联处理完成:`, result);
                    return { success: true };
                  })
                  .catch(error => {
                    console.error(`[${new Date().toISOString()}] 服务类型关联处理失败:`, error);
                    return { success: false, error };
                  });
              } else {
                return Promise.reject(response || new Error("更新失败"));
              }
            })
            .then(result => {
              // 处理结果
              if (result.success) {
                this.$modal.msgSuccess("修改成功");
              } else {
                this.$modal.msgWarning("项目区域已更新，但服务类型关联部分失败");
              }
              
              // 关闭弹框
              this.open = false;
              
              // 等待弹框完全关闭后再刷新列表
              setTimeout(() => {
                this.getList();
              }, 100);
            })
            .catch(error => {
              this.handleError(error, "修改项目区域失败");
            })
            .finally(() => {
              this.submitting = false;
              this.submitLoading = false;
            });
        } else {
          // 新增项目区域
          addProject_region(this.form).then(response => {
            if (response && response.code === 200) {
              // 成功消息
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              
              // 保存服务类型，用于下一步关联
              const savedServiceTypes = [...this.form.serviceTypes];
              const savedRegionName = this.form.regionName;
              
              // 先刷新列表
              this.getList();
              
              // 延迟执行关联操作，不阻塞用户界面
              setTimeout(() => {
                // 查询新添加的区域
                listProject_region({
                  regionName: savedRegionName,
                  pageSize: 10,
                  pageNum: 1
                }).then(listResp => {
                  if (listResp?.rows?.length > 0) {
                    // 按创建时间降序排序，取最新的匹配记录
                    const candidates = listResp.rows.filter(r => r.regionName === savedRegionName);
                    const latestRegion = candidates.sort((a, b) => 
                      new Date(b.createTime || 0) - new Date(a.createTime || 0)
                    )[0];
                    
                    if (latestRegion?.id) {
                      // 静默处理服务类型关联
                      this.handleServiceTypeAssociations(latestRegion.id, savedServiceTypes)
                        .then(() => console.log("服务类型关联成功"))
                        .catch(err => console.error("服务类型关联失败:", err));
                    }
                  }
                }).catch(err => console.error("查询新增区域失败:", err));
              }, 500);
            } else {
              this.handleError(response, "新增失败");
            }
          }).catch(error => {
            this.handleError(error, "新增项目区域失败");
          }).finally(() => {
            this.submitting = false;
            this.submitLoading = false;
          });
        }
      });
    },
    /**
     * 处理服务类型关联 - 统一方法，用于添加和更新场景
     * @param {number} regionId - 区域ID
     * @param {Array} serviceTypes - 服务类型数组
     * @returns {Promise} - Promise对象
     */
    handleServiceTypeAssociations(regionId, serviceTypes) {
      if (!regionId || !serviceTypes || !Array.isArray(serviceTypes)) {
        return Promise.reject(new Error("参数无效"));
      }
      
      console.log("处理区域服务类型关联，区域ID:", regionId, "服务类型:", serviceTypes);
      
      // 标准化服务类型数组
      const uniqueServiceTypes = [...new Set(serviceTypes)].map(t => String(t));
      
      // 查询现有关联
      return listProject_region_service({ regionId })
        .then(response => {
          const existingRelations = response?.rows || [];
          const existingTypes = existingRelations.map(r => String(r.serviceType));
          
          // 计算需要添加和删除的关联
          const typesToAdd = uniqueServiceTypes.filter(t => !existingTypes.includes(t));
          const toDelete = existingRelations.filter(r => !uniqueServiceTypes.includes(String(r.serviceType)));
          
          console.log("需要添加的类型:", typesToAdd, "需要删除的关联:", toDelete);
          
          // 如果没有变化，直接返回
          if (typesToAdd.length === 0 && toDelete.length === 0) {
            return Promise.resolve({ unchanged: true });
          }
          
          // 定义操作链 - 先删除再添加
          const promises = [];
          
          // 并行处理删除操作
          if (toDelete.length > 0) {
            toDelete.forEach(r => {
              promises.push(
                delProject_region_service(r.id)
                  .catch(err => {
                    console.warn(`删除关联失败(ID:${r.id}):`, err);
                    return { error: true, id: r.id };
                  })
              );
            });
          }
          
          // 对每个Promise添加超时处理
          const addServiceWithTimeout = (data) => {
            const addPromise = addProject_region_service(data);
            const timeoutPromise = new Promise((_, reject) => 
              setTimeout(() => reject(new Error("添加操作超时")), 8000)); // 8秒超时
            
            return Promise.race([addPromise, timeoutPromise]);
          };
          
          // 并行处理添加操作，使用优化的添加方法
          if (typesToAdd.length > 0) {
            typesToAdd.forEach(type => {
              const data = {
                regionId: Number(regionId),
                serviceType: String(type)
              };
              promises.push(
                addServiceWithTimeout(data)
                  .catch(err => {
                    // 忽略唯一约束错误
                    if (err?.response?.data?.includes?.("Duplicate entry")) {
                      return { ignored: true };
                    }
                    console.warn(`添加服务类型关联失败(${type}):`, err);
                    return { error: true, type, reason: err.message || '未知错误' };
                  })
              );
            });
          }
          
          // 并行执行所有操作，提高效率
          return Promise.all(promises);
        })
        .catch(err => {
          console.error("处理服务类型关联出错:", err);
          return Promise.reject(err);
        });
    },
    /**
     * 通过查询查找新添加的区域记录
     * @param {string} regionName - 区域名称
     * @param {Array} serviceTypes - 服务类型数组
     */
    findNewlyAddedRegion(regionName, serviceTypes) {
      this.$modal.msgSuccess("新增成功，正在关联服务类型...");
      this.open = false;
      
      // 立即刷新列表
      this.getList();
      
      // 查询新添加的记录
      const query = {
        regionName: regionName,
        pageSize: 10,
        pageNum: 1
      };
      
      // 尝试查询，如果失败则不再尝试关联
      listProject_region(query)
        .then(response => {
          if (response && response.code === 200 && response.rows && response.rows.length > 0) {
            // 找到最新添加的记录
            const latestRegion = response.rows.find(r => r.regionName === regionName);
            
            if (latestRegion && latestRegion.id) {
              // 找到了ID，处理服务类型关联
              return this.handleServiceTypeAssociations(latestRegion.id, serviceTypes);
            } else {
              return Promise.reject(new Error("找到区域记录，但ID无效"));
            }
          } else {
            return Promise.reject(new Error("未找到新添加的区域记录"));
          }
        })
        .then(() => {
          // 服务类型关联成功，静默处理，避免过多提示干扰用户
          this.getList(); // 再次刷新列表以显示最新状态
        })
        .catch(error => {
          console.error("延迟关联服务类型失败:", error);
          // 已经显示过新增成功，这里只在控制台记录错误，不再弹窗
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
      this.$modal.alert("正在刷新服务类型数据...");
      this.loadServiceTypes();
    },
    // 添加以下方法使API函数在组件中可用
    listProject_region_service(query) {
      return listProject_region_service(query);
    },
    
    addProject_region_service(data) {
      return addProject_region_service(data);
    },
    
    delProject_region_service(id) {
      return delProject_region_service(id);
    },
    batchAddRegionServices(data) {
      return batchAddRegionServices(data);
    },
    updateRegionWithServices(data) {
      return updateRegionWithServices(data);
    },
    // 改进错误处理，提供更好的用户反馈
    handleError(error, defaultMessage) {
      console.error(defaultMessage, error);
      
      // 提取响应中的错误信息
      let errorMsg = defaultMessage;
      if (error?.response?.data) {
        // 尝试提取API返回的详细错误信息
        if (typeof error.response.data === 'string') {
          errorMsg = error.response.data;
        } else if (error.response.data.msg) {
          errorMsg = error.response.data.msg;
        }
      } else if (error?.message) {
        errorMsg = error.message;
      }
      
      // 显示错误信息
      this.$modal.msgError(errorMsg);
      
      // 总是重置提交状态
      this.submitting = false;
      this.submitLoading = false;
      
      // 关闭任何可能活跃的加载器
      try {
        this.$loading().close();
      } catch (e) {
        // 忽略关闭加载器可能的错误
      }
    },
    // 创建一个单独的方法处理弹框关闭和刷新
    closeDialogAndRefresh() {
      // 如果弹框已经关闭，则不再处理
      if (!this.open) {
        return;
      }
      
      // 关闭弹框
      this.open = false;
      
      // 确保弹框完全关闭后再刷新列表
      setTimeout(() => {
        this.getList();
      }, 100);
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