package com.quip.backend.tool.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.tool.model.Tool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper interface for Tool entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating Tool data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * and adds custom query methods specific to Tool entities.
 * </p>
 */
@Mapper
public interface ToolMapper extends BaseMapper<Tool> {

}