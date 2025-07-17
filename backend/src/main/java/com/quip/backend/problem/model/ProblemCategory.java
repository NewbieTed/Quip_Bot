package com.quip.backend.problem.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Represents a category for organizing problems.
 * <p>
 * Problem categories help organize problems into logical groups within a server.
 * Each category has a name and description and can contain multiple problems.
 * Categories are server-specific, allowing different servers to have their own
 * categorization systems.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("problem_category")
public class ProblemCategory {
    /**
     * Unique identifier for the problem category, auto-generated.
     */
    @TableId(type = IdType.AUTO, value = "id")
    private Long id;

    /**
     * ID of the server this category belongs to.
     */
    @TableField("server_id")
    private Long serverId;

    /**
     * Name of the category.
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * Detailed description of the category.
     */
    @TableField("description")
    private String description;

    /**
     * ID of the member who created this category.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this category.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this category was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this category was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
