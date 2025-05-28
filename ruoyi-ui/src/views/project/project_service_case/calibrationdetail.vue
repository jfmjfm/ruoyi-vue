<template>
  <div class="app-container">
    <div class="calibration-header">
      <h4>{{ modelIntroduction.name || 'SWAT' }}模型参数率定进度</h4>
      <el-button type="primary" size="small" @click="startCalibration" :disabled="isCalibrationRunning">
        {{ isCalibrationRunning ? '正在率定' : '开始率定' }}
      </el-button>
    </div>
    
    <el-row :gutter="20" class="calibration-content">
      <!-- 左侧面板 -->
      <el-col :span="12">
        <!-- 左上：进度信息 -->
        <el-card class="progress-card" shadow="hover" v-loading="loading">
          <div slot="header" class="card-header">
            <span>率定进度信息</span>
          </div>
          <div class="progress-info">
            <div class="progress-item">
              <el-progress :percentage="calibrationProgress" :format="progressFormat" :stroke-width="18"></el-progress>
              <div class="progress-stats">
                <div class="stat-item">
                  <div class="stat-label">当前迭代次数:</div>
                  <div class="stat-value">{{ currentIteration }}/{{ maxIterations }}</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">当前最大NSE:</div>
                  <div class="stat-value highlight">{{ maxNSE.toFixed(4) }}</div>
                </div>
              </div>
            </div>
            
            <div class="parameters-table">
              <h5>最优参数值</h5>
              <div class="table-wrapper">
                <el-table :data="bestParameters" size="mini" border stripe height="180">
                  <el-table-column prop="name" label="参数" width="100" fixed></el-table-column>
                  <el-table-column prop="value" label="当前最优值"></el-table-column>
                  <el-table-column prop="range" label="参数范围" width="100"></el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </el-card>
        
        <!-- 左下：上一次率定的时间序列图 -->
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="card-header">
            <span>上一次率定时间序列对比</span>
          </div>
          <div class="chart-container">
            <div id="previous-chart" class="chart"></div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧面板：最大NSE时的时间序列图 -->
      <el-col :span="12">
        <el-card class="chart-card full-height" shadow="hover">
          <div slot="header" class="card-header">
            <span>最大NSE值({{ maxNSE.toFixed(4) }})率定结果</span>
          </div>
          
          <!-- 添加迭代-NSE曲线图 -->
          <div class="chart-section">
            <div class="section-title">迭代优化过程</div>
            <div class="chart-container nse-chart-container">
              <div id="iteration-chart" class="chart"></div>
            </div>
          </div>
          
          <!-- 保留原有时间序列图 -->
          <div class="chart-section">
            <div class="section-title">当前最优率定结果</div>
            <div class="chart-container">
              <div id="best-chart" class="chart"></div>
            </div>
          </div>
          
          <div class="chart-stats">
            <div class="stat-panel">
              <div class="stat-item">
                <div class="stat-label">纳什效率系数(NSE):</div>
                <div class="stat-value highlight">{{ maxNSE.toFixed(4) }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">决定系数(R²):</div>
                <div class="stat-value">{{ rSquared.toFixed(4) }}</div>
              </div>
            </div>
            <div class="stat-panel">
              <div class="stat-item">
                <div class="stat-label">均方根误差(RMSE):</div>
                <div class="stat-value">{{ rmse.toFixed(4) }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">偏差百分比(PBIAS):</div>
                <div class="stat-value">{{ pbias.toFixed(2) }}%</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getProject_service_case, updateProject_service_case } from "@/api/project/project_service_case";
import { getDicts } from "@/api/system/dict/data";
import * as echarts from 'echarts';

export default {
  name: "CalibrationDetail",
  data() {
    return {
      // 案例信息
      caseInfo: {},
      // 服务类型
      serviceType: "",
      // 物理模型名称
      modelName: "",
      // 模型介绍
      modelIntroduction: {},
      // 从路由中获取ID参数
      caseId: this.$route.query.id || null,
      // 加载状态
      loading: false,
      
      // 率定相关数据
      isCalibrationRunning: false,
      currentIteration: 15,
      maxIterations: 50,
      calibrationProgress: 30,
      maxNSE: 0.75,
      
      // 评价指标
      rSquared: 0.78,
      rmse: 3.42,
      pbias: -1.85,
      
      // 最优参数值
      bestParameters: [],
      
      // 模拟和观测数据
      simulatedData: [],
      observedData: [],
      previousSimulatedData: [],
      previousObservedData: [],
      
      // 增加迭代历史记录
      iterationHistory: [],
      
      // 图表实例
      bestChart: null,
      previousChart: null,
      iterationChart: null,
      
      // 定时器ID
      timer: null,
      
      // 模型介绍数据映射
      modelIntroductions: {
        "VIC": {
          name: "VIC",
          fullName: "可变渗透能力模型(Variable Infiltration Capacity)",
          description: "VIC是一个大尺度、半分布式水文模型，主要用于模拟流域水文过程，尤其是水源涵养与水源供给服务评估。"
        },
        "SWAT": {
          name: "SWAT",
          fullName: "土壤与水分评估工具(Soil and Water Assessment Tool)",
          description: "SWAT是一个流域尺度模型，能够模拟长时间序列下复杂流域的水文和水质过程，特别适用于土壤保持与水质净化服务评估。"
        },
        "HSPF": {
          name: "HSPF",
          fullName: "水文模拟程序-FORTRAN(Hydrological Simulation Program-FORTRAN)",
          description: "HSPF是一个综合性流域模型，用于模拟流域中的水文、水质和生态过程，结合DELFT3D可实现洪水调蓄服务评估。"
        },
        "RWEQ": {
          name: "RWEQ",
          fullName: "修正风蚀方程(Revised Wind Erosion Equation)",
          description: "RWEQ是一个基于过程的风蚀预测模型，专门用于评估土壤风蚀量和防风固沙服务。"
        },
        "LPJ": {
          name: "LPJ",
          fullName: "陆地生态系统动态全球植被模型(Lund-Potsdam-Jena)",
          description: "LPJ是一个动态全球植被模型，用于模拟植被动态、碳循环和水循环，特别适用于固碳服务评估。"
        },
        "DSSAT": {
          name: "DSSAT",
          fullName: "决策支持系统与农业技术转让(Decision Support System for Agrotechnology Transfer)",
          description: "DSSAT是一个作物模型系统，用于模拟作物生长、产量和环境影响，适用于食物供给服务评估。"
        },
        "WRF": {
          name: "WRF",
          fullName: "天气研究与预报(Weather Research and Forecasting)模型",
          description: "WRF是一个大气模型，用于模拟大气运动、天气变化和气候模式，特别适用于气候变化影响研究。"
        }
      },
      
      // 模型参数数据映射（简化版）
      modelParameters: {
        "VIC": [
          { name: "b_infilt", meaning: "渗透曲线参数", range: "0-0.4", value: null },
          { name: "Ds", meaning: "排水速率", range: "0-1", value: null },
          { name: "Dsmax", meaning: "最大基流速率", range: "0-30", value: null },
          { name: "Ws", meaning: "地下水分数阈值", range: "0.5-1", value: null },
          { name: "d1", meaning: "第一层土壤厚度", range: "0.05-0.15", value: null },
          { name: "d2", meaning: "第二层土壤厚度", range: "0.1-1.0", value: null },
          { name: "d3", meaning: "第三层土壤厚度", range: "0.1-1.5", value: null },
          { name: "snow_rough", meaning: "雪面粗糙度", range: "0.001-0.01", value: null }
        ],
        "SWAT": [
          { name: "CN2", meaning: "SCS径流曲线值", range: "35-98", value: null },
          { name: "ALPHA_BF", meaning: "基流衰退常数", range: "0-1", value: null },
          { name: "GW_DELAY", meaning: "地下水延迟时间", range: "0-500", value: null },
          { name: "GWQMN", meaning: "浅层含水层回归流阈值", range: "0-5000", value: null },
          { name: "ESCO", meaning: "土壤蒸发补偿系数", range: "0-1", value: null },
          { name: "SOL_AWC", meaning: "有效水含量", range: "0-1", value: null },
          { name: "SOL_K", meaning: "饱和导水率", range: "0-2000", value: null },
          { name: "CH_N2", meaning: "主河道曼宁粗糙系数", range: "0.01-0.3", value: null }
        ]
      }
    };
  },
  created() {
    // 如果有ID参数，加载对应案例的数据
    if (this.caseId) {
      this.loadCaseData(this.caseId);
    } else {
      // 默认使用 VIC 模型作为演示
      this.modelName = "VIC";
      this.updateModelContent();
    }
  },
  mounted() {
    // 初始化图表
    this.$nextTick(() => {
      // 将延迟时间增加到500ms，确保DOM完全渲染
      setTimeout(() => {
        this.initCharts();
        this.initDemoParameters();
      }, 500);
    });
  },
  beforeDestroy() {
    // 清除定时器
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    
    // 销毁图表实例
    if (this.bestChart) {
      this.bestChart.dispose();
    }
    if (this.previousChart) {
      this.previousChart.dispose();
    }
    if (this.iterationChart) {
      this.iterationChart.dispose();
    }
    
    // 移除窗口大小变化监听器
    window.removeEventListener('resize', this.resizeCharts);
  },
  methods: {
    // 加载案例数据
    loadCaseData(id) {
      this.loading = true;
      getProject_service_case(id).then(response => {
        this.caseInfo = response.data;
        
        // 从案例名称中提取服务类型（格式：区域名称-服务类型-baseline）
        if (this.caseInfo.caseName) {
          const parts = this.caseInfo.caseName.split('-');
          if (parts.length >= 2) {
            this.serviceType = parts[1].trim();
            this.getModelByServiceType(this.serviceType);
          } else {
            this.$message.warning("案例名称格式不符合预期，无法提取服务类型");
            this.loading = false;
          }
        } else {
          this.$message.warning("获取案例数据失败，无法提取服务类型");
          this.loading = false;
        }
      }).catch(error => {
        console.error("获取案例数据失败", error);
        this.$message.error("获取案例数据失败");
        this.loading = false;
      });
    },
    
    // 根据服务类型获取物理模型
    getModelByServiceType(serviceType) {
      getDicts("sys_model_type").then(response => {
        const dictDatas = response.data;
        
        // 查找匹配的字典项
        const matchedDict = dictDatas.find(dict => dict.dictLabel === serviceType);
        
        if (matchedDict) {
          this.modelName = matchedDict.dictValue;
          this.updateModelContent();
        } else {
          this.$message.warning(`未找到"${serviceType}"对应的物理模型配置`);
        }
        this.loading = false;
      }).catch(error => {
        console.error("获取字典数据失败", error);
        this.$message.error("获取模型数据失败");
        this.loading = false;
      });
    },
    
    // 更新模型内容
    updateModelContent() {
      // 更新模型介绍
      if (this.modelIntroductions[this.modelName]) {
        this.modelIntroduction = this.modelIntroductions[this.modelName];
      } else {
        this.modelIntroduction = {
          name: this.modelName,
          fullName: this.modelName,
          description: "暂无详细介绍"
        };
      }
      
      // 初始化最优参数表格数据
      if (this.modelParameters[this.modelName]) {
        this.bestParameters = this.modelParameters[this.modelName].map(param => {
          // 预先生成一些随机参数值
          const range = param.range.split('-');
          const min = parseFloat(range[0]);
          const max = parseFloat(range[1]);
          const value = (min + Math.random() * (max - min)).toFixed(4);
          return { 
            name: param.name, 
            value: value, 
            range: param.range 
          };
        });
      } else {
        this.bestParameters = [];
      }
    },
    
    // 初始化图表
    initCharts() {
      console.log('初始化图表...');
      // 确保DOM元素已加载
      const bestChartDom = document.getElementById('best-chart');
      const previousChartDom = document.getElementById('previous-chart');
      const iterationChartDom = document.getElementById('iteration-chart');
      
      if (!bestChartDom || !previousChartDom || !iterationChartDom) {
        console.warn('Chart DOM elements not found, retrying in 500ms');
        setTimeout(() => this.initCharts(), 500);
        return;
      }
      
      console.log('找到图表DOM元素');
      
      try {
        // 销毁现有实例（如果存在）
        if (this.bestChart) {
          this.bestChart.dispose();
        }
        if (this.previousChart) {
          this.previousChart.dispose();
        }
        if (this.iterationChart) {
          this.iterationChart.dispose();
        }
        
        // 初始化图表
        this.bestChart = echarts.init(bestChartDom);
        this.previousChart = echarts.init(previousChartDom);
        this.iterationChart = echarts.init(iterationChartDom);
        
        console.log('ECharts实例已创建');
        
        // 生成模拟数据用于初始显示
        this.generateDemoData();
        
        // 生成模拟的迭代历史数据
        this.generateIterationHistory();
        
        // 更新图表
        this.updateCharts();
        
        // 添加窗口大小变化监听器
        window.addEventListener('resize', this.resizeCharts);
        
        // 延迟调整大小，确保渲染
        setTimeout(() => {
          console.log('强制重新调整图表大小');
          this.resizeCharts();
        }, 500);
      } catch (error) {
        console.error('初始化图表时出错:', error);
      }
    },
    
    // 重新调整图表大小
    resizeCharts() {
      if (this.bestChart) {
        this.bestChart.resize();
      }
      if (this.previousChart) {
        this.previousChart.resize();
      }
      if (this.iterationChart) {
        this.iterationChart.resize();
      }
    },
    
    // 生成演示数据
    generateDemoData() {
      const dates = [];
      this.simulatedData = [];
      this.observedData = [];
      this.previousSimulatedData = [];
      this.previousObservedData = [];
      
      // 生成VIC模型的水文数据（以月为单位的流量数据）
      // 使用2023年全年数据
      const baseDate = new Date('2023-01-01');
      for (let i = 0; i < 12; i++) {
        const date = new Date(baseDate);
        date.setMonth(baseDate.getMonth() + i);
        const monthStr = date.toISOString().slice(0, 7); // 格式为 YYYY-MM
        dates.push(monthStr);
        
        // 模拟长江上游流域的流量季节性变化
        // 冬季(12-2月)流量低，春季(3-5月)开始增加，夏季(6-8月)达到峰值，秋季(9-11月)逐渐降低
        let seasonFactor;
        if (i <= 1 || i === 11) { // 冬季月份
          seasonFactor = 0.3 + Math.random() * 0.2; // 低流量
        } else if (i >= 2 && i <= 4) { // 春季月份
          seasonFactor = 0.5 + Math.random() * 0.3; // 中等流量
        } else if (i >= 5 && i <= 7) { // 夏季月份
          seasonFactor = 0.8 + Math.random() * 0.4; // 高流量
        } else { // 秋季月份
          seasonFactor = 0.6 + Math.random() * 0.3; // 中高流量
        }
        
        // 基准流量 (m³/s)
        const baseFlow = 1000; 
        
        // 观测流量
        const observedValue = baseFlow * seasonFactor * (1 + Math.random() * 0.1 - 0.05);
        
        // 当前最优模拟结果 (与观测值相近但有小偏差)
        const simulatedValue = observedValue * (1 + Math.random() * 0.16 - 0.08);
        
        // 上一轮率定的结果 (偏差更大)
        const prevObservedValue = observedValue * (1 + Math.random() * 0.06 - 0.03);
        const prevSimulatedValue = prevObservedValue * (1 + Math.random() * 0.3 - 0.15);
        
        // 添加到数组
        this.observedData.push([monthStr, Math.round(observedValue)]);
        this.simulatedData.push([monthStr, Math.round(simulatedValue)]);
        this.previousObservedData.push([monthStr, Math.round(prevObservedValue)]);
        this.previousSimulatedData.push([monthStr, Math.round(prevSimulatedValue)]);
      }
    },
    
    // 生成迭代历史数据
    generateIterationHistory() {
      // 清空历史数据
      this.iterationHistory = [];
      
      // 生成15次迭代的NSE历史记录（当前迭代为15，对应界面上的当前进度）
      let nseValue = 0.2; // 初始NSE值
      for (let i = 1; i <= this.currentIteration; i++) {
        // NSE值呈现上升趋势，但有波动
        nseValue += Math.random() * 0.08 - 0.01;
        if (nseValue > 0.95) nseValue = 0.95;
        
        // 保存迭代次数和NSE值
        this.iterationHistory.push([i, parseFloat(nseValue.toFixed(4))]);
      }
      
      // 确保最后一个值匹配当前显示的maxNSE
      if (this.iterationHistory.length > 0) {
        this.iterationHistory[this.iterationHistory.length - 1][1] = this.maxNSE;
      }
      
      // 更新迭代图表
      this.updateIterationChart();
    },
    
    // 新增方法：更新迭代-NSE图表
    updateIterationChart() {
      if (!this.iterationChart) {
        return;
      }
      
      const option = {
        title: {
          text: '参数优化过程',
          left: 'center',
          top: 0,
          textStyle: {
            fontSize: 14
          }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            return `迭代次数: ${params[0].value[0]}<br/>NSE值: ${params[0].value[1]}`;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '10%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          name: '迭代次数',
          nameLocation: 'middle',
          nameGap: 25,
          minInterval: 1
        },
        yAxis: {
          type: 'value',
          name: 'NSE值',
          min: 0,
          max: 1,
          axisLabel: {
            formatter: '{value}'
          }
        },
        series: [
          {
            type: 'line',
            data: this.iterationHistory,
            lineStyle: {
              width: 2,
              color: '#409EFF'
            },
            itemStyle: {
              color: '#409EFF'
            },
            symbol: 'circle',
            symbolSize: 6,
            markPoint: {
              data: [
                { type: 'max', name: '最大值', symbolSize: 50 }
              ]
            }
          }
        ]
      };
      
      this.iterationChart.setOption(option);
    },
    
    // 更新图表
    updateCharts() {
      // 确保图表已初始化
      if (!this.bestChart || !this.previousChart) {
        console.warn('Charts not initialized yet');
        return;
      }
      
      // 更新最佳结果图表
      const bestOption = {
        title: {
          text: '最优结果模拟与实测对比',
          left: 'center',
          top: 0,
          textStyle: {
            fontSize: 14
          }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = params[0].name + '<br/>';
            params.forEach(param => {
              result += param.seriesName + ': ' + param.value[1] + ' m³/s<br/>';
            });
            return result;
          }
        },
        legend: {
          data: ['模拟值', '实测值'],
          bottom: 10
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.simulatedData.map(item => item[0]),
          axisLabel: {
            formatter: '{value}'
          }
        },
        yAxis: {
          type: 'value',
          name: '流量(m³/s)',
          axisLabel: {
            formatter: '{value}'
          }
        },
        series: [
          {
            name: '模拟值',
            type: 'line',
            data: this.simulatedData,
            lineStyle: {
              width: 2,
              color: '#409EFF'
            },
            symbol: 'circle',
            symbolSize: 6,
            smooth: true
          },
          {
            name: '实测值',
            type: 'line',
            data: this.observedData,
            lineStyle: {
              width: 2,
              color: '#F56C6C'
            },
            symbol: 'square',
            symbolSize: 6,
            smooth: true
          }
        ]
      };
      
      // 更新上一次率定图表
      const previousOption = {
        title: {
          text: '上一次率定模拟与实测对比',
          left: 'center',
          top: 0,
          textStyle: {
            fontSize: 14
          }
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = params[0].name + '<br/>';
            params.forEach(param => {
              result += param.seriesName + ': ' + param.value[1] + ' m³/s<br/>';
            });
            return result;
          }
        },
        legend: {
          data: ['模拟值', '实测值'],
          bottom: 10
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.previousSimulatedData.map(item => item[0]),
          axisLabel: {
            formatter: '{value}'
          }
        },
        yAxis: {
          type: 'value',
          name: '流量(m³/s)',
          axisLabel: {
            formatter: '{value}'
          }
        },
        series: [
          {
            name: '模拟值',
            type: 'line',
            data: this.previousSimulatedData,
            lineStyle: {
              width: 2,
              color: '#67C23A'
            },
            symbol: 'circle',
            symbolSize: 6,
            smooth: true
          },
          {
            name: '实测值',
            type: 'line',
            data: this.previousObservedData,
            lineStyle: {
              width: 2,
              color: '#E6A23C'
            },
            symbol: 'square',
            symbolSize: 6,
            smooth: true
          }
        ]
      };
      
      // 设置图表选项
      this.bestChart.setOption(bestOption);
      this.previousChart.setOption(previousOption);
      
      // 更新迭代图表
      this.updateIterationChart();
    },
    
    // 格式化进度条文本
    progressFormat(percentage) {
      return percentage === 100 ? '完成' : `${percentage}%`;
    },
    
    // 开始率定
    startCalibration() {
      if (this.isCalibrationRunning) {
        return;
      }
      
      this.isCalibrationRunning = true;
      this.currentIteration = 0;
      this.calibrationProgress = 0;
      this.maxNSE = 0;
      this.rSquared = 0;
      this.rmse = 0;
      this.pbias = 0;
      
      // 重置最优参数值
      if (this.bestParameters.length > 0) {
        this.bestParameters.forEach(param => {
          param.value = '-';
        });
      }
      
      // 清空迭代历史
      this.iterationHistory = [];
      
      // 更新案例描述为"参数率定"
      if (this.caseId) {
        // 先获取当前案例的完整信息
        getProject_service_case(this.caseId).then(response => {
          const caseData = response.data;
          // 设置描述为"参数率定"
          caseData.description = "参数率定";
          
          // 提交更新
          updateProject_service_case(caseData).then(res => {
            console.log("案例描述已更新为'参数率定'");
          }).catch(error => {
            console.error("更新案例描述失败", error);
          });
        }).catch(error => {
          console.error("获取案例信息失败", error);
        });
      }
      
      // 这里应该调用后端API开始真正的率定过程
      this.$message.success("开始率定参数");
      
      // 模拟率定过程的定时器
      this.timer = setInterval(() => {
        // 更新迭代次数和进度
        this.currentIteration++;
        this.calibrationProgress = Math.round((this.currentIteration / this.maxIterations) * 100);
        
        // 随机生成一个新的NSE值，有一定概率比当前最大值更高
        const newNSE = Math.min(0.95, this.maxNSE + (Math.random() * 0.1 - 0.02));
        
        // 将当前迭代的NSE值添加到历史记录
        this.iterationHistory.push([this.currentIteration, parseFloat(newNSE.toFixed(4))]);
        
        // 如果新NSE值更高，则更新最大NSE值和其他指标
        if (newNSE > this.maxNSE) {
          this.maxNSE = newNSE;
          this.rSquared = Math.min(0.95, this.maxNSE - 0.05 + Math.random() * 0.1);
          this.rmse = 10 - this.maxNSE * 8;
          this.pbias = (Math.random() * 6) - 3;
          
          // 更新最优参数值
          if (this.bestParameters.length > 0) {
            this.bestParameters.forEach(param => {
              // 从参数范围中生成一个随机值
              const range = param.range.split('-');
              const min = parseFloat(range[0]);
              const max = parseFloat(range[1]);
              param.value = (min + Math.random() * (max - min)).toFixed(4);
            });
          }
          
          // 生成新的模拟数据并更新图表
          this.generateDemoData();
          this.updateCharts();
        } else {
          // 即使NSE没有提高，也更新迭代图表
          this.updateIterationChart();
        }
        
        // 当达到最大迭代次数时，停止模拟
        if (this.currentIteration >= this.maxIterations) {
          clearInterval(this.timer);
          this.timer = null;
          this.isCalibrationRunning = false;
          this.$message.success("模型参数率定完成！");
        }
      }, 1000); // 每秒更新一次，模拟实时反馈
    },
    
    // 初始化演示参数
    initDemoParameters() {
      // 如果参数表还是空的或者是VIC模型，使用VIC参数示例
      if (this.modelName === "VIC" || this.bestParameters.length === 0) {
        // 典型VIC模型参数值
        const vicParams = [
          { name: "b_infilt", meaning: "渗透曲线参数", range: "0-0.4", value: "0.2375" },
          { name: "Ds", meaning: "排水速率", range: "0-1", value: "0.4826" },
          { name: "Dsmax", meaning: "最大基流速率", range: "0-30", value: "12.6831" },
          { name: "Ws", meaning: "地下水分数阈值", range: "0.5-1", value: "0.8652" },
          { name: "d1", meaning: "第一层土壤厚度", range: "0.05-0.15", value: "0.1015" },
          { name: "d2", meaning: "第二层土壤厚度", range: "0.1-1.0", value: "0.4362" },
          { name: "d3", meaning: "第三层土壤厚度", range: "0.1-1.5", value: "0.7845" },
          { name: "snow_rough", meaning: "雪面粗糙度", range: "0.001-0.01", value: "0.0053" }
        ];
        
        this.bestParameters = vicParams.map(param => {
          return {
            name: param.name,
            value: param.value,
            range: param.range
          };
        });
      }
    }
  }
};
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.calibration-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.calibration-header h4 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.calibration-content {
  margin: 0;
}

