package com.quip.backend.problem.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.problem.model.Problem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper interface for Problem entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating Problem data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * and adds custom query methods specific to Problem entities.
 * </p>
 */
@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {
//    /**
//     * Retrieves a random problem from the database.
//     * This method is currently commented out for future implementation.
//     *
//     * @return A randomly selected Problem entity
//     */
//    public Problem getRandomProblem();

    /**
     * Retrieves all problems belonging to a specific category.
     *
     * @param problemCategoryId The ID of the problem category to filter by
     * @return A list of Problem entities in the specified category
     */
    List<Problem> selectByProblemCategoryId(@Param("problemCategoryId") Long problemCategoryId);
}