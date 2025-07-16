
package com.quip.backend.problem.service;

import com.quip.backend.common.BaseTest;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.problem.dto.request.CreateProblemChoiceRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ProblemChoiceServiceTest extends BaseTest {

    @InjectMocks
    private ProblemChoiceService problemChoiceService;

    @Test
    void validateProblemChoices_validChoices_noException() {
        List<CreateProblemChoiceRequestDto> choices = new ArrayList<>();
        choices.add(new CreateProblemChoiceRequestDto("Choice 1", null, true));
        choices.add(new CreateProblemChoiceRequestDto(null, 1L, false));
        choices.add(new CreateProblemChoiceRequestDto("Choice 3", 2L, false));

        assertDoesNotThrow(() -> {
            problemChoiceService.validateProblemChoices(choices, "testOperation");
        });
    }

    @Test
    void validateProblemChoices_invalidChoice_throwsException() {
        List<CreateProblemChoiceRequestDto> choices = new ArrayList<>();
        choices.add(new CreateProblemChoiceRequestDto(null, null, false));

        assertThrows(ValidationException.class, () -> {
            problemChoiceService.validateProblemChoices(choices, "testOperation");
        });
    }

    @Test
    void validateProblemChoices_nullOrEmptyChoices_noException() {
        assertDoesNotThrow(() -> {
            problemChoiceService.validateProblemChoices(null, "testOperation");
        });

        assertDoesNotThrow(() -> {
            problemChoiceService.validateProblemChoices(new ArrayList<>(), "testOperation");
        });
    }
}