/* Card styling */
.el-card {
  margin-bottom: 20px;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1) !important;
}

.el-card:hover {
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15) !important;
  transition: all 0.3s ease;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 15px;
}

/* Progress card styling */
.progress-card {
  height: 340px;
}

.progress-info {
  padding: 10px 0;
}

.progress-item {
  margin-bottom: 15px;
}

.progress-stats {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 5px 0;
}

.stat-label {
  color: #606266;
  font-size: 13px;
  margin-right: 8px;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.highlight {
  color: #409EFF;
  font-size: 16px;
}

.parameters-table h5 {
  margin: 5px 0 10px;
  font-size: 14px;
  color: #606266;
}

/* Chart card styling */
.chart-card {
  height: 340px;
  overflow: hidden;
}

.full-height {
  height: 700px;
}

.chart-container {
  height: 250px;  /* 使用固定高度替代计算值 */
  padding-top: 10px;
  position: relative;
}

.chart {
  width: 100%;
  height: 250px;  /* 使用固定高度确保图表显示 */
  position: relative;
}

.chart-stats {
  display: flex;
  justify-content: space-around;
  padding: 10px 0;
  margin-top: 10px;
  border-top: 1px solid #EBEEF5;
}

.stat-panel {
  display: flex;
  flex-direction: column;
}

/* Make table compact */
.el-table--mini th, .el-table--mini td {
  padding: 4px 0;
}

/* 参数表格容器样式 */
.parameters-table {
  margin-top: 15px;
}

.table-wrapper {
  height: 180px;
  overflow-y: auto;
}

/* 添加了迭代曲线后的右侧面板样式 */
.chart-section {
  margin-bottom: 15px;
}

.section-title {
  font-size: 14px;
  color: #606266;
  margin: 5px 0;
  font-weight: 500;
}

.chart-container {
  height: 235px;
  position: relative;
}

.nse-chart-container {
  height: 200px;
}

.chart {
  width: 100%;
  height: 100%;
  position: relative;
}

/* 右侧面板高度调整 */
.full-height {
  height: 700px;
  display: flex;
  flex-direction: column;
}

.chart-stats {
  margin-top: auto;
}
</style>
