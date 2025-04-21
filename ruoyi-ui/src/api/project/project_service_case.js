import request from '@/utils/request'

// 查询服务案例列表
export function listProject_service_case(query) {
  return request({
    url: '/project/project_service_case/list',
    method: 'get',
    params: query
  })
}

// 根据区域服务ID查询服务案例列表
export function listProject_service_case_by_region_service_id(region_service_id) {
  return request({
    url: '/project/project_service_case/list',
    method: 'get',
    params: {
      region_service_id: region_service_id,
      pageNum: 1,
      pageSize: 999, // 设置较大的页面大小以获取所有记录
      isAsc: 'asc', // 升序排序
      orderByColumn: 'createTime' // 按创建时间排序
    }
  })
}

// 查询服务案例详细
export function getProject_service_case(id) {
  return request({
    url: '/project/project_service_case/' + id,
    method: 'get'
  })
}

// 新增服务案例
export function addProject_service_case(data) {
  return request({
    url: '/project/project_service_case',
    method: 'post',
    data: data
  })
}

// 修改服务案例
export function updateProject_service_case(data) {
  return request({
    url: '/project/project_service_case',
    method: 'put',
    data: data
  })
}

// 删除服务案例
export function delProject_service_case(id) {
  return request({
    url: '/project/project_service_case/' + id,
    method: 'delete'
  })
}

// 获取默认项目服务案例（is_default=1）
export function getDefaultProject_service_case(region_service_id) {
  return request({
    url: '/project/project_service_case/list',
    method: 'get',
    params: {
      region_service_id: region_service_id,
      is_default: 1
    }
  })
}
