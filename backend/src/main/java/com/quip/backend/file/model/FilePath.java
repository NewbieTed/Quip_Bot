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
 * Represents the storage location of a file in the system.
 * <p>
 * This entity contains information about where a file is physically stored,
 * allowing the system to retrieve the file content when needed. It separates
 * the file metadata (stored in the File entity) from the storage details.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("file_path")
public class FilePath {
    /**
     * Unique identifier for the file path, auto-generated.
     */
    @TableId(type = IdType.AUTO)
    @TableField("id")
    private Long id;
    
    /**
     * Name or identifier for this path.
     */
    @TableField("path_name")
    private String pathName;
    
    /**
     * Actual file system path or URL where the file is stored.
     */
    @TableField("file_path")
    private String filePath;

    /**
     * ID of the member who created this file path.
     */
    @TableField("created_by")
    private Long createdBy;
    
    /**
     * ID of the member who last updated this file path.
     */
    @TableField("updated_by")
    private Long updatedBy;
    
    /**
     * Timestamp when this file path was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    
    /**
     * Timestamp when this file path was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
