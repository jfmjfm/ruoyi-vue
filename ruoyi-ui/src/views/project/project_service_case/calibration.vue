<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 第一部分：模型介绍 -->
      <el-col :span="12" class="equal-height-col">
        <el-card class="box-card" shadow="hover" v-loading="loading">
          <div slot="header" class="custom-card-header">
            <span>{{ modelIntroduction.name || 'SWAT' }}模型介绍</span>
          </div>
          <div class="text-content card-body text-card-body position-relative">
            <p>{{ modelIntroduction.description || 'SWAT（Soil and Water Assessment Tool）是一个流域尺度的水文模型，用于预测土地管理措施对水、泥沙和农业化学品产量的影响。' }}</p>
            <p>主要特点：</p>
            <ul>
              <li v-for="(feature, index) in (modelIntroduction.features || ['物理过程基础：基于物理方程模拟水循环过程', '分布式参数：能够表示空间异质性', '连续模拟：能够模拟长时间序列的水文过程', '综合性：能够模拟水文、泥沙、水质等多种过程'])" :key="index">{{ feature }}</li>
            </ul>
            <p>{{ modelIntroduction.applications || 'SWAT模型广泛应用于流域水文模拟、非点源污染评估、气候变化影响研究和土地利用规划等领域。' }}</p>
            
            <!-- 模型图片 -->
            <div class="model-image">
              <img :src="getModelImage()" alt="模型示意图" />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 第二部分：模型参数表格 -->
      <el-col :span="12" class="equal-height-col">
        <el-card class="box-card" shadow="hover" v-loading="loading">
          <div slot="header" class="custom-card-header">
            <span>模型率定参数</span>
          </div>
          <div class="card-body table-card-body">
            <el-table 
              :data="parameterData" 
              border 
              style="width: 100%" 
              height="230"
              class="parameter-table">
              <el-table-column prop="name" label="参数名称" width="120"></el-table-column>
              <el-table-column prop="meaning" label="物理含义"></el-table-column>
              <el-table-column prop="range" label="取值范围" width="150"></el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20">
      <!-- 第三部分：贝叶斯优化算法介绍 -->
      <el-col :span="12" class="equal-height-col">
        <el-card class="box-card" shadow="hover">
          <div slot="header" class="custom-card-header">
            <span>贝叶斯优化算法介绍</span>
          </div>
          <div class="text-content card-body text-card-body">
            <p>贝叶斯优化（Bayesian Optimization）是一种用于优化黑盒函数（昂贵评估或未知导数）的序贯设计策略。</p>
            <p>核心特点：</p>
            <ul>
              <li>代理模型：使用高斯过程（Gaussian Process）构建目标函数的概率模型</li>
              <li>采集函数：平衡探索与利用，确定下一个采样点</li>
              <li>样本效率：通常需要较少的函数评估即可找到全局最优解</li>
              <li>概率框架：提供优化结果的不确定性估计</li>
            </ul>
            <p>在模型参数率定中，贝叶斯优化能高效探索参数空间，快速收敛到最优参数组合，显著减少计算资源消耗。</p>
          </div>
        </el-card>
      </el-col>

      <!-- 第四部分：算法率定参数设置 -->
      <el-col :span="12" class="equal-height-col">
        <el-card class="box-card" shadow="hover">
          <div slot="header" class="custom-card-header">
            <span>算法率定参数设置</span>
          </div>
          <div class="card-body form-card-body">
            <el-form ref="settingForm" :model="settingForm" label-width="180px" size="small">
              <el-form-item label="初始样本数量" prop="initialSamples">
                <el-input-number v-model="settingForm.initialSamples" :min="5" :max="50"></el-input-number>
                <span class="param-desc">初始构建代理模型的样本点数量</span>
              </el-form-item>
              <el-form-item label="最大迭代次数" prop="maxIterations">
                <el-input-number v-model="settingForm.maxIterations" :min="10" :max="200"></el-input-number>
                <span class="param-desc">算法最大迭代次数</span>
              </el-form-item>
              <el-form-item label="选择采集函数" prop="acquisitionFunction">
                <el-select v-model="settingForm.acquisitionFunction" placeholder="请选择采集函数">
                  <el-option label="预期改进(EI)" value="ei"></el-option>
                  <el-option label="置信上界(UCB)" value="ucb"></el-option>
                  <el-option label="概率改进(PI)" value="pi"></el-option>
                </el-select>
                <span class="param-desc">决定下一个采样点的策略</span>
              </el-form-item>
              <el-form-item label="选择目标函数" prop="objectiveFunction">
                <el-select v-model="settingForm.objectiveFunction" placeholder="请选择目标函数">
                  <el-option label="Nash-Sutcliffe效率系数(NSE)" value="nse"></el-option>
                  <el-option label="平均绝对误差(MAE)" value="mae"></el-option>
                  <el-option label="均方根误差(RMSE)" value="rmse"></el-option>
                </el-select>
                <span class="param-desc">模型评价指标</span>
              </el-form-item>
            <el-form-item label="上传观测数据">
              <el-tooltip content="严格按照模板格式提供观察数据" placement="top">
                <el-button type="primary" size="small" @click="downloadTemplate">下载模板</el-button>
              </el-tooltip>
              <el-tooltip content="请先点击'下载模板'按钮" placement="top" :disabled="templateDownloaded">
                <el-button type="primary" size="small" @click="uploadData" :disabled="!templateDownloaded">上传数据</el-button>
              </el-tooltip>
              <el-tooltip content="请先点击'上传数据'按钮" placement="top" :disabled="dataUploaded">
                <el-button type="primary" size="small" @click="viewData" :disabled="!dataUploaded">查看数据</el-button>
              </el-tooltip>
              <span class="param-desc">请上传观测数据并检查数据格式</span>
            </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加一个底部按钮区域 -->
    <div class="action-bar">
      <el-tooltip content="请先上传观测数据" placement="top" :disabled="dataUploaded">
        <el-button type="primary" @click="startCalibration" size="medium" :disabled="!dataUploaded">下一步</el-button>
      </el-tooltip>
    </div>
  </div>
