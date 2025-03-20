import request from '@/utils/request'

// 查询项目区域列表
export function listProject_region(query) {
  return request({
    url: '/project/project_region/list',
    method: 'get',
    params: query
  })
}

// 查询项目区域详细
export function getProject_region(id) {
  return request({
    url: '/project/project_region/' + id,
    method: 'get'
  })
}

// 新增项目区域
export function addProject_region(data) {
  return request({
    url: '/project/project_region',
    method: 'post',
    data: data
  })
}

// 修改项目区域
export function updateProject_region(data) {
  return request({
    url: '/project/project_region',
    method: 'put',
    data: data
  })
}

// 删除项目区域
export function delProject_region(id) {
  return request({
    url: '/project/project_region/' + id,
    method: 'delete'
  })
}

// 更新项目区域的服务类型关联 - 尝试不同的API端点
export function updateRegionServices(data) {
  return request({
    url: '/project/project_region_service/batchUpdate',  // 使用可能存在的专门API
    method: 'post',
    data: data
  })
}

// 添加单个项目区域-服务类型关联
export function addRegionService(data) {
  return request({
    url: '/project/project_region_service',
    method: 'post',
    data: data
  })
}

// 删除项目区域的所有服务类型关联
export function deleteRegionServices(regionId) {
  return request({
    url: '/project/project_region_service/deleteByRegionId/' + regionId,
    method: 'delete'
  })
}
