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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("file")
public class File {
    @TableId(type = IdType.AUTO)
    @TableField("id")
    private Long id;
    @TableField("file_name")
    private String fileName;
    @TableField("file_type_id")
    private Long fileTypeId;
    @TableField("size")
    private Long size;
    @TableField("mime_type")
    private String mimeType;

    @TableField("created_by")
    private Long createdBy;
    @TableField("updated_by")
    private Long updatedBy;
    @TableField("created_at")
    private OffsetDateTime createdAt;
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
