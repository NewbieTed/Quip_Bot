
package com.quip.backend.problem.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("problem_choice")
public class ProblemChoice {
    @TableId(type = IdType.AUTO, value = "id")
    private Long id;

    @TableField("problem_id")
    private Long problemId;

    @TableField("choice_text")
    private String choiceText;

    @TableField("media_file_id")
    private Long mediaFileId;

    @TableField("is_correct")
    private Boolean isCorrect;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
