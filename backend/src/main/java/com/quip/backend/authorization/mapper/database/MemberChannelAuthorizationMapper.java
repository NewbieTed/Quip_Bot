package com.quip.backend.authorization.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.authorization.model.MemberChannelAuthorization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper interface for MemberChannelAuthorization entity database operations.
 * <p>
 * This interface provides methods for accessing and manipulating MemberChannelAuthorization data
 * in the database. It extends BaseMapper to inherit standard CRUD operations
 * and adds custom query methods specific to MemberChannelAuthorization entities.
 * </p>
 */
@Mapper
public interface MemberChannelAuthorizationMapper extends BaseMapper<MemberChannelAuthorization> {

    /**
     * Retrieves a member's authorization for a specific channel and authorization type.
     * <p>
     * This method is used to check if a member has a specific permission in a channel.
     * </p>
     *
     * @param memberId The ID of the member
     * @param channelId The ID of the channel
     * @param authorizationTypeId The ID of the authorization type
     * @return The MemberChannelAuthorization entity if found, or null if the member doesn't have the authorization
     */
    MemberChannelAuthorization selectByIds(
            @Param("memberId") Long memberId,
            @Param("channelId") Long channelId,
            @Param("authorizationTypeId") Long authorizationTypeId
    );

}
