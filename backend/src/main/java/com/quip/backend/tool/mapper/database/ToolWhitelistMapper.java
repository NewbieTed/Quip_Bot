package com.quip.backend.tool.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.tool.enums.ToolWhitelistScope;
import com.quip.backend.tool.model.ToolWhitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * MyBatis mapper interface for ToolWhitelist entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating ToolWhitelist data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * and adds custom query methods specific to ToolWhitelist entities.
 * </p>
 */
@Mapper
public interface ToolWhitelistMapper extends BaseMapper<ToolWhitelist> {

    /**
     * Retrieves active whitelist entries for a specific member and server.
     *
     * @param memberId The ID of the member
     * @param serverId The ID of the server
     * @param currentTime The current timestamp to check for expired entries
     * @return List of active ToolWhitelist entries
     */
    List<ToolWhitelist> selectActiveByMemberIdAndServerId(@Param("memberId") Long memberId, 
                                                         @Param("serverId") Long serverId, 
                                                         @Param("currentTime") OffsetDateTime currentTime);
}