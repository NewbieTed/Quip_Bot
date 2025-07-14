package com.quip.backend.problem.service;

import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.problem.dto.request.CreateProblemChoiceRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemChoiceService {

    public void validateProblemChoices(List<CreateProblemChoiceRequestDto> choices, String operation) {
        if (choices != null) {
            for (CreateProblemChoiceRequestDto choice : choices) {
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
}
