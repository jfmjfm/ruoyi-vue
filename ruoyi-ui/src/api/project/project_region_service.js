import request from '@/utils/request'

// 查询项目区域-服务类型关联列表
export function listProject_region_service(query) {
  return request({
    url: '/project/project_region_service/list',
    method: 'get',
    params: query
  })
}

// 查询项目区域-服务类型关联详细
export function getProject_region_service(id) {
  return request({
    url: '/project/project_region_service/' + id,
    method: 'get'
  })
}

// 新增项目区域-服务类型关联
export function addProject_region_service(data) {
  return request({
    url: '/project/project_region_service',
    method: 'post',
    data: data
  })
}

// 修改项目区域-服务类型关联
export function updateProject_region_service(data) {
  return request({
    url: '/project/project_region_service',
    method: 'put',
    data: data
  })
}

// 删除项目区域-服务类型关联
export function delProject_region_service(id) {
  return request({
    url: '/project/project_region_service/' + id,
    method: 'delete'
  })
}
