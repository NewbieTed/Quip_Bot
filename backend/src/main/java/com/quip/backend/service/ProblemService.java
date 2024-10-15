package com.quip.backend.service;

import com.quip.backend.mapper.ProblemMapper;
import com.quip.backend.model.ProblemDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.Random;

@Service
public class ProblemService {

    private final ProblemMapper problemMapper;

    public ProblemService(ProblemMapper problemMapper) {
        this.problemMapper = problemMapper;
    }

    // Retrieve a random question
    public ProblemDTO getProblem() {
        int num = problemMapper.numProblems();
        if (num == 0) {
            return null;
        }
        Random rand = new Random();
        long id = (long) (rand.nextInt(num) + 1);

        ProblemDTO problem = problemMapper.selectProblemDTOById(id);
        return problem;
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
