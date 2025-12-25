<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="案例名称" prop="caseName">
        <el-input
          v-model="queryParams.caseName"
          placeholder="请输入案例名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>
<!-- 模型运行状态页面 -->
    <el-table v-loading="loading" :data="project_service_caseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="案例名称" align="left" prop="caseName" width="300" />
      <el-table-column label="当前进度" align="center" min-width="400">
        <template slot-scope="scope">
          <div class="progress-container">
            <el-progress 
              :percentage="getProgressPercentage(scope.row)" 
              :status="getProgressStatus(scope.row)"
              :stroke-width="18"
              :format="format"
            ></el-progress>
            <span class="progress-text">{{ scope.row.progress }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-document"
            @click="handleViewLogs(scope.row)"
            v-hasPermi="['project:project_service_case:edit']"
          >查看日志</el-button>
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

    <!-- 添加或修改服务案例对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="案例名称" prop="caseName">
          <el-input v-model="form.caseName" placeholder="请输入案例名称" />
          <div v-if="caseNameExists" style="color: red; font-size: 12px; margin-top: 5px;">{{ caseNameError }}</div>
        </el-form-item>
        <el-form-item label="服务路径" prop="caseDir">
          <el-input v-model="form.caseDir" placeholder="服务路径" :readonly="title === '创建情景'" />
        </el-form-item>
        <el-form-item label="是否默认" prop="isDefault">
          <el-input v-model="form.isDefault" placeholder="是否默认案例" :readonly="title === '创建情景'" />
        </el-form-item>
        <el-form-item label="模型参数" prop="validparam">
          <el-input v-model="form.validparam" placeholder="请输入模型参数" :readonly="title === '创建情景'" />
        </el-form-item>
        <el-form-item label="人类活动" prop="humanActivity">
          <el-select v-model="form.humanActivity" @change="val => handleChange(val, 'humanActivity')">      
            <el-option label="默认" value="默认"></el-option>
            <el-option label="城市化" value="城市化"></el-option>
            <el-option label="农业扩张" value="农业扩张"></el-option>
            <el-option label="林业活动" value="林业活动"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="气候变化" prop="climateChange">
          <el-select v-model="form.climateChange" @change="val => handleChange(val, 'climateChange')">
            <el-option label="默认" value="默认"></el-option>
            <el-option label="全球变暖" value="全球变暖"></el-option>
            <el-option label="降水模式变化" value="降水模式变化"></el-option>
            <el-option label="气候极端事件增加" value="气候极端事件增加"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="当前状态" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" :readonly="title === '创建情景'" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" :disabled="caseNameExists">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 查看模型运行日志对话框 -->
    <el-dialog title="模型运行日志" :visible.sync="logDialogVisible" width="70%" append-to-body>
      <div class="log-container">
        <el-card class="log-card" shadow="hover">
          <div slot="header" class="log-header">
            <span>案例名称: {{ currentCase.caseName }}</span>
            <el-tag :type="getProgressTagType(currentCase.progress)" size="medium">{{ currentCase.progress }}</el-tag>
          </div>
          <div class="log-content">
            <pre v-if="modelLogs.length > 0">{{ modelLogs.join('\n') }}</pre>
            <el-empty v-else description="暂无日志信息"></el-empty>
          </div>
        </el-card>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="refreshLogs" icon="el-icon-refresh">刷新日志</el-button>
        <el-button @click="logDialogVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProject_service_case, getProject_service_case, delProject_service_case, addProject_service_case, updateProject_service_case, getTaskStatus } from "@/api/project/project_service_case";
import { getDicts } from "@/api/system/dict/data";

export default {
  name: "Project_service_case",
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
      // 服务案例表格数据
      project_service_caseList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        regionServiceId: null,
        caseName: null,
        caseDir: null,
        isDefault: null,
        validparam: null,
        description: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        regionServiceId: [
          { required: true, message: "区域服务关联ID不能为空", trigger: "blur" }
        ],
        caseName: [
          { required: true, message: "案例名称不能为空", trigger: "blur" },
          { validator: this.validateCaseName, trigger: "blur" }
        ],
        caseDir: [
          { required: true, message: "不能为空", trigger: "blur" }
        ],
        isDefault: [
          { required: true, message: "是否默认案例(0否 1是)不能为空", trigger: "blur" }
        ],
      },
      baseCase: null,
      caseNameExists: false,
      caseNameError: "",
      // 日志对话框可见性
      logDialogVisible: false,
      // 当前查看的案例
      currentCase: {},
      // 模型运行日志
      modelLogs: [],
      // 日志轮询定时器
      logTimer: null,
      // 状态刷新定时器
      statusRefreshTimer: null
    };
  },
  created() {
    this.getList();
    this.$message.info("本页面用于追踪计算任务状态");
    // 启动状态自动刷新（每30秒刷新一次）
    this.startStatusRefresh();
  },
  beforeDestroy() {
    // 清除定时器
    if (this.logTimer) {
      clearInterval(this.logTimer);
    }
    if (this.statusRefreshTimer) {
      clearInterval(this.statusRefreshTimer);
    }
  },
  methods: {
    /** 查询服务案例列表 */
    getList() {
      this.loading = true;
      const params = {
        ...this.queryParams,
        _t: new Date().getTime()
      };
      listProject_service_case(params).then(response => {
        this.project_service_caseList = response.rows;
        this.total = response.total;
        
        // 为每个案例获取真实的任务状态
        this.fetchAllTaskStatuses();
      });
    },
    
    /** 获取所有案例的任务状态 */
    fetchAllTaskStatuses() {
      if (!this.project_service_caseList || this.project_service_caseList.length === 0) {
        this.loading = false;
        return;
      }
      
      // 使用Promise.all并行获取所有任务状态
      const statusPromises = this.project_service_caseList.map(row => {
        return this.fetchTaskStatusForRow(row);
      });
      
      Promise.all(statusPromises).then(() => {
        this.loading = false;
      }).catch(error => {
        console.error('获取任务状态失败:', error);
        this.loading = false;
      });
    },
    
    /** 为单个案例获取任务状态 */
    fetchTaskStatusForRow(row) {
      // 检查必要参数
      if (!row.id || !row.createBy || !row.updateBy) {
        // 如果缺少必要参数，设置默认状态
        this.$set(row, 'progress', "参数不完整");
        this.$set(row, 'taskStatus', "Unknown");
        this.$set(row, 'taskProgress', 0);
        return Promise.resolve();
      }
      
      const caseId = row.id;
      const regionId = parseInt(row.createBy, 10);
      const serviceType = parseInt(row.updateBy, 10);
      
      if (isNaN(regionId) || isNaN(serviceType)) {
        this.$set(row, 'progress', "参数格式错误");
        this.$set(row, 'taskStatus', "Unknown");
        this.$set(row, 'taskProgress', 0);
        return Promise.resolve();
      }
      
      // 根据服务类型获取模型名称
      return this.getModelByServiceType(serviceType).then(model => {
        const payload = {
          caseId: caseId,
          model: model,
          regionId: regionId,
          serviceType: serviceType
        };
        
        return getTaskStatus(payload).then(response => {
          // axios返回的数据在response.data中
          const result = response.data;
          if (result && result.code === 200 && result.data) {
            const taskData = result.data;
            // 更新行的状态信息（使用$set确保Vue响应式更新）
            this.$set(row, 'taskStatus', taskData.status || "Unknown");
            this.$set(row, 'taskProgress', taskData.progress || 0);
            this.$set(row, 'progress', this.formatProgressText(taskData.status, taskData.progress));
            this.$set(row, 'taskInfo', taskData); // 保存完整任务信息，用于日志显示
          } else {
            // API返回失败，设置默认状态
            this.$set(row, 'progress', result?.message || "获取状态失败");
            this.$set(row, 'taskStatus', "Unknown");
            this.$set(row, 'taskProgress', 0);
          }
        }).catch(error => {
          console.error(`获取案例 ${caseId} 的任务状态失败:`, error);
          // 请求失败，设置默认状态
          this.$set(row, 'progress', "状态未知");
          this.$set(row, 'taskStatus', "Unknown");
          this.$set(row, 'taskProgress', 0);
        });
      }).catch(error => {
        console.error(`获取案例 ${caseId} 的模型名称失败:`, error);
        this.$set(row, 'progress', "模型名称获取失败");
        this.$set(row, 'taskStatus', "Unknown");
        this.$set(row, 'taskProgress', 0);
      });
    },
    
    /** 根据服务类型获取模型名称 */
    getModelByServiceType(serviceType) {
      // serviceType 是数字，需要先查询 sys_service_type 获取中文名字（dict_label）
      // 然后根据中文名字查询 sys_model_type 获取模型名称（dict_value）
      return getDicts("sys_service_type").then(response => {
        const dictDatas = response.data;
        // 将 serviceType 转换为字符串进行比较（因为字典值可能是字符串）
        const serviceTypeStr = String(serviceType);
        const matchedDict = dictDatas.find(dict => String(dict.dictValue) === serviceTypeStr);
        
        if (!matchedDict || !matchedDict.dictLabel) {
          return Promise.reject(new Error(`未找到服务类型 ${serviceType} 对应的字典项`));
        }
        
        // 获取服务类型的中文名字
        const serviceTypeLabel = matchedDict.dictLabel;
        
        // 查询 sys_model_type 字典，根据中文名字查找模型名称
        return getDicts("sys_model_type").then(modelResponse => {
          const modelDictDatas = modelResponse.data;
          const modelDict = modelDictDatas.find(dict => dict.dictLabel === serviceTypeLabel);
          
          if (!modelDict || !modelDict.dictValue) {
            return Promise.reject(new Error(`未找到服务类型"${serviceTypeLabel}"对应的模型名称`));
          }
          
          return modelDict.dictValue;
        });
      });
    },
    
    /** 格式化进度文本 */
    formatProgressText(status, progress) {
      if (!status) return "状态未知";
      
      const progressNum = progress || 0;
      
      if (status === "Succeeded" || status === "Completed") {
        return "计算完成";
      } else if (status === "Failed" || status === "Error") {
        return "运行失败";
      } else if (status === "Running" || status === "InProgress") {
        return `运行中 (${progressNum}%)`;
      } else if (status === "Pending" || status === "Waiting") {
        return "等待运行";
      } else if (status === "Initializing" || status === "Preparing") {
        return "初始化中";
      } else {
        return `${status} (${progressNum}%)`;
      }
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
        regionServiceId: null,
        caseName: null,
        caseDir: null,
        isDefault: null,
        validparam: null,
        description: null,
        createTime: null,
        updateTime: null,
        createBy: null,
        updateBy: null,
        humanActivity: "默认",
        climateChange: "默认"
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
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加服务案例";
    },
    /** 创建情景按钮操作 */
    handleCreate(row) {
      this.reset();
      this.baseCase = row;
      
      // 设置默认值，确保两个字段不同时为默认
      const humanActivity = "默认";
      const climateChange = "全球变暖"; // 默认不让两个都是默认值
      
      this.form = {
        regionServiceId: row.regionServiceId,
        caseName: row.caseName.split('-').slice(0, 2).join('-') + '-' + humanActivity + '-' + climateChange,
        caseDir: row.caseDir,
        isDefault: 0,
        validparam: row.validparam,
        description: '情景分析',
        humanActivity: humanActivity,
        climateChange: climateChange
      };
      
      this.checkCaseNameExists();
      this.open = true;
      this.title = "创建情景";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getProject_service_case(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改服务案例";
      });
    },
    /** 提交按钮 */
    submitForm() {
      if (this.title !== '创建情景') {
        this.form.description = '参数率定';
      }
      
      // 先检查名称是否重复
      this.checkCaseNameExists();
      
      if (this.caseNameExists) {
        this.$message.error("案例名称已存在，请修改");
        return;
      }
      
      // 检查人类活动和气候变化不能同时为默认
      if (this.form.humanActivity === "默认" && this.form.climateChange === "默认") {
        this.$message.error("人类活动和气候变化不能同时为默认");
        return;
      }
      
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateProject_service_case(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addProject_service_case(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除服务案例编号为"' + ids + '"的数据项？').then(function() {
        return delProject_service_case(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('project/project_service_case/export', {
        ...this.queryParams
      }, `project_service_case_${new Date().getTime()}.xlsx`)
    },
    /** 对比情景分析 */
    handleScenarioAnalysis(row) {
      this.$router.push({
        path: '/project/project_scenario',
        query: {
          region_service_id: row.regionServiceId
        }
      });
    },
    // 下拉框变化时更新案例名称
    handleChange(val, field) {
      // 当改变当前下拉框为"默认"时，确保另一个下拉框不是"默认"
      if (field === 'humanActivity' && this.form.humanActivity === "默认" && this.form.climateChange === "默认") {
        this.form.climateChange = "全球变暖";
        this.$message.warning("人类活动为默认时，气候变化不能为默认");
      } else if (field === 'climateChange' && this.form.climateChange === "默认" && this.form.humanActivity === "默认") {
        this.form.humanActivity = "城市化";
        this.$message.warning("气候变化为默认时，人类活动不能为默认");
      }
      
      // 更新案例名称
      if (this.baseCase) {
        this.form.caseName = this.baseCase.caseName.split('-').slice(0, 2).join('-') + '-' + this.form.humanActivity + '-' + this.form.climateChange;
        this.checkCaseNameExists();
      }
    },
    // 检查案例名称是否存在
    checkCaseNameExists() {
      if (!this.form.caseName) return;
      
      const params = {
        caseName: this.form.caseName,
        _t: new Date().getTime()
      };
      
      listProject_service_case(params).then(response => {
        if (response.rows && response.rows.length > 0) {
          this.caseNameExists = true;
          this.caseNameError = "案例名称已存在，请修改";
        } else {
          this.caseNameExists = false;
          this.caseNameError = "";
        }
      });
    },
    validateCaseName(rule, value, callback) {
      if (!value) {
        callback(new Error('请输入案例名称'));
      } else {
        const params = {
          caseName: value,
          _t: new Date().getTime()
        };
        listProject_service_case(params).then(response => {
          if (response.rows && response.rows.length > 0) {
            callback(new Error('案例名称已存在'));
          } else {
            callback();
          }
        }).catch(() => {
          callback(new Error('查询案例名称时出错'));
        });
      }
    },
    /** 查看日志按钮操作 */
    handleViewLogs(row) {
      // 找到表格中的原始行对象（保持引用，确保更新能同步到表格）
      const originalRow = this.project_service_caseList.find(r => r.id === row.id) || row;
      this.currentCase = originalRow; // 使用原始对象的引用
      this.logDialogVisible = true;
      this.modelLogs = [];
      
      // 如果任务信息不存在或需要刷新，先获取任务状态
      if (!originalRow.taskInfo || !originalRow.taskStatus) {
        this.fetchTaskStatusForRow(originalRow).then(() => {
          // 使用$set确保Vue能检测到变化
          this.$set(originalRow, 'taskStatus', originalRow.taskStatus);
          this.$set(originalRow, 'taskProgress', originalRow.taskProgress);
          this.$set(originalRow, 'progress', originalRow.progress);
          this.$set(originalRow, 'taskInfo', originalRow.taskInfo);
          // 更新currentCase引用
          this.currentCase = originalRow;
          this.fetchModelLogs(row.id);
        });
      } else {
        this.fetchModelLogs(row.id);
      }
      
      // 设置定时刷新
      if (this.logTimer) {
        clearInterval(this.logTimer);
      }
      
      // 如果任务未完成，则每10秒自动刷新日志
      if (this.isTaskRunning(originalRow.progress)) {
        this.logTimer = setInterval(() => {
          this.refreshLogs();
        }, 10000);
      }
    },
    
    /** 刷新日志 */
    refreshLogs() {
      // 找到表格中的原始行对象
      const originalRow = this.project_service_caseList.find(r => r.id === this.currentCase.id);
      if (!originalRow) {
        return;
      }
      
      // 刷新原始行的任务状态
      this.fetchTaskStatusForRow(originalRow).then(() => {
        // 使用$set确保Vue能检测到变化并更新视图
        this.$set(originalRow, 'taskStatus', originalRow.taskStatus);
        this.$set(originalRow, 'taskProgress', originalRow.taskProgress);
        this.$set(originalRow, 'progress', originalRow.progress);
        this.$set(originalRow, 'taskInfo', originalRow.taskInfo);
        
        // 更新currentCase引用
        this.currentCase = originalRow;
        
        // 重新获取日志
        this.fetchModelLogs(originalRow.id);
        
        // 如果任务已完成，清除定时器
        if (originalRow && !this.isTaskRunning(originalRow.progress)) {
          if (this.logTimer) {
            clearInterval(this.logTimer);
            this.logTimer = null;
          }
        }
      });
    },
    
    /** 获取模型日志 */
    fetchModelLogs(caseId) {
      // 从表格中获取原始行对象
      const row = this.project_service_caseList.find(r => r.id === caseId);
      
      if (!row || !row.taskInfo) {
        // 如果没有任务信息，尝试重新获取（使用表格中的原始行）
        if (row) {
          this.fetchTaskStatusForRow(row).then(() => {
            // 更新currentCase引用
            this.currentCase = row;
            this.buildLogsFromTaskInfo();
          });
        } else {
          // 如果找不到行，使用currentCase
          this.fetchTaskStatusForRow(this.currentCase).then(() => {
            this.buildLogsFromTaskInfo();
          });
        }
        return;
      }
      
      // 确保currentCase引用的是表格中的原始行
      this.currentCase = row;
      this.buildLogsFromTaskInfo();
    },
    
    /** 从任务信息构建日志 */
    buildLogsFromTaskInfo() {
      const taskInfo = this.currentCase.taskInfo;
      const logs = [];
      
      if (taskInfo) {
        if (taskInfo.createdAt) {
          logs.push(`[${this.formatDateTime(taskInfo.createdAt)}] 任务创建`);
        }
        if (taskInfo.startTime) {
          logs.push(`[${this.formatDateTime(taskInfo.startTime)}] 任务开始运行`);
        }
        if (taskInfo.description) {
          logs.push(`[${this.formatDateTime(taskInfo.updateAt || taskInfo.createdAt)}] ${taskInfo.description}`);
        }
        if (taskInfo.status) {
          logs.push(`[${this.formatDateTime(taskInfo.updateAt || new Date())}] 当前状态: ${taskInfo.status}`);
        }
        if (taskInfo.progress !== undefined) {
          logs.push(`[${this.formatDateTime(taskInfo.updateAt || new Date())}] 当前进度: ${taskInfo.progress}%`);
        }
        if (taskInfo.remark) {
          logs.push(`[${this.formatDateTime(taskInfo.updateAt || new Date())}] 备注: ${taskInfo.remark}`);
        }
        if (taskInfo.endTime) {
          logs.push(`[${this.formatDateTime(taskInfo.endTime)}] 任务${taskInfo.status === 'Succeeded' ? '完成' : '结束'}`);
        }
        
        // 根据状态添加额外信息
        if (taskInfo.status === "Succeeded") {
          logs.push(`[${this.formatDateTime(taskInfo.updateAt || new Date())}] 模型计算已完成!`);
          if (taskInfo.saveResult) {
            logs.push(`[${this.formatDateTime(taskInfo.updateAt || new Date())}] 结果已保存`);
          }
        } else if (taskInfo.status === "Failed") {
          logs.push(`[${this.formatDateTime(taskInfo.updateAt || new Date())}] 错误: 模型运行失败，请检查参数配置`);
        } else if (taskInfo.status === "Running") {
          logs.push(`[${this.formatDateTime(taskInfo.updateAt || new Date())}] 模型计算进行中，请耐心等待...`);
        }
      } else {
        logs.push(`[${new Date().toLocaleString()}] 暂无任务信息`);
      }
      
      this.modelLogs = logs.length > 0 ? logs : [`[${new Date().toLocaleString()}] 暂无日志信息`];
    },
    
    /** 格式化日期时间 */
    formatDateTime(dateTimeStr) {
      if (!dateTimeStr) return new Date().toLocaleString();
      try {
        const date = new Date(dateTimeStr);
        return date.toLocaleString('zh-CN');
      } catch (e) {
        return dateTimeStr;
      }
    },
    
    /** 获取进度条百分比 */
    getProgressPercentage(row) {
      if (!row) return 0;
      
      // 优先根据任务状态判断
      if (row.taskStatus) {
        // 如果任务成功完成，确保显示100%
        if (row.taskStatus === "Succeeded" || row.taskStatus === "Completed") {
          return 100;
        }
        // 如果任务失败，也显示100%（表示已完成，但失败）
        if (row.taskStatus === "Failed" || row.taskStatus === "Error") {
          return 100;
        }
        // 如果任务正在运行，使用taskProgress或默认值
        if (row.taskStatus === "Running" || row.taskStatus === "InProgress") {
          if (row.taskProgress !== undefined && row.taskProgress !== null) {
            return row.taskProgress;
          }
          return 50; // 默认值
        }
        // 其他状态使用taskProgress
        if (row.taskProgress !== undefined && row.taskProgress !== null) {
          return row.taskProgress;
        }
      }
      
      // 如果没有taskStatus，从taskProgress获取
      if (row.taskProgress !== undefined && row.taskProgress !== null) {
        return row.taskProgress;
      }
      
      // 从进度文本中提取百分比
      const progressText = row.progress || "";
      const match = progressText.match(/(\d+)%/);
      if (match && match[1]) {
        return parseInt(match[1]);
      }
      
      // 根据文本内容判断
      if (progressText.includes("完成")) {
        return 100;
      } else if (progressText.includes("错误") || progressText.includes("失败")) {
        return 100;
      } else if (progressText.includes("运行中")) {
        return 50; // 默认值
      } else if (progressText.includes("准备") || progressText.includes("等待")) {
        return 10;
      } else if (progressText.includes("初始化")) {
        return 5;
      }
      
      return 0;
    },
    
    /** 获取进度条状态 */
    getProgressStatus(row) {
      if (!row) return "";
      
      // 优先根据taskStatus判断（最准确）
      if (row.taskStatus) {
        if (row.taskStatus === "Succeeded" || row.taskStatus === "Completed") {
          return "success"; // 成功 - 绿色
        } else if (row.taskStatus === "Failed" || row.taskStatus === "Error") {
          return "exception"; // 失败 - 红色
        } else if (row.taskStatus === "Running" || row.taskStatus === "InProgress") {
          return ""; // 运行中 - 蓝色（默认）
        } else if (row.taskStatus === "Pending" || row.taskStatus === "Waiting") {
          return "warning"; // 等待 - 黄色
        }
      }
      
      // 如果没有taskStatus，从进度文本判断
      const progressText = row.progress || "";
      if (progressText.includes("完成")) {
        return "success";
      } else if (progressText.includes("错误") || progressText.includes("失败")) {
        return "exception";
      } else if (progressText.includes("运行中")) {
        return "";
      } else if (progressText.includes("准备") || progressText.includes("等待")) {
        return "warning";
      }
      
      return "";
    },
    
    /** 格式化进度条文本 */
    format(percentage) {
      return percentage === 100 ? '完成' : `${percentage}%`;
    },
    
    /** 获取进度标签类型 */
    getProgressTagType(progressText) {
      if (!progressText) return "info";
      
      if (progressText.includes("完成")) {
        return "success";
      } else if (progressText.includes("错误") || progressText.includes("失败")) {
        return "danger";
      } else if (progressText.includes("运行中")) {
        return "primary";
      } else if (progressText.includes("准备") || progressText.includes("初始化")) {
        return "warning";
      }
      
      return "info";
    },
    
    /** 判断任务是否正在运行 */
    isTaskRunning(progressText) {
      if (!progressText) return false;
      
      // 检查任务状态
      const row = this.project_service_caseList.find(r => r.progress === progressText);
      if (row && row.taskStatus) {
        return row.taskStatus === "Running" || 
               row.taskStatus === "InProgress" || 
               row.taskStatus === "Pending" || 
               row.taskStatus === "Waiting" ||
               row.taskStatus === "Initializing";
      }
      
      // 回退到文本匹配
      return progressText.includes("运行中") || 
             progressText.includes("准备") || 
             progressText.includes("初始化") ||
             progressText.includes("等待");
    },
    
    /** 启动状态自动刷新 */
    startStatusRefresh() {
      // 清除旧的定时器
      if (this.statusRefreshTimer) {
        clearInterval(this.statusRefreshTimer);
      }
      
      // 每30秒刷新一次正在运行的任务状态
      this.statusRefreshTimer = setInterval(() => {
        this.refreshRunningTasks();
      }, 30000); // 30秒
    },
    
    /** 刷新正在运行的任务状态 */
    refreshRunningTasks() {
      if (!this.project_service_caseList || this.project_service_caseList.length === 0) {
        return;
      }
      
      // 只刷新正在运行的任务
      const runningTasks = this.project_service_caseList.filter(row => {
        if (!row.taskStatus) return false;
        return row.taskStatus === "Running" || 
               row.taskStatus === "InProgress" || 
               row.taskStatus === "Pending" || 
               row.taskStatus === "Waiting" ||
               row.taskStatus === "Initializing";
      });
      
      if (runningTasks.length === 0) {
        return; // 没有正在运行的任务，不需要刷新
      }
      
      // 并行刷新所有正在运行的任务
      runningTasks.forEach(row => {
        this.fetchTaskStatusForRow(row).then(() => {
          // 使用Vue.set或$set确保响应式更新
          this.$set(row, 'taskStatus', row.taskStatus);
          this.$set(row, 'taskProgress', row.taskProgress);
          this.$set(row, 'progress', row.progress);
          this.$set(row, 'taskInfo', row.taskInfo);
        });
      });
    }
  }
};
</script>

<style scoped>
.progress-container {
  position: relative;
  margin: 0 10px;
}

.progress-text {
  display: block;
  margin-top: 5px;
  text-align: center;
  font-size: 12px;
  color: #606266;
}

.log-container {
  max-height: 60vh;
  overflow: auto;
}

.log-card {
  margin-bottom: 15px;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.log-content {
  background-color: #f5f7fa;
  color: #303133;
  padding: 15px;
  height: 400px;
  overflow-y: auto;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-content pre {
  margin: 0;
}
</style>
