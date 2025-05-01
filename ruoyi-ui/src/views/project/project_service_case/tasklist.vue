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

    <el-table v-loading="loading" :data="project_service_caseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="案例名称" align="left" prop="caseName" width="300" />
      <el-table-column label="当前进度" align="center" min-width="400">
        <template slot-scope="scope">
          <div class="progress-container">
            <el-progress 
              :percentage="getProgressPercentage(scope.row.progress)" 
              :status="getProgressStatus(scope.row.progress)"
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
import { listProject_service_case, getProject_service_case, delProject_service_case, addProject_service_case, updateProject_service_case } from "@/api/project/project_service_case";

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
      logTimer: null
    };
  },
  created() {
    this.getList();
    this.$message.info("本页面用于追踪计算任务状态");
  },
  beforeDestroy() {
    // 清除定时器
    if (this.logTimer) {
      clearInterval(this.logTimer);
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
        // 为每一行数据添加随机进度
        this.project_service_caseList.forEach(row => {
          // 如果没有进度或需要重新生成随机进度
          if (!row.progress || this.shouldRefreshProgress()) {
            row.progress = this.generateRandomProgress(row.id);
          }
        });
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
      this.currentCase = row;
      this.logDialogVisible = true;
      this.modelLogs = [];
      
      // 模拟获取日志数据 - 实际项目中应该替换为真实的API调用
      this.fetchModelLogs(row.id);
      
      // 设置定时刷新
      if (this.logTimer) {
        clearInterval(this.logTimer);
      }
      
      // 如果任务未完成，则每10秒自动刷新日志
      if (this.isTaskRunning(row.progress)) {
        this.logTimer = setInterval(() => {
          this.refreshLogs();
        }, 10000);
      }
    },
    
    /** 刷新日志 */
    refreshLogs() {
      this.fetchModelLogs(this.currentCase.id);
      
      // 同时刷新表格数据以更新进度
      this.getList();
      
      // 如果任务已完成，清除定时器
      if (this.currentCase && !this.isTaskRunning(this.currentCase.progress)) {
        if (this.logTimer) {
          clearInterval(this.logTimer);
          this.logTimer = null;
        }
      }
    },
    
    /** 获取模型日志 */
    fetchModelLogs(caseId) {
      // 这里应该替换为实际的API调用
      // 模拟获取日志数据
      // 实际实现中，应该调用后端API获取日志
      setTimeout(() => {
        // 模拟日志数据
        const progress = this.currentCase.progress || "准备运行";
        const mockLogs = [
          `[${new Date().toLocaleString()}] 模型初始化中...`,
          `[${new Date().toLocaleString()}] 加载参数...`,
          `[${new Date().toLocaleString()}] 加载数据集...`,
          `[${new Date().toLocaleString()}] 当前进度: ${progress}`
        ];
        
        // 根据进度添加不同的日志
        if (progress.includes("运行中")) {
          mockLogs.push(`[${new Date().toLocaleString()}] 模型计算进行中，请耐心等待...`);
        } else if (progress.includes("完成")) {
          mockLogs.push(`[${new Date().toLocaleString()}] 模型计算已完成!`);
          mockLogs.push(`[${new Date().toLocaleString()}] 结果已保存.`);
        } else if (progress.includes("错误") || progress.includes("失败")) {
          mockLogs.push(`[${new Date().toLocaleString()}] 错误: 模型运行异常，请检查参数配置.`);
        }
        
        this.modelLogs = mockLogs;
      }, 500);
    },
    
    /** 获取进度条百分比 */
    getProgressPercentage(progressText) {
      if (!progressText) return 0;
      
      if (progressText.includes("完成")) {
        return 100;
      } else if (progressText.includes("错误") || progressText.includes("失败")) {
        return 100;
      } else if (progressText.includes("运行中")) {
        // 从进度文本中提取百分比（如果有）
        const match = progressText.match(/(\d+)%/);
        if (match && match[1]) {
          return parseInt(match[1]);
        }
        // 如果没有具体百分比，给一个默认值
        return 50;
      } else if (progressText.includes("准备")) {
        return 10;
      } else if (progressText.includes("初始化")) {
        return 5;
      }
      
      return 0;
    },
    
    /** 获取进度条状态 */
    getProgressStatus(progressText) {
      if (!progressText) return "";
      
      if (progressText.includes("完成")) {
        return "success";
      } else if (progressText.includes("错误") || progressText.includes("失败")) {
        return "exception";
      } else if (progressText.includes("运行中")) {
        return "";
      }
      
      return "warning";
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
      
      return progressText.includes("运行中") || 
             progressText.includes("准备") || 
             progressText.includes("初始化");
    },
    
    /** 生成随机进度数据 */
    generateRandomProgress(id) {
      // 使用id作为种子，同一个案例获得稳定的随机状态
      const seed = id % 10;
      
      // 可能的进度状态
      const progressStates = [
        "初始化模型参数...",
        "准备运行环境...",
        "运行中 (10%)",
        "运行中 (25%)",
        "运行中 (43%)",
        "运行中 (67%)",
        "运行中 (89%)",
        "计算完成",
        "运行失败: 参数错误",
        "等待资源分配..."
      ];
      
      // 以30%概率生成完成状态
      if (seed > 6) {
        return "计算完成";
      } 
      // 以10%概率生成错误状态
      else if (seed === 0) {
        return "运行失败: 参数错误";
      }
      // 以60%概率生成运行中的状态
      else {
        // 根据seed选择一个运行中的状态
        return progressStates[seed];
      }
    },
    
    /** 判断是否需要刷新进度数据 (20%概率更新) */
    shouldRefreshProgress() {
      return Math.random() < 0.2;
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
