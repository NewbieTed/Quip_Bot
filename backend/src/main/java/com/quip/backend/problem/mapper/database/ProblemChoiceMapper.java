package com.quip.backend.problem.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.problem.model.ProblemChoice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProblemChoiceMapper extends BaseMapper<ProblemChoice> {
}
