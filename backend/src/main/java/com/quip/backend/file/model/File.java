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
 * Represents a file stored in the system.
 * <p>
 * This entity contains metadata about files uploaded to the application,
 * such as their name, type, size, and MIME type. The actual file content
 * is stored separately and referenced through FilePath entities.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("file")
public class File {
    /**
     * Unique identifier for the file, auto-generated.
     */
    @TableId(type = IdType.AUTO)
    @TableField("id")
    private Long id;
    
    /**
     * Name of the file, including extension.
     */
    @TableField("file_name")
    private String fileName;
    
    /**
     * Reference to the type of file (e.g., image, document).
     */
    @TableField("file_type_id")
    private Long fileTypeId;
    
    /**
     * Size of the file in bytes.
     */
    @TableField("size")
    private Long size;
    
    /**
     * MIME type of the file (e.g., "image/jpeg", "application/pdf").
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * ID of the member who uploaded this file.
     */
    @TableField("created_by")
    private Long createdBy;
    
    /**
     * ID of the member who last updated this file's metadata.
     */
    @TableField("updated_by")
    private Long updatedBy;
    
    /**
     * Timestamp when this file was uploaded.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    
    /**
     * Timestamp when this file's metadata was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
