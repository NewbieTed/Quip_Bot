package com.quip.backend.server.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.server.model.ServerRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServerRoleMapper extends BaseMapper<ServerRole> {
}
