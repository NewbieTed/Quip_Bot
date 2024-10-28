package com.quip.backend.problem.service;

import com.quip.backend.dto.BaseResponse;
import com.quip.backend.problem.mapper.ProblemMapper;
import com.quip.backend.problem.dto.ProblemDTO;
import com.quip.backend.problem.model.Problem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemMapper problemMapper;

    // Retrieve a random question
    public BaseResponse<ProblemDTO> getProblem() {
        int num = problemMapper.numProblems();
        if (num == 0) {
            return null;
        }
        Random rand = new Random();
        long problemId = (long) (rand.nextInt(num) + 1);

        ProblemDTO problem = problemMapper.selectProblemDTOById(problemId);
        problem.shuffleChoices();

        problemMapper.updateProblemNumAskedById(problemId, problem.getNumAsked() + 1);

        return BaseResponse.success("success", problem);
    }

    public BaseResponse<Boolean> verifyAnswer(long problemId, String answer) {
        Problem problem = problemMapper.selectProblemById(problemId);

        if (problem == null) {
            return BaseResponse.failure(HttpStatus.NOT_FOUND.value(), "Problem with problem id " + problemId +
                                                                               " is not found");
        }

        List<String> choices = problem.getChoices();
        int correctAnswerIndex = problem.getCorrectAnswerIndex();

        if (choices.get(correctAnswerIndex).equals(answer)) {
            problemMapper.updateProblemNumCorrectById(problemId, problem.getNumCorrect() + 1);
            return BaseResponse.success(true);
        }

        return BaseResponse.success(false);
    }



//    // Retrieve all users
//    public List<Problem> getAllUsers() {Ï
//        return problems;
//    }

//    // Retrieve a user by ID
//    public Problem getUserById(Long id) {
//        return problemMapper.findById(id);
//    }
//
//    // Create a new user
//    public Problem createUser(Problem problem) {
//        problems.add(problem);
//        return problem;
//    }
//
//    // Update an existing user
//    public Problem updateUser(Long id, Problem updatedProblem) {
//        Problem problem = getUserById(id);
//        if (problem != null) {
//            problem.setUsername(updatedProblem.getUsername());
//            problem.setEmail(updatedProblem.getEmail());
//        }
//        return problem;
//    }
//
//    // Delete a user
//    public void deleteUser(Long id) {
//        problems.removeIf(problem -> problem.getId().equals(id));
//    }
}
