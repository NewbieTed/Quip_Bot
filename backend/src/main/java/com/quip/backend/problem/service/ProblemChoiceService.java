package com.quip.backend.problem.service;

import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.problem.dto.request.CreateProblemChoiceRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for managing problem choices.
 * <p>
 * This service handles the validation and management of choices associated with problems.
 * It ensures that choices have valid content (either text or media) before they are
 * created or updated in the system.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemChoiceService {

    /**
     * Validates a list of problem choices for creation or update operations.
     * <p>
     * This method ensures that each choice has either valid text content or a valid media file ID.
     * It checks that at least one of these fields is properly populated for each choice.
     * </p>
     * <p>
     * Note: Media file validation is currently commented out and will be implemented in the future.
     * </p>
     *
     * @param choices List of problem choice DTOs to validate
     * @param operation A descriptive name of the operation being performed (for error messages)
     * @throws ValidationException If any choice lacks both valid text and a valid media file ID
     */
    public void validateProblemChoices(List<CreateProblemChoiceRequestDto> choices, String operation) {
        if (choices != null) {
            for (CreateProblemChoiceRequestDto choice : choices) {
                boolean isTextValid = choice.getChoiceText() != null && !choice.getChoiceText().trim().isEmpty();
                boolean isMediaValid = choice.getMediaFileId() != null;
                if (!isTextValid && !isMediaValid) {
                    throw new ValidationException(
                            "Problem Choice Creation",
                            "choiceText/mediaFileId",
                            "must have non-empty text or a valid media file ID for each choice"
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
