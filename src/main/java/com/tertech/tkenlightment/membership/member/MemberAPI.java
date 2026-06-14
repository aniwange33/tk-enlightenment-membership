package com.tertech.tkenlightment.membership.member;

import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.*;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class MemberAPI {

    private final MemberService memberService;

    MemberAPI(MemberService memberService) {
        this.memberService = memberService;
    }

    public MemberResult registerMember(RegisterMemberCmd cmd) {
        return memberService.register(cmd);
    }

    public Page<MemberResult> listMembers(String query, MemberStatus status, Pageable pageable) {
        return memberService.search(query, status, pageable);
    }

    public MemberResult getMember(String memberId) {
        return memberService.getById(memberId);
    }

    public MemberResult changeStatus(ChangeStatusCmd cmd) {
        return memberService.changeStatus(cmd);
    }

    public MemberResult updateProfile(UpdateMemberProfileCmd cmd) {
        return memberService.updateProfile(cmd);
    }

    public List<MemberResult> findActiveMembers() {
        return memberService.findActiveMembers();
    }

    public List<MemberResult> findAllMembers() {
        return memberService.findAllMembers();
    }

    public MemberResult inactivateMember(String memberId) {
        return memberService.inactivateMember(memberId);
    }

    public MemberResult activateMember(String memberId) {
        return memberService.activateMember(memberId);
    }
}
