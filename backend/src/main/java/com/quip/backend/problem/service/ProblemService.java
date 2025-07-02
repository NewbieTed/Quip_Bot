package com.quip.backend.problem.service;

import com.quip.backend.asset.utils.AssetUtils;
import com.quip.backend.problem.dto.ProblemCreateDto;
import com.quip.backend.problem.dto.ProblemDto;
import com.quip.backend.problem.mapper.database.ProblemMapper;
import com.quip.backend.problem.mapper.dto.ProblemDtoMapper;
import com.quip.backend.problem.model.Problem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemMapper problemMapper;
    private final ProblemDtoMapper problemDtoMapper;
    private final AssetUtils assetUtils;

    // Retrieve a random question
    public ProblemDto getProblem() {
        Problem problem = problemMapper.selectRandomProblem();

        if (problem == null) {
            return null;
        }

        ProblemDto problemDto = problemDtoMapper.toProblemDto(problem);
        problemDto.shuffleChoices();

        long problemId = problemDto.getProblemId();

        problemMapper.updateProblemNumAskedById(problemId, problem.getNumAsked() + 1);

        String mediaUrl = problemDto.getMediaUrl();
        problemDto.setMediaUrl(assetUtils.getBaseUrl() + mediaUrl);

        return problemDto;
    }

    public boolean verifyAnswer(Problem problem, String answer) {
        long problemId = problem.getProblemId();
        List<String> choices = problem.getChoices();
        int correctAnswerIndex = problem.getCorrectAnswerIndex();

        if (choices.get(correctAnswerIndex).equals(answer)) {
            problemMapper.updateProblemNumCorrectById(problemId, problem.getNumCorrect() + 1);
            return true;
        }

        return false;
    }

    public void addProblem(ProblemCreateDto problemCreateDto) {
        problemMapper.addProblem(problemCreateDto);
    }
}