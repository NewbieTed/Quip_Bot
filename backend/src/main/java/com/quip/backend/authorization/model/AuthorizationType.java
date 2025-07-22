package com.quip.backend.authorization.model;

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
 * Represents a type of authorization or permission in the system.
 * <p>
 * This entity defines the various types of permissions that can be assigned to members
 * for specific channels, such as READ_PERMISSION, WRITE_PERMISSION, etc.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("authorization_type")
public class AuthorizationType {
    /**
     * Unique identifier for the authorization type.
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Name of the authorization type (e.g., "READ_PERMISSION", "WRITE_PERMISSION").
     */
    @TableField("authorization_type_name")
    private String authorizationTypeName;

    /**
     * ID of the member who created this authorization type.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this authorization type.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this authorization type was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this authorization type was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
