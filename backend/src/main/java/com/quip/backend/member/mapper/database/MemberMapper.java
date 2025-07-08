package com.quip.backend.member.mapper.database;

import com.quip.backend.member.model.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {
    Member findById(@Param("id") Long id);
}
