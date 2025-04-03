<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="24" style="padding-left: 20px">
        <el-row>
            <h2>物理模型概述</h2>
        <el-table :data="serviceData" border stripe stripe-class="light-blue-row" style="width: 100%" class="custom-table">
          <el-table-column prop="serviceName" label="服务名称" width="120"></el-table-column>
          <el-table-column prop="serviceDefinition" label="服务定义"></el-table-column>
          <el-table-column prop="physicalModel" label="物理模型"></el-table-column>
          <el-table-column prop="physicalIndicator" label="物理指标"></el-table-column>
          <el-table-column prop="paraCalibration" label="参数率定方法"></el-table-column>
          <el-table-column prop="modeCalculation" label="并行计算框架"></el-table-column>
        </el-table>
        </el-row>
      </el-col>
    </el-row>
    
    <!-- 添加开始使用按钮 -->
    <divider/>

    <el-row style="margin-top: 20px; margin-bottom: 20px;">
      <el-col :span="24" style="text-align: center;">
        <el-button type="primary" size="large" @click="handleStartUsing" style="background-color: #336699; border-color: #336699;">开始使用</el-button>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "Index",
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
          climate_regulation: "食物供给"
        },
        {
          attribute: "服务定义",
          water_conservation: "枯水期基流中众数",
          water_supply: "年均径流量",
          soil_conservation: "土壤侵蚀量",
          water_purification: "氮磷负荷量",
          flood_regulation: "洪峰淹没面积，持续时间",
          windbreak_sand: "沙尘通量",
          carbon_fixation: "植被固碳量",
          climate_regulation: "干物质量"
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
          climate_regulation: "DSSAT"
        },
        {
          attribute: "物理指标",
          water_conservation: "枯水期基流中众数",
          water_supply: "年均径流量",
          soil_conservation: "土壤侵蚀量",
          water_purification: "氮磷负荷量",
          flood_regulation: "洪峰淹没面积，持续时间",
          windbreak_sand: "沙尘通量",
          carbon_fixation: "植被固碳量",
          climate_regulation: "干物质量"
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
          climate_regulation: "单/多目标贝叶斯优化"
        },
        {
          attribute: "并行计算方法",
          water_conservation: "Mesos",
          water_supply: "Mesos",
          soil_conservation: "Mesos",
          water_purification: "Mesos",
          flood_regulation: "Mesos",
          windbreak_sand: "Mesos",
          carbon_fixation: "Mesos",
          climate_regulation: "Mesos"
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
  overflow-x: hidden;

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
        border-bottom: 1px solid #294c73;
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
</style>