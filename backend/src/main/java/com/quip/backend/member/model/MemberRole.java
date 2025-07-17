package com.quip.backend.member.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Represents the association between a member and a server role.
 * <p>
 * This entity defines which roles a member has within a server. It serves as a junction
 * table in a many-to-many relationship between members and server roles. The roles
 * determine what permissions and capabilities a member has within a server.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("member_role")
public class MemberRole {
    /**
     * ID of the member who has this role.
     * Part of the composite primary key.
     */
    @TableField("member_id")
    private Long memberId;

    /**
     * ID of the server role assigned to the member.
     * Part of the composite primary key.
     */
    @TableField("server_role_id")
    private Long serverRoleId;

    /**
     * Timestamp when this role was assigned to the member.
     */
    @TableField("assigned_at")
    private OffsetDateTime assignedAt;

    /**
     * ID of the member who created this role assignment.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this role assignment.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this role assignment was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this role assignment was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
