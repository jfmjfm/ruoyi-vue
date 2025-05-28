<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="区域名称" prop="regionName">
        <el-input
          v-model="queryParams.regionName"
          placeholder="请输入区域名称"
          clearable
          @input="debouncedSearch"
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

    <!-- 使用el-table虚拟滚动提高大数据量性能 -->
    <el-table 
      v-loading="loading" 
      :data="displayedData" 
      class="project-table"
      height="450"
      @scroll="handleTableScroll"
      v-bind="tableProps"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="区域名称" prop="regionName" width="180" align="center" />
      <el-table-column label="服务类型" min-width="500">
        <template slot-scope="scope">
          <div class="service-button-container">
            <el-button
              v-for="(service, index) in scope.row.services"
              :key="service.id || index"
              size="mini"
              type="primary"
              plain
              class="service-tag-in-list"
            >{{ cachedServiceTypeName(service.serviceType) }}</el-button>
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
      @pagination="handlePagination"
      class="project-pagination"
    />
  </div>
</template>

<script>
import { listProject_service_case } from "@/api/project/project_service_case";
import { listProject_region } from "@/api/project/project_region";
import { listProject_region_service } from "@/api/project/project_region_service";
import { getDicts } from "@/api/system/dict/data";

// 加载防抖函数，使用函数式编程优化
const debounce = (fn, delay) => {
  let timer = null;
  return function() {
    const context = this;
    const args = arguments;
    clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(context, args);
    }, delay);
  };
};

