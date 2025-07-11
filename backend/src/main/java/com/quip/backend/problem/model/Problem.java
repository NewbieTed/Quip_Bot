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
@TableName("problem")
public class Problem {
    // TODO: P2 Feature: Make public?
    @TableId(type = IdType.AUTO, value = "id")
    private Long id;

    @TableField("server_id")
    private Long serverId;

    @TableField("problem_category_id")
    private Long problemCategoryId;

    @TableField("question")
    private String question;

    @TableField("media_file_id")
    private Long mediaFileId;

    @TableField("contributor_id")
    private Long contributorId;

    @TableField("is_valid")
    private Boolean isValid;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
