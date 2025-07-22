
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

/**
 * Represents a possible answer choice for a problem.
 * <p>
 * Problem choices are the possible answers that can be selected for a given problem.
 * Each choice can have text content, an optional media file, and a flag indicating
 * whether it is the correct answer. A problem can have multiple choices, but typically
 * only one is marked as correct.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("problem_choice")
public class ProblemChoice {
    /**
     * Unique identifier for the problem choice, auto-generated.
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * ID of the problem this choice belongs to.
     */
    @TableField("problem_id")
    private Long problemId;

    /**
     * Text content of this choice.
     */
    @TableField("choice_text")
    private String choiceText;

    /**
     * Optional ID of a media file associated with this choice.
     */
    @TableField("media_file_id")
    private Long mediaFileId;

    /**
     * Flag indicating whether this choice is the correct answer to the problem.
     */
    @TableField("is_correct")
    private Boolean isCorrect;

    /**
     * ID of the member who created this choice.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this choice.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this choice was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this choice was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
