package com.quip.backend.problem.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.problem.model.Problem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {
//    public Problem getRandomProblem();
}