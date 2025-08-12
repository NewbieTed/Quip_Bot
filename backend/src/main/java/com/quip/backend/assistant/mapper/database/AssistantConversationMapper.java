package com.quip.backend.assistant.mapper.database;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quip.backend.assistant.model.database.AssistantConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssistantConversationMapper extends BaseMapper<AssistantConversation> {


    /**
     * Checks if any of the given tool names are currently interrupting any of the given conversations.
     * Returns the conflicting tool names that are both being added and currently interrupting.
     *
     * @param conversations list of conversations to check for interrupted tools
     * @param toolNames list of tool names being added to whitelist
     * @return list of tool names that are both being added and currently interrupting
     */
    List<String> getConflictingInterruptedToolNames(@Param("conversations") List<AssistantConversation> conversations, 
                                                   @Param("toolNames") List<String> toolNames);

}
