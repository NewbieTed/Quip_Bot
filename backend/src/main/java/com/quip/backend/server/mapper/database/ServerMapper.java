package com.quip.backend.server.mapper.database;

import com.quip.backend.server.model.Server;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ServerMapper {
    Server findById(@Param("id") Long id);
}
