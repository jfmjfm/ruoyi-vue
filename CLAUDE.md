# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RuoYi-Vue is an ecological value accounting platform built as a modular monorepo with Spring Boot 2.5.15/Java 8 backend and Vue 2.6.12 frontend. The system integrates environmental modeling tools (DSSAT, HSPF, SWAT, VIC, WRF) with GIS visualization for ecological project management.

## Architecture

### Multi-Module Backend (Spring Boot)
- **ruoyi-admin**: REST controllers, web layer, application entry point
- **ruoyi-framework**: Security (JWT, RBAC), configuration, interceptors, aspects
- **ruoyi-system**: User management, roles, menus, departments, dictionaries
- **ruoyi-common**: Utilities, constants, annotations, exception handling
- **ruoyi-quartz**: Scheduled tasks and job management
- **ruoyi-generator**: Code generation with Velocity templates
- **ruoyi-project**: Ecological project management (regions, services, calibration)

### Frontend (Vue.js SPA)
- **ruoyi-ui**: Vue 2.6.12 with Element UI 2.15.14
- **Routing**: Vue Router 3.4.9 with nested routes
- **State**: Vuex 3.6.0 with modular store structure
- **API**: Axios-based REST integration
- **GIS**: OpenLayers 10.4.0 for map visualization
- **Charts**: ECharts 5.4.0 for data visualization

## Key Architecture Patterns

### Backend Layered Architecture
```
Controller → Service → Mapper → MyBatis XML → MySQL
```
- **Controllers**: REST endpoints in `ruoyi-admin/src/main/java/com/ruoyi/web/controller/`
- **Services**: Business logic in each module's service package
- **Mappers**: MyBatis interfaces with XML queries in `resources/mapper/`
- **Domain**: Entity classes with validation annotations

### Frontend Component Architecture
```
Views → API → Components → Store → Router
```
- **Views**: Page components in `ruoyi-ui/src/views/`
- **API**: REST client modules in `ruoyi-ui/src/api/`
- **Components**: Reusable components in `ruoyi-ui/src/components/`
- **Store**: Vuex modules in `ruoyi-ui/src/store/modules/`

## Development Commands

### Backend (Java)
```bash
# Build & run
cd ruoyi-vue-main
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin-3.8.9.jar

# Development with hot reload
mvn spring-boot:run -pl ruoyi-admin

# Run specific profile
java -jar -Dspring.profiles.active=dev ruoyi-admin/target/ruoyi-admin-3.8.9.jar

# Database setup
mysql -u root -p < sql/ry-vue.sql
mysql -u root -p < sql/quartz.sql
```

### Frontend (Vue.js)
```bash
# Install dependencies
cd ruoyi-ui
npm install

# Development server
npm run dev                    # http://localhost:8890
npm run build:prod            # Production build
npm run lint                  # ESLint check
npm run lint -- --fix         # Auto-fix lint issues

# Build analysis
npm run build:prod -- --report  # Bundle analyzer
```

### Quick Development Scripts
```bash
# Windows batch files
bin/run.bat                   # Start backend + frontend
ruoyi-ui/bin/run-web.bat      # Start frontend only
ry.bat                        # Interactive backend management
```

## Configuration

### Backend (application.yml)
```yaml
server:
  port: 8181                 # Backend port
  servlet:
    context-path: /

spring:
  profiles:
    active: druid            # Database profile
  redis:
    host: 172.16.124.139     # Redis cache
    port: 31398
    password: Ecotox205
  
ruoyi:
  profile: D:/ruoyi/uploadPath  # File upload path
```

### Database (application-druid.yml)
```yaml
spring:
  datasource:
    url: jdbc:mysql://172.16.124.139:32041/ry-vue
    username: root
    password: Ecotox205
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Frontend (vue.config.js)
```javascript
module.exports = {
  devServer: {
    port: 8890,
    proxy: {
      '/dev-api': {
        target: 'http://localhost:8181',
        changeOrigin: true,
        pathRewrite: { '^/dev-api': '' }
      }
    }
  }
}
```

## Key Development Patterns

### Backend
- **REST Controllers**: Annotated with `@RestController`, return `AjaxResult`
- **Service Layer**: Transactional business logic with `@Service`
- **Data Access**: MyBatis XML mappers with dynamic SQL
- **Security**: JWT tokens, role-based access with `@PreAuthorize`
- **Code Generation**: Velocity templates generate CRUD operations

### Frontend
- **API Integration**: Axios interceptors for authentication
- **Permission System**: `v-hasPermi` and `v-hasRole` directives
- **Form Validation**: Element UI form rules
- **Data Tables**: `ruoyi-table` component with pagination
- **GIS Integration**: OpenLayers maps in project views

## Testing

### Backend Tests
```bash
# Run all tests
mvn test

# Run specific module
mvn test -pl ruoyi-system

# Run single test
mvn test -Dtest=SysUserServiceTest

# Skip tests in build
mvn package -DskipTests
```

### Frontend Tests
```bash
# Lint check
npm run lint

# Fix lint issues
npm run lint -- --fix
```

## Environment Setup

### Requirements
- **Java**: 1.8+ (OpenJDK 8 tested)
- **Node.js**: 8.9+ (14+ recommended)
- **MySQL**: 5.7+ or 8.0+
- **Redis**: 3.2+
- **Maven**: 3.6+

### Database Setup
1. Create MySQL database: `CREATE DATABASE ry-vue CHARACTER SET utf8mb4`
2. Import schema: `mysql -u root -p ry-vue < sql/ry-vue.sql`
3. Import quartz: `mysql -u root -p ry-vue < sql/quartz.sql`
4. Default admin: username `admin`, password `admin123`

## Project-Specific Features

### Ecological Modeling
- **Models**: DSSAT, HSPF, LPJ, RWEQ, SWAT, VIC, WRF integration
- **Calibration**: Multi-scenario calibration workflows
- **GIS**: Interactive maps with project regions
- **Services**: Project service case management

### Security Features
- **JWT Authentication**: Stateless with 30min expiry
- **Rate Limiting**: `@RateLimiter` annotation
- **XSS Protection**: Servlet filter for request sanitization
- **Data Scope**: Row-level security based on user permissions
- **Audit Logging**: Comprehensive operation logging

## Common File Locations

### Backend
- **Controllers**: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/`
- **Services**: `ruoyi-system/src/main/java/com/ruoyi/system/service/`
- **Mappers**: `ruoyi-system/src/main/resources/mapper/system/`
- **Config**: `ruoyi-framework/src/main/java/com/ruoyi/framework/config/`

### Frontend
- **Views**: `ruoyi-ui/src/views/` (organized by module)
- **API**: `ruoyi-ui/src/api/` (organized by module)
- **Components**: `ruoyi-ui/src/components/`
- **Store**: `ruoyi-ui/src/store/modules/`
- **Assets**: `ruoyi-ui/src/assets/`

## Troubleshooting

### Port Conflicts
- Backend: Edit `application.yml` port: 8181
- Frontend: Edit `vue.config.js` or use `PORT=xxxx npm run dev`

### Database Issues
- Check MySQL service status
- Verify credentials in `application-druid.yml`
- Ensure utf8mb4 charset for full Unicode support

### Build Issues
- Clear Maven cache: `mvn clean`
- Clear node_modules: `rm -rf ruoyi-ui/node_modules && npm install`
- Check Node.js version compatibility