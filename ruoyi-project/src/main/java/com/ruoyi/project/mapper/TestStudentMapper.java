package com.ruoyi.project.mapper;

import java.util.List;
import com.ruoyi.project.domain.TestStudent;

/**
 * 学生信息Mapper接口
 * 
 * @author ruoyi
 * @date 2025-03-17
 */
public interface TestStudentMapper 
{
    /**
     * 查询学生信息
     * 
     * @param studentId 学生信息主键
     * @return 学生信息
     */
    public TestStudent selectTestStudentByStudentId(Long studentId);

    /**
     * 查询学生信息列表
     * 
     * @param testStudent 学生信息
     * @return 学生信息集合
     */
    public List<TestStudent> selectTestStudentList(TestStudent testStudent);

    /**
     * 新增学生信息
     * 
     * @param testStudent 学生信息
     * @return 结果
     */
    public int insertTestStudent(TestStudent testStudent);

    /**
     * 修改学生信息
     * 
     * @param testStudent 学生信息
     * @return 结果
     */
    public int updateTestStudent(TestStudent testStudent);

    /**
     * 删除学生信息
     * 
     * @param studentId 学生信息主键
     * @return 结果
     */
    public int deleteTestStudentByStudentId(Long studentId);

    /**
     * 批量删除学生信息
     * 
     * @param studentIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTestStudentByStudentIds(Long[] studentIds);
}
