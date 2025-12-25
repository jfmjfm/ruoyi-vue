<template>
  <div class="app-container home">
    <el-row :gutter="20" style="width: 100%;">
      <el-col :sm="24" :lg="24" style="padding-left: 20px; width: 100%;">
        <el-row style="width: 100%;">
            <h3 class="section-title">物理模型概述</h3>
        <el-row :gutter="20">
          <el-col :span="3">
            <div class="model-container">
              <img src="@/assets/images/VIC_model.png" alt="VIC模型" class="model-image">
              <p class="model-name">VIC模型</p>
            </div>
          </el-col>
          <el-col :span="3">
            <div class="model-container">
              <img src="@/assets/images/SWAT_model.png" alt="SWAT模型" class="model-image">
              <p class="model-name">SWAT模型</p>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="model-container">
              <img src="@/assets/images/RWEQ_model.png" alt="RWEQ模型" class="model-image">
              <p class="model-name">RWEQ模型</p>
            </div>
          </el-col>
          <el-col :span="4">
            <div class="model-container">
              <img src="@/assets/images/HSPF_model.png" alt="HSPF模型" class="model-image">
              <p class="model-name">HSPF模型</p>
            </div>
          </el-col>
          <el-col :span="3">
            <div class="model-container">
              <img src="@/assets/images/LPJ_model.png" alt="LPJ模型" class="model-image">
              <p class="model-name">LPJ模型</p>
            </div>
          </el-col>
          <el-col :span="3">
            <div class="model-container">
              <img src="@/assets/images/DSSAT_model.png" alt="DSSAT模型" class="model-image">
              <p class="model-name">DSSAT模型</p>
            </div>
          </el-col>
          <el-col :span="3">
            <div class="model-container">
              <img src="@/assets/images/WRF_model.png" alt="WRF模型" class="model-image">
              <p class="model-name">WRF模型</p>
            </div>
          </el-col>
        </el-row>
        <el-table :data="serviceData" border stripe stripe-class="light-blue-row" style="width: 100%" class="custom-table">
          <el-table-column prop="serviceName" label="服务名称" width="120"></el-table-column>
          <el-table-column prop="serviceDefinition" label="服务定义"></el-table-column>
          <el-table-column prop="physicalModel" label="物理模型"></el-table-column>
          <el-table-column prop="physicalIndicator" label="物理指标"></el-table-column>
          <el-table-column prop="paraCalibration" label="参数率定方法"></el-table-column>
          <el-table-column prop="modeCalculation" label="学生突出贡献"></el-table-column>
        </el-table>
        </el-row>
      </el-col>
    </el-row>
    
    <!-- 添加开始使用按钮 -->
    <el-divider></el-divider>

    <el-row style="margin-top: 20px; margin-bottom: 20px;">
      <el-col :span="24" style="text-align: center;">
        <el-button type="primary" size="large" @click="handleStartUsing" style="background-color: #34495e; border-color: #34495e;">开始使用</el-button>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "Index",
  // mounted 钩子不再需要，因为滚动条现在由 app-main 容器自动处理
  data() {
    return {
      // 版本号
      version: "3.8.9",
      // 原始表格数据
      tableData: [
        {
          attribute: "服务名称",
          water_conservation: "水源涵养",
          water_supply: "水源供给",
          soil_conservation: "土壤保持",
          water_purification: "水质净化",
          flood_regulation: "洪水调蓄",
          windbreak_sand: "防风固沙",
          carbon_fixation: "固碳服务",
          climate_regulation: "气候调节",
          food_supply: "食物供给"
        },
        {
          attribute: "服务定义",
          water_conservation: "枯水期基流众数",
          water_supply: "年均径流量",
          soil_conservation: "土壤侵蚀量",
          water_purification: "氮磷负荷量",
          flood_regulation: "洪峰，淹没面积，持续时间",
          windbreak_sand: "沙尘通量",
          carbon_fixation: "植被固碳量",
          climate_regulation: "空气质量",
          food_supply: "干物质量"
        },
        {
          attribute: "物理模型",
          water_conservation: "VIC",
          water_supply: "VIC",
          soil_conservation: "SWAT",
          water_purification: "SWAT",
          flood_regulation: "HSPF+DELFT3D",
          windbreak_sand: "RWEQ",
          carbon_fixation: "LPJ",
          climate_regulation: "WRF",
          food_supply: "DSSAT"
        },
        {
          attribute: "物理指标",
          water_conservation: "枯水期基流中众数",
          water_supply: "年均径流量",
          soil_conservation: "土壤侵蚀量",
          water_purification: "氮磷负荷量",
          flood_regulation: "洪峰，淹没面积，持续时间",
          windbreak_sand: "沙尘通量",
          carbon_fixation: "植被固碳量",
          climate_regulation: "空气质量",
          food_supply: "干物质量"
        },
        {
          attribute: "参数率定方法",
          water_conservation: "Borg,单/多目标贝叶斯优化",
          water_supply: "Borg,单/多目标贝叶斯优化",
          soil_conservation: "单/多目标贝叶斯优化",
          water_purification: "单/多目标贝叶斯优化",
          flood_regulation: "单/多目标贝叶斯优化",
          windbreak_sand: "单/多目标贝叶斯优化",
          carbon_fixation: "单/多目标贝叶斯优化",
          climate_regulation: "单/多目标贝叶斯优化",
          food_supply: "单/多目标贝叶斯优化"
        },
        {
          attribute: "学生突出贡献",
          water_conservation: "Mesos",
          water_supply: "Mesos",
          soil_conservation: "张京，李东升",
          water_purification: "张京，李东升",
          flood_regulation: "徐子萱，古慧敏，刘阳",
          windbreak_sand: "徐慧",
          carbon_fixation: "朱靓怡",
          climate_regulation: "仇良成",
          food_supply: "梁梅"
        }
      ]
    };
  },
  computed: {
    // 处理服务数据以适应表格展示
    serviceData() {
      const keys = Object.keys(this.tableData[0]).filter(key => key !== 'attribute');
      const result = [];
      
      keys.forEach(key => {
        result.push({
          serviceName: this.tableData[0][key],
          serviceDefinition: this.tableData[1][key],
          physicalModel: this.tableData[2][key],
          physicalIndicator: this.tableData[3][key],
          paraCalibration:this.tableData[4][key],
          modeCalculation:this.tableData[5][key]
        });
      });
      
      return result;
    }
  },
  methods: {
    goTarget(href) {
      window.open(href, "_blank");
    },
    // 添加开始使用按钮点击处理方法
    handleStartUsing() {
      this.$router.push('/project/project_region');
    }
  }
};
</script>

