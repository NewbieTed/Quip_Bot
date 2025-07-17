package com.quip.backend.file.model;

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
 * Represents a category or type of file in the system.
 * <p>
 * This entity defines different types of files that can be stored in the system,
 * such as images, documents, or videos. Each file type can have its own storage
 * location and server-specific settings.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("file_type")
public class FileType {
    /**
     * Unique identifier for the file type, auto-generated.
     */
    @TableId(type = IdType.AUTO)
    @TableField("id")
    private Long id;
    
    /**
     * ID of the server this file type belongs to.
     */
    @TableField("server_id")
    private Long serverId;
    
    /**
     * ID of the file path where files of this type are stored.
     */
    @TableField("file_path_id")
    private Long filePathId;
    
    /**
     * Name of the file type (e.g., "Image", "Document", "Video").
     */
    @TableField("type_name")
    private String typeName;
    
    /**
     * Detailed description of this file type.
     */
    @TableField("description")
    private String description;

    /**
     * ID of the member who created this file type.
     */
    @TableField("created_by")
    private Long createdBy;
    
    /**
     * ID of the member who last updated this file type.
     */
    @TableField("updated_by")
    private Long updatedBy;
    
    /**
     * Timestamp when this file type was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    
    /**
     * Timestamp when this file type was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
