package com.quip.backend.member.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.member.model.MemberRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberRoleMapper extends BaseMapper<MemberRole> {
}