<style scoped lang="scss">
.home {
  // 确保容器可以完整显示所有内容
  min-height: 100%;
  height: auto !important; // 强制高度自动适应内容
  overflow: visible; // 改为 visible，让 app-main 处理滚动
  padding-bottom: 40px; // 底部留出空间
  
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }
  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }
  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .update-log {
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
  }
}

.custom-table {
  ::v-deep .el-table__row--striped {
    background-color: rgb(153, 204, 255); // 更改为指定的偶数行颜色
  }
  
  ::v-deep .el-table__header-wrapper {
    background-color: rgb(48, 65, 86);
    
    .el-table__header {
      background-color: rgb(48, 65, 86) !important;
      
      th.el-table__cell {
        background-color: rgb(48, 65, 86) !important;
        color: white !important;
        font-weight: 600;
        border-bottom: 1px solidrgb(219, 95, 12);
        padding: 12px 0;
        text-align: center !important; /* 表头文字居中 */
      }
    }
  }
  
  ::v-deep .el-table__row {
    height: 50px; // 增加行高让表格更加大气
  }
  
  ::v-deep .el-table--border {
    border-radius: 4px;
    overflow: hidden;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  }
  
  /* 移除表头的默认背景色和悬停效果 */
  ::v-deep .el-table th.is-leaf {
    background-color: rgb(48, 65, 86) !important;
  }
  
  ::v-deep .el-table__fixed-header-wrapper th.el-table__cell {
    background-color: rgb(48, 65, 86) !important;
  }
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #34495e;
  margin-bottom: 15px;
  margin-top: 0px;
}

.model-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 10px;
  margin-top: 10px;
}

.model-image {
  width: 100%;
  height: 135px;
  object-fit: contain;
  border-radius: 4px;
  background-color: #f7f7f7;
  padding: 10px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  transition: transform 0.3s;
}

.model-image:hover {
  transform: scale(1.25);
}

.model-name {
  text-align: center;
  margin-top: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}
</style>

<style lang="scss">
// 全局样式（不带 scoped），确保父容器不限制高度
.app-container.home {
  height: auto !important; // 自动高度，根据内容扩展
  min-height: 100% !important; // 至少占满父容器
  max-height: none !important; // 移除最大高度限制
  overflow: visible !important; // 让内容可以完全显示
  padding: 20px !important; // 添加内边距
}

// 确保Element UI的行和列不限制高度
.app-container.home .el-row {
  height: auto !important;
  min-height: auto !important;
}

.app-container.home .el-col {
  height: auto !important;
  min-height: auto !important;
}
</style>