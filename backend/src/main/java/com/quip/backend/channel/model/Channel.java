package com.quip.backend.channel.model;

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
 * Represents a communication channel within a server.
 * <p>
 * Channels are spaces within servers where members can interact. Each channel
 * belongs to a specific server and can optionally be categorized within that server.
 * Channels have their own permission settings that determine which members can
 * access and perform actions within them.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("channel")
public class Channel {
    /**
     * Unique identifier for the channel.
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * ID of the server this channel belongs to.
     */
    @TableField("server_id")
    private Long serverId;

    /**
     * Optional ID of the category within the server that this channel belongs to.
     */
    @TableField("server_category_id")
    private Long serverCategoryId;

    /**
     * Display name of the channel.
     */
    @TableField("channel_name")
    private String channelName;

    /**
     * ID of the member who created this channel.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this channel.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this channel was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this channel was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
