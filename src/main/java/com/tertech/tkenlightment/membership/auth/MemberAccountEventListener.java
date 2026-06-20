package com.tertech.tkenlightment.membership.auth;

import com.tertech.tkenlightment.membership.member.domain.events.MemberRegisteredEvent;
import com.tertech.tkenlightment.membership.member.domain.events.MemberStatusChangedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Drives account lifecycle from member-module events (one-way: member never calls auth). */
@Component
class MemberAccountEventListener {

    private static final String ACTIVE = "ACTIVE";

    private final AccountService accountService;

    MemberAccountEventListener(AccountService accountService) {
        this.accountService = accountService;
    }

    @ApplicationModuleListener
    void onMemberRegistered(MemberRegisteredEvent event) {
        accountService.provisionForMember(
                event.memberId(), event.email(), event.fullName(), event.membershipNumber());
    }

    @ApplicationModuleListener
    void onMemberStatusChanged(MemberStatusChangedEvent event) {
        accountService.syncEnabledForStatus(event.memberId(), ACTIVE.equals(event.newStatus()));
    }
}
