## 开发

```bash
# 克隆项目
git clone https://gitee.com/y_project/RuoYi-Vue

# 进入项目目录
cd ruoyi-ui

# 安装依赖
npm install

# 建议不要直接使用 cnpm 安装依赖，会有各种诡异的 bug。可以通过如下操作解决 npm 下载速度慢的问题
npm install --registry=https://registry.npmmirror.com

# 启动服务
npm run dev
```

浏览器访问 http://localhost:80

## 发布

```bash
# 构建测试环境
npm run build:stage

# 构建生产环境
npm run build:prod
```

graph TB
    A[RuoYi-Vue项目] --> B[前端ruoyi-ui]
    A --> C[后端服务]
    
    B --> B1[Vue 2.6.12]
    B --> B2[Element UI]
    B --> B3[Axios]
    B --> B4[Webpack]

    C --> D[核心模块]
    C --> E[业务模块]
    
    D --> D1[ruoyi-admin<br/>后台服务入口]
    D --> D2[ruoyi-framework<br/>框架核心]
    D --> D3[ruoyi-common<br/>公共工具]
    D --> D4[ruoyi-system<br/>系统管理]
    
    E --> E1[用户认证]
    E --> E2[权限管理]
    E --> E3[系统监控]
    E --> E4[开发工具]
    
    E1 --> E1A[JWT认证]
    E1 --> E1B[多终端认证]
    
    E2 --> E2A[用户管理]
    E2 --> E2B[角色管理]
    E2 --> E2C[菜单管理]
    E2 --> E2D[部门管理]
    
    E3 --> E3A[操作日志]
    E3 --> E3B[登录日志] 
    E3 --> E3C[服务监控]
    E3 --> E3D[在线用户]
    
    E4 --> E4A[代码生成]
    E4 --> E4B[接口文档]
    E4 --> E4C[表单构建]

    classDef default fill:#f9f9f9,stroke:#333,stroke-width:2px;
    classDef highlight fill:#e1f3d8,stroke:#333,stroke-width:2px;
    
    class A,B,C highlight;