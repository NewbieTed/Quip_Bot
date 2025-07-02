package com.quip.backend.problem.mapper.database;

import com.quip.backend.problem.dto.ProblemCreateDto;
import com.quip.backend.problem.dto.ProblemDto;
import com.quip.backend.problem.model.Problem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProblemMapper {

    int numProblems();

    ProblemDto selectProblemDtoById(@Param("problemId") long problemId);

    Problem selectRandomProblem();

    Problem selectProblemById(@Param("problemId") long problemId);

    void updateProblemChoicesById(@Param("problemId") long problemId, @Param("problem") Problem problem);

    void updateProblemNumCorrectById(@Param("problemId") long problemId, @Param("numCorrect") int numCorrect);

    void updateProblemNumAskedById(@Param("problemId") long problemId, @Param("numAsked") int numAsked);

    void addProblem(@Param("problemCreateDto") ProblemCreateDto problemCreateDto);
}