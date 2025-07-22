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
 * Represents a problem or question in the system.
 * <p>
 * Problems are questions or challenges that can be presented to users. Each problem
 * belongs to a specific category and server. Problems can have associated media files
 * and multiple choices as possible answers.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("problem")
public class Problem {
    // TODO: P2 Feature: Make public?
    /**
     * Unique identifier for the problem, auto-generated.
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * ID of the server this problem belongs to.
     */
    @TableField("server_id")
    private Long serverId;

    /**
     * ID of the category this problem belongs to.
     */
    @TableField("problem_category_id")
    private Long problemCategoryId;

    /**
     * The text of the question or problem.
     */
    @TableField("question")
    private String question;

    /**
     * Optional ID of a media file associated with this problem.
     */
    @TableField("media_file_id")
    private Long mediaFileId;

    /**
     * ID of the member who contributed this problem.
     */
    @TableField("contributor_id")
    private Long contributorId;

    /**
     * Flag indicating whether this problem has been validated and is ready for use.
     */
    @TableField("is_valid")
    private Boolean isValid;

    /**
     * ID of the member who created this problem.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this problem.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this problem was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this problem was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
