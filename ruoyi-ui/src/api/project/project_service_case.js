import request from '@/utils/request'

// 查询服务案例列表
export function listProject_service_case(query) {
  return request({
    url: '/project/project_service_case/list',
    method: 'get',
    params: query
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
