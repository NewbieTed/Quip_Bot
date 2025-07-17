package com.quip.backend.member.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.member.model.MemberRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper interface for MemberRole entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating MemberRole data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * for MemberRole entities.
 * </p>
 */
@Mapper
public interface MemberRoleMapper extends BaseMapper<MemberRole> {
}
