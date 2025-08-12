package com.quip.backend.problem.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.problem.model.ProblemCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper interface for ProblemCategory entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating ProblemCategory data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * and adds custom query methods specific to ProblemCategory entities.
 * </p>
 */
@Mapper
public interface ProblemCategoryMapper extends BaseMapper<ProblemCategory> {

}
