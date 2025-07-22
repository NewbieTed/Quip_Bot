package com.quip.backend.server.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.server.model.Server;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper interface for Server entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating Server data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * and adds custom query methods specific to Server entities.
 * </p>
 */
@Mapper
public interface ServerMapper extends BaseMapper<Server> {
    /**
     * Retrieves the server that a channel belongs to.
     *
     * @param channelId The ID of the channel
     * @return The Server entity that the channel belongs to, or null if not found
     */
    Server selectByChannelId(@Param("channelId") Long channelId);
}