// 内存缓存管理工具
const cacheManager = {
  set(key, data, ttl = 30 * 60 * 1000) { // 默认30分钟
    const cache = {
      data,
      expiry: Date.now() + ttl
    };
    localStorage.setItem(key, JSON.stringify(cache));
  },
  get(key) {
    try {
      const cache = JSON.parse(localStorage.getItem(key));
      if (!cache) return null;
      if (Date.now() > cache.expiry) {
        localStorage.removeItem(key);
        return null;
      }
      return cache.data;
    } catch (e) {
      console.error('缓存读取错误:', e);
      return null;
    }
  },
  remove(key) {
    localStorage.removeItem(key);
  }
};

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
      // 当前显示的分页数据 (用于虚拟滚动)
      displayedData: [],
      // 虚拟滚动相关变量
      virtualScrolling: {
        itemSize: 50, // 每行高度
        bufferSize: 5, // 额外加载的行数
        startIndex: 0,
        visibleCount: 10, // 可视区域行数
      },
      // 已率定服务的区域IDs缓存
      calibratedServiceCache: {},
      // 服务类型字典
      serviceTypeOptions: [],
      // 服务类型字典映射
      serviceTypeMap: {},
      // 搜索防抖计时器
      searchTimer: null,
      // 上次搜索查询以避免重复加载
      lastQuery: '',
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        regionName: null,
      },
      // 预加载的下一页数据
      nextPageData: [],
      // 预加载标志
      isPreloading: false,
    };
  },
  computed: {
    // 表格属性
    tableProps() {
      return {
        'header-row-class-name': 'table-header',
        'row-class-name': 'table-row',
        'cell-class-name': 'table-cell',
      };
    },
    // 是否有数据
    hasData() {
      return this.regionServicesList.length > 0;
    },
    // 计算查询条件的唯一标识
    queryKey() {
      return `${this.queryParams.pageNum}_${this.queryParams.pageSize}_${this.queryParams.regionName || ''}`;
    }
  },
  watch: {
    // 监听分页变化，优化列表更新
    'queryParams.pageNum'() {
      // 检查是否有预加载的数据
      if (this.nextPageData.length > 0 && !this.isPreloading) {
        // 使用预加载的数据
        this.displayedData = this.nextPageData;
        this.nextPageData = [];
        return;
      }
      
      this.updateDisplayedData();
    }
  },
  // 使用keep-alive缓存组件
  activated() {
    // 仅当查询条件变化时才重新加载
    const currentQueryKey = this.queryKey;
    const cachedQueryKey = sessionStorage.getItem('multidecision_query_key');
    
    if (cachedQueryKey !== currentQueryKey) {
      this.getList();
      sessionStorage.setItem('multidecision_query_key', currentQueryKey);
    }
  },
  created() {
    this.initDictData();
    this.getList();
    
    // 绑定滚动优化函数
    this.handleTableScroll = debounce(this.handleTableScroll, 100);
    this.debouncedSearch = debounce(this.handleQuery, 300);
    
    this.$message.info("本页面用于多服务权衡（多目标优化），只显示已完成参数率定的服务");
  },
  beforeDestroy() {
    // 清理计时器
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
      this.searchTimer = null;
    }
  },
  methods: {
    /** 初始化字典数据，优先从本地缓存加载 */
    initDictData() {
      // 从缓存获取字典数据
      const dictData = cacheManager.get('sys_service_type_dict');
      if (dictData) {
        this.serviceTypeOptions = dictData;
        this.serviceTypeMap = this.createServiceTypeMap(dictData);
        return;
      }
      
      // 缓存不存在，从服务器获取
      this.getServiceTypeDicts();
    },
    
    /** 获取服务类型字典数据 */
    getServiceTypeDicts() {
      getDicts("sys_service_type").then(response => {
        if (response && response.data) {
          this.serviceTypeOptions = response.data;
          this.serviceTypeMap = this.createServiceTypeMap(response.data);
          
          // 将字典数据保存到缓存
          cacheManager.set('sys_service_type_dict', response.data, 24 * 60 * 60 * 1000); // 24小时
        }
      }).catch(error => {
        console.error("获取字典数据失败:", error);
      });
    },
    
    /** 创建服务类型映射 */
    createServiceTypeMap(dictData) {
      return dictData.reduce((map, item) => {
        map[item.dictValue] = item.dictLabel;
        return map;
      }, {});
    },
    
    /** 根据服务类型代码获取中文名称（带有本地记忆功能） */
    cachedServiceTypeName(typeCode) {
      return this.serviceTypeMap[typeCode] || typeCode;
    },
    
    /** 处理表格滚动，实现虚拟滚动 */
    handleTableScroll(event) {
      const { scrollTop, scrollHeight, clientHeight } = event.target;
      
      // 预加载下一页数据
      if (scrollTop + clientHeight > scrollHeight * 0.7 && !this.isPreloading) {
        this.preloadNextPage();
      }

      // 更新虚拟滚动区域
      const startIndex = Math.floor(scrollTop / this.virtualScrolling.itemSize);
      if (startIndex !== this.virtualScrolling.startIndex) {
        this.virtualScrolling.startIndex = startIndex;
        this.updateDisplayedData();
      }
    },
    
    /** 更新显示的数据（用于虚拟滚动） */
    updateDisplayedData() {
      const { startIndex, bufferSize, visibleCount } = this.virtualScrolling;
      const start = Math.max(0, startIndex - bufferSize);
      const end = Math.min(this.regionServicesList.length, startIndex + visibleCount + bufferSize);
      
      this.displayedData = this.regionServicesList.slice(start, end);
    },
    
    /** 预加载下一页数据 */
    preloadNextPage() {
      if (this.queryParams.pageNum * this.queryParams.pageSize >= this.total) {
        return; // 已经是最后一页
      }
      
      this.isPreloading = true;
      
      const nextPageParams = {
        ...this.queryParams,
        pageNum: this.queryParams.pageNum + 1
      };
      
      // 从缓存检查
      const cacheKey = `region_list_${nextPageParams.pageNum}_${nextPageParams.pageSize}_${nextPageParams.regionName || ''}`;
      const cachedData = this.calibratedServiceCache[cacheKey];
      
      if (cachedData) {
        this.nextPageData = cachedData;
        this.calibratedServiceCache[cacheKey] = cachedData;
        this.isPreloading = false;
        return;
      }
      
      // 异步加载下一页
      listProject_region(nextPageParams).then(response => {
        if (!response || !response.rows || response.rows.length === 0) {
          this.isPreloading = false;
          return;
        }
        
        const regions = response.rows;
        const regionIds = regions.map(region => region.id);
        
        // 查询与这些区域相关的服务
        this.loadRegionServicesDetails(regions, regionIds, true);
      }).catch(() => {
        this.isPreloading = false;
      });
    },
  
    /** 查询区域及其对应的服务列表 */
    getList() {
      // 检查是否与上次查询相同
      const queryStr = JSON.stringify(this.queryParams);
      if (queryStr === this.lastQuery && this.regionServicesList.length > 0) {
        return; // 避免重复查询
      }
      
      this.lastQuery = queryStr;
      this.loading = true;
      
      // 检查缓存
      const cacheKey = `region_list_${this.queryParams.pageNum}_${this.queryParams.pageSize}_${this.queryParams.regionName || ''}`;
      const cachedData = this.calibratedServiceCache[cacheKey];
      
      if (cachedData) {
        this.regionServicesList = cachedData;
        this.displayedData = this.regionServicesList.slice(0, this.virtualScrolling.visibleCount + this.virtualScrolling.bufferSize);
        this.loading = false;
        
        // 预加载下一页
        if (!this.isPreloading) {
          setTimeout(() => {
            this.preloadNextPage();
          }, 100);
        }
        return;
      }
      
      const regionParams = { 
        ...this.queryParams,
        pageSize: this.queryParams.pageSize,
        pageNum: this.queryParams.pageNum
      };
      
      // 使用 nextTick 确保 DOM 更新在数据获取后进行
      this.$nextTick(() => {
        // 获取已分页的区域数据
        listProject_region(regionParams).then(response => {
          if (!response || !response.rows) {
            this.loading = false;
            this.total = 0;
            this.regionServicesList = [];
            this.displayedData = [];
            return;
          }
          
          const regions = response.rows;
          this.total = response.total;
          
          if (regions.length === 0) {
            this.regionServicesList = [];
            this.displayedData = [];
            this.loading = false;
            return;
          }
          
          // 收集所有区域ID，用于批量查询
          const regionIds = regions.map(region => region.id);
          
          // 懒加载优化：先设置区域基本数据，再异步加载服务详情
          this.regionServicesList = regions.map(region => ({
            ...region,
            services: [] // 初始为空数组
          }));
          
          this.displayedData = this.regionServicesList.slice(0, this.virtualScrolling.visibleCount + this.virtualScrolling.bufferSize);
          
          // 异步加载服务详情
          setTimeout(() => {
            this.loadRegionServicesDetails(regions, regionIds);
          }, 0);
        }).catch(error => {
          console.error("获取区域列表失败:", error);
          this.loading = false;
          this.regionServicesList = [];
          this.displayedData = [];
        });
      });
    },
    
    /** 异步加载区域服务详情 */
    loadRegionServicesDetails(regions, regionIds, isPreload = false) {
      // 生成缓存key
      const cacheKey = isPreload 
        ? `region_list_${this.queryParams.pageNum + 1}_${this.queryParams.pageSize}_${this.queryParams.regionName || ''}`
        : `region_list_${this.queryParams.pageNum}_${this.queryParams.pageSize}_${this.queryParams.regionName || ''}`;
      
      // 检查缓存
      const serviceMapKey = regionIds.sort().join(',');
      if (this.calibratedServiceCache[serviceMapKey]) {
        // 从缓存处理数据
        const processedData = this.processRegionsWithCache(regions, this.calibratedServiceCache[serviceMapKey]);
        
        if (isPreload) {
          this.nextPageData = processedData;
          this.calibratedServiceCache[cacheKey] = processedData;
          this.isPreloading = false;
        } else {
          this.regionServicesList = processedData;
          this.calibratedServiceCache[cacheKey] = processedData;
          this.displayedData = this.regionServicesList.slice(0, this.virtualScrolling.visibleCount + this.virtualScrolling.bufferSize);
          this.loading = false;
        }
        return;
      }
      
      // 查询与这些区域相关的区域服务映射
      listProject_region_service({ 
        regionIds: regionIds.join(','),
        pageSize: 999
      }).then(rsResponse => {
        if (!rsResponse || !rsResponse.rows) {
          const emptyData = regions.map(r => ({...r, services: []}));
          
          if (isPreload) {
            this.nextPageData = emptyData;
            this.calibratedServiceCache[cacheKey] = emptyData;
            this.isPreloading = false;
          } else {
            this.regionServicesList = emptyData;
            this.calibratedServiceCache[cacheKey] = emptyData;
            this.displayedData = this.regionServicesList.slice(0, this.virtualScrolling.visibleCount + this.virtualScrolling.bufferSize);
            this.loading = false;
          }
          return;
        }
        
        const allRegionServices = rsResponse.rows;
        
        // 创建区域ID到其区域服务的映射
        const regionServiceMap = allRegionServices.reduce((map, rs) => {
          if (!map[rs.regionId]) map[rs.regionId] = [];
          map[rs.regionId].push(rs);
          return map;
        }, {});
        
        // 查询所有已率定的服务案例
        listProject_service_case({
          description: "参数率定",
          pageSize: 999
        }).then(caseResponse => {
          // 创建region_service_id到已率定案例的映射
          const calibratedCasesMap = {};
          
          if (caseResponse && caseResponse.rows) {
            caseResponse.rows.forEach(caseItem => {
              if (caseItem.regionServiceId) {
                calibratedCasesMap[caseItem.regionServiceId] = true;
              }
            });
          }
          
          // 缓存服务数据映射
          this.calibratedServiceCache[serviceMapKey] = {
            regionServiceMap,
            calibratedCasesMap,
            timestamp: Date.now()
          };
          
          // 处理区域服务数据
          const processedRegions = this.processRegionsWithCache(regions, {
            regionServiceMap,
            calibratedCasesMap
          });
          
          if (isPreload) {
            this.nextPageData = processedRegions;
            this.calibratedServiceCache[cacheKey] = processedRegions;
            this.isPreloading = false;
          } else {
            this.regionServicesList = processedRegions;
            this.calibratedServiceCache[cacheKey] = processedRegions;
            this.updateDisplayedData();
            this.loading = false;
            
            // 预加载下一页
            setTimeout(() => {
              this.preloadNextPage();
            }, 200);
          }
        }).catch(error => {
          console.error("获取率定案例失败:", error);
          const emptyData = regions.map(r => ({...r, services: []}));
          
          if (isPreload) {
            this.nextPageData = emptyData;
            this.isPreloading = false;
          } else {
            this.regionServicesList = emptyData;
            this.displayedData = this.regionServicesList.slice(0, this.virtualScrolling.visibleCount + this.virtualScrolling.bufferSize);
            this.loading = false;
          }
        });
      }).catch(error => {
        console.error("获取区域服务失败:", error);
        const emptyData = regions.map(r => ({...r, services: []}));
        
        if (isPreload) {
          this.nextPageData = emptyData;
          this.isPreloading = false;
        } else {
          this.regionServicesList = emptyData;
          this.displayedData = this.regionServicesList.slice(0, this.virtualScrolling.visibleCount + this.virtualScrolling.bufferSize);
          this.loading = false;
        }
      });
    },
    
    /** 使用缓存数据处理区域 */
    processRegionsWithCache(regions, cache) {
      const { regionServiceMap, calibratedCasesMap } = cache;
      
      return regions.map(region => {
        const regionServices = regionServiceMap[region.id] || [];
        
        // 过滤出已率定的服务
        const calibratedServices = regionServices.filter(service => 
          calibratedCasesMap[service.id]
        );
        
        return {
          ...region,
          services: calibratedServices
        };
      });
    },
    
    /** 处理区域服务数据 */
    processRegionServices(regions, regionServiceMap, calibratedCasesMap) {
      // 批量直接处理
      const processedRegions = regions.map(region => {
        const regionServices = regionServiceMap[region.id] || [];
        
        // 过滤出已率定的服务
        const calibratedServices = regionServices.filter(service => 
          calibratedCasesMap[service.id]
        );
        
        return {
          ...region,
          services: calibratedServices
        };
      });
      
      this.regionServicesList = processedRegions;
      this.updateDisplayedData();
    },
    
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    
    /** 分页处理 */
    handlePagination() {
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
  will-change: transform; /* 提示浏览器这个元素会动画变化，优化渲染 */
}

.project-table /deep/ .el-table__header-wrapper th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 500;
  height: 40px;
}

.project-table /deep/ .el-table__row {
  height: 50px;
  contain: layout; /* 告诉浏览器这个元素的内部布局不会影响其他元素 */
  contain: content; /* 这些属性有助于提高滚动性能 */
}

.project-pagination {
  margin-top: 15px;
  text-align: right;
}

/* 优化滚动性能 */
.table-header {
  font-weight: 500 !important;
  background-color: #f5f7fa !important;
}

.table-row {
  transform: translateZ(0); /* 强制GPU加速 */
}

/* 避免重绘闪烁 */
.el-table::before {
  height: 0 !important;
}
</style>