</template>

<script>
import { getProject_service_case } from "@/api/project/project_service_case";
import { getDicts } from "@/api/system/dict/data";

export default {
  name: "Calibration",
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
      // 参数表格数据
      parameterData: [],
      // 算法参数设置表单
      settingForm: {
        initialSamples: 20,
        maxIterations: 50,
        acquisitionFunction: 'ei',
        objectiveFunction: 'nse'
      },
      // 从路由中获取ID参数
      caseId: this.$route.query.id || null,
      // 加载状态
      loading: false,
      
      // 按钮状态控制
      templateDownloaded: false,
      dataUploaded: false,
      
      // 模型介绍数据映射
      modelIntroductions: {
        "VIC": {
          name: "VIC",
          fullName: "可变渗透能力模型(Variable Infiltration Capacity)",
          description: "VIC是一个大尺度、半分布式水文模型，主要用于模拟流域水文过程，尤其是水源涵养与水源供给服务评估。",
          features: [
            "多层土壤结构：精确模拟土壤水分运动",
            "可变渗透能力曲线：反映降雨入渗过程",
            "考虑植被影响：精确模拟蒸散发过程",
            "网格化模拟：适合大尺度流域应用"
          ],
          applications: "水源涵养评估、径流模拟、气候变化影响研究"
        },
        "SWAT": {
          name: "SWAT",
          fullName: "土壤与水分评估工具(Soil and Water Assessment Tool)",
          description: "SWAT是一个流域尺度模型，能够模拟长时间序列下复杂流域的水文和水质过程，特别适用于土壤保持与水质净化服务评估。",
          features: [
            "物理过程基础：基于物理方程模拟水循环",
            "分布式参数：考虑空间异质性",
            "连续模拟：支持长时间序列模拟",
            "综合性：能模拟水文、泥沙、养分等多过程"
          ],
          applications: "土壤侵蚀评估、非点源污染模拟、土地利用变化影响研究"
        },
        "HSPF": {
          name: "HSPF",
          fullName: "水文模拟程序-FORTRAN(Hydrological Simulation Program-FORTRAN)",
          description: "HSPF是一个综合性流域模型，用于模拟流域中的水文、水质和生态过程，结合DELFT3D可实现洪水调蓄服务评估。Delft3D是一款水动力数值模拟软件，用于模拟河流、河口、海岸及近海区域的水流、泥沙输运、水质及生态过程。",
          features: [
            "综合模拟：整合水文、水质和生态过程",
            "灵活性高：可模拟不同时间尺度的过程",
            "与DELFT3D耦合：实现水力学精细模拟",
            "多种算法：支持复杂水文过程计算"
          ],
          applications: "洪水调蓄评估、城市水文模拟、流域综合管理"
        },
        "RWEQ": {
          name: "RWEQ",
          fullName: "修正风蚀方程(Revised Wind Erosion Equation)",
          description: "RWEQ是一个基于过程的风蚀预测模型，专门用于评估土壤风蚀量和防风固沙服务。",
          features: [
            "基于物理过程：考虑地表特性和气象因素",
            "空间分布：能够评估不同空间位置的风蚀状况",
            "敏感性分析：评估不同因素对风蚀的影响",
            "季节性模拟：考虑季节变化对风蚀的影响"
          ],
          applications: "防风固沙评估、荒漠化防治、土地利用规划"
        },
        "LPJ": {
          name: "LPJ",
          fullName: "陆地生态系统动态全球植被模型(Lund-Potsdam-Jena)",
          description: "LPJ是一个动态全球植被模型，用于模拟植被动态、碳循环和水循环，特别适用于固碳服务评估。",
          features: [
            "植被动态：模拟植被生长、死亡和竞争",
            "碳循环：精确模拟碳储存和碳通量",
            "气候响应：考虑气候变化对植被的影响",
            "生态系统过程：整合水循环和养分循环"
          ],
          applications: "固碳服务评估、气候变化影响研究、生态系统管理"
        },
        "DSSAT": {
          name: "DSSAT",
          fullName: "决策支持系统与农业技术转让(Decision Support System for Agrotechnology Transfer)",
          description: "DSSAT是一个作物模型系统，用于模拟作物生长、产量和环境影响，适用于食物供给服务评估。",
          features: [
            "多作物模拟：支持多种作物类型",
            "精细过程：模拟作物生长的生理过程",
            "管理方案：评估不同管理措施的效果",
            "环境响应：考虑气候、土壤和管理的综合影响"
          ],
          applications: "食物供给评估、农业管理优化、气候变化适应研究"
        },
        "WRF": {
          name: "WRF",
          fullName: "天气研究与预报(Weather Research and Forecasting)模型",
          description: "WRF是一个大气模型，用于模拟大气运动、天气变化和气候模式，特别适用于气候变化影响研究。",
          features: [
            "大气模拟：模拟大气运动、天气变化",
            "气候模式：模拟长期气候变化",
            "气候响应：考虑气候变化对植被的影响",
            "生态系统过程：整合水循环和养分循环"
          ],
          applications: "气象预报、极端天气研究、空气质量模拟、气候建模等"
        },
      },
      
      // 模型参数数据映射
      modelParameters: {
        "VIC": [
          { name: "b_infilt", meaning: "渗透曲线参数", range: "0-0.4" },
          { name: "Ds", meaning: "排水速率", range: "0-1" },
          { name: "Dsmax", meaning: "最大基流速率", range: "0-30" },
          { name: "Ws", meaning: "地下水分数阈值", range: "0.5-1" },
          { name: "d1", meaning: "第一层土壤厚度", range: "0.05-0.15" },
          { name: "d2", meaning: "第二层土壤厚度", range: "0.1-1.0" },
          { name: "d3", meaning: "第三层土壤厚度", range: "0.1-1.5" },
          { name: "snow_rough", meaning: "雪面粗糙度", range: "0.001-0.01" }
        ],
        "SWAT": [
          { name: "CN2", meaning: "SCS径流曲线值", range: "35-98" },
          { name: "ALPHA_BF", meaning: "基流衰退常数", range: "0-1" },
          { name: "GW_DELAY", meaning: "地下水延迟时间", range: "0-500" },
          { name: "GWQMN", meaning: "浅层含水层回归流阈值", range: "0-5000" },
          { name: "ESCO", meaning: "土壤蒸发补偿系数", range: "0-1" },
          { name: "SOL_AWC", meaning: "有效水含量", range: "0-1" },
          { name: "SOL_K", meaning: "饱和导水率", range: "0-2000" },
          { name: "CH_N2", meaning: "主河道曼宁粗糙系数", range: "0.01-0.3" }
        ],
        "HSPF": [
          { name: "LZSN", meaning: "下层土壤区域标称储存", range: "2-15" },
          { name: "INFILT", meaning: "入渗参数", range: "0.001-0.5" },
          { name: "KVARY", meaning: "地下水衰退参数", range: "0-5" },
          { name: "AGWRC", meaning: "基本地下水衰退率", range: "0.85-0.999" },
          { name: "INTFW", meaning: "间流渗透比率", range: "1-10" },
          { name: "IRC", meaning: "间流衰退参数", range: "0.3-0.85" },
          { name: "LZETP", meaning: "下层蒸发系数", range: "0.1-0.9" },
          { name: "SLSUR", meaning: "坡度", range: "0.001-0.3" }
        ],
        "RWEQ": [
          { name: "WF", meaning: "气象因子", range: "0-500" },
          { name: "EF", meaning: "可蚀性因子", range: "0-1" },
          { name: "SCF", meaning: "土壤粗糙度因子", range: "0-1" },
          { name: "K", meaning: "土壤结皮因子", range: "0-1" },
          { name: "COG", meaning: "植被盖度", range: "0-1" },
          { name: "Z", meaning: "临界场长", range: "10-1000" },
          { name: "Qmax", meaning: "最大输沙量", range: "0-500" },
          { name: "Sλ", meaning: "临界切变速度", range: "0.1-1" }
        ],
        "LPJ": [
          { name: "k_beer", meaning: "消光系数", range: "0.4-0.7" },
          { name: "alpha_a", meaning: "光合作用效率", range: "0.3-0.7" },
          { name: "respcoeff", meaning: "呼吸系数", range: "0.15-0.35" },
          { name: "k_mort1", meaning: "死亡率系数1", range: "0.01-0.1" },
          { name: "k_mort2", meaning: "死亡率系数2", range: "0.01-0.1" },
          { name: "fireresist", meaning: "耐火性", range: "0-1" },
          { name: "GDD5min", meaning: "最小生长度日", range: "0-2000" },
          { name: "Tcmin", meaning: "最小耐寒温度", range: "-40-5" }
        ],
        "DSSAT": [
          { name: "P1", meaning: "幼穗期发育率", range: "100-500" },
          { name: "P2", meaning: "光周期敏感系数", range: "0-5" },
          { name: "P5", meaning: "粒子充实期发育率", range: "500-1000" },
          { name: "G1", meaning: "粒子数系数", range: "0-100" },
          { name: "G2", meaning: "粒重系数", range: "0.1-1" },
          { name: "G3", meaning: "茎秆干重系数", range: "0.1-10" },
          { name: "PHINT", meaning: "叶片出现间隔", range: "30-100" },
          { name: "SKERWT", meaning: "标准粒重", range: "20-80" }
        ],
      "WRF": [
          { "name": "SF_SURFACE_PHYSICS", "meaning": "陆面过程参数化方案", "range": "1-5（如1=热扩散方案，2=Noah LSM等）" },  
          { "name": "BL_PBL_PHYSICS", "meaning": "边界层参数化方案", "range": "1-11（如1=YSU方案，2=MYJ方案等）" },  
          { "name": "CU_PHYSICS", "meaning": "积云对流参数化方案", "range": "1-7（如1=Kain-Fritsch，6=Tiedtke等）" },  
          { "name": "RA_LW_PHYSICS", "meaning": "长波辐射参数化方案", "range": "1-4（如1=RRTM方案）" },  
          { "name": "RA_SW_PHYSICS", "meaning": "短波辐射参数化方案", "range": "1-4（如1=Dudhia方案）" },  
          { "name": "MP_PHYSICS", "meaning": "微物理过程参数化方案", "range": "1-28（如8=Thompson方案）" },  
          { "name": "SST_UPDATE", "meaning": "海表温度更新选项", "range": "0或1（1=启用）" },  
          { "name": "ISFFLX", "meaning": "地表热通量计算开关", "range": "0或1（1=启用）" }  
      ]
      },
      
      // 模型图片映射
      modelImages: {
        "VIC": require("@/assets/images/VIC_model.png"),
        "SWAT": require("@/assets/images/SWAT_model.png"),
        "HSPF": require("@/assets/images/HSPF_model.png"),
        "RWEQ": require("@/assets/images/RWEQ_model.png"),
        "LPJ": require("@/assets/images/LPJ_model.png"),
        "DSSAT": require("@/assets/images/DSSAT_model.png"),
        "WRF": require("@/assets/images/WRF_model.png")
      }
    };
  },
  created() {
    // 如果有ID参数，加载对应案例的数据
    if (this.caseId) {
      this.loadCaseData(this.caseId);
    }
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
          description: "暂无详细介绍",
          features: ["暂无特点信息"],
          applications: "暂无应用信息"
        };
      }
      
      // 更新参数表格
      if (this.modelParameters[this.modelName]) {
        this.parameterData = this.modelParameters[this.modelName];
      } else {
        this.parameterData = [];
      }
    },
    
    // 开始率定
    startCalibration() {
      // 这里添加调用率定API的代码
      this.$message.success("开始率定参数，请在[系统监控/率定任务列表]中查看进度");
      this.$router.push({
        path: '/project/project_calibrationdetail',
        query: { id: this.caseId }
      });
    },
    
    // 重置表单
    resetForm(formName) {
      this.$refs[formName].resetFields();
    },
    
    // 获取模型图片
    getModelImage() {
      if (this.modelName && this.modelImages[this.modelName]) {
        return this.modelImages[this.modelName];
      }
      // 如果找不到对应模型图片，则根据服务类型选择默认图片
      if (this.serviceType === "水源涵养") {
        return require("@/assets/images/VIC_model.png");
      } else if (this.serviceType === "土壤保持") {
        return require("@/assets/images/SWAT_model.png");
      } else if (this.serviceType === "洪水调蓄") {
        return require("@/assets/images/HSPF_model.png");
      } else if (this.serviceType === "防风固沙") {
        return require("@/assets/images/RWEQ_model.png");
      } else if (this.serviceType === "固碳") {
        return require("@/assets/images/LPJ_model.png");
      } else if (this.serviceType === "食物供给") {
        return require("@/assets/images/DSSAT_model.png");
      } else {
        return require("@/assets/images/SWAT_model.png"); // 默认图片
      }
    },
    
    // 下载模板
    downloadTemplate() {
      this.$message.success("模板下载成功");
      this.templateDownloaded = true;
    },
    
    // 上传数据
    uploadData() {
      this.$message.success("数据上传成功");
      this.dataUploaded = true;
    },
    
    // 查看数据
    viewData() {
      this.$message.success("查看数据");
    },
  }
};
</script>

