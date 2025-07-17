package com.quip.backend.server.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.server.model.Server;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ServerMapper extends BaseMapper<Server> {
    Server selectByChannelId(@Param("channelId") Long channelId);
}
