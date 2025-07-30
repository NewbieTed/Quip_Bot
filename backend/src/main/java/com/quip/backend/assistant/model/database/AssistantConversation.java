package com.quip.backend.assistant.model.database;

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
 * Represents a conversation between a member and the assistant agent.
 *
 * <p><b>State Invariant (Database-Enforced):</b></p>
 * The combination of {@code isActive}, {@code isInterrupt}, and {@code isProcessing}
 * is restricted by a CHECK constraint in the database. Only the following state combinations are valid:
 *
 * <ul>
 *   <li>{@code isActive = false, isInterrupt = false, isProcessing = false} — Not active </li>
 *   <li>{@code isActive = true, isInterrupt = false, isProcessing = true} — Actively processing a request</li>
 *   <li>{@code isActive = true, isInterrupt = true, isProcessing = false} — Interrupted and awaiting human input</li>
 *   <li>{@code isActive = true, isInterrupt = false, isProcessing = false} — Waiting for user input (idle)</li>
 * </ul>
 *
 * <p>All other combinations will be rejected by the database layer.</p>
 *
 * <p>Note: Keep this documentation in sync with the schema constraint <code>chk_valid_conversation_state</code>
 * defined in {@code V0.1.0__init_schema.sql}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("assistant_conversation")
public class AssistantConversation {
    /**
     * Unique identifier for the conversation, auto-generated.
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * ID of the server this conversation belongs to.
     */
    @TableField("server_id")
    private Long serverId;

    /**
     * ID of the member this conversation belongs to.
     */
    @TableField("member_id")
    private Long memberId;

    /**
     * Flag indicating whether this conversation is the current active conversation.
     */
    @TableField("is_active")
    private Boolean isActive;

    /**
     * Flag indicating whether this conversation is interrupted.
     */
    @TableField("is_interrupt")
    private Boolean isInterrupt;

    /**
     * Flag indicating whether this conversation is being processed.
     */
    @TableField("is_processing")
    private Boolean isProcessing;

    /**
     * ID of the member who created this choice.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this choice.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this choice was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this choice was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
