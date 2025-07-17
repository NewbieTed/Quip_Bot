package com.quip.backend.problem.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.problem.model.ProblemChoice;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper interface for ProblemChoice entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating ProblemChoice data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * for ProblemChoice entities.
 * </p>
 */
@Mapper
public interface ProblemChoiceMapper extends BaseMapper<ProblemChoice> {
}
