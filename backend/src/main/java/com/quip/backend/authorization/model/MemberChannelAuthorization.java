package com.quip.backend.authorization.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Represents the association between a member, a channel, and an authorization type.
 * <p>
 * This entity defines what permissions a specific member has in a specific channel.
 * It serves as a junction table in a many-to-many relationship between members, channels,
 * and authorization types.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("member_channel_authorization")
public class MemberChannelAuthorization {
    /**
     * ID of the member who has this authorization.
     * Part of the composite primary key.
     */
    @TableField("member_id")
    private Long memberId;

    /**
     * ID of the channel this authorization applies to.
     * Part of the composite primary key.
     */
    @TableField("channel_id")
    private Long channelId;

    /**
     * ID of the authorization type granted.
     * Part of the composite primary key.
     */
    @TableField("authorization_type_id")
    private Long authorizationTypeId;

    /**
     * ID of the member who created this authorization.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this authorization.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this authorization was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this authorization was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
