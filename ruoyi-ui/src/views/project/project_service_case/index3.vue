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
<!-- 情景分析页面 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['project:project_service_case:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['project:project_service_case:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="project_service_caseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="基线情景" align="left" prop="caseName" width="250" />
      <el-table-column label="远程服务" align="left" prop="caseDir" min-width="400" />
      <el-table-column label="当前状态" align="left" prop="description" width="150" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleCreate(scope.row)"
            v-hasPermi="['project:project_service_case:edit']"
          >创建情景</el-button>
        <el-button
          size="mini"
          type="text"
          icon="el-icon-view"
          @click="handleScenarioAnalysis(scope.row)"
          v-hasPermi="['project:project_service_case:analysis']"
        >对比情景</el-button>
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
        isDefault: 1,
        validparam: null,
        description: '参数率定',
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
      caseNameError: ""
    };
  },
  created() {
    this.getList();
    this.$message.info("本页面用于创建情景分析");
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
    }
  }
};
</script>
