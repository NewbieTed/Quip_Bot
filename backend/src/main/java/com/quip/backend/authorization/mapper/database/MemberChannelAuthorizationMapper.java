package com.quip.backend.authorization.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.authorization.model.MemberChannelAuthorization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberChannelAuthorizationMapper extends BaseMapper<MemberChannelAuthorization> {

    MemberChannelAuthorization selectByIds(
            @Param("memberId") Long memberId,
            @Param("channelId") Long channelId,
            @Param("authorizationTypeId") Long authorizationTypeId
    );

}
