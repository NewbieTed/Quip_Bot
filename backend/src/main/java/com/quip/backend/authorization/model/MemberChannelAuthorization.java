package com.quip.backend.authorization.model;

import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("member_channel_authorization")
public class MemberChannelAuthorization {
    @TableField("member_id")
    private Long memberId;

    @TableField("channel_id")
    private Long channelId;

    @TableField("authorization_type_id")
    private Long authorizationTypeId;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