<style scoped>
.text-content {
  font-size: 13px;
  line-height: 1.4;
  color: #606266;
}

.text-content p {
  margin-bottom: 6px;
}

.text-content ul {
  padding-left: 18px;
  margin-bottom: 6px;
  margin-top: 6px;
}

.param-desc {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}

.box-card {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
}

.card-body {
  height: 250px;
  overflow-y: auto;
  padding: 5px 0;
}

.el-table {
  margin-top: 0;
  height: calc(100% - 10px) !important;
}

.el-form-item {
  margin-bottom: 12px;
}

.custom-card-header {
  background-color: #33495e;
  color: white;
  padding: 8px 12px;
  margin: -20px -20px 0;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
  font-weight: bold;
  font-size: 15px;
}

/* 覆盖Element UI的默认样式 */
.el-card {
  border: none;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,.1) !important;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.el-card__body {
  flex: 1;
  padding: 15px;
  display: flex;
  flex-direction: column;
}

.el-card:hover {
  box-shadow: 0 4px 18px 0 rgba(0,0,0,.15) !important;
  transition: all 0.3s ease;
}

.el-table th {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: bold;
}

.el-form-item__label {
  font-weight: 500;
}

.el-button--primary {
  background-color: #33495e;
  border-color: #33495e;
}

