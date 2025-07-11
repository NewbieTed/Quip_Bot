package com.quip.backend.problem.service;

import com.quip.backend.asset.utils.AssetUtils;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.problem.dto.ProblemChoiceCreateDto;
import com.quip.backend.problem.dto.ProblemCreateDto;
import com.quip.backend.problem.dto.ProblemDto;
import com.quip.backend.problem.mapper.database.ProblemChoiceMapper;
import com.quip.backend.problem.mapper.database.ProblemMapper;
import com.quip.backend.problem.mapper.dto.ProblemChoiceCreateDtoMapper;
import com.quip.backend.problem.mapper.dto.ProblemCreateDtoMapper;
import com.quip.backend.problem.model.Problem;
import com.quip.backend.problem.model.ProblemChoice;
import com.quip.backend.server.service.ServerService;
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
    private final ProblemCreateDtoMapper problemCreateDtoMapper;
    private final ProblemChoiceCreateDtoMapper problemChoiceCreateDtoMapper;
    private final AssetUtils assetUtils; // selected line
    private final MemberService memberService;
    private final ServerService serverService;

    public ProblemDto getProblem() {
        return null; // Placeholder for future implementation
    }

    public boolean verifyAnswer(Problem problem, String answer) {
        return false; // Placeholder for future implementation
    }

    @Transactional
    public void addProblem(ProblemCreateDto problemCreateDto) {
        validateProblemCreateDto(problemCreateDto);
        validateContributor(problemCreateDto.getContributorId());
        validateProblemChoices(problemCreateDto.getChoices());
        validateProblemMedia(problemCreateDto.getMediaFileId());
        // TODO: validate problem category

        Problem problem = problemCreateDtoMapper.toProblem(problemCreateDto);
        problemMapper.insert(problem);

        if (problem.getId() == null) {
            throw new IllegalStateException("Problem insertion failed, no ID returned.");
        }
        log.info("Inserted problem with ID: {}", problem.getId());

        List<ProblemChoiceCreateDto> choices = problemCreateDto.getChoices();
        if (choices != null) {
            for (ProblemChoiceCreateDto choiceDto : choices) {
                ProblemChoice problemChoice = problemChoiceCreateDtoMapper.toProblemChoice(choiceDto);
                problemChoice.setProblemId(problem.getId());
                problemChoiceMapper.insert(problemChoice);
                log.info("Inserted problem choice for problemId: {}", problem.getId());
            }
        }
    }

    private void validateProblemCreateDto(ProblemCreateDto dto) {
        if (dto == null) {
            throw new ValidationException("problem", "body", "must not be null");
        }
        if (dto.getQuestion() == null || dto.getQuestion().trim().isEmpty()) {
            throw new ValidationException("Problem Creation", "question", "must not be empty");
        }
        log.info("Validated problem creation DTO with question: '{}'", dto.getQuestion().trim());
    }

    private void validateServer(Long serverId) {
        if (!serverService.isServerExists(serverId)) {
            throw new ValidationException("Problem Creation", "serverId", "must refer to an existing member");
        }
        log.info("Validated serverId: {}", serverId);

    }

    private void validateProblemCategory(Long categoryId) {

    }

    private void validateContributor(Long contributorId) {
        if (!memberService.isMemberExists(contributorId)) {
            throw new ValidationException("Problem Creation", "contributorId", "must refer to an existing member");
        }
        log.info("Validated contributorId: {}", contributorId);
    }

    private void validateProblemChoices(List<ProblemChoiceCreateDto> choices) {
        if (choices != null) {
            for (ProblemChoiceCreateDto choice : choices) {
                boolean isTextValid = choice.getChoiceText() != null && !choice.getChoiceText().trim().isEmpty();
                boolean isMediaValid = choice.getMediaFileId() != null;
                if (!isTextValid && !isMediaValid) {
                    throw new ValidationException(
                            "Problem Choice Creation",
                            "choiceText/mediaFileId",
                            "each choice must have non-empty text or a valid media file ID"
                    );
                }
//                if (choice.getMediaFileId() != null && !assetUtils.fileExists(choice.getMediaFileId())) {
//                    throw new ValidationException(
//                            "Problem Choice Creation",
//                            "mediaFileId",
//                            "specified media file does not exist"
//                    );
//                }
            }
            log.info("Validated {} problem choices.", choices.size());
        }
    }

    private void validateProblemMedia(Long mediaFileId) {
//        if (mediaFileId != null && !assetUtils.fileExists(mediaFileId)) {
//            throw new ValidationException(
//                    "Problem Creation",
//                    "mediaFileId",
//                    "specified media file does not exist"
//            );
//        }
    }

}