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
@TableName("file_type")
public class FileType {
    @TableId(type = IdType.AUTO)
    @TableField("id")
    private Long id;
    @TableField("server_id")
    private Long serverId;
    @TableField("file_path_id")
    private Long filePathId;
    @TableField("type_name")
    private String typeName;
    @TableField("description")
    private String description;

    @TableField("created_by")
    private Long createdBy;
    @TableField("updated_by")
    private Long updatedBy;
    @TableField("created_at")
    private OffsetDateTime createdAt;
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
