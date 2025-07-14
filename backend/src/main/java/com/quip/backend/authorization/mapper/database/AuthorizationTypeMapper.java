package com.quip.backend.authorization.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.authorization.model.AuthorizationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthorizationTypeMapper extends BaseMapper<AuthorizationType> {
    AuthorizationType selectByAuthorizationTypeName(@Param("authorizationTypeName") String authorizationTypeName);

}
