package com.quip.backend.server.model;

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
 * Represents a role within a server.
 * <p>
 * Server roles define sets of permissions and capabilities that can be assigned to members.
 * Each role belongs to a specific server and can be assigned to multiple members.
 * Roles can have different assignment rules, such as auto-assignment or self-assignment,
 * and may require members to reach certain levels before they can be assigned.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("server_role")
public class ServerRole {
    /**
     * Unique identifier for the server role.
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * ID of the server this role belongs to.
     */
    @TableField("server_id")
    private Long serverId;

    /**
     * Display name of the role.
     */
    @TableField("role_name")
    private String roleName;

    /**
     * Flag indicating whether this role can be automatically assigned to members.
     */
    @TableField("is_auto_assignable")
    private Boolean isAutoAssignable;

    /**
     * Flag indicating whether members can assign this role to themselves.
     */
    @TableField("is_self_assignable")
    private Boolean isSelfAssignable;

    /**
     * Minimum level required for a member to be eligible for this role.
     */
    @TableField("level_required")
    private Integer levelRequired;

    /**
     * ID of the member who created this role.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this role.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this role was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this role was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
