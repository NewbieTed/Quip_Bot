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
 * Represents a server in the system.
 * <p>
 * Servers are the top-level organizational units in the application. Each server
 * can have multiple channels, members, and roles. Servers provide a space for
 * communities to interact and organize their content and communications.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("server")
public class Server {
    /**
     * Unique identifier for the server.
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Display name of the server.
     */
    @TableField("server_name")
    private String serverName;

    /**
     * ID of the member who created this server.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this server.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this server was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this server was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
