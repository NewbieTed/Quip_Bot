package com.quip.backend.problem.mapper;

import com.quip.backend.problem.model.Problem;
import com.quip.backend.problem.dto.ProblemDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


import java.util.List;

@Mapper
public interface ProblemMapper {

    int numProblems();

    ProblemDTO selectProblemDTOById(@Param("problemId") long problemId);

    Problem selectProblemById(@Param("problemId") long problemId);

    void updateProblemChoicesById(@Param("problemId") long problemId, @Param("problem") Problem problem);

    void updateProblemNumCorrectById(@Param("problemId") long problemId, @Param("numCorrect") int numCorrect);

    void updateProblemNumAskedById(@Param("problemId") long problemId, @Param("numAsked") int numAsked);
}
