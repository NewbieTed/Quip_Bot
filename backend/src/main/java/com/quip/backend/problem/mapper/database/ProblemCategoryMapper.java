package com.quip.backend.problem.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.problem.model.ProblemCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface ProblemCategoryMapper extends BaseMapper<ProblemCategory> {
    List<ProblemCategory> selectByServerId(@Param("serverId") Long serverId);
}
