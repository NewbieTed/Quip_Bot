package com.quip.backend.authorization.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.authorization.model.MemberChannelAuthorization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper interface for MemberChannelAuthorization entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating MemberChannelAuthorization data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * and adds custom query methods specific to MemberChannelAuthorization entities.
 * </p>
 */
@Mapper
public interface MemberChannelAuthorizationMapper extends BaseMapper<MemberChannelAuthorization> {



}
