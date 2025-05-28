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

    <el-table v-loading="loading" :data="regionServicesList" class="project-table">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="区域名称" prop="regionName" width="180" align="center" />
      <el-table-column label="服务类型" min-width="500">
        <template slot-scope="scope">
          <div class="service-button-container">
            <el-button
              v-for="(service, index) in scope.row.services"
              :key="index"
              size="mini"
              type="primary"
              plain
              class="service-tag-in-list"
            >{{ getServiceTypeName(service.serviceType) }}</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleScenarioAnalysis(scope.row)"
            v-hasPermi="['project:project_service_case:analysis']"
          >下一步</el-button>
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
  </div>
</template>

<script>
import { listProject_service_case } from "@/api/project/project_service_case";
import { listProject_region } from "@/api/project/project_region";
import { listProject_region_service } from "@/api/project/project_region_service";
import { getDicts } from "@/api/system/dict/data";

export default {
  name: "MultiServiceDecision",
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
      // 区域服务列表数据
      regionServicesList: [],
      // 所有区域
      allRegions: [],
      // 已率定服务的区域IDs
      calibratedRegionIds: new Set(),
      // 服务类型字典
      serviceTypeOptions: [],
      // 服务类型字典映射
      serviceTypeMap: {},
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        regionName: null,
      },
    };
  },
  created() {
    this.getServiceTypeDicts();
    this.getList();
    this.$message.info("本页面用于多服务权衡（多目标优化），只显示已完成参数率定的服务");
  },
  methods: {
    /** 获取服务类型字典数据 */
    getServiceTypeDicts() {
      getDicts("sys_service_type").then(response => {
        this.serviceTypeOptions = response.data;
        // 创建一个字典值到标签的映射，方便查找
        this.serviceTypeMap = {};
        this.serviceTypeOptions.forEach(item => {
          this.serviceTypeMap[item.dictValue] = item.dictLabel;
        });
      });
    },
    
    /** 根据服务类型代码获取中文名称 */
    getServiceTypeName(typeCode) {
      return this.serviceTypeMap[typeCode] || typeCode;
    },

    /** 查询区域及其对应的服务列表 */
    getList() {
      this.loading = true;
      
      // 1. 先查找所有有"参数率定"案例的区域服务
      this.findCalibratedServices().then(() => {
        // 2. 获取区域列表，使用regionName进行前端筛选
        const regionParams = { ...this.queryParams };
        delete regionParams.pageNum;
        delete regionParams.pageSize;
        
        // 获取所有满足条件的区域（不分页）
        listProject_region(regionParams).then(response => {
          this.allRegions = response.rows || [];
          
          // 3. 筛选出有已率定服务的区域
          const filteredRegions = this.allRegions.filter(region => 
            this.calibratedRegionIds.has(region.id)
          );
          
          // 4. 手动处理分页
          this.total = filteredRegions.length;
          const startIndex = (this.queryParams.pageNum - 1) * this.queryParams.pageSize;
          const endIndex = startIndex + this.queryParams.pageSize;
          const pagedRegions = filteredRegions.slice(startIndex, endIndex);
          
          // 5. 获取每个区域的已率定服务详情
          const promises = pagedRegions.map(region => this.getRegionCalibratedServices(region));
          
          Promise.all(promises).then(results => {
            this.regionServicesList = results;
            this.loading = false;
          }).catch(error => {
            console.error("获取区域服务详情失败:", error);
            this.loading = false;
          });
        }).catch(error => {
          console.error("获取区域列表失败:", error);
          this.loading = false;
        });
      }).catch(error => {
        console.error("获取已率定服务失败:", error);
        this.loading = false;
      });
    },
    
    /** 查找所有有参数率定案例的区域服务 */
    findCalibratedServices() {
      return new Promise((resolve, reject) => {
        // 查询所有description为"参数率定"的服务案例
        listProject_service_case({
          description: "参数率定",
          pageSize: 999 // 获取尽可能多的记录
        }).then(response => {
          const calibratedCases = response.rows || [];
          this.calibratedRegionIds.clear();
          
          // 获取所有这些案例关联的region_service_id
          const regionServiceIds = new Set(calibratedCases.map(item => item.regionServiceId));
          
          // 如果没有率定案例，直接返回
          if (regionServiceIds.size === 0) {
            resolve();
            return;
          }
          
          // 查询这些region_service对应的区域ID
          const promises = Array.from(regionServiceIds).map(rsId => {
            return listProject_region_service({ id: rsId }).then(rsResponse => {
              const services = rsResponse.rows || [];
              services.forEach(service => {
                if (service.regionId) {
                  this.calibratedRegionIds.add(service.regionId);
                }
              });
            });
          });
          
          Promise.all(promises).then(() => {
            resolve();
          }).catch(error => {
            reject(error);
          });
        }).catch(error => {
          reject(error);
        });
      });
    },
    
    /** 获取区域的已率定服务 */
    getRegionCalibratedServices(region) {
      return new Promise((resolve) => {
        // 获取区域关联的所有服务
        listProject_region_service({ regionId: region.id }).then(serviceResponse => {
          const regionServices = serviceResponse.rows || [];
          
          // 对每个服务，检查是否有已率定的案例
          const servicePromises = regionServices.map(service => {
            return listProject_service_case({
              regionServiceId: service.id,
              description: "参数率定"
            }).then(caseResponse => {
              const calibratedCases = caseResponse.rows || [];
              return {
                ...service,
                isCalibrated: calibratedCases.length > 0,
                calibratedCases: calibratedCases
              };
            }).catch(() => {
              return {
                ...service,
                isCalibrated: false,
                calibratedCases: []
              };
            });
          });
          
          Promise.all(servicePromises).then(services => {
            // 过滤出已率定的服务
            const calibratedServices = services.filter(s => s.isCalibrated);
            
            resolve({
              ...region,
              services: calibratedServices
            });
          }).catch(() => {
            resolve({
              ...region,
              services: []
            });
          });
        }).catch(() => {
          resolve({
            ...region,
            services: []
          });
        });
      });
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
    
    /** 导出按钮操作 */
    handleExport() {
      this.download('project/project_service_case/export', {
        ...this.queryParams
      }, `project_service_case_${new Date().getTime()}.xlsx`)
    },
    
    /** 跳转到情景分析 */
    handleScenarioAnalysis(row) {
      this.$router.push({
        path: '/project/project_multidecision',
        query: {
          region_id: row.id
        }
      });
    }
  }
};
</script>

<style scoped>
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

.project-pagination {
  margin-top: 15px;
  text-align: right;
}
</style>
