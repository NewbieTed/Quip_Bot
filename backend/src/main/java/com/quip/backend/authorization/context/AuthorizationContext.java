package com.quip.backend.authorization.context;

import com.quip.backend.authorization.model.MemberChannelAuthorization;
import com.quip.backend.channel.model.Channel;
import com.quip.backend.member.model.Member;
import com.quip.backend.server.model.Server;
;

public record AuthorizationContext(Member member, Channel channel, Server server, MemberChannelAuthorization memberChannelAuthorization) {
}