package com.quip.backend.controller;

import com.quip.backend.model.ProblemDTO;
import com.quip.backend.service.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/problem")
public class ProblemController {

    private final ProblemService problemService;

    @Autowired
    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    // Handle GET request to retrieve all users
    @GetMapping
    public ProblemDTO getProblem() {
        return problemService.getProblem();
    }

//    // Handle GET request to retrieve a user by ID
//    @GetMapping("/statistics")
//    public Problem getUserById(@PathVariable Long id) {
//        return problemService.getUserById(id);
//    }
//
//    // Handle POST request to create a new user
//    @PostMapping
//    public Problem createUser(@RequestBody Problem problem) {
//        return problemService.createUser(problem);
//    }
//
//    // Handle PUT request to update an existing user
//    @PutMapping("/{id}")
//    public Problem updateUser(@PathVariable Long id, @RequestBody Problem problem) {
//        return problemService.updateUser(id, problem);
//    }
//
//    // Handle DELETE request to delete a user
//    @DeleteMapping("/{id}")
//    public void deleteUser(@PathVariable Long id) {
//        problemService.deleteUser(id);
//    }
}
