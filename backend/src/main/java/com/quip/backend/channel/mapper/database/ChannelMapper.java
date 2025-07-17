package com.quip.backend.channel.mapper.database;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.channel.model.Channel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper interface for Channel entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating Channel data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * for Channel entities.
 * </p>
 */
@Mapper
public interface ChannelMapper extends BaseMapper<Channel> {

}
