package com.quip.backend.member.model;

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
 * Represents a user or member in the system.
 * <p>
 * Members are the users of the application who can join servers, participate in channels,
 * and interact with content. Each member has a unique identifier and a display name.
 * Members can have different roles and permissions across different servers and channels.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("member")
public class Member {
    /**
     * Unique identifier for the member.
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Display name of the member.
     */
    @TableField("member_name")
    private String memberName;

    /**
     * ID of the member who created this member record.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this member record.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this member record was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this member record was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
