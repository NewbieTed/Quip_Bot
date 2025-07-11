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

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("server_role")
public class ServerRole {
    @TableId(value = "id", type = IdType.INPUT)
    @TableField("id")
    private Long id;

    @TableField("server_id")
    private Long serverId;

    @TableField("role_name")
    private String roleName;

    @TableField("is_auto_assignable")
    private Boolean isAutoAssignable;

    @TableField("is_self_assignable")
    private Boolean isSelfAssignable;

    @TableField("level_required")
    private Integer levelRequired;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
