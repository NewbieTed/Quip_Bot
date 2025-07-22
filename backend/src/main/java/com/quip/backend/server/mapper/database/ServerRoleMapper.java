package com.quip.backend.server.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.server.model.ServerRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper interface for ServerRole entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating ServerRole data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * for ServerRole entities.
 * </p>
 */
@Mapper
public interface ServerRoleMapper extends BaseMapper<ServerRole> {
}
