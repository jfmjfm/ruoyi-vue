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
      <el-table-column label="案例名称" align="left" prop="caseName" width="250" />
      <el-table-column label="远程服务" align="left" prop="caseDir" min-width="500" />
      <el-table-column label="模型参数" align="left" prop="validparam" min-width="150" />
      <el-table-column label="当前状态" align="left" prop="description" width="150" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['project:project_service_case:edit']"
          >修改</el-button>
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
          <el-input v-model="form.caseName" placeholder="请输入案例名称" readonly />
        </el-form-item>
        <el-form-item label="远程服务" prop="caseDir">
          <el-input v-model="form.caseDir" placeholder="请输入保存路径" readonly />
        </el-form-item>
        <el-form-item label="模型参数" prop="validparam">
          <el-button type="primary" plain @click="handleValidparam">使用默认值</el-button>
          <el-input v-model="form.validparam" placeholder="请输入模型参数" readonly />
          <div v-if="validparamError" class="el-form-item__error">模型参数不能为空</div>
        </el-form-item>
        <el-form-item label="当前状态" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" readonly/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">运行模型</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listProject_service_case, getProject_service_case, delProject_service_case, addProject_service_case, updateProject_service_case } from "@/api/project/project_service_case";
import { getDicts } from "@/api/system/dict/data";
import axios from 'axios';

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
      // 参数错误标志
      validparamError: false,
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
          { required: true, message: "案例名称不能为空", trigger: "blur" }
        ],
        caseDir: [
          { required: true, message: "不能为空", trigger: "blur" }
        ],
        isDefault: [
          { required: true, message: "是否默认案例(0否 1是)不能为空", trigger: "blur" }
        ],
        validparam: [
          { required: true, message: "模型参数不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
    this.$message.info("本页面用于检查模型配置文件");
  },
  methods: {
    /** 查询服务案例列表 */
    getList() {
      this.loading = true;
      // 获取所有数据，以便前端过滤
      const params = {
        ...this.queryParams,
        pageNum: 1,
        pageSize: 100, // 设置较大的页面大小以获取所有数据
        _t: new Date().getTime()
      };
      listProject_service_case(params).then(response => {
        // 过滤数据，使用更宽松的匹配方式
        const filteredData = response.rows.filter(item => {
          if (!item.description) return false;
          const desc = item.description.toString().trim();
          return desc.includes('上传模型');
        });
        
        // 计算总数据量
        this.total = filteredData.length;
        
        // 手动处理分页
        const pageSize = this.queryParams.pageSize;
        const pageNum = this.queryParams.pageNum;
        const startIndex = (pageNum - 1) * pageSize;
        const endIndex = Math.min(startIndex + pageSize, filteredData.length);
        
        // 设置当前页数据
        this.project_service_caseList = filteredData.slice(startIndex, endIndex);
        
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
        updateBy: null
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
    /** 处理使用默认参数按钮点击 */
    handleValidparam() {
      if (!this.form.caseName) {
        return;
      }
      
      // 从案例名称中提取服务类型
      const caseName = this.form.caseName;
      const parts = caseName.split('-');
      
      // 检查是否能提取服务类型
      if (parts.length < 2) {
        return;
      }
      
      // 提取服务类型（位于第二个位置）
      const serviceType = parts[1].trim();
      
      if (!serviceType) {
        return;
      }
      
      // 查询字典数据
      getDicts("sys_model_para").then(response => {
        const dictDatas = response.data;
        
        // 查找匹配的字典项
        const matchedDict = dictDatas.find(dict => dict.dictLabel === serviceType);
        
        if (matchedDict) {
          // 更新validparam的值
          this.form.validparam = matchedDict.dictValue;
          // 清除错误标志
          this.validparamError = false;
        }
      }).catch(error => {
        console.error("获取字典数据失败", error);
      });
    },
    /** 提交按钮 */
    submitForm() {
      // 先检查模型参数是否为空
      if (!this.form.validparam || this.form.validparam.trim() === '') {
        // 设置错误标志
        this.validparamError = true;
        return;
      }
      
      // 清除错误标志
      this.validparamError = false;

      // 检查是否有必要的参数
      if (!this.form.createBy || !this.form.updateBy) {
        this.$message.error("缺少必要的参数：create_by 或 update_by");
        return;
      }

      this.$refs["form"].validate(valid => {
        if (valid) {
          // 先更新表单数据
          if (this.form.id != null) {
            if(this.form.description === '上传模型'){
              this.form.description = '参数配置';
            }
            updateProject_service_case(this.form).then(response => {
              // 更新成功后，调用运行模型API
              this.runModel();
            }).catch(error => {
              this.$message.error("更新失败：" + error.message);
            });
          } else {
            addProject_service_case(this.form).then(response => {
              // 添加成功后，调用运行模型API
              this.runModel();
            }).catch(error => {
              this.$message.error("添加失败：" + error.message);
            });
          }
        }
      });
    },
    
    /** 运行模型 */
    runModel() {
      this.$message.info("正在启动模型运行...");
      
      // 构建请求URL
      const url = `/repa/model/getuploadstatus?subdir=${this.form.createBy}/${this.form.updateBy}`;
      console.log('运行模型请求URL:', url);
      
      // 发送HTTP请求
      axios.get(url, {
        timeout: 15000, // 设置超时时间
        headers: {
          'Content-Type': 'application/json'
        }
      })
        .then(response => {
          console.log('运行模型响应:', response);
          const result = response.data;
          
          // 检查API返回的数据结构
          if (result && result.code === 200) {
            this.$message.success("模型启动成功！正在跳转到任务列表...");
            // 关闭对话框
            this.open = false;
            // 跳转到tasklist.vue页面
            this.$router.push('/project/project_service_case4');
          } else {
            this.$message.warning("模型启动失败：" + (result.message || '未知错误'));
          }
        })
        .catch(error => {
          console.error('运行模型失败:', error);
          console.error('错误详情:', {
            message: error.message,
            status: error.response?.status,
            statusText: error.response?.statusText,
            data: error.response?.data,
            config: error.config
          });
          
          if (error.response?.status === 404) {
            this.$message.error("运行模型失败：API接口不存在 (404)，请检查服务器地址和路径是否正确");
          } else if (error.code === 'ECONNABORTED') {
            this.$message.error("运行模型失败：请求超时，请检查网络连接");
          } else if (error.message === 'Network Error') {
            this.$message.error("运行模型失败：网络连接错误，请检查网络连接");
          } else {
            this.$message.error("运行模型失败：" + (error.message || '网络错误'));
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
    }
  }
};
</script>
