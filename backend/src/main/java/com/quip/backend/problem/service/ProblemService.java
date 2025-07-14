package com.quip.backend.problem.service;

import com.quip.backend.asset.utils.AssetUtils;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.problem.dto.request.CreateProblemChoiceRequestDto;
import com.quip.backend.problem.dto.request.CreateProblemRequestDto;
import com.quip.backend.problem.dto.request.GetProblemRequestDto;
import com.quip.backend.problem.dto.response.GetProblemListItemResponseDto;
import com.quip.backend.problem.dto.response.GetProblemResponseDto;
import com.quip.backend.problem.mapper.database.ProblemCategoryMapper;
import com.quip.backend.problem.mapper.database.ProblemChoiceMapper;
import com.quip.backend.problem.mapper.database.ProblemMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemChoiceRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.request.CreateProblemRequestDtoMapper;
import com.quip.backend.problem.mapper.dto.response.GetProblemListItemResponseDtoMapper;
import com.quip.backend.problem.model.Problem;
import com.quip.backend.problem.model.ProblemChoice;
import com.quip.backend.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemService {

    private final AssetUtils assetUtils;

    // Service modules
    private final ProblemCategoryService problemCategoryService;
    private final ProblemChoiceService problemChoiceService;
    private final MemberService memberService;
    private final ChannelService channelService;
    private final ServerService serverService;
    private final AuthorizationService authorizationService;

    // Mybatis mappers
    private final ProblemMapper problemMapper;
    private final ProblemChoiceMapper problemChoiceMapper;
    private final ProblemCategoryMapper problemCategoryMapper;

    // Mapstruct mappers
    private final CreateProblemRequestDtoMapper createProblemRequestDtoMapper;
    private final CreateProblemChoiceRequestDtoMapper createProblemChoiceRequestDtoMapper;
    private final GetProblemListItemResponseDtoMapper getProblemListItemResponseDtoMapper;

    private static final String CREATE_PROBLEM = "Problem Creation";
    private static final String CREATE_PROBLEM_CHOICE = "Problem Choice Creation";
    private static final String RETRIEVE_PROBLEM = "Problem Retrieval";

    public GetProblemResponseDto getProblem() {
        return null; // Placeholder for future implementation
    }

    // TODO: Change other feature's file structure on DTOs as well as naming

    public List<GetProblemListItemResponseDto> getProblemsByCategory(GetProblemRequestDto getProblemRequestDto) {
        Long memberId = getProblemRequestDto.getMemberId();
        Long problemCategoryId = getProblemRequestDto.getProblemCategoryId();
        Long channelId = getProblemRequestDto.getChannelId();

        memberService.validateMember(memberId, RETRIEVE_PROBLEM);
        channelService.validateChannel(channelId, RETRIEVE_PROBLEM);
        problemCategoryService.validateProblemCategory(problemCategoryId, RETRIEVE_PROBLEM);
        authorizationService.validateAuthorization(
                memberId,
                channelId,
                AuthorizationConstants.VIEW_PROBLEM,
                RETRIEVE_PROBLEM
        );

        List<Problem> problems = problemMapper.selectByProblemCategoryId(problemCategoryId);
        List<GetProblemListItemResponseDto> getProblemResponseDtos = new ArrayList<>();

        for (Problem problem : problems) {
            getProblemResponseDtos.add(getProblemListItemResponseDtoMapper.toGetProblemListResponseDto(problem));
        }

        return getProblemResponseDtos;
    }

    @Transactional
    public void addProblem(CreateProblemRequestDto problemCreateDto) {
        if (problemCreateDto == null) {
            throw new ValidationException(CREATE_PROBLEM, "body", "must not be null");
        }

        // Validate authorization
        memberService.validateMember(problemCreateDto.getMemberId(), CREATE_PROBLEM);
        channelService.validateChannel(problemCreateDto.getChannelId(), CREATE_PROBLEM);
        Long serverId = channelService.findServerId(problemCreateDto.getChannelId());
        serverService.validateServer(serverId, CREATE_PROBLEM);
        authorizationService.validateAuthorization(
                problemCreateDto.getMemberId(),
                problemCreateDto.getChannelId(),
                AuthorizationConstants.MANAGE_PROBLEM,
                CREATE_PROBLEM
        );

        // Validate fields
        problemCategoryService.validateProblemCategory(problemCreateDto.getProblemCategoryId(), CREATE_PROBLEM);
        this.validateProblem(problemCreateDto.getQuestion(), CREATE_PROBLEM);
        problemChoiceService.validateProblemChoices(problemCreateDto.getChoices(), CREATE_PROBLEM_CHOICE);

        // TODO: Move this to files
        this.validateProblemMedia(problemCreateDto.getMediaFileId());

        // Insert problem
        Problem problem = createProblemRequestDtoMapper.toProblem(problemCreateDto);
        problem.setServerId(serverId);
        problemMapper.insert(problem);

        if (problem.getId() == null) {
            throw new IllegalStateException("Problem insertion failed, no ID returned.");
        }
        log.info("Inserted problem with ID: {}", problem.getId());

        // Insert choices
        List<CreateProblemChoiceRequestDto> choices = problemCreateDto.getChoices();
        if (choices != null) {
            for (CreateProblemChoiceRequestDto choiceDto : choices) {
                ProblemChoice problemChoice = createProblemChoiceRequestDtoMapper.toProblemChoice(choiceDto);
                problemChoice.setProblemId(problem.getId());
                problemChoiceMapper.insert(problemChoice);
                log.info("Inserted problem choice for problemId: {}", problem.getId());
            }
        }
    }

    public void validateProblem(String question, String operation) {
        if (question == null || question.trim().isEmpty()) {
            throw new ValidationException(operation, "question", "must not be empty");
        }
        // TODO Validate duplicates
        log.info("Validated problem creation DTO with question: '{}'", question.trim());
    }


    // TODO: Move to fileService
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