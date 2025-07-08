package com.quip.backend.problem.service;

import com.quip.backend.asset.utils.AssetUtils;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.problem.dto.ProblemChoiceCreateDto;
import com.quip.backend.problem.dto.ProblemCreateDto;
import com.quip.backend.problem.dto.ProblemDto;
import com.quip.backend.problem.mapper.database.ProblemChoiceMapper;
import com.quip.backend.problem.mapper.database.ProblemMapper;
import com.quip.backend.problem.mapper.dto.ProblemChoiceDtoMapper;
import com.quip.backend.problem.mapper.dto.ProblemDtoMapper;
import com.quip.backend.problem.model.Problem;
import com.quip.backend.problem.model.ProblemChoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemMapper problemMapper;
    private final ProblemChoiceMapper problemChoiceMapper;
    private final ProblemDtoMapper problemDtoMapper;
    private final ProblemChoiceDtoMapper problemChoiceDtoMapper;
    private final AssetUtils assetUtils;
    private final MemberService memberService;

    // Retrieve a random question
    public ProblemDto getProblem() {
        return null;
    }

    public boolean verifyAnswer(Problem problem, String answer) {
        return false;
    }

    @Transactional
    public void addProblem(ProblemCreateDto problemCreateDto) {
        if (problemCreateDto == null) {
            throw new ValidationException("problem", "body", "must not be null");
        }

        log.info("Adding new problem with question: '{}', contributorId: {}, choices count: {}",
                 problemCreateDto.getQuestion(), problemCreateDto.getContributorId(),
                 problemCreateDto.getChoices() != null ? problemCreateDto.getChoices().size() : 0);

        String question = problemCreateDto.getQuestion().trim();
        Long contributorId = problemCreateDto.getContributorId();
        List<ProblemChoiceCreateDto> choices = problemCreateDto.getChoices();

        // Verify if fields are valid
        if (question.isEmpty()) {
            // TODO: Make a more comprehensive message
            throw new ValidationException("Problem Creation", "question", "must not be empty");
        }
        log.info("Question validation passed.");

        boolean isContributorValid = memberService.isMemberExists(contributorId);
        if (!isContributorValid) {
            log.warn("Contributor validation not passed for contributorId: {}", contributorId);
            throw new ValidationException("Problem Creation", "contributorId", "must refer to an existing member");
        }
        log.info("Contributor validation passed for contributorId: {}", contributorId);

        for (ProblemChoiceCreateDto choice : choices) {
            if ((choice.getChoiceText() == null || choice.getChoiceText().trim().isEmpty())
                    && choice.getMediaFileId() == null) {
                throw new ValidationException(
                        "Problem Choice Creation",
                        "choiceText/mediaFileId",
                        "each choice must have non-empty text or a valid media file ID"
                );
            }
        }

        if (problemCreateDto.getMediaFileId() != null) {
            // TODO: Check if file exists
            // TODO: Check if choice file exists
        }


        // All validations done, continue to adding phase
        Problem problem = problemDtoMapper.toProblem(problemCreateDto);

        // TODO: Remove this
        problem.setMediaFileId(null);

        log.info("Inserting problem into database...");
        problemMapper.insert(problem);
        Long problemId = problem.getId();
        log.info("Problem inserted with generated ID: {}", problemId);

        if (problemId == null) {
            throw new IllegalStateException("Insertion failed!");
        }

        // Add problem choices
        for (ProblemChoiceCreateDto choice : choices) {
            log.info("Inserting problem choice: {}", choice);
            ProblemChoice problemChoice = problemChoiceDtoMapper.toProblemChoice(choice);
            problemChoice.setProblemId(problemId);

            problemChoiceMapper.insert(problemChoice);
            log.info("Problem choice inserted for problemId: {}", problemId);
        }
    }
}