.el-button--primary:hover,
.el-button--primary:focus {
  background-color: #4a6b8a;
  border-color: #4a6b8a;
}

.el-row {
  margin-bottom: 0;
}

/* 添加一个底部按钮区域 */
.action-bar {
  text-align: center;
  margin-top: 10px;
  margin-bottom: 5px;
}

.action-bar .el-button {
  min-width: 120px;
}

/* 确保所有卡片高度一致 */
.el-col {
  display: flex;
  flex-direction: column;
}

.table-card-body {
  padding: 5px 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.table-card-body .el-table {
  flex: 1;
}

.parameter-table {
  margin: 0;
}

.form-card-body {
  padding: 5px 0;
}

.form-card-body .el-form-item {
  margin-bottom: 10px;
}

.text-card-body {
  padding: 10px 5px 180px 5px;
}

.text-card-body p:first-child {
  margin-top: 0;
}

.text-card-body p:last-child {
  margin-bottom: 0;
}

.equal-height-col {
  margin-bottom: 15px;
}

.position-relative {
  position: relative;
}

.model-image {
  position: absolute;
  right: 15px;
  bottom: 15px;
  width: 240px;
  height: 190px;
  overflow: hidden;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.15);
  z-index: 10;
}

.model-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background-color: #f9f9f9;
  padding: 5px;
}
</style>
