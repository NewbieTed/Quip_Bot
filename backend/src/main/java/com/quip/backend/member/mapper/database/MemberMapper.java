package com.quip.backend.member.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.member.model.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper interface for Member entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating Member data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * for Member entities.
 * </p>
 */
@Mapper
public interface MemberMapper extends BaseMapper<Member> {
}
