package com.quip.backend.mapper;

import com.quip.backend.model.Problem;
import com.quip.backend.model.ProblemDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProblemMapper {

    @Select("SELECT COUNT(*) FROM problem")
    int numProblems();

    @Select("SELECT id, question, choices, num_asked, num_correct, img_url\n" +
            "        FROM problem\n" +
            "        WHERE id = #{id}")
    ProblemDTO selectProblemDTOById(@Param("id") long id);

    @Select("SELECT * FROM problem WHERE id = #{id}")
    Problem selectProblemById(@Param("id") long id);

}
