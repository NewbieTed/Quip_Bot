package com.quip.backend.channel.mapper.database;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.channel.model.Channel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChannelMapper extends BaseMapper<Channel> {

}
