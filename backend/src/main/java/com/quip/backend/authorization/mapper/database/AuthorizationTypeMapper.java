package com.quip.backend.authorization.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.authorization.model.AuthorizationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper interface for AuthorizationType entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating AuthorizationType data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * and adds custom query methods specific to AuthorizationType entities.
 * </p>
 */
@Mapper
public interface AuthorizationTypeMapper extends BaseMapper<AuthorizationType> {
    /**
     * Retrieves an authorization type by its name.
     *
     * @param authorizationTypeName The name of the authorization type to look up
     * @return The AuthorizationType entity with the specified name, or null if not found
     */
    AuthorizationType selectByAuthorizationTypeName(@Param("authorizationTypeName") String authorizationTypeName);

}